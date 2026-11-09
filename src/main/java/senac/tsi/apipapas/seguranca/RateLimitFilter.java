package senac.tsi.apipapas.seguranca;

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

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Limita a quantidade de requisições por cliente em uma janela de tempo fixa.
 *
 * O cliente é identificado pela chave de API (quando o header X-API-Key traz uma chave válida) ou, sem chave,
 * pelo endereço IP. Clientes com chave têm um limite maior. Toda resposta traz os headers
 * X-RateLimit-Limit, X-RateLimit-Remaining e X-RateLimit-Reset; ao estourar o limite a API responde
 * 429 Too Many Requests com o header Retry-After (segundos até a janela reiniciar).
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
    private final long janelaMillis;

    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();
    private final AtomicLong contadorRequisicoes = new AtomicLong();

    RateLimitFilter(ChaveAdmin chaveAdmin, ChaveApiService chaveApiService,
                    @Value("${app.rate-limit.limite-anonimo:60}") int limiteAnonimo,
                    @Value("${app.rate-limit.limite-com-chave:120}") int limiteComChave,
                    @Value("${app.rate-limit.janela:60s}") Duration janela) {
        this.chaveAdmin = chaveAdmin;
        this.chaveApiService = chaveApiService;
        this.limiteAnonimo = limiteAnonimo;
        this.limiteComChave = limiteComChave;
        this.janelaMillis = janela.toMillis();
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
        long agora = System.currentTimeMillis();

        long[] estado = new long[2]; // [quantidade na janela, início da janela]
        janelas.compute(cliente, (k, janela) -> {
            if (janela == null || agora - janela.inicio >= janelaMillis) {
                janela = new Janela(agora);
            }
            janela.quantidade++;
            estado[0] = janela.quantidade;
            estado[1] = janela.inicio;
            return janela;
        });
        limparJanelasExpiradas(agora);

        long segundosParaReset = Math.max(1, (estado[1] + janelaMillis - agora + 999) / 1000);
        response.setHeader(HEADER_LIMITE, String.valueOf(limite));
        response.setHeader(HEADER_RESTANTE, String.valueOf(Math.max(0, limite - estado[0])));
        response.setHeader(HEADER_RESET, String.valueOf(segundosParaReset));

        if (estado[0] > limite) {
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(segundosParaReset));
            ApiKeyFilter.erro(request, response, 429, "Limite de " + limite + " requisições por "
                    + (janelaMillis / 1000) + " segundos excedido. Tente novamente em " + segundosParaReset + " segundos.");
            return;
        }
        chain.doFilter(request, response);
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

    private void limparJanelasExpiradas(long agora) {
        if (contadorRequisicoes.incrementAndGet() % LIMPAR_A_CADA == 0) {
            janelas.values().removeIf(j -> agora - j.inicio >= janelaMillis);
        }
    }

    private static final class Janela {
        final long inicio;
        long quantidade;

        Janela(long inicio) {
            this.inicio = inicio;
        }
    }
}
