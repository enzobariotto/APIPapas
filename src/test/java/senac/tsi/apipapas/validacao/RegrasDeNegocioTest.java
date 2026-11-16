package senac.tsi.apipapas.validacao;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regras de coerência histórica e de leitura do JSON, testadas pela API de verdade (com os dados iniciais:
 * 6 = Francisco, 2013-03-13 a 2025-04-21; 3 = Paulo VI, a partir de 1963-06-21, sem conclave cadastrado).
 * Cada teste usa dados próprios, para não depender da ordem de execução.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.seguranca.admin-key=" + RegrasDeNegocioTest.ADMIN,
                "app.rate-limit.limite-com-chave=10000"
        })
class RegrasDeNegocioTest {

    static final String ADMIN = "admin-teste";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    private static String papa(String nome, String numero, String inicio, String fim) {
        return """
                {"nomePapal": "%s", "nomeBatismo": "Fulano", "numeroOrdem": %s, "paisNascimento": "Itália",
                 "inicioPontificado": "%s", "fimPontificado": "%s", "situacao": "FALECIMENTO"}
                """.formatted(nome, numero, inicio, fim);
    }

    // ------------------------------------------------------------ papas

    @Test
    void exemploDoSwaggerPioXIIECadastrado() throws Exception {
        assertThat(enviar("POST", "/papas", papa("Pio XII", "260", "1939-03-02", "1958-10-09")).statusCode()).isEqualTo(201);
    }

    @Test
    void numeroDeOrdemRepetidoDa409() throws Exception {
        HttpResponse<String> r = enviar("POST", "/papas", papa("Outro", "266", "1700-01-01", "1700-12-31"));
        assertThat(r.statusCode()).isEqualTo(409);
        assertThat(campo(r, "numeroOrdem")).contains("Francisco");
    }

    @Test
    void doisPapasAoMesmoTempoDa409() throws Exception {
        HttpResponse<String> r = enviar("POST", "/papas", papa("Outro", "500", "2020-01-01", "2021-01-01"));
        assertThat(r.statusCode()).isEqualTo(409);
        assertThat(campo(r, "inicioPontificado")).contains("Francisco");

        // Em exercício também conta (vai até hoje): coincide com Leão XIV
        String emExercicio = """
                {"nomePapal": "Outro", "nomeBatismo": "Fulano", "numeroOrdem": 501, "paisNascimento": "Itália",
                 "inicioPontificado": "2026-01-01", "situacao": "EM_EXERCICIO"}
                """;
        assertThat(enviar("POST", "/papas", emExercicio).statusCode()).isEqualTo(409);
    }

    @Test
    void alterarPapaDeixandoEnciclicaForaDoPontificadoDa409() throws Exception {
        HttpResponse<String> r = enviar("PUT", "/papas/6", """
                {"nomePapal": "Francisco", "nomeBatismo": "Jorge Mario Bergoglio", "numeroOrdem": 266,
                 "paisNascimento": "Argentina", "inicioPontificado": "2013-03-13", "fimPontificado": "2014-12-31",
                 "situacao": "FALECIMENTO"}
                """);
        assertThat(r.statusCode()).isEqualTo(409);
        assertThat(campo(r, "inicioPontificado")).contains("Laudato Si'");
    }

    @Test
    void alterarPapaSemMudarAsDatasContinuaFuncionando() throws Exception {
        // O próprio papa não conta como "outro papa" no número de ordem nem no período
        HttpResponse<String> r = enviar("PUT", "/papas/5", """
                {"nomePapal": "Bento XVI", "nomeBatismo": "Joseph Aloisius Ratzinger", "numeroOrdem": 265,
                 "paisNascimento": "Alemanha", "inicioPontificado": "2005-04-19", "fimPontificado": "2013-02-28",
                 "situacao": "RENUNCIA"}
                """);
        assertThat(r.statusCode()).isEqualTo(200);
    }

    @Test
    void numeroDecimalEmCampoInteiroDa400() throws Exception {
        HttpResponse<String> r = enviar("POST", "/papas", papa("Outro", "1.5", "1600-01-01", "1600-12-31"));
        assertThat(r.statusCode()).isEqualTo(400);
        assertThat(campo(r, "numeroOrdem")).contains("número inteiro");
    }

    @Test
    void espacosNasPontasSaoRemovidos() throws Exception {
        HttpResponse<String> r = enviar("POST", "/papas", papa("   Leão XIII  ", "257", "1878-02-20", "1903-07-20"));
        assertThat(r.statusCode()).isEqualTo(201);
        assertThat(JSON.readTree(r.body()).get("nomePapal").asString()).isEqualTo("Leão XIII");
    }

    // ------------------------------------------------------------ registros ligados a um papa

    @Test
    void enciclicaForaDoPontificadoDa400() throws Exception {
        HttpResponse<String> r = enviar("POST", "/enciclicas",
                "{\"titulo\": \"T\", \"dataPublicacao\": \"1990-01-01\", \"papa\": {\"id\": 6}}");
        assertThat(r.statusCode()).isEqualTo(400);
        assertThat(campo(r, "dataPublicacao")).contains("Francisco");
    }

    @Test
    void canonizacaoForaDoPontificadoDa400() throws Exception {
        HttpResponse<String> r = enviar("POST", "/santos",
                "{\"nome\": \"S\", \"paisOrigem\": \"Brasil\", \"dataCanonizacao\": \"1990-01-01\", \"canonizadoPor\": {\"id\": 6}}");
        assertThat(r.statusCode()).isEqualTo(400);
        assertThat(campo(r, "dataCanonizacao")).contains("Francisco");
    }

    @Test
    void conclavePrecisaTerminarNoInicioDoPontificado() throws Exception {
        String conclave = "{\"dataInicio\": \"%s\", \"dataFim\": \"%s\", \"numeroCardeais\": 80, \"numeroEscrutinios\": 6, \"papa\": {\"id\": 3}}";
        HttpResponse<String> errado = enviar("POST", "/conclaves", conclave.formatted("2013-03-12", "2013-03-13"));
        assertThat(errado.statusCode()).isEqualTo(400);
        assertThat(campo(errado, "dataFim")).contains("Paulo VI");

        // O exemplo do Swagger (conclave real de 1963) é aceito
        assertThat(enviar("POST", "/conclaves", conclave.formatted("1963-06-19", "1963-06-21")).statusCode()).isEqualTo(201);
    }

    @Test
    void concilioComPapaDeOutraEpocaDa400() throws Exception {
        HttpResponse<String> r = enviar("POST", "/concilios", """
                {"nome": "Concílio Teste", "local": "Roma", "dataInicio": "1962-10-11", "dataFim": "1965-12-08",
                 "papas": [{"id": 2}, {"id": 6}]}
                """);
        assertThat(r.statusCode()).isEqualTo(400);
        assertThat(campo(r, "papas[1]")).contains("Francisco");
    }

    @Test
    void enciclicasDePapaInexistenteDa404() throws Exception {
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(base() + "/enciclicas/papa/9999")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(r.statusCode()).isEqualTo(404);
    }

    // ------------------------------------------------------------ idempotência

    @Test
    void mesmoJsonEscritoDeOutroJeitoContaComoRepeticao() throws Exception {
        String chave = UUID.randomUUID().toString();
        HttpResponse<String> primeira = enviar("POST", "/papas", papa("Bento XV", "258", "1914-09-03", "1922-01-22"), chave);
        String reescrito = """
                {
                  "situacao" : "FALECIMENTO",   "fimPontificado": "1922-01-22", "inicioPontificado": "1914-09-03",
                  "paisNascimento": "Itália", "numeroOrdem": 258, "nomeBatismo": "Fulano", "nomePapal": "Bento XV"
                }""";
        HttpResponse<String> repetida = enviar("POST", "/papas", reescrito, chave);

        assertThat(primeira.statusCode()).isEqualTo(201);
        assertThat(repetida.statusCode()).isEqualTo(201);
        assertThat(repetida.headers().firstValue("Idempotency-Replayed")).hasValue("true");
        assertThat(JSON.readTree(repetida.body()).get("id")).isEqualTo(JSON.readTree(primeira.body()).get("id"));
    }

    // ------------------------------------------------------------ auxiliares

    private HttpResponse<String> enviar(String metodo, String caminho, String corpo) throws Exception {
        return enviar(metodo, caminho, corpo, UUID.randomUUID().toString());
    }

    private HttpResponse<String> enviar(String metodo, String caminho, String corpo, String chaveIdempotencia) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(base() + caminho))
                .header("X-API-Key", ADMIN)
                .header("Content-Type", "application/json")
                .method(metodo, HttpRequest.BodyPublishers.ofString(corpo));
        if (metodo.equals("POST")) {
            req.header("X-Idempotency-Key", chaveIdempotencia);
        }
        return http.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String campo(HttpResponse<String> r, String nome) throws Exception {
        JsonNode campos = JSON.readTree(r.body()).path("campos");
        assertThat(campos.has(nome)).as("campos de %s", r.body()).isTrue();
        return campos.get(nome).asString();
    }

    private String base() {
        return "http://localhost:" + porta;
    }
}
