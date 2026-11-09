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
        description = """
                API REST sobre os papas da Igreja Católica, seus conclaves, encíclicas, concílios e santos canonizados.

                **Como usar**
                - **Leitura (GET):** pública, sem chave.
                - **Escrita (POST, PUT, DELETE):** exige o header `X-API-Key` (clique em **Authorize**). \
                As chaves são geradas pelo administrador em `/chaves-api`.
                - **POST:** exige também o header `X-Idempotency-Key` (ex.: um UUID); repetir a mesma chave não cadastra de novo.
                - **Versão:** header `X-API-Version` (1 ou 2; padrão 1). Escolha a versão em **Select a definition**.
                - **Limite de requisições:** por padrão, 60/min sem chave e 120/min com chave; acima disso, `429` com `Retry-After`.

                **Erros:** todas as respostas de erro seguem o formato `ErroApi` \
                (`status`, `erro`, `mensagem`, `caminho` e, em erros de validação, `campos` com a mensagem de cada campo).
                """,
        termsOfService = "https://github.com/enzobariotto/APIPapas/blob/main/TERMOS_DE_USO.md",
        contact = @Contact(name = "Enzo Bariotto", url = "https://github.com/enzobariotto"),
        license = @License(name = "Licença de Uso da API de Papas",
                url = "https://github.com/enzobariotto/APIPapas/blob/main/LICENSE")))
public class ApiPapasApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(ApiPapasApplication.class, args);
    }

}
