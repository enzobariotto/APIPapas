package senac.tsi.apipapas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/** As informações gerais da documentação (título, introdução, termos e licença) ficam em config.OpenApiConfig. */
@SpringBootApplication
public class ApiPapasApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(ApiPapasApplication.class, args);
    }

}
