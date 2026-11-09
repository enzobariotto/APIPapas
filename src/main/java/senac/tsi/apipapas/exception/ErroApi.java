package senac.tsi.apipapas.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Formato único de erro da API, usado tanto pelo tratamento global de exceções (ApiExceptionHandler)
 * quanto pelos filtros (chave de API, idempotência e rate limiting).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ErroApi", description = "Formato padrão das respostas de erro")
public record ErroApi(
        @Schema(description = "Momento do erro (UTC)", example = "2026-11-08T14:30:00Z")
        Instant timestamp,
        @Schema(description = "Código de status HTTP", example = "400")
        int status,
        @Schema(description = "Descrição padrão do status HTTP", example = "Bad Request")
        String erro,
        @Schema(description = "O que aconteceu e como corrigir", example = "Dados inválidos. Corrija os campos indicados.")
        String mensagem,
        @Schema(description = "Caminho da requisição", example = "/papas")
        String caminho,
        @Schema(description = "Erros por campo (somente em erros de validação)",
                example = "{\"nomePapal\": \"O nome papal é obrigatório\"}")
        Map<String, String> campos) {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    public static ErroApi de(HttpStatus status, String mensagem, HttpServletRequest request, Map<String, String> campos) {
        return new ErroApi(Instant.now().truncatedTo(ChronoUnit.MILLIS), status.value(), status.getReasonPhrase(), mensagem,
                request.getRequestURI(), campos == null || campos.isEmpty() ? null : campos);
    }

    public static ErroApi de(HttpStatus status, String mensagem, HttpServletRequest request) {
        return de(status, mensagem, request, null);
    }

    /** Escreve o erro direto na resposta; usado pelos filtros, que rodam antes dos controllers. */
    public static void escrever(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                                String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(JSON.writeValueAsBytes(de(status, mensagem, request)));
    }
}
