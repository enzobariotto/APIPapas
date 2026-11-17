package senac.tsi.apipapas.config;

import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * O Swagger UI escreve em inglês os rótulos do cabeçalho da documentação ("Terms of service" e
 * "Nome - Website" no contato). Este componente substitui o transformador padrão do springdoc e acrescenta ao
 * swagger-initializer.js um pequeno script que troca esses rótulos por textos em português.
 */
@Component
class SwaggerUiPortugues extends SwaggerIndexPageTransformer {

    static final String SCRIPT = """

            (function () {
              const traducoes = { "Terms of service": "Termos de Uso" };
              function traduzir() {
                document.querySelectorAll(".info a").forEach(function (link) {
                  const texto = link.textContent;
                  if (traducoes[texto]) {
                    link.textContent = traducoes[texto];
                  } else if (texto.endsWith(" - Website")) {
                    link.textContent = texto.replace(/ - Website$/, " - Repositório");
                  }
                });
              }
              new MutationObserver(traduzir).observe(document.body, { childList: true, subtree: true });
            })();
            """;

    SwaggerUiPortugues(SwaggerUiConfigProperties config, SwaggerUiOAuthProperties oauth,
                       SwaggerWelcomeCommon welcome, ObjectMapperProvider objectMapper) {
        super(config, oauth, welcome, objectMapper);
    }

    @Override
    protected String defaultTransformations(SwaggerUiConfigParameters parametros, InputStream entrada) throws IOException {
        return super.defaultTransformations(parametros, entrada) + SCRIPT;
    }
}
