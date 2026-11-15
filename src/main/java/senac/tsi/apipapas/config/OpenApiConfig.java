package senac.tsi.apipapas.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import senac.tsi.apipapas.controller.ChaveApiController;
import senac.tsi.apipapas.exception.ErroApi;
import senac.tsi.apipapas.idempotencia.IdempotenciaFilter;
import senac.tsi.apipapas.seguranca.ApiKeyFilter;

import java.lang.reflect.Method;

/**
 * Configuração da documentação Swagger/OpenAPI:
 * - informações gerais (introdução, termos de uso, licença), uma por versão da API;
 * - autenticação por chave de API, idempotência e rate limiting, aplicados pelos filtros
 *   ApiKeyFilter, IdempotenciaFilter e RateLimitFilter.
 */
@Configuration
@SecurityScheme(name = "ApiKey", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER,
        paramName = ApiKeyFilter.HEADER,
        description = "Chave de API para cadastrar, alterar e excluir. Para solicitar uma, veja a seção 3 dos "
                + "Termos de Uso. O gerenciamento de chaves (/chaves-api) aceita somente a chave de admin.")
class OpenApiConfig {

    /*
     * Um grupo de documentação por versão da API (seletor "Select a definition" no Swagger UI).
     * O OpenAPI não permite duas operações no mesmo caminho e método, então cada grupo mostra os endpoints
     * sem versão declarada mais os da sua versão.
     */
    @Bean
    GroupedOpenApi versao1() {
        return grupo("v1", "1");
    }

    @Bean
    GroupedOpenApi versao2() {
        return grupo("v2", "2");
    }

    private GroupedOpenApi grupo(String nome, String versao) {
        return GroupedOpenApi.builder()
                .group(nome)
                .displayName("Versão " + versao + " (X-API-Version: " + versao + ")")
                .addOpenApiMethodFilter(metodo -> atendeVersao(metodo, versao))
                .addOpenApiCustomizer(openApi -> {
                    openApi.setInfo(informacoes(versao));
                    // Registra o schema ErroApi, referenciado por todas as respostas de erro
                    if (openApi.getComponents() == null) {
                        openApi.setComponents(new Components());
                    }
                    ModelConverters.getInstance().readAll(ErroApi.class)
                            .forEach((nomeSchema, schema) -> openApi.getComponents().addSchemas(nomeSchema, schema));
                })
                .addOperationCustomizer(autenticacaoEIdempotencia())
                .addOperationCustomizer((operation, handlerMethod) -> {
                    // Header X-API-Version já preenchido com a versão do grupo, para o "Try it out" chamar a versão certa.
                    if (operation.getParameters() != null) {
                        operation.getParameters().stream()
                                .filter(p -> "header".equals(p.getIn()) && "X-API-Version".equals(p.getName()))
                                .forEach(p -> {
                                    p.setSchema(new StringSchema()._default(versao).addEnumItem(versao));
                                    p.setDescription("Versão da API. Sem o header, vale a versão 1.");
                                });
                    }
                    return operation;
                })
                .build();
    }

    private static boolean atendeVersao(Method metodo, String versao) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(metodo, RequestMapping.class);
        return mapping == null || mapping.version().isEmpty() || mapping.version().equals(versao);
    }

    // ---------------------------------------------------------------- introdução

    private static Info informacoes(String versao) {
        return new Info()
                .title("API de Papas")
                .version(versao)
                .description(introducao(versao))
                .termsOfService(Documentacao.TERMOS_DE_USO)
                .contact(new Contact().name("Enzo Bariotto").url(Documentacao.REPOSITORIO))
                .license(new License().name("Licença de Uso da API de Papas").url(Documentacao.LICENCA));
    }

    private static String introducao(String versao) {
        String sobreVersao = "1".equals(versao)
                ? """
                  Você está vendo a **versão 1**, a versão padrão da API. Na versão 2, a consulta de papas \
                  agrupa os nomes e os dados do pontificado e informa a duração de cada pontificado. \
                  Para trocar de versão, use **Select a definition**, no topo da página."""
                : """
                  Você está vendo a **versão 2**. Em relação à versão 1, só muda a consulta de papas, que agrupa \
                  os nomes e os dados do pontificado e informa a duração de cada pontificado. \
                  Para trocar de versão, use **Select a definition**, no topo da página.""";

        return """
                API REST com dados históricos sobre os papas da Igreja Católica e os registros ligados a eles: \
                conclaves, encíclicas, concílios e santos canonizados. Projeto acadêmico do curso de Tecnologia \
                em Sistemas para Internet do Senac.

                | Recurso | O que é | Relação com Papa |
                |---|---|---|
                | **Papas** | Papas da Igreja Católica (recurso central) | — |
                | **Conclaves** | Eleição que escolheu cada papa | One-to-One: cada papa tem no máximo um conclave |
                | **Encíclicas** | Cartas escritas pelos papas | One-to-Many: um papa escreve várias encíclicas |
                | **Concílios** | Concílios ecumênicos | Many-to-Many: um concílio reúne vários papas |
                | **Santos** | Santos canonizados | Many-to-One: cada santo foi canonizado por um papa |

                %s

                O uso da API está sujeito aos [Termos de Uso](%s), que tratam das [chaves de API](%s), \
                dos [limites de requisições](%s) e das [versões](%s). \
                Também é possível usar a API pela [interface web](/).
                """.formatted(sobreVersao,
                Documentacao.TERMOS_DE_USO, Documentacao.TERMOS_CHAVES,
                Documentacao.TERMOS_LIMITES, Documentacao.TERMOS_VERSOES);
    }

    // ---------------------------------------------------------------- respostas comuns

    @Bean
    OperationCustomizer autenticacaoEIdempotencia() {
        return (operation, handlerMethod) -> {
            adicionar(operation, "429", "Limite de requisições excedido. Aguarde os segundos indicados no header "
                    + "`Retry-After` ([limites de uso](" + Documentacao.TERMOS_LIMITES + "))");
            if (!handlerMethod.getBeanType().equals(ChaveApiController.class)) {
                documentarEscrita(operation, handlerMethod.hasMethodAnnotation(PostMapping.class),
                        handlerMethod.hasMethodAnnotation(PutMapping.class) || handlerMethod.hasMethodAnnotation(DeleteMapping.class));
            }
            corpoDeErro(operation);
            return operation;
        };
    }

    /** Toda resposta 4xx/5xx documenta o corpo no formato ErroApi. */
    private static void corpoDeErro(Operation operation) {
        operation.getResponses().forEach((status, resposta) -> {
            if (status.startsWith("4") || status.startsWith("5")) {
                resposta.setContent(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErroApi"))));
            }
        });
    }

    /** POST, PUT e DELETE exigem chave de API; POST também exige X-Idempotency-Key. */
    private static void documentarEscrita(Operation operation, boolean post, boolean putOuDelete) {
        boolean escrita = post || putOuDelete;
        if (!escrita) {
            return;
        }

        operation.addSecurityItem(new SecurityRequirement().addList("ApiKey"));
        adicionar(operation, "401", "Header `X-API-Key` ausente, ou chave inválida ou revogada "
                + "([como obter uma chave](" + Documentacao.TERMOS_CHAVES + "))");

        if (post) {
            operation.addParametersItem(new HeaderParameter()
                    .name(IdempotenciaFilter.HEADER)
                    .required(true)
                    .description("Valor único por operação (ex.: um UUID). Repetir a mesma chave com o mesmo corpo "
                            + "devolve a resposta original sem cadastrar de novo. A resposta fica guardada por 24 horas.")
                    .schema(new StringSchema().maxLength(100).example("3f2b8c1e-9d4a-4c6b-8e2f-1a7d5c9b0e34")));
            adicionar(operation, "201", "Requisição repetida: devolve a resposta original, com o header "
                    + IdempotenciaFilter.HEADER_REPETIDA + ": true");
            adicionar(operation, "400", "Header " + IdempotenciaFilter.HEADER + " ausente");
            adicionar(operation, "409", "Já existe uma requisição com esta chave de idempotência em processamento");
            adicionar(operation, "422", "A chave de idempotência já foi usada com uma requisição diferente");
        }
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
