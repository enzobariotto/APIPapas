package senac.tsi.apipapas.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Confere, em todos os endpoints das duas versões, se cada status da documentação tem o exemplo certo. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DocumentacaoTest {

    @LocalServerPort
    int porta;

    @ParameterizedTest
    @ValueSource(strings = {"v1", "v2"})
    void todoStatusTemExemploCoerente(String grupo) throws Exception {
        JsonNode paths = documentacao(grupo).get("paths");
        List<String> problemas = new ArrayList<>();

        for (Map.Entry<String, JsonNode> caminho : paths.properties()) {
            for (Map.Entry<String, JsonNode> metodo : caminho.getValue().properties()) {
                String endpoint = metodo.getKey().toUpperCase() + " " + caminho.getKey();
                for (Map.Entry<String, JsonNode> resposta : metodo.getValue().get("responses").properties()) {
                    String status = resposta.getKey();
                    JsonNode r = resposta.getValue();
                    String onde = endpoint + " " + status;

                    if (r.path("description").asString().isBlank() || r.path("description").asString().contains("; ou:")) {
                        problemas.add(onde + ": descrição vazia ou concatenada");
                    }
                    if (status.equals("204")) {
                        continue;
                    }
                    if (status.startsWith("2")) {
                        JsonNode exemplo = r.path("content").path("application/hal+json").path("example");
                        if (!exemplo.has("_links")) {
                            problemas.add(onde + ": sem exemplo de sucesso com _links");
                        }
                        continue;
                    }
                    JsonNode exemplos = r.path("content").path("application/json").path("examples");
                    if (exemplos.isEmpty()) {
                        problemas.add(onde + ": sem exemplos de erro");
                    }
                    for (Map.Entry<String, JsonNode> ex : exemplos.properties()) {
                        int noExemplo = ex.getValue().path("value").path("status").asInt();
                        if (noExemplo != Integer.parseInt(status)) {
                            problemas.add(onde + ": exemplo '" + ex.getKey() + "' mostra status " + noExemplo);
                        }
                    }
                }
            }
        }
        assertThat(problemas).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1", "v2"})
    void headersImportantesDocumentados(String grupo) throws Exception {
        JsonNode post = documentacao(grupo).get("paths").get("/papas").get("post").get("responses");
        assertThat(post.get("201").get("headers").has("Location")).isTrue();
        assertThat(post.get("201").get("headers").has("Idempotency-Replayed")).isTrue();
        assertThat(post.get("429").get("headers").has("Retry-After")).isTrue();
        assertThat(post.get("401").get("content").get("application/json").get("examples").size()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1", "v2"})
    void exemploDoPapaSegueAVersao(String grupo) throws Exception {
        JsonNode exemplo = documentacao(grupo).get("paths").get("/papas/{id}").get("get").get("responses")
                .get("200").get("content").get("application/hal+json").get("example");
        if (grupo.equals("v1")) {
            assertThat(exemplo.get("nomePapal").asString()).isEqualTo("Francisco");
        } else {
            assertThat(exemplo.get("nome").get("papal").asString()).isEqualTo("Francisco");
            assertThat(exemplo.get("pontificado").has("duracaoDias")).isTrue();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1", "v2"})
    void secoesNaOrdemCerta(String grupo) throws Exception {
        List<String> secoes = new ArrayList<>();
        documentacao(grupo).get("tags").forEach(tag -> secoes.add(tag.get("name").asString()));
        assertThat(secoes).containsExactly("Papas", "Conclaves", "Encíclicas", "Concílios", "Santos", "Chaves de API");
    }

    @ParameterizedTest
    @ValueSource(strings = {"v1", "v2"})
    void novasRegrasDocumentadas(String grupo) throws Exception {
        JsonNode paths = documentacao(grupo).get("paths");
        assertThat(paths.get("/papas").get("post").get("responses").get("409").get("content").get("application/json")
                .get("examples").has("numero-repetido")).isTrue();
        assertThat(paths.get("/papas/{id}").get("put").get("responses").get("409").get("content").get("application/json")
                .get("examples").has("registro-ligado")).isTrue();
        assertThat(paths.get("/enciclicas").get("post").get("responses").get("400").get("content").get("application/json")
                .get("examples").has("fora-do-pontificado")).isTrue();
        assertThat(paths.get("/enciclicas/papa/{papaId}").get("get").get("responses").has("404")).isTrue();
    }

    private JsonNode documentacao(String grupo) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/v3/api-docs/" + grupo)).build();
        String corpo = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString()).body();
        return JsonMapper.builder().build().readTree(corpo);
    }
}
