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
 * Como no Render: o IP vem do CF-Connecting-IP (que o Cloudflare preenche com o IP real), e o X-Forwarded-For
 * é ignorado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.rate-limit.limite-anonimo=2", "app.rate-limit.header-ip=CF-Connecting-IP"})
class RateLimitCloudflareTest {

    @LocalServerPort
    int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void cadaIpRealTemOSeuBaldeEOXForwardedForNaoMuda() throws Exception {
        assertThat(get("200.1.1.1", "10.0.0.1").statusCode()).isEqualTo(200);
        assertThat(get("200.1.1.1", "10.0.0.2").statusCode()).isEqualTo(200);
        assertThat(get("200.1.1.1", "10.0.0.3").statusCode()).isEqualTo(429); // mesmo IP real, X-Forwarded-For trocado

        HttpResponse<Void> outroCliente = get("200.2.2.2", "10.0.0.1");
        assertThat(outroCliente.statusCode()).isEqualTo(200);
        assertThat(outroCliente.headers().firstValue(RateLimitFilter.HEADER_RESTANTE)).hasValue("1");
    }

    private HttpResponse<Void> get(String ipReal, String xForwardedFor) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/papas?size=1"))
                .header("CF-Connecting-IP", ipReal)
                .header("X-Forwarded-For", xForwardedFor)
                .build();
        return http.send(req, HttpResponse.BodyHandlers.discarding());
    }
}
