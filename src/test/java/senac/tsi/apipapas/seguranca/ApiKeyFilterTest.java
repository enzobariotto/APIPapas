package senac.tsi.apipapas.seguranca;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa o ApiKeyFilter com o Tomcat de verdade (porta aleatória), e não com MockMvc:
 * o que importa aqui é como o servidor interpreta a URL crua (codificação %xx, ";", "//", "/./"),
 * e o MockMvc não reproduz esse comportamento.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.seguranca.admin-key=" + ApiKeyFilterTest.CHAVE_ADMIN,
                // Evita que o rate limiting interfira nos testes
                "app.rate-limit.limite-anonimo=10000",
                "app.rate-limit.limite-com-chave=10000"
        })
class ApiKeyFilterTest {

    static final String CHAVE_ADMIN = "admin-teste";

    private static final String PAPA_VALIDO = """
            {"nomePapal": "Teste", "nomeBatismo": "Fulano de Tal", "numeroOrdem": 999,
             "paisNascimento": "Brasil", "inicioPontificado": "1900-01-01", "fimPontificado": "1900-12-31",
             "situacao": "FALECIMENTO"}
            """;

    @LocalServerPort
    int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void getDosRecursosEhPublico() throws Exception {
        assertThat(enviar("GET", "/papas", null, null).statusCode()).isEqualTo(200);
    }

    @Test
    void postSemChaveRetorna401() throws Exception {
        assertThat(enviar("POST", "/papas", PAPA_VALIDO, null).statusCode()).isEqualTo(401);
    }

    @Test
    void postComChaveInvalidaRetorna401() throws Exception {
        assertThat(enviar("POST", "/papas", PAPA_VALIDO, "papas_chave-que-nao-existe").statusCode()).isEqualTo(401);
    }

    @Test
    void postComChaveDeAdminCriaOPapa() throws Exception {
        assertThat(enviar("POST", "/papas", PAPA_VALIDO, CHAVE_ADMIN).statusCode()).isEqualTo(201);
    }

    @Test
    void chavesApiExigemChaveDeAdmin() throws Exception {
        assertThat(enviar("GET", "/chaves-api", null, null).statusCode()).isEqualTo(401);
        assertThat(enviar("GET", "/chaves-api", null, "papas_chave-qualquer").statusCode()).isEqualTo(403);
        assertThat(enviar("GET", "/chaves-api", null, CHAVE_ADMIN).statusCode()).isEqualTo(200);
    }

    /**
     * Variações da URL que o Spring encaminha para o mesmo controller. Nenhuma delas pode
     * passar sem chave: ou o filtro bloqueia (401) ou o servidor recusa a URL (400/404).
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "/%70apas",           // "p" codificado
            "/papas;x=1",         // parâmetro de matriz
            "/papas;jsessionid=1",
            "//papas",            // barra duplicada
            "/./papas",           // segmento "."
            "/foo/../papas",      // segmento ".."
            "/papas/",            // barra no final
    })
    void urlAlteradaNaoBurlaAChave(String caminho) throws Exception {
        HttpResponse<String> resposta = enviar("POST", caminho, PAPA_VALIDO, null);
        assertThat(resposta.statusCode())
                .as("POST %s sem chave respondeu %d: %s", caminho, resposta.statusCode(), resposta.body())
                .isIn(400, 401, 404);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/%63haves-api", "/chaves-api;x=1", "//chaves-api"})
    void urlAlteradaNaoBurlaAChaveDeAdmin(String caminho) throws Exception {
        HttpResponse<String> resposta = enviar("GET", caminho, null, null);
        assertThat(resposta.statusCode())
                .as("GET %s sem chave respondeu %d: %s", caminho, resposta.statusCode(), resposta.body())
                .isIn(400, 401, 404);
    }

    private HttpResponse<String> enviar(String metodo, String caminho, String corpo, String chave)
            throws IOException, InterruptedException {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corpo));
        if (corpo != null) {
            req.header("Content-Type", "application/json");
        }
        if (chave != null) {
            req.header(ApiKeyFilter.HEADER, chave);
        }
        return http.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }
}
