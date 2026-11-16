package senac.tsi.apipapas.seguranca;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Sem header de IP configurado (fora do Render): um X-Forwarded-For inventado não dá um balde novo. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.rate-limit.limite-anonimo=5", "app.rate-limit.header-ip="})
class RateLimitIpTest {

    @LocalServerPort
    int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void xForwardedForInventadoNaoBurlaOLimite() throws Exception {
        for (int i = 1; i <= 5; i++) {
            HttpResponse<Void> r = get("X-Forwarded-For", "203.0.113." + i);
            assertThat(r.statusCode()).isEqualTo(200);
            assertThat(r.headers().firstValue(RateLimitFilter.HEADER_RESTANTE)).hasValue(String.valueOf(5 - i));
        }
        assertThat(get("X-Forwarded-For", "198.51.100.1").statusCode()).isEqualTo(429);
    }

    private HttpResponse<Void> get(String header, String valor) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/papas?size=1"))
                .header(header, valor).build();
        return http.send(req, HttpResponse.BodyHandlers.discarding());
    }
}
