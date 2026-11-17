package senac.tsi.apipapas.seguranca;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import senac.tsi.apipapas.config.Documentacao;
import senac.tsi.apipapas.model.ChaveApi;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Limita a quantidade de requisições por cliente com o algoritmo token bucket (biblioteca Bucket4j).
 *
 * Cada cliente tem um "balde" de fichas: cada requisição gasta uma ficha e, sem fichas, a API responde
 * 429 Too Many Requests. As fichas voltam aos poucos (recarga gradual): com o limite de 30 por minuto,
 * volta uma ficha a cada 2 segundos. Assim não há "virada de janela" em que o limite zera de uma vez.
 *
 * O cliente é identificado pela chave de API (quando o header X-API-Key traz uma chave válida) ou, sem chave,
 * pelo endereço IP. Com chave, o tamanho do balde depende do plano da chave (Gratuito, Básico ou Pro, ver Planos).
 *
 * Além do limite por minuto, cada chave de usuário tem uma cota mensal (total de requisições no mês, conforme o
 * plano). Quando a cota acaba, a API responde 429 até o dia 1º do mês seguinte. As respostas com chave trazem os
 * headers X-Quota-Limit (cota do mês), X-Quota-Remaining (requisições restantes no mês) e X-Quota-Reset (data em
 * que a cota renova). As consultas de /planos não contam na cota, para que o cliente sempre consiga ver o seu uso.
 *
 * O IP nunca vem do header X-Forwarded-For, porque o cliente pode escrever nele o que quiser (e o proxy do Render
 * só acrescenta o IP verdadeiro no fim da lista): trocando o valor a cada requisição, ele ganharia um balde novo
 * a cada vez. No Render, o IP vem do header CF-Connecting-IP, que o Cloudflare (na frente do Render) sempre
 * sobrescreve com o IP real; fora dele, vem da própria conexão. Toda resposta traz os headers
 * X-RateLimit-Limit (tamanho do balde), X-RateLimit-Remaining (fichas restantes) e X-RateLimit-Reset
 * (segundos até o balde encher de novo); o 429 traz Retry-After (segundos até a próxima ficha).
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    public static final String HEADER_LIMITE = "X-RateLimit-Limit";
    public static final String HEADER_RESTANTE = "X-RateLimit-Remaining";
    public static final String HEADER_RESET = "X-RateLimit-Reset";
    public static final String HEADER_COTA = "X-Quota-Limit";
    public static final String HEADER_COTA_RESTANTE = "X-Quota-Remaining";
    public static final String HEADER_COTA_RENOVA = "X-Quota-Reset";
    /** Header que o Cloudflare preenche com o IP real do cliente (o cliente não consegue alterá-lo). */
    static final String HEADER_IP_CLOUDFLARE = "CF-Connecting-IP";

    /** Caminhos fora do limite: documentação e console do banco (o Swagger UI carrega vários arquivos de uma vez). */
    private static final List<String> IGNORADOS = List.of("/swagger-ui", "/v3/api-docs", "/h2-console", "/favicon.ico", "/error");
    private static final int LIMPAR_A_CADA = 1000;

    private final ChaveAdmin chaveAdmin;
    private final ChaveApiService chaveApiService;
    private final Planos planos;
    private final int limiteAnonimo;
    private final int limiteComChave;
    private final Duration periodo;
    /** Header com o IP real do cliente, preenchido pelo proxy (vazio: usa o IP da conexão). */
    private final String headerIp;

    /** Um balde por cliente ("ip:...", "chave-{id}" ou "admin"). */
    private final Map<String, Balde> baldes = new ConcurrentHashMap<>();
    private final AtomicLong contadorRequisicoes = new AtomicLong();

    RateLimitFilter(ChaveAdmin chaveAdmin, ChaveApiService chaveApiService, Planos planos,
                    @Value("${app.rate-limit.limite-anonimo:30}") int limiteAnonimo,
                    @Value("${app.rate-limit.limite-com-chave:60}") int limiteComChave,
                    @Value("${app.rate-limit.periodo:60s}") Duration periodo,
                    @Value("${app.rate-limit.header-ip:}") String headerIp) {
        this.chaveAdmin = chaveAdmin;
        this.chaveApiService = chaveApiService;
        this.planos = planos;
        this.limiteAnonimo = limiteAnonimo;
        this.limiteComChave = limiteComChave;
        this.periodo = periodo;
        this.headerIp = headerIp.isBlank() && "true".equals(System.getenv("RENDER")) ? HEADER_IP_CLOUDFLARE : headerIp.strip();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = ApiKeyFilter.caminho(request);
        return CorsUtils.isPreFlightRequest(request)
                || IGNORADOS.stream().anyMatch(i -> caminho.equals(i) || caminho.startsWith(i + "/") || caminho.startsWith(i + "."));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Cliente cliente = identificarCliente(request);
        int limite = cliente.porMinuto();

        // O limite faz parte do nome do balde: ao trocar o plano da chave, ela passa a usar um balde do tamanho novo.
        Balde balde = baldes.computeIfAbsent(cliente.id() + "/" + limite, c -> new Balde(novoBucket(limite), limite));
        ConsumptionProbe consumo = balde.bucket().tryConsumeAndReturnRemaining(1);
        limparBaldesCheios();

        response.setHeader(HEADER_LIMITE, String.valueOf(limite));
        response.setHeader(HEADER_RESTANTE, String.valueOf(consumo.getRemainingTokens()));
        response.setHeader(HEADER_RESET, String.valueOf(segundos(consumo.getNanosToWaitForReset())));

        if (!consumo.isConsumed()) {
            long espera = Math.max(1, segundos(consumo.getNanosToWaitForRefill()));
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(espera));
            ApiKeyFilter.erro(request, response, 429, "Limite de " + limite + " requisições por "
                    + descreverPeriodo() + " excedido. Tente novamente em " + espera
                    + (espera == 1 ? " segundo" : " segundos") + ". Limites de uso: " + Documentacao.TERMOS_LIMITES);
            return;
        }
        if (cliente.chaveId() != null && !foraDaCota(request)) {
            ChaveApiService.Cota cota = chaveApiService.consumirCota(cliente.chaveId());
            response.setHeader(HEADER_COTA, String.valueOf(cota.cotaMensal()));
            response.setHeader(HEADER_COTA_RESTANTE, String.valueOf(cota.restantes()));
            response.setHeader(HEADER_COTA_RENOVA, Planos.renovacao().toString());
            if (!cota.liberada()) {
                long espera = Math.max(1, Duration.between(ZonedDateTime.now(Planos.FUSO),
                        Planos.renovacao().atStartOfDay(Planos.FUSO)).toSeconds());
                response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(espera));
                ApiKeyFilter.erro(request, response, 429, mensagemCotaEsgotada(cota));
                return;
            }
        }
        chain.doFilter(request, response);
    }

    public static String mensagemCotaEsgotada(ChaveApiService.Cota cota) {
        return "Cota mensal de " + cota.cotaMensal() + " requisições do plano " + cota.plano().getNome()
                + " esgotada. A cota renova em " + Planos.renovacao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + ". Para aumentar a cota, veja os planos em GET /planos e " + Documentacao.TERMOS_PLANOS;
    }

    /** As consultas de /planos (tabela de preços e uso da chave) não gastam a cota mensal. */
    private static boolean foraDaCota(HttpServletRequest request) {
        String caminho = ApiKeyFilter.caminho(request);
        return caminho.equals(ApiKeyFilter.PLANOS) || caminho.startsWith(ApiKeyFilter.PLANOS + "/");
    }

    /**
     * Balde com capacidade para "limite" fichas e recarga gradual (refillGreedy): as "limite" fichas do período
     * voltam distribuídas ao longo dele, uma de cada vez, em vez de todas juntas no fim.
     */
    private Bucket novoBucket(int limite) {
        return Bucket.builder()
                .addLimit(regra -> regra.capacity(limite).refillGreedy(limite, periodo))
                .build();
    }

    /**
     * "chave-{id}" (limite do plano da chave) ou "admin" (limite com chave, sem cota mensal) quando a chave de API
     * é válida; caso contrário, "ip:{endereço}" (limite sem chave).
     */
    private Cliente identificarCliente(HttpServletRequest request) {
        String chave = request.getHeader(ApiKeyFilter.HEADER);
        if (chave != null && !chave.isBlank()) {
            if (chaveAdmin.confere(chave)) {
                return new Cliente("admin", limiteComChave, null);
            }
            var chaveApi = chaveApiService.identificar(chave);
            if (chaveApi.isPresent()) {
                ChaveApi c = chaveApi.get();
                return new Cliente("chave-" + c.getId(), planos.de(c.getPlano()).porMinuto(), c.getId());
            }
        }
        return new Cliente("ip:" + ipDoCliente(request), limiteAnonimo, null);
    }

    private record Cliente(String id, int porMinuto, Long chaveId) {
    }

    private String ipDoCliente(HttpServletRequest request) {
        if (!headerIp.isEmpty()) {
            String ip = request.getHeader(headerIp);
            if (ip != null && !ip.isBlank()) {
                return ip.split(",")[0].strip();
            }
        }
        // Com server.forward-headers-strategy=framework, request.getRemoteAddr() traria o primeiro IP do
        // X-Forwarded-For, que o cliente controla. A requisição original (sem o filtro) tem o IP da conexão.
        ServletRequest original = request;
        while (original instanceof ServletRequestWrapper embrulho) {
            original = embrulho.getRequest();
        }
        return original.getRemoteAddr();
    }

    /** Um balde cheio é igual a um balde novo; remover os cheios evita guardar um balde para cada IP que já passou. */
    private void limparBaldesCheios() {
        if (contadorRequisicoes.incrementAndGet() % LIMPAR_A_CADA == 0) {
            baldes.values().removeIf(b -> b.bucket().getAvailableTokens() >= b.capacidade());
        }
    }

    private String descreverPeriodo() {
        long s = periodo.toSeconds();
        return s == 60 ? "minuto" : s + " segundos";
    }

    private static long segundos(long nanos) {
        return (nanos + TimeUnit.SECONDS.toNanos(1) - 1) / TimeUnit.SECONDS.toNanos(1);
    }

    private record Balde(Bucket bucket, int capacidade) {
    }
}
