package senac.tsi.apipapas.seguranca;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Planos: limite por minuto e cota mensal de cada chave. Cotas pequenas (Gratuito 3, Básico 5) para esgotar
 * durante o teste; limites por minuto altos para que o rate limiting não interfira.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.seguranca.admin-key=" + PlanosTest.ADMIN,
                "app.rate-limit.limite-anonimo=1000",
                "app.rate-limit.limite-com-chave=1000",
                "app.planos.gratuito.cota-mensal=3",
                "app.planos.basico.por-minuto=2000",
                "app.planos.basico.cota-mensal=5"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PlanosTest {

    static final String ADMIN = "admin-teste";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void tabelaDePlanosEPublica() throws Exception {
        HttpResponse<String> r = enviar("GET", "/planos", null, null);
        assertThat(r.statusCode()).isEqualTo(200);
        JsonNode planos = JSON.readTree(r.body()).get("_embedded").get("planos");
        assertThat(planos.size()).isEqualTo(3);
        assertThat(planos.get(1).get("plano").asString()).isEqualTo("BASICO");
        assertThat(planos.get(1).get("precoMensal").decimalValue()).isEqualByComparingTo("19.90");
        assertThat(planos.get(1).get("requisicoesPorMinuto").asInt()).isEqualTo(2000);
        assertThat(planos.get(1).get("cotaMensal").asLong()).isEqualTo(5);
    }

    @Test
    void tabelaDePlanosNaoGastaCota() throws Exception {
        String chave = gerarChave(null);
        for (int i = 0; i < 5; i++) {
            HttpResponse<String> r = enviar("GET", "/planos", null, chave);
            assertThat(r.statusCode()).isEqualTo(200);
            assertThat(r.headers().firstValue(RateLimitFilter.HEADER_COTA)).isEmpty();
        }
        HttpResponse<String> r = enviar("GET", "/papas?size=1", null, chave);
        assertThat(header(r, RateLimitFilter.HEADER_COTA_RESTANTE)).isEqualTo("2");
    }

    @Test
    void chaveSemPlanoEGratuitaEDa429QuandoACotaAcaba() throws Exception {
        String chave = gerarChave(null);
        for (int restantes = 2; restantes >= 0; restantes--) {
            HttpResponse<String> r = enviar("GET", "/papas?size=1", null, chave);
            assertThat(r.statusCode()).isEqualTo(200);
            assertThat(header(r, RateLimitFilter.HEADER_COTA)).isEqualTo("3");
            assertThat(header(r, RateLimitFilter.HEADER_COTA_RESTANTE)).isEqualTo(String.valueOf(restantes));
        }
        HttpResponse<String> esgotada = enviar("GET", "/papas?size=1", null, chave);
        assertThat(esgotada.statusCode()).isEqualTo(429);
        assertThat(JSON.readTree(esgotada.body()).get("mensagem").asString()).contains("Cota mensal de 3 requisições do plano Gratuito");
        assertThat(Long.parseLong(header(esgotada, "Retry-After"))).isPositive();
    }

    @Test
    void trocarDePlanoAumentaACotaEOLimite() throws Exception {
        String chave = gerarChave(null);
        for (int i = 0; i < 3; i++) {
            enviar("GET", "/papas?size=1", null, chave);
        }
        assertThat(enviar("GET", "/papas?size=1", null, chave).statusCode()).isEqualTo(429);

        HttpResponse<String> alterada = enviar("PUT", "/chaves-api/1/plano", "{\"plano\": \"BASICO\"}", ADMIN);
        assertThat(alterada.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(alterada.body()).get("plano").asString()).isEqualTo("BASICO");

        HttpResponse<String> r = enviar("GET", "/papas?size=1", null, chave);
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(header(r, RateLimitFilter.HEADER_LIMITE)).isEqualTo("2000");
        assertThat(header(r, RateLimitFilter.HEADER_COTA)).isEqualTo("5");
        assertThat(header(r, RateLimitFilter.HEADER_COTA_RESTANTE)).isEqualTo("1");
    }

    @Test
    void chaveGeradaComPlano() throws Exception {
        HttpResponse<String> r = enviar("POST", "/chaves-api", "{\"nome\": \"Cliente\", \"plano\": \"PRO\"}", ADMIN);
        assertThat(r.statusCode()).isEqualTo(201);
        assertThat(JSON.readTree(r.body()).get("plano").asString()).isEqualTo("PRO");
    }

    @Test
    void planoInvalidoOuAusenteDa400() throws Exception {
        assertThat(enviar("PUT", "/chaves-api/1/plano", "{\"plano\": \"XYZ\"}", ADMIN).statusCode()).isEqualTo(400);
        gerarChave(null);
        assertThat(enviar("PUT", "/chaves-api/1/plano", "{}", ADMIN).statusCode()).isEqualTo(400);
        assertThat(enviar("PUT", "/chaves-api/99/plano", "{\"plano\": \"PRO\"}", ADMIN).statusCode()).isEqualTo(404);
    }

    @Test
    void headersDeCotaLiberadosParaONavegador() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/papas?size=1"))
                .header("Origin", "http://localhost:5173")
                .header(ApiKeyFilter.HEADER, gerarChave(null))
                .build();
        String expostos = header(http.send(req, HttpResponse.BodyHandlers.ofString()), "Access-Control-Expose-Headers");
        assertThat(expostos).contains(RateLimitFilter.HEADER_COTA, RateLimitFilter.HEADER_COTA_RESTANTE,
                RateLimitFilter.HEADER_COTA_RENOVA);
    }

    @Test
    void adminNaoTemCotaMensal() throws Exception {
        for (int i = 0; i < 5; i++) {
            HttpResponse<String> r = enviar("GET", "/papas?size=1", null, ADMIN);
            assertThat(r.statusCode()).isEqualTo(200);
            assertThat(r.headers().firstValue(RateLimitFilter.HEADER_COTA)).isEmpty();
        }
    }

    private String gerarChave(String plano) throws Exception {
        String corpo = plano == null ? "{\"nome\": \"Teste\"}" : "{\"nome\": \"Teste\", \"plano\": \"" + plano + "\"}";
        return JSON.readTree(enviar("POST", "/chaves-api", corpo, ADMIN).body()).get("chave").asString();
    }

    private HttpResponse<String> enviar(String metodo, String caminho, String corpo, String chave) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                .method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corpo));
        if (corpo != null) {
            req.header("Content-Type", "application/json");
        }
        if (chave != null) {
            req.header(ApiKeyFilter.HEADER, chave);
        }
        return http.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String header(HttpResponse<?> r, String nome) {
        return r.headers().firstValue(nome).orElseThrow(() -> new AssertionError("Header ausente: " + nome));
    }
}
