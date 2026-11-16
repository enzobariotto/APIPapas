package senac.tsi.apipapas.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.module.SimpleModule;

/** Ajustes na leitura do JSON enviado pelos clientes. */
@Configuration
class JacksonConfig {

    @Bean
    JsonMapperBuilderCustomizer leituraDoJson() {
        return builder -> builder
                // "numeroOrdem": 1.5 é recusado (400), em vez de virar 1 sem aviso
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                // Textos chegam sem espaços nas pontas: "  Francisco " é salvo como "Francisco"
                .addModule(new SimpleModule("textos-sem-espacos-nas-pontas").addDeserializer(String.class, new TextoAparado()));
    }

    /** Lê textos tirando os espaços do começo e do fim. Número ou objeto no lugar de texto continua sendo erro (400). */
    static class TextoAparado extends ValueDeserializer<String> {

        @Override
        public String deserialize(JsonParser parser, DeserializationContext contexto) {
            if (parser.currentToken() == JsonToken.VALUE_STRING) {
                return parser.getString().strip();
            }
            return (String) contexto.handleUnexpectedToken(String.class, parser);
        }
    }
}
