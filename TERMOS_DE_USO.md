# Termos de Uso da API de Papas

Última atualização: 16 de novembro de 2026

Estes Termos de Uso regulam o acesso e a utilização da **API de Papas**
(doravante "API"), desenvolvida por Enzo Bariotto como projeto acadêmico do
curso de Tecnologia em Sistemas para Internet do Senac. Ao acessar ou utilizar
a API, inclusive pela documentação Swagger ou pela interface web, você declara
que leu e concorda com estes termos. Se não concordar, não utilize a API.

## 1. Sobre a API

A API é um serviço REST que disponibiliza informações sobre os papas da Igreja
Católica, seus conclaves, encíclicas, concílios e santos canonizados. Ela
permite consultar, cadastrar, atualizar e excluir registros, com finalidade
educacional e de demonstração.

## 2. Acesso

1. As **consultas** (requisições `GET`) são públicas e não exigem cadastro
   nem chave. Com uma chave de API, o uso segue o plano da chave (Gratuito,
   Básico ou Pro), conforme a seção [Planos e preços](#planos-e-preços).
2. Os **cadastros, alterações e exclusões** (`POST`, `PUT` e `DELETE`) exigem
   uma chave de API, conforme a [seção 3](#3-chaves-de-api).
3. A API pode ser usada pela documentação Swagger (`/swagger-ui.html`), pela
   interface web (`/`) ou por qualquer cliente HTTP.
4. A API é hospedada em um serviço de nuvem gratuito. Por isso, ela pode
   ficar fora do ar, demorar para responder na primeira requisição após um
   período sem uso, ou ser desativada a qualquer momento, sem aviso prévio.
5. O autor pode alterar, limitar ou encerrar a API, total ou parcialmente,
   a qualquer momento.

## 3. Chaves de API

1. As chaves de API são geradas somente pelo administrador da API. Para
   solicitar uma, entre em contato com o autor pelo repositório indicado na
   [seção 12](#12-legislação-e-contato), informando seu nome ou o nome da
   aplicação que vai usá-la e o plano desejado. Sem indicação, a chave é
   criada no plano Gratuito.
2. A chave é **pessoal e intransferível**. Você é responsável por todas as
   requisições feitas com ela e não deve publicá-la (em repositórios públicos,
   código de páginas web, capturas de tela etc.) nem compartilhá-la.
3. O valor completo da chave é mostrado uma única vez, no momento da geração.
   A API guarda apenas um resumo criptográfico (hash) dela, então uma chave
   perdida não pode ser recuperada: será preciso solicitar uma nova.
4. O administrador pode **revogar uma chave a qualquer momento**, em especial
   em caso de vazamento ou de descumprimento destes termos. Uma chave
   revogada deixa de ser aceita imediatamente, e as requisições com ela
   recebem o status `401`.

## 4. Limites de uso

Para manter o serviço disponível para todos, a API limita a quantidade de
requisições de cada cliente (*rate limiting*):

| Cliente | Identificado por | Limite atual |
|---|---|---|
| Sem chave de API | Endereço IP | 30 requisições por minuto |
| Com chave de API | Chave | O do plano da chave (tabela abaixo) |

O limite funciona como um saldo que se recupera aos poucos: cada requisição
consome uma unidade e o saldo volta gradualmente ao longo do minuto (por
exemplo, uma unidade a cada 2 segundos sem chave).

### Planos e preços

Cada chave de API tem um plano, que define o limite por minuto e a **cota
mensal** (total de requisições no mês):

| Plano | Preço | Limite por minuto | Cota mensal |
|---|---|---|---|
| Gratuito | R$ 0 | 60 requisições | 3.000 requisições |
| Básico | R$ 19,90 por mês | 120 requisições | 50.000 requisições |
| Pro | R$ 49,90 por mês | 300 requisições | 300.000 requisições |

- A cota renova à meia-noite do dia 1º de cada mês (horário de Brasília).
  Requisições não usadas não acumulam para o mês seguinte.
- Para trocar de plano, fale com o autor
  ([seção 12](#12-legislação-e-contato)). O administrador altera o plano da
  chave, e os novos limites valem a partir da requisição seguinte.
- A tabela de planos também pode ser consultada em `GET /planos`, que é
  pública e não gasta a cota.
- O consumo da sua chave aparece em toda resposta, nos headers
  `X-Quota-Limit`, `X-Quota-Remaining` e `X-Quota-Reset` (ver abaixo).

1. Toda resposta informa o limite e o consumo nos headers
   `X-RateLimit-Limit` (limite), `X-RateLimit-Remaining` (requisições
   disponíveis agora) e `X-RateLimit-Reset` (segundos até o saldo ser
   totalmente restaurado). Com chave de usuário, também trazem
   `X-Quota-Limit` (cota mensal), `X-Quota-Remaining` (requisições que restam
   no mês) e `X-Quota-Reset` (dia em que a cota renova).
2. Ao passar do limite por minuto ou esgotar a cota mensal, a API responde **`429 Too Many Requests`** com o header
   **`Retry-After`**, que indica quantos segundos esperar. Aplicações clientes
   devem respeitar esse tempo antes de tentar novamente, em vez de repetir a
   requisição em sequência.
3. Os limites podem ser alterados pelo autor sem aviso prévio. Os valores
   vigentes são sempre os informados nos headers das respostas.
4. Tentar contornar os limites (por exemplo, usando várias chaves ou vários
   endereços IP para a mesma aplicação) é considerado uso proibido
   ([seção 7](#7-uso-proibido)).

## 5. Cadastros e requisições repetidas

1. Todo cadastro (`POST`) deve enviar o header `X-Idempotency-Key` com um
   valor único por operação, por exemplo um UUID. Assim, se a mesma requisição
   for reenviada (por falha de rede ou clique duplo), a API devolve a resposta
   original em vez de cadastrar o registro duas vezes.
2. A resposta de um cadastro bem-sucedido fica guardada por **24 horas**
   associada à chave de idempotência e à chave de API de quem fez o pedido.
3. Reutilizar a mesma chave de idempotência com dados diferentes resulta no
   status `422`. Use sempre uma chave nova para cada novo cadastro.

## 6. Versões da API

1. A versão é escolhida pelo header `X-API-Version`. Sem o header, vale a
   **versão 1**.
2. Versões antigas podem ser descontinuadas. Sempre que possível, isso será
   avisado no repositório do projeto com antecedência.
3. Uma versão não suportada retorna o status `400`.

## 7. Uso proibido

Ao utilizar a API, você se compromete a **não**:

1. enviar conteúdo ilícito, ofensivo, discriminatório, difamatório ou que
   desrespeite a fé ou a dignidade de qualquer pessoa ou grupo;
2. realizar requisições em excesso, ataques, testes de carga ou qualquer
   tentativa de contornar os [limites de uso](#4-limites-de-uso);
3. tentar explorar falhas de segurança, adivinhar ou usar chaves de API de
   outras pessoas, ou obter acesso não autorizado ao servidor;
4. compartilhar publicamente a sua chave de API;
5. utilizar a API com fins comerciais sem autorização prévia e por escrito
   do autor;
6. apresentar a API ou seus resultados como trabalho acadêmico de autoria
   própria.

O descumprimento destas regras pode resultar na **revogação da chave de API**,
no bloqueio do acesso e na remoção do conteúdo enviado, sem aviso prévio.

## 8. Dados armazenados

1. A API usa um banco de dados em memória. **Todos os dados cadastrados,
   alterados ou excluídos são perdidos** sempre que o serviço é reiniciado, e
   o banco volta ao seu conteúdo inicial de exemplo. Isso inclui as chaves de
   API geradas.
2. Os dados são **compartilhados entre todos os usuários**: qualquer registro
   que você cadastrar pode ser visto por todos e alterado ou excluído por
   outras pessoas com chave de API.
3. **Não envie dados pessoais, sensíveis ou confidenciais** por meio da API.
4. Os dados históricos de exemplo têm finalidade educacional, não constituem
   fonte oficial da Santa Sé e podem conter imprecisões.

## 9. Privacidade

1. A API não pede dados pessoais. Para funcionar, ela registra apenas:
   - o **nome informado** na geração de cada chave de API e as datas de
     criação, último uso e revogação da chave;
   - o **endereço IP** de quem faz requisições sem chave, mantido somente em
     memória e apenas enquanto o [limite de uso](#4-limites-de-uso) não é
     totalmente restaurado;
   - a **resposta de cada cadastro** por 24 horas, para a proteção contra
     requisições repetidas ([seção 5](#5-cadastros-e-requisições-repetidas)).
2. O provedor de hospedagem pode registrar informações técnicas das
   requisições, como endereço IP, data e horário, de acordo com as políticas
   dele.

## 10. Propriedade intelectual

O código-fonte, a documentação e os dados de exemplo da API são distribuídos
sob a [Licença MIT](LICENSE): qualquer pessoa pode usar, copiar, modificar e
distribuir o código, desde que mantenha o aviso de direitos autorais e o texto
da licença. Estes Termos de Uso tratam do uso do serviço publicado, e a
licença trata do uso do código.

## 11. Isenção de garantias e alterações

1. A API é fornecida "no estado em que se encontra", sem garantia de
   funcionamento, disponibilidade, desempenho ou exatidão dos dados. O autor
   não se responsabiliza por danos, perdas ou prejuízos decorrentes do uso ou
   da impossibilidade de uso da API, nem pelo conteúdo enviado por outros
   usuários.
2. Estes termos podem ser alterados a qualquer momento. A versão vigente é
   sempre a publicada no repositório do projeto, e o uso contínuo da API após
   uma alteração significa a aceitação dos novos termos.

## 12. Legislação e contato

Estes termos são regidos pelas leis da República Federativa do Brasil. Dúvidas,
pedidos de chave de API ou de autorização podem ser enviados ao autor pelo
repositório <https://github.com/enzobariotto/APIPapas>.
