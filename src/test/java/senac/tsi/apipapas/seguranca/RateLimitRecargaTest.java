package senac.tsi.apipapas.seguranca;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Recarga gradual: com 2 fichas por 2 segundos, volta 1 ficha por segundo. Depois de esvaziar o balde e
 * esperar pouco mais de 1 segundo, só UMA requisição é liberada (e não o limite inteiro, como numa janela fixa).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.rate-limit.limite-anonimo=2",
                "app.rate-limit.periodo=2s"
        })
class RateLimitRecargaTest {

    @LocalServerPort
    int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void fichasVoltamUmaDeCadaVez() throws Exception {
        assertThat(status()).isEqualTo(200);
        assertThat(status()).isEqualTo(200);
        assertThat(status()).isEqualTo(429);

        Thread.sleep(1_200); // tempo de 1 ficha voltar

        assertThat(status()).isEqualTo(200); // a ficha que voltou
        assertThat(status()).isEqualTo(429); // a segunda ainda não voltou
    }

    private int status() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/papas?size=1")).build();
        return http.send(req, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
