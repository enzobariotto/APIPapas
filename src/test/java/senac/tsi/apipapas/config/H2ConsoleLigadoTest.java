package senac.tsi.apipapas.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/** Com H2_CONSOLE=true (uso local) o console continua funcionando. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "H2_CONSOLE=true")
class H2ConsoleLigadoTest {

    @LocalServerPort
    int porta;

    @Test
    void consoleDisponivelQuandoLigado() throws Exception {
        assertThat(H2ConsoleDesligadoTest.statusDoConsole(porta)).isEqualTo(200);
    }
}
