package senac.tsi.apipapas.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * Exige o header X-API-Key:
 * - em /chaves-api: somente a chave de admin;
 * - em POST/PUT/PATCH/DELETE dos recursos: chave de admin ou chave de usuário ativa.
 * Os GETs dos recursos continuam públicos.
 */
@Component
@Order(2)
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";
    /** Atributo da requisição com a identificação de quem fez a chamada ("admin" ou "chave-{id}"). */
    public static final String ATRIBUTO_CLIENTE = "apiPapas.cliente";

    public static final List<String> RECURSOS = List.of("/papas", "/conclaves", "/enciclicas", "/concilios", "/santos");
    public static final String CHAVES_API = "/chaves-api";
    private static final Set<String> METODOS_ESCRITA = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final ChaveAdmin chaveAdmin;
    private final ChaveApiService chaveApiService;

    ApiKeyFilter(ChaveAdmin chaveAdmin, ChaveApiService chaveApiService) {
        this.chaveAdmin = chaveAdmin;
        this.chaveApiService = chaveApiService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String caminho = caminho(request);
        String chave = request.getHeader(HEADER);

        if (pertence(caminho, CHAVES_API)) {
            if (chave == null || chave.isBlank()) {
                erro(response, HttpServletResponse.SC_UNAUTHORIZED, "Informe a chave de admin no header " + HEADER + ".");
                return;
            }
            if (!chaveAdmin.confere(chave)) {
                erro(response, HttpServletResponse.SC_FORBIDDEN, "Somente o administrador pode gerenciar chaves de API.");
                return;
            }
            request.setAttribute(ATRIBUTO_CLIENTE, "admin");
        } else if (METODOS_ESCRITA.contains(request.getMethod()) && ehRecurso(caminho)) {
            if (chave == null || chave.isBlank()) {
                erro(response, HttpServletResponse.SC_UNAUTHORIZED, "Informe uma chave de API válida no header " + HEADER + ".");
                return;
            }
            if (chaveAdmin.confere(chave)) {
                request.setAttribute(ATRIBUTO_CLIENTE, "admin");
            } else {
                var chaveApi = chaveApiService.validar(chave);
                if (chaveApi.isEmpty()) {
                    erro(response, HttpServletResponse.SC_UNAUTHORIZED, "Chave de API inválida ou revogada.");
                    return;
                }
                request.setAttribute(ATRIBUTO_CLIENTE, "chave-" + chaveApi.get().getId());
            }
        }
        chain.doFilter(request, response);
    }

    public static String caminho(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    public static boolean ehRecurso(String caminho) {
        return RECURSOS.stream().anyMatch(r -> pertence(caminho, r));
    }

    private static boolean pertence(String caminho, String base) {
        return caminho.equals(base) || caminho.startsWith(base + "/");
    }

    public static void erro(HttpServletResponse response, int status, String mensagem) throws IOException {
        response.setStatus(status);
        response.setContentType("text/plain;charset=UTF-8");
        response.getOutputStream().write(mensagem.getBytes(StandardCharsets.UTF_8));
    }
}
