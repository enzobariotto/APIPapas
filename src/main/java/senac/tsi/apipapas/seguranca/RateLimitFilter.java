package senac.tsi.apipapas.seguranca;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
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

import java.io.IOException;
import java.time.Duration;
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
 * pelo endereço IP. Clientes com chave têm um balde maior. Toda resposta traz os headers
 * X-RateLimit-Limit (tamanho do balde), X-RateLimit-Remaining (fichas restantes) e X-RateLimit-Reset
 * (segundos até o balde encher de novo); o 429 traz Retry-After (segundos até a próxima ficha).
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    public static final String HEADER_LIMITE = "X-RateLimit-Limit";
    public static final String HEADER_RESTANTE = "X-RateLimit-Remaining";
    public static final String HEADER_RESET = "X-RateLimit-Reset";

    /** Caminhos fora do limite: documentação e console do banco (o Swagger UI carrega vários arquivos de uma vez). */
    private static final List<String> IGNORADOS = List.of("/swagger-ui", "/v3/api-docs", "/h2-console", "/favicon.ico", "/error");
    private static final int LIMPAR_A_CADA = 1000;

    private final ChaveAdmin chaveAdmin;
    private final ChaveApiService chaveApiService;
    private final int limiteAnonimo;
    private final int limiteComChave;
    private final Duration periodo;

    /** Um balde por cliente ("ip:...", "chave-{id}" ou "admin"). */
    private final Map<String, Balde> baldes = new ConcurrentHashMap<>();
    private final AtomicLong contadorRequisicoes = new AtomicLong();

    RateLimitFilter(ChaveAdmin chaveAdmin, ChaveApiService chaveApiService,
                    @Value("${app.rate-limit.limite-anonimo:30}") int limiteAnonimo,
                    @Value("${app.rate-limit.limite-com-chave:60}") int limiteComChave,
                    @Value("${app.rate-limit.periodo:60s}") Duration periodo) {
        this.chaveAdmin = chaveAdmin;
        this.chaveApiService = chaveApiService;
        this.limiteAnonimo = limiteAnonimo;
        this.limiteComChave = limiteComChave;
        this.periodo = periodo;
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
        String cliente = identificarCliente(request);
        int limite = cliente.startsWith("ip:") ? limiteAnonimo : limiteComChave;

        Balde balde = baldes.computeIfAbsent(cliente, c -> new Balde(novoBucket(limite), limite));
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
        chain.doFilter(request, response);
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

    /** "chave-{id}" ou "admin" quando a chave de API é válida; caso contrário, "ip:{endereço}". */
    private String identificarCliente(HttpServletRequest request) {
        String chave = request.getHeader(ApiKeyFilter.HEADER);
        if (chave != null && !chave.isBlank()) {
            if (chaveAdmin.confere(chave)) {
                return "admin";
            }
            var chaveApi = chaveApiService.identificar(chave);
            if (chaveApi.isPresent()) {
                return "chave-" + chaveApi.get().getId();
            }
        }
        // Com server.forward-headers-strategy=framework, getRemoteAddr() já considera o X-Forwarded-For do Render.
        return "ip:" + request.getRemoteAddr();
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
