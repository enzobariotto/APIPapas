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

Status: 200 (ok), 201 (criado), 204 (excluído), 400 (dados inválidos), 401 (sem chave de API ou chave inválida), 403 (não é admin), 404 (não encontrado), 409 (registro vinculado / duplicado / requisição idempotente em processamento), 422 (chave de idempotência reutilizada com outro corpo).

## Autenticação com chave de API (`X-API-Key`)

- Os `GET` são públicos. Os `POST`, `PUT` e `DELETE` de papas, conclaves, encíclicas, concílios e santos exigem o header `X-API-Key` com uma chave válida.
- As chaves são geradas e gerenciadas em `/chaves-api`, que só aceita a **chave de admin**:

| Endpoint | O que faz |
|---|---|
| `POST /chaves-api` | Gera uma chave para um usuário (`{"nome": "Professor"}`). O valor completo aparece **só nesta resposta** |
| `GET /chaves-api` | Lista as chaves (apenas o prefixo, nunca o valor) |
| `GET /chaves-api/{id}` | Detalha uma chave (ativa, criação, último uso, revogação) |
| `DELETE /chaves-api/{id}` | Revoga a chave, que deixa de ser aceita na hora |

- A chave de admin vem da variável de ambiente `API_ADMIN_KEY` (no Render: *Environment*). Se ela não for definida, uma chave aleatória é gerada a cada inicialização e mostrada no log.
- No banco fica só o hash SHA-256 de cada chave, nunca o valor.
- No Swagger, clique em **Authorize** e informe a chave para testar os endpoints protegidos.

## Idempotência (`X-Idempotency-Key`)

Os `POST` de cadastro exigem o header `X-Idempotency-Key` com um valor único por operação (por exemplo, um UUID):

| Situação | Resposta |
|---|---|
| Header ausente | `400` |
| Primeira requisição com a chave | Processa normalmente e guarda a resposta de sucesso por 24 h |
| Mesma chave e mesmo corpo | Devolve a resposta original **sem cadastrar de novo**, com o header `Idempotency-Replayed: true` |
| Mesma chave com corpo diferente | `422` |
| Mesma chave enquanto a primeira ainda está em processamento | `409` |

As respostas de erro não são guardadas: dá para corrigir os dados e tentar de novo com a mesma chave. Cada chave de API tem o seu próprio espaço de chaves de idempotência.

## Rate limiting

Cada cliente tem um limite de requisições por janela de tempo. O cliente é identificado pela chave de API (se o `X-API-Key` for válido) ou, sem chave, pelo IP.

| Cliente | Limite padrão |
|---|---|
| Sem chave (por IP) | 60 requisições por minuto |
| Com chave de API | 120 requisições por minuto |

- Toda resposta traz `X-RateLimit-Limit` (limite), `X-RateLimit-Remaining` (quanto ainda resta) e `X-RateLimit-Reset` (segundos até a janela reiniciar).
- Ao passar do limite a API responde **`429 Too Many Requests`** com o header **`Retry-After`** (segundos para tentar de novo).
- Swagger, `/v3/api-docs` e console do H2 ficam fora do limite.
- Configurável pelas variáveis `RATE_LIMIT_ANONIMO`, `RATE_LIMIT_COM_CHAVE` e `RATE_LIMIT_JANELA` (ex.: `60s`).

## CORS

| Configuração | Valor |
|---|---|
| Origens permitidas | `http://localhost:3000`, `http://localhost:5173`, `http://localhost:4200` (variável `CORS_ORIGENS`, separadas por vírgula) |
| Métodos | `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS` |
| Headers aceitos | `Content-Type`, `Accept`, `X-API-Key`, `X-Idempotency-Key`, `X-API-Version` |
| Headers expostos ao navegador | `Location`, `Retry-After`, `X-API-Version`, `Idempotency-Replayed`, `X-RateLimit-*` |
| Credenciais (cookies) | Não (a autenticação é pelo header `X-API-Key`) |
| Cache do preflight | 1 hora |

Requisições de outras origens recebem `403`. O próprio Swagger da API é mesma origem e não é afetado.

## Versionamento (`X-API-Version`)

A versão é escolhida pelo header `X-API-Version`. Sem o header, vale a **versão 1**; versão não suportada retorna `400`. A resposta informa a versão usada no header `X-API-Version`.

Os endpoints `GET /papas` e `GET /papas/{id}` têm duas versões:

| Versão 1 | Versão 2 |
|---|---|
| `{"id": 6, "nomePapal": "Francisco", "nomeBatismo": "Jorge Mario Bergoglio", "numeroOrdem": 266, "paisNascimento": "Argentina", "inicioPontificado": "2013-03-13", "fimPontificado": "2025-04-21", "situacao": "FALECIMENTO"}` | `{"id": 6, "nome": {"papal": "Francisco", "batismo": "Jorge Mario Bergoglio"}, "numeroOrdem": 266, "paisNascimento": "Argentina", "pontificado": {"inicio": "2013-03-13", "fim": "2025-04-21", "situacao": "FALECIMENTO", "duracaoDias": 4422, "emExercicio": false}}` |

Os demais endpoints atendem às duas versões. No Swagger, escolha a versão em **Select a definition** (canto superior direito).
