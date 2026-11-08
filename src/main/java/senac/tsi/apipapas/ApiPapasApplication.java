package senac.tsi.apipapas;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "API de Papas",
        version = "1.0",
        description = "API REST sobre os papas da Igreja Católica, seus conclaves, encíclicas, concílios e santos canonizados.",
        termsOfService = "https://github.com/enzobariotto/APIPapas",
        contact = @Contact(name = "Enzo Bariotto", url = "https://github.com/enzobariotto"),
        license = @License(name = "MIT", url = "https://opensource.org/licenses/MIT")))
public class ApiPapasApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(ApiPapasApplication.class, args);
    }

}
