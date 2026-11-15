package senac.tsi.apipapas.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Sem H2_CONSOLE=true o console do H2 não pode existir (ele dá acesso direto ao banco). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class H2ConsoleDesligadoTest {

    @LocalServerPort
    int porta;

    @Test
    void consoleDesligadoPorPadrao() throws Exception {
        assertThat(statusDoConsole(porta)).isEqualTo(404);
    }

    static int statusDoConsole(int porta) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/h2-console/")).GET().build();
        return HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
