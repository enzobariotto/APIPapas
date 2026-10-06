package senac.tsi.apipapas;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "API de Papas",
        version = "1.0",
        description = "API REST sobre os papas da Igreja Católica, seus conclaves, encíclicas, concílios e santos canonizados."))
public class ApiPapasApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(ApiPapasApplication.class, args);
    }

}
