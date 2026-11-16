package senac.tsi.apipapas.seguranca;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Limites do token bucket: balde pequeno (3 sem chave, 5 com chave) e recarga lenta (1 ficha a cada 20s),
 * para que nenhuma ficha volte durante o teste. Cada teste começa com os baldes cheios (contexto novo).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.seguranca.admin-key=" + RateLimitFilterTest.ADMIN,
                "app.rate-limit.limite-anonimo=3",
                "app.rate-limit.limite-com-chave=5",
                "app.rate-limit.periodo=60s"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class RateLimitFilterTest {

    static final String ADMIN = "admin-teste";

    @LocalServerPort
    int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void semChaveGastaUmaFichaPorRequisicaoEDa429QuandoAcaba() throws Exception {
        for (int restantes = 2; restantes >= 0; restantes--) {
            HttpResponse<String> r = get(null);
            assertThat(r.statusCode()).isEqualTo(200);
            assertThat(header(r, RateLimitFilter.HEADER_LIMITE)).isEqualTo("3");
            assertThat(header(r, RateLimitFilter.HEADER_RESTANTE)).isEqualTo(String.valueOf(restantes));
        }

        HttpResponse<String> bloqueada = get(null);
        assertThat(bloqueada.statusCode()).isEqualTo(429);
        assertThat(header(bloqueada, RateLimitFilter.HEADER_RESTANTE)).isEqualTo("0");
        // Recarga de 3 fichas por 60s = 1 ficha a cada 20s
        assertThat(Long.parseLong(header(bloqueada, "Retry-After"))).isBetween(1L, 20L);
        assertThat(Long.parseLong(header(bloqueada, RateLimitFilter.HEADER_RESET))).isBetween(41L, 60L);
        assertThat(JsonMapper.builder().build().readTree(bloqueada.body()).get("status").asInt()).isEqualTo(429);
    }

    @Test
    void comChaveOLimiteEMaiorEOBaldeESeparadoDoIp() throws Exception {
        for (int i = 0; i < 3; i++) {
            get(null);
        }
        assertThat(get(null).statusCode()).isEqualTo(429); // IP sem fichas

        HttpResponse<String> comChave = get(ADMIN);
        assertThat(comChave.statusCode()).isEqualTo(200);
        assertThat(header(comChave, RateLimitFilter.HEADER_LIMITE)).isEqualTo("5");
        assertThat(header(comChave, RateLimitFilter.HEADER_RESTANTE)).isEqualTo("4");
    }

    @Test
    void cadaChaveDeUsuarioTemOSeuProprioBalde() throws Exception {
        String chaveUsuario = gerarChave();
        for (int i = 0; i < 5; i++) {
            assertThat(get(chaveUsuario).statusCode()).isEqualTo(200);
        }
        assertThat(get(chaveUsuario).statusCode()).isEqualTo(429);
        // O balde do admin (que só gastou 1 ficha para gerar a chave) continua com fichas
        assertThat(get(ADMIN).statusCode()).isEqualTo(200);
    }

    @Test
    void chaveInventadaContaComoSemChave() throws Exception {
        HttpResponse<String> r = get("papas_chave-inventada");
        assertThat(header(r, RateLimitFilter.HEADER_LIMITE)).isEqualTo("3");
        assertThat(header(r, RateLimitFilter.HEADER_RESTANTE)).isEqualTo("2");
        assertThat(header(get(null), RateLimitFilter.HEADER_RESTANTE)).isEqualTo("1"); // mesmo balde do IP
    }

    private String gerarChave() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/chaves-api"))
                .header(ApiKeyFilter.HEADER, ADMIN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"nome\": \"Teste\"}"))
                .build();
        String corpo = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
        return JsonMapper.builder().build().readTree(corpo).get("chave").asString();
    }

    private HttpResponse<String> get(String chave) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/papas?size=1"));
        if (chave != null) {
            req.header(ApiKeyFilter.HEADER, chave);
        }
        return http.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String header(HttpResponse<?> r, String nome) {
        return r.headers().firstValue(nome).orElseThrow(() -> new AssertionError("Header ausente: " + nome));
    }
}
