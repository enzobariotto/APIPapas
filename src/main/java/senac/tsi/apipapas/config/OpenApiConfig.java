package senac.tsi.apipapas.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import senac.tsi.apipapas.controller.ChaveApiController;
import senac.tsi.apipapas.idempotencia.IdempotenciaFilter;
import senac.tsi.apipapas.seguranca.ApiKeyFilter;

/**
 * Documenta no Swagger a autenticação por chave de API e a idempotência,
 * que são aplicadas pelos filtros ApiKeyFilter e IdempotenciaFilter.
 */
@Configuration
@SecurityScheme(name = "ApiKey", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER,
        paramName = ApiKeyFilter.HEADER,
        description = "Chave de API gerada em /chaves-api (ou a chave de admin). Exigida em POST, PUT e DELETE.")
class OpenApiConfig {

    @Bean
    OperationCustomizer autenticacaoEIdempotencia() {
        return (operation, handlerMethod) -> {
            if (handlerMethod.getBeanType().equals(ChaveApiController.class)) {
                return operation;
            }
            boolean post = handlerMethod.hasMethodAnnotation(PostMapping.class);
            boolean escrita = post || handlerMethod.hasMethodAnnotation(PutMapping.class)
                    || handlerMethod.hasMethodAnnotation(DeleteMapping.class);
            if (!escrita) {
                return operation;
            }

            operation.addSecurityItem(new SecurityRequirement().addList("ApiKey"));
            adicionar(operation, "401", "Header X-API-Key ausente, ou chave inválida ou revogada");

            if (post) {
                operation.addParametersItem(new HeaderParameter()
                        .name(IdempotenciaFilter.HEADER)
                        .required(true)
                        .description("Valor único por operação (ex.: um UUID). Repetir a mesma chave com o mesmo corpo "
                                + "devolve a resposta original sem cadastrar de novo.")
                        .schema(new StringSchema().maxLength(100).example("3f2b8c1e-9d4a-4c6b-8e2f-1a7d5c9b0e34")));
                adicionar(operation, "201", "Requisição repetida: devolve a resposta original, com o header "
                        + IdempotenciaFilter.HEADER_REPETIDA + ": true");
                adicionar(operation, "400", "Header " + IdempotenciaFilter.HEADER + " ausente");
                adicionar(operation, "409", "Já existe uma requisição com esta chave de idempotência em processamento");
                adicionar(operation, "422", "A chave de idempotência já foi usada com uma requisição diferente");
            }
            return operation;
        };
    }

    /** Adiciona a resposta ou, se o status já estiver documentado, complementa a descrição existente. */
    private static void adicionar(Operation operation, String status, String descricao) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        ApiResponse existente = operation.getResponses().get(status);
        if (existente == null) {
            operation.getResponses().addApiResponse(status, new ApiResponse().description(descricao));
        } else {
            existente.setDescription(existente.getDescription() + "; ou: " + descricao);
        }
    }
}
