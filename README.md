# API de Papas

API REST em Spring Boot (baseada no tutorial https://spring.io/guides/tutorials/rest) sobre os papas da Igreja Católica.

## Como rodar

Rode `ApiPapasApplication` no IntelliJ e acesse:

- Swagger: http://localhost:8081/swagger-ui.html
- Console do H2: http://localhost:8081/h2-console (JDBC URL: `jdbc:h2:mem:papasdb`, usuário `sa`, sem senha)

## Entidades e relacionamentos

| Entidade | Relacionamento |
|---|---|
| Papa | tem o enum `SituacaoPontificado` |
| Conclave | **One-to-One** com Papa (o papa eleito) |
| Enciclica | **One-to-Many** (Papa tem várias encíclicas) |
| Concilio | **Many-to-Many** com Papa |
| Santo | Many-to-One com Papa (quem canonizou) |

Para informar um relacionamento no POST/PUT basta passar o id: `"papa": {"id": 7}`.

## Endpoints

Cada entidade tem: `GET` (lista paginada), `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}` e uma consulta personalizada:

| Entidade | Consulta personalizada |
|---|---|
| Papas | `GET /papas/situacao/{situacao}` |
| Conclaves | `GET /conclaves/rapidos?maxEscrutinios=4` |
| Encíclicas | `GET /enciclicas/papa/{papaId}` |
| Concílios | `GET /concilios/busca?nome=vaticano` |
| Santos | `GET /santos/pais/{pais}` |

Paginação: `?page=0&size=5&sort=nome`

Status: 200 (ok), 201 (criado), 204 (excluído), 400 (dados inválidos), 404 (não encontrado), 409 (registro vinculado / duplicado).
