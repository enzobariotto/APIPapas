package senac.tsi.apipapas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import senac.tsi.apipapas.idempotencia.IdempotenciaFilter;
import senac.tsi.apipapas.model.Concilio;
import senac.tsi.apipapas.model.Conclave;
import senac.tsi.apipapas.model.Enciclica;
import senac.tsi.apipapas.model.Papa;
import senac.tsi.apipapas.model.PapaV2;
import senac.tsi.apipapas.model.Santo;
import senac.tsi.apipapas.model.SituacaoPontificado;
import senac.tsi.apipapas.seguranca.RateLimitFilter;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Exemplos da documentação Swagger: para cada endpoint e cada status, um exemplo igual ao que a API
 * realmente devolve.
 *
 * - Sucesso: montado a partir das próprias classes do modelo (Papa, PapaV2, Conclave...), com os links
 *   HATEOAS e a paginação no mesmo formato das respostas reais.
 * - Erro: o formato ErroApi com a mensagem que a API devolve em cada situação. Quando um status tem mais de
 *   uma causa (ex.: 401 sem chave ou com chave inválida), cada causa vira um exemplo selecionável.
 */
final class ExemplosOpenApi {

    static final String BASE = "https://apipapas.onrender.com";
    private static final String MOMENTO = "2026-11-08T14:30:00Z";
    private static final String HAL = "application/hal+json";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    // ------------------------------------------------------------ dados dos exemplos (os mesmos do LoadDatabase)

    private static final Map<Long, Papa> PAPAS = new LinkedHashMap<>();
    private static final Map<Long, Conclave> CONCLAVES = new LinkedHashMap<>();
    private static final Map<Long, Enciclica> ENCICLICAS = new LinkedHashMap<>();
    private static final Map<Long, Concilio> CONCILIOS = new LinkedHashMap<>();
    private static final Map<Long, Santo> SANTOS = new LinkedHashMap<>();

    static {
        papa(1, "Pio IX", "Giovanni Maria Mastai-Ferretti", 255, "Itália", "1846-06-16", "1878-02-07", SituacaoPontificado.FALECIMENTO);
        papa(2, "João XXIII", "Angelo Giuseppe Roncalli", 261, "Itália", "1958-10-28", "1963-06-03", SituacaoPontificado.FALECIMENTO);
        papa(3, "Paulo VI", "Giovanni Battista Montini", 262, "Itália", "1963-06-21", "1978-08-06", SituacaoPontificado.FALECIMENTO);
        papa(4, "João Paulo II", "Karol Józef Wojtyła", 264, "Polônia", "1978-10-16", "2005-04-02", SituacaoPontificado.FALECIMENTO);
        papa(5, "Bento XVI", "Joseph Aloisius Ratzinger", 265, "Alemanha", "2005-04-19", "2013-02-28", SituacaoPontificado.RENUNCIA);
        papa(6, "Francisco", "Jorge Mario Bergoglio", 266, "Argentina", "2013-03-13", "2025-04-21", SituacaoPontificado.FALECIMENTO);
        papa(7, "Leão XIV", "Robert Francis Prevost", 267, "Estados Unidos", "2025-05-08", null, SituacaoPontificado.EM_EXERCICIO);

        conclave(1, "1978-10-14", "1978-10-16", 111, 8, 4);
        conclave(2, "2005-04-18", "2005-04-19", 115, 4, 5);
        conclave(3, "2013-03-12", "2013-03-13", 115, 5, 6);
        conclave(4, "2025-05-07", "2025-05-08", 133, 4, 7);

        enciclica(1, "Pacem in Terris", "Paz na Terra", "1963-04-11", 2);
        enciclica(2, "Populorum Progressio", "O Desenvolvimento dos Povos", "1967-03-26", 3);
        enciclica(5, "Laudato Si'", "Louvado Sejas", "2015-05-24", 6);
        enciclica(6, "Fratelli Tutti", "Todos Irmãos", "2020-10-03", 6);

        concilio(1, "Concílio Vaticano I", "1869-12-08", "1870-10-20", 1);
        concilio(2, "Concílio Vaticano II", "1962-10-11", "1965-12-08", 2, 3);

        santo(1, "Santo Antônio de Sant'Anna Galvão (Frei Galvão)", "Brasil", "2007-05-11", 5);
        santo(2, "São João Paulo II", "Polônia", "2014-04-27", 6);
        santo(4, "Santa Dulce dos Pobres", "Brasil", "2019-10-13", 6);
    }

    /** Recurso de cada caminho: nome no singular (para as mensagens de erro) e chave da lista na paginação. */
    private record Recurso(String singular, boolean feminino) {
    }

    private static final Map<String, Recurso> RECURSOS = Map.of(
            "papas", new Recurso("papa", false),
            "conclaves", new Recurso("conclave", false),
            "enciclicas", new Recurso("encíclica", true),
            "concilios", new Recurso("concílio", false),
            "santos", new Recurso("santo", false),
            "planos", new Recurso("plano", false),
            "chaves-api", new Recurso("chave de API", true));

    private ExemplosOpenApi() {
    }

    // ------------------------------------------------------------ aplicação

    /** Preenche descrições, exemplos e headers de todas as respostas de todos os endpoints. */
    static void aplicar(OpenAPI openApi, String versao) {
        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().forEach((caminho, item) -> item.readOperationsMap().forEach((metodo, operacao) -> {
            if (!RECURSOS.containsKey(recursoDe(caminho))) {
                return;
            }
            Endpoint endpoint = new Endpoint(metodo, caminho, versao);
            if (endpoint.listagem()) {
                // Toda listagem paginada aceita ?sort=; ordenar por um campo inexistente dá 400.
                garantir(operacao, "400");
            }
            if (endpoint.versionado()) {
                garantir(operacao, "400");
            }
            ApiResponses ordenadas = new ApiResponses();
            new TreeMap<>(operacao.getResponses()).forEach((status, resposta) -> {
                preencher(endpoint, status, resposta);
                ordenadas.addApiResponse(status, resposta);
            });
            operacao.setResponses(ordenadas);
        }));
    }

    private static void garantir(Operation operacao, String status) {
        if (!operacao.getResponses().containsKey(status)) {
            operacao.getResponses().addApiResponse(status, new ApiResponse().description(""));
        }
    }

    private static void preencher(Endpoint e, String status, ApiResponse resposta) {
        if (status.startsWith("2")) {
            sucesso(e, status, resposta);
            return;
        }
        List<Exemplo> exemplos = erros(e, Integer.parseInt(status));
        if (exemplos.isEmpty()) {
            return;
        }
        boolean chaveDeUsuario = status.equals("401") && exemplos.size() > 1;
        resposta.setDescription(chaveDeUsuario ? "Chave de API ausente, inválida ou revogada" : descricao(exemplos));
        Map<String, Example> mapa = new LinkedHashMap<>();
        for (Exemplo ex : exemplos) {
            mapa.put(ex.id(), new Example().summary(maiuscula(ex.resumo())).value(ex.corpo()));
        }
        resposta.setContent(new Content().addMediaType("application/json", new MediaType()
                .schema(new Schema<>().$ref("#/components/schemas/ErroApi"))
                .examples(mapa)));
        if (status.equals("429")) {
            resposta.addHeaderObject("Retry-After", header("Segundos até a próxima requisição ser liberada", new IntegerSchema().example(2)));
            resposta.addHeaderObject(RateLimitFilter.HEADER_LIMITE, header("Limite de requisições por minuto: 30 sem chave; com chave, o do plano da chave (ver GET /planos)", new IntegerSchema().example(30)));
            resposta.addHeaderObject(RateLimitFilter.HEADER_RESTANTE, header("Requisições disponíveis agora", new IntegerSchema().example(0)));
            resposta.addHeaderObject(RateLimitFilter.HEADER_RESET, header("Segundos até o limite ser totalmente restaurado", new IntegerSchema().example(60)));
            if (e.contaNaCota()) {
                resposta.addHeaderObject(RateLimitFilter.HEADER_COTA, header("Cota mensal do plano da chave (só com chave de usuário)", new IntegerSchema().example(3000)));
                resposta.addHeaderObject(RateLimitFilter.HEADER_COTA_RESTANTE, header("Requisições que restam no mês (só com chave de usuário)", new IntegerSchema().example(0)));
                resposta.addHeaderObject(RateLimitFilter.HEADER_COTA_RENOVA, header("Dia em que a cota renova (só com chave de usuário)", new StringSchema().example("2026-12-01")));
            }
        }
    }

    // ------------------------------------------------------------ sucesso

    private static void sucesso(Endpoint e, String status, ApiResponse resposta) {
        if (status.equals("201")) {
            resposta.addHeaderObject("Location", header("Endereço do registro criado",
                    new StringSchema().example(BASE + "/" + e.recurso + "/" + idDoExemplo(e))));
            if (!e.recurso.equals("chaves-api")) {
                resposta.setDescription(resposta.getDescription() + ". Se a requisição for repetida com a mesma "
                        + IdempotenciaFilter.HEADER + " e o mesmo corpo, a resposta original é devolvida sem cadastrar de novo, "
                        + "com o header " + IdempotenciaFilter.HEADER_REPETIDA + ": true");
                resposta.addHeaderObject(IdempotenciaFilter.HEADER_REPETIDA, header(
                        "Presente (true) quando a resposta é a repetição de uma requisição anterior", new StringSchema().example("true")));
            }
        }
        if (resposta.getContent() == null || resposta.getContent().isEmpty()) {
            return; // 204: sem corpo
        }
        Object exemplo = e.listagem() ? pagina(e) : item(e);
        Schema<?> schema = resposta.getContent().values().iterator().next().getSchema();
        resposta.setContent(new Content().addMediaType(HAL, new MediaType().schema(schema).example(exemplo)));
    }

    private static Object item(Endpoint e) {
        return switch (e.recurso) {
            case "papas" -> e.versao.equals("2") ? papaV2(6) : papa(6);
            case "conclaves" -> conclave(3);
            case "enciclicas" -> enciclica(5);
            case "concilios" -> concilio(2);
            case "santos" -> santo(4);
            case "planos" -> planos();
            default -> e.metodo == PathItem.HttpMethod.POST ? chaveGerada() : chave(1, "Professor", true);
        };
    }

    private static Object pagina(Endpoint e) {
        String r = e.recurso;
        return switch (e.caminho) {
            case "/papas" -> e.versao.equals("2")
                    ? pagina("/papas", "papaV2List", List.of(papaV2(1), papaV2(2)), 2, 7)
                    : pagina("/papas", "papaList", List.of(papa(1), papa(2)), 2, 7);
            case "/papas/situacao/{situacao}" -> pagina("/papas/situacao/RENUNCIA", "papaList", List.of(papa(5)), 20, 1);
            case "/conclaves" -> pagina("/conclaves", "conclaveList", List.of(conclave(1), conclave(2)), 2, 4);
            case "/conclaves/rapidos" -> pagina("/conclaves/rapidos?maxEscrutinios=4", "conclaveList", List.of(conclave(2), conclave(4)), 20, 2);
            case "/enciclicas" -> pagina("/enciclicas", "enciclicaList", List.of(enciclica(1), enciclica(2)), 2, 6);
            case "/enciclicas/papa/{papaId}" -> pagina("/enciclicas/papa/6", "enciclicaList", List.of(enciclica(5), enciclica(6)), 20, 2);
            case "/concilios" -> pagina("/concilios", "concilioList", List.of(concilio(1), concilio(2)), 20, 2);
            case "/concilios/busca" -> pagina("/concilios/busca?nome=vaticano", "concilioList", List.of(concilio(1), concilio(2)), 20, 2);
            case "/santos" -> pagina("/santos", "santoList", List.of(santo(1), santo(2)), 2, 4);
            case "/santos/pais/{pais}" -> pagina("/santos/pais/Brasil", "santoList", List.of(santo(1), santo(4)), 20, 2);
            default -> pagina("/chaves-api", "chaveApiList",
                    List.of(chave(1, "Professor", true), chave(2, "Colega de grupo", false)), 20, 2);
        };
    }

    /** Página no formato do PagedResourcesAssembler: _embedded, _links (first/self/next/last) e page. */
    private static Map<String, Object> pagina(String caminho, String chaveLista, List<Object> itens, int tamanho, int total) {
        int paginas = (total + tamanho - 1) / tamanho;
        String sep = caminho.contains("?") ? "&" : "?";
        String url = BASE + caminho + sep + "page=%d&size=" + tamanho;

        Map<String, Object> links = new LinkedHashMap<>();
        if (paginas > 1) {
            links.put("first", Map.of("href", url.formatted(0)));
        }
        links.put("self", Map.of("href", url.formatted(0)));
        if (paginas > 1) {
            links.put("next", Map.of("href", url.formatted(1)));
            links.put("last", Map.of("href", url.formatted(paginas - 1)));
        }
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("number", 0);
        page.put("size", tamanho);
        page.put("totalElements", total);
        page.put("totalPages", paginas);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("_embedded", Map.of(chaveLista, itens));
        resultado.put("_links", links);
        resultado.put("page", page);
        return resultado;
    }

    private static Map<String, Object> papa(long id) {
        return comLinks(PAPAS.get(id), linksPapa(id));
    }

    private static Map<String, Object> papaV2(long id) {
        return comLinks(PapaV2.de(PAPAS.get(id)), linksPapa(id));
    }

    private static Map<String, Object> linksPapa(long id) {
        return links("self", "/papas/" + id, "update", "/papas/" + id, "delete", "/papas/" + id,
                "papas", "/papas", "enciclicas", "/enciclicas/papa/" + id);
    }

    private static Map<String, Object> conclave(long id) {
        Conclave c = CONCLAVES.get(id);
        return comLinks(c, links("self", "/conclaves/" + id, "update", "/conclaves/" + id, "delete", "/conclaves/" + id,
                "conclaves", "/conclaves", "papa", "/papas/" + c.getPapa().getId()));
    }

    private static Map<String, Object> enciclica(long id) {
        Enciclica en = ENCICLICAS.get(id);
        return comLinks(en, links("self", "/enciclicas/" + id, "update", "/enciclicas/" + id, "delete", "/enciclicas/" + id,
                "enciclicas", "/enciclicas", "papa", "/papas/" + en.getPapa().getId()));
    }

    private static Map<String, Object> concilio(long id) {
        return comLinks(CONCILIOS.get(id), links("self", "/concilios/" + id, "update", "/concilios/" + id,
                "delete", "/concilios/" + id, "concilios", "/concilios"));
    }

    private static Map<String, Object> santo(long id) {
        Santo s = SANTOS.get(id);
        return comLinks(s, links("self", "/santos/" + id, "update", "/santos/" + id, "delete", "/santos/" + id,
                "santos", "/santos", "papa", "/papas/" + s.getCanonizadoPor().getId()));
    }

    private static Map<String, Object> chave(long id, String nome, boolean ativa) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", id);
        dados.put("nome", nome);
        dados.put("prefixo", id == 1 ? "papas_DhE6Z" : "papas_q81Lm");
        dados.put("ativa", ativa);
        dados.put("criadaEm", "2026-11-08T14:00:00Z");
        dados.put("ultimoUsoEm", "2026-11-08T14:25:12Z");
        dados.put("revogadaEm", ativa ? null : "2026-11-08T14:28:40Z");
        dados.put("plano", id == 1 ? "BASICO" : "GRATUITO");
        dados.put("requisicoesNoMes", id == 1 ? 1234 : 87);
        return comLinks(dados, links("self", "/chaves-api/" + id, "plano", "/chaves-api/" + id + "/plano",
                "revogar", "/chaves-api/" + id, "chaves-api", "/chaves-api"));
    }

    private static Map<String, Object> planos() {
        List<Object> itens = List.of(
                plano("GRATUITO", "Gratuito", "0", 60, 3000),
                plano("BASICO", "Básico", "19.90", 120, 50000),
                plano("PRO", "Pro", "49.90", 300, 300000));
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("_embedded", Map.of("planos", itens));
        resultado.put("_links", links("self", "/planos"));
        return resultado;
    }

    private static Map<String, Object> plano(String plano, String nome, String preco, int porMinuto, long cota) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("plano", plano);
        dados.put("nome", nome);
        dados.put("precoMensal", new BigDecimal(preco));
        dados.put("requisicoesPorMinuto", porMinuto);
        dados.put("cotaMensal", cota);
        return dados;
    }

    private static Map<String, Object> chaveGerada() {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", 1);
        dados.put("nome", "Professor");
        dados.put("prefixo", "papas_DhE6Z");
        dados.put("chave", "papas_DhE6Z6x0Z1qnhiMbM4_m2bUh0Ie6myQy_F5ykSUzKnk");
        dados.put("plano", "BASICO");
        dados.put("criadaEm", "2026-11-08T14:00:00Z");
        return comLinks(dados, links("self", "/chaves-api/1", "plano", "/chaves-api/1/plano",
                "revogar", "/chaves-api/1", "chaves-api", "/chaves-api"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> comLinks(Object objeto, Map<String, Object> links) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("_links", links);
        resultado.putAll(objeto instanceof Map<?, ?> mapa ? (Map<String, Object>) mapa : JSON.convertValue(objeto, Map.class));
        return resultado;
    }

    private static Map<String, Object> links(String... relECaminho) {
        Map<String, Object> links = new LinkedHashMap<>();
        for (int i = 0; i < relECaminho.length; i += 2) {
            links.put(relECaminho[i], Map.of("href", BASE + relECaminho[i + 1]));
        }
        return links;
    }

    // ------------------------------------------------------------ erros

    private record Exemplo(String id, String resumo, Map<String, Object> corpo) {
    }

    private static List<Exemplo> erros(Endpoint e, int status) {
        List<Exemplo> lista = new ArrayList<>();
        String singular = RECURSOS.get(e.recurso).singular();
        boolean chaves = e.recurso.equals("chaves-api");
        boolean comCorpo = e.metodo == PathItem.HttpMethod.POST || e.metodo == PathItem.HttpMethod.PUT;
        String caminho = e.concreto("6");

        switch (status) {
            case 400 -> {
                if (comCorpo) {
                    lista.add(erro("validacao", "dados inválidos", 400, "Dados inválidos. Corrija os campos indicados.",
                            e.concreto(idDoExemplo(e)), e.caminho.equals("/chaves-api/{id}/plano")
                                    ? Map.of("plano", "O plano é obrigatório") : camposInvalidos(e.recurso)));
                    Map<String, String> formato = campoComFormatoInvalido(e.recurso);
                    if (formato != null) {
                        lista.add(erro("formato", "valor em formato inválido", 400, "Dados inválidos. Corrija os campos indicados.",
                                e.concreto(idDoExemplo(e)), formato));
                    }
                    lista.add(erro("json", "JSON malformado", 400,
                            "JSON malformado. Verifique a sintaxe do corpo da requisição (aspas, vírgulas e chaves).",
                            e.concreto(idDoExemplo(e)), null));
                }
                if (comCorpo) {
                    Exemplo historico = foraDoPontificado(e);
                    if (historico != null) {
                        lista.add(historico);
                    }
                }
                if (e.metodo == PathItem.HttpMethod.POST && !chaves) {
                    lista.add(erro("idempotencia", "header " + IdempotenciaFilter.HEADER + " ausente", 400,
                            "O header " + IdempotenciaFilter.HEADER + " é obrigatório em requisições POST.", e.caminho, null));
                }
                switch (e.caminho) {
                    case "/papas/situacao/{situacao}" -> lista.add(erro("situacao", "situação inválida", 400,
                            "Valor inválido para o parâmetro 'situacao'.", "/papas/situacao/XYZ",
                            Map.of("situacao", "Valor 'XYZ' inválido: deve ser um dos valores EM_EXERCICIO, FALECIMENTO, RENUNCIA")));
                    case "/conclaves/rapidos" -> {
                        lista.add(erro("ausente", "parâmetro maxEscrutinios ausente", 400, "Parâmetro obrigatório ausente.",
                                e.caminho, Map.of("maxEscrutinios", "O parâmetro 'maxEscrutinios' é obrigatório")));
                        lista.add(erro("minimo", "maxEscrutinios menor que 1", 400, "Parâmetros inválidos. Corrija os campos indicados.",
                                e.caminho, Map.of("maxEscrutinios", "Deve ser maior ou igual a 1")));
                    }
                    case "/concilios/busca" -> {
                        lista.add(erro("ausente", "parâmetro nome ausente", 400, "Parâmetro obrigatório ausente.",
                                e.caminho, Map.of("nome", "O parâmetro 'nome' é obrigatório")));
                        lista.add(erro("vazio", "nome em branco", 400, "Parâmetros inválidos. Corrija os campos indicados.",
                                e.caminho, Map.of("nome", "Informe um texto para a busca")));
                    }
                    default -> {
                    }
                }
                if (e.listagem()) {
                    lista.add(erro("ordenacao", "ordenação por campo inexistente (?sort=xyz)", 400,
                            "Não é possível ordenar por 'xyz': o campo não existe neste recurso.",
                            e.concreto("6"), Map.of("sort", "Campo 'xyz' inexistente")));
                }
                if (e.versionado()) {
                    lista.add(erro("versao", "versão não suportada (X-API-Version: 3)", 400,
                            "A versão '3' não é suportada. Envie no header X-API-Version uma das versões disponíveis: 1 ou 2.",
                            e.concreto("6"), null));
                }
            }
            case 401 -> {
                if (chaves) {
                    lista.add(erro("sem-chave", "chave de admin não informada", 401,
                            "Informe a chave de admin no header X-API-Key.", caminho, null));
                } else {
                    lista.add(erro("sem-chave", "chave de API ausente", 401,
                            "Informe uma chave de API válida no header X-API-Key. Para solicitar uma chave, veja "
                                    + Documentacao.TERMOS_CHAVES, caminho, null));
                    lista.add(erro("invalida", "chave de API inválida ou revogada", 401,
                            "Chave de API inválida ou revogada. Para solicitar uma nova chave, veja "
                                    + Documentacao.TERMOS_CHAVES, caminho, null));
                }
            }
            case 403 -> lista.add(erro("nao-admin", "a chave informada não é a de admin", 403,
                    "Somente o administrador pode gerenciar chaves de API.", caminho, null));
            case 404 -> {
                if (e.caminho.contains("/{id}")) {
                    lista.add(erro("nao-encontrado", singular + (RECURSOS.get(e.recurso).feminino() ? " não encontrada" : " não encontrado"), 404,
                            "Não foi encontrado(a) " + singular + " com id 99", e.concreto("99"), null));
                }
                if (comCorpo && !e.recurso.equals("papas") && !chaves) {
                    lista.add(erro("papa", "o papa informado não existe", 404,
                            "Não foi encontrado(a) papa com id 99", e.concreto(idDoExemplo(e)), null));
                }
                if (e.caminho.equals("/enciclicas/papa/{papaId}")) {
                    lista.add(erro("papa", "o papa informado não existe", 404,
                            "Não foi encontrado(a) papa com id 99", "/enciclicas/papa/99", null));
                }
            }
            case 409 -> {
                if (e.metodo == PathItem.HttpMethod.DELETE) {
                    lista.add(erro("vinculado", "existem registros vinculados", 409,
                            "Operação não permitida: existem registros vinculados a este recurso. "
                                    + "Exclua ou altere os registros vinculados antes.", caminho, null));
                }
                if (comCorpo && e.recurso.equals("papas")) {
                    String repetido = "Já existe um papa com o número de ordem 266: Francisco.";
                    lista.add(erro("numero-repetido", "número de ordem já usado por outro papa", 409, repetido,
                            e.concreto(idDoExemplo(e)), Map.of("numeroOrdem", repetido)));
                    String coincide = "O pontificado informado (2020-01-01 a 2021-01-01) coincide com o de Francisco "
                            + "(2013-03-13 a 2025-04-21). Não pode haver dois papas ao mesmo tempo.";
                    lista.add(erro("pontificado-coincidente", "pontificado ao mesmo tempo que o de outro papa", 409, coincide,
                            e.concreto(idDoExemplo(e)), Map.of("inicioPontificado", coincide)));
                    if (e.metodo == PathItem.HttpMethod.PUT) {
                        String ligado = "A encíclica Laudato Si' (2015-05-24) ficaria fora do novo pontificado "
                                + "(2013-03-13 a 2014-12-31). Altere ou exclua esse registro antes.";
                        lista.add(erro("registro-ligado", "registros ligados ao papa ficariam fora do novo pontificado", 409,
                                ligado, e.concreto(idDoExemplo(e)), Map.of("inicioPontificado", ligado)));
                    }
                }
                if (comCorpo && e.recurso.equals("conclaves")) {
                    lista.add(erro("duplicado", "o papa informado já possui um conclave", 409,
                            "Registro duplicado: já existe um registro com estes dados "
                                    + "(por exemplo, o papa informado já possui um conclave).", e.concreto(idDoExemplo(e)), null));
                }
                if (e.metodo == PathItem.HttpMethod.POST && !chaves) {
                    lista.add(erro("processando", "requisição com a mesma " + IdempotenciaFilter.HEADER + " ainda em processamento", 409,
                            "Já existe uma requisição com esta chave " + IdempotenciaFilter.HEADER + " em processamento.", e.caminho, null));
                }
            }
            case 422 -> lista.add(erro("reutilizada", IdempotenciaFilter.HEADER + " já usada com outro corpo", 422,
                    "A chave " + IdempotenciaFilter.HEADER + " informada já foi usada com uma requisição diferente.", e.caminho, null));
            case 429 -> {
                lista.add(erro("limite", "limite de requisições por minuto excedido", 429,
                        "Limite de 30 requisições por minuto excedido. Tente novamente em 2 segundos. Limites de uso: "
                                + Documentacao.TERMOS_LIMITES, e.concreto("6"), null));
                if (e.contaNaCota()) {
                    lista.add(erro("cota", "cota mensal do plano esgotada", 429,
                            "Cota mensal de 3000 requisições do plano Gratuito esgotada. A cota renova em 01/12/2026. "
                                    + "Para aumentar a cota, veja os planos em GET /planos e " + Documentacao.TERMOS_PLANOS,
                            e.concreto("6"), null));
                }
            }
            default -> {
            }
        }
        return lista;
    }

    /** 400 das regras históricas: a data precisa combinar com o pontificado do papa ligado ao registro. */
    private static Exemplo foraDoPontificado(Endpoint e) {
        String caminho = e.concreto(idDoExemplo(e));
        return switch (e.recurso) {
            case "enciclicas" -> erro("fora-do-pontificado", "data fora do pontificado do papa", 400,
                    "Dados inválidos. Corrija os campos indicados.", caminho, Map.of("dataPublicacao",
                            "A data de publicação (1990-01-01) precisa estar dentro do pontificado de Francisco (2013-03-13 a 2025-04-21)"));
            case "santos" -> erro("fora-do-pontificado", "data fora do pontificado do papa", 400,
                    "Dados inválidos. Corrija os campos indicados.", caminho, Map.of("dataCanonizacao",
                            "A data de canonização (1990-01-01) precisa estar dentro do pontificado de Francisco (2013-03-13 a 2025-04-21)"));
            case "conclaves" -> erro("fora-do-pontificado", "conclave não combina com o início do pontificado", 400,
                    "Dados inválidos. Corrija os campos indicados.", caminho, Map.of("dataFim",
                            "O conclave deve terminar no dia em que começa o pontificado de Paulo VI (1963-06-21) ou até 30 dias antes"));
            case "concilios" -> erro("fora-do-pontificado", "papa que não era papa durante o concílio", 400,
                    "Dados inválidos. Corrija os campos indicados.", caminho, Map.of("papas[1]",
                            "Francisco (2013-03-13 a 2025-04-21) não era papa durante o concílio (1962-10-11 a 1965-12-08)"));
            default -> null;
        };
    }

    /** Id do registro mostrado nos exemplos (o mesmo do corpo de sucesso, do header Location e dos PUT). */
    private static String idDoExemplo(Endpoint e) {
        return switch (e.recurso) {
            case "conclaves" -> "3";
            case "enciclicas" -> "5";
            case "concilios" -> "2";
            case "santos" -> "4";
            case "chaves-api" -> "1";
            default -> "6";
        };
    }

    private static Map<String, String> camposInvalidos(String recurso) {
        return switch (recurso) {
            case "papas" -> Map.of("nomePapal", "O nome papal é obrigatório",
                    "fimPontificado", "Informe a data de fim do pontificado para a situação RENUNCIA");
            case "conclaves" -> Map.of("numeroCardeais", "O número de cardeais é obrigatório",
                    "papa", "O papa eleito é obrigatório");
            case "enciclicas" -> Map.of("titulo", "O título é obrigatório",
                    "dataPublicacao", "A data de publicação não pode estar no futuro");
            case "concilios" -> Map.of("nome", "O nome é obrigatório", "papas", "Informe pelo menos um papa");
            case "santos" -> Map.of("nome", "O nome é obrigatório", "canonizadoPor", "O papa que canonizou é obrigatório");
            default -> Map.of("nome", "O nome é obrigatório");
        };
    }

    private static Map<String, String> campoComFormatoInvalido(String recurso) {
        String data = "Valor '13/03/2013' inválido: use uma data no formato AAAA-MM-DD";
        return switch (recurso) {
            case "papas" -> Map.of("situacao", "Valor 'XYZ' inválido: use um dos valores EM_EXERCICIO, FALECIMENTO, RENUNCIA");
            case "conclaves", "concilios" -> Map.of("dataInicio", data);
            case "enciclicas" -> Map.of("dataPublicacao", data);
            case "santos" -> Map.of("dataCanonizacao", data);
            case "chaves-api" -> Map.of("plano", "Valor 'XYZ' inválido: use um dos valores GRATUITO, BASICO, PRO");
            default -> null;
        };
    }

    private static Exemplo erro(String id, String resumo, int status, String mensagem, String caminho, Map<String, String> campos) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", MOMENTO);
        corpo.put("status", status);
        corpo.put("erro", org.springframework.http.HttpStatus.valueOf(status).getReasonPhrase());
        corpo.put("mensagem", mensagem);
        corpo.put("caminho", caminho.replaceAll("\\?.*", ""));
        if (campos != null) {
            corpo.put("campos", new TreeMap<>(campos));
        }
        return new Exemplo(id, resumo, corpo);
    }

    /** "dados inválidos", "JSON malformado" -> "Dados inválidos ou JSON malformado". */
    private static String descricao(List<Exemplo> exemplos) {
        List<String> resumos = exemplos.stream().map(Exemplo::resumo).toList();
        String texto = resumos.size() == 1 ? resumos.get(0)
                : String.join(", ", resumos.subList(0, resumos.size() - 1)) + " ou " + resumos.get(resumos.size() - 1);
        return maiuscula(texto);
    }

    private static String maiuscula(String texto) {
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static Header header(String descricao, Schema<?> schema) {
        return new Header().description(descricao).schema(schema);
    }

    // ------------------------------------------------------------ endpoint

    private static String recursoDe(String caminho) {
        String semBarra = caminho.substring(1);
        int fim = semBarra.indexOf('/');
        return fim < 0 ? semBarra : semBarra.substring(0, fim);
    }

    private static final class Endpoint {
        final PathItem.HttpMethod metodo;
        final String caminho;
        final String recurso;
        final String versao;

        Endpoint(PathItem.HttpMethod metodo, String caminho, String versao) {
            this.metodo = metodo;
            this.caminho = caminho;
            this.recurso = recursoDe(caminho);
            this.versao = versao;
        }

        /** GET que devolve uma página: a coleção ou uma consulta personalizada (tudo que não é /{id}). */
        boolean listagem() {
            return metodo == PathItem.HttpMethod.GET && !caminho.endsWith("/{id}") && !recurso.equals("planos");
        }

        /** Requisições que gastam a cota mensal da chave (as de admin e a tabela de planos não gastam). */
        boolean contaNaCota() {
            return !recurso.equals("chaves-api") && !recurso.equals("planos");
        }

        /** Endpoints com versão 1 e 2 (consultas de papas). */
        boolean versionado() {
            return metodo == PathItem.HttpMethod.GET && (caminho.equals("/papas") || caminho.equals("/papas/{id}"));
        }

        /** Troca as variáveis do caminho por valores de exemplo: /papas/{id} -> /papas/6. */
        String concreto(String id) {
            return caminho.replace("{id}", id).replace("{situacao}", "RENUNCIA")
                    .replace("{papaId}", "6").replace("{pais}", "Brasil");
        }
    }

    // ------------------------------------------------------------ construção dos dados

    private static void papa(long id, String nome, String batismo, int ordem, String pais, String inicio, String fim,
                             SituacaoPontificado situacao) {
        Papa p = new Papa(nome, batismo, ordem, pais, LocalDate.parse(inicio), fim == null ? null : LocalDate.parse(fim), situacao);
        p.setId(id);
        PAPAS.put(id, p);
    }

    private static void conclave(long id, String inicio, String fim, int cardeais, int escrutinios, long papa) {
        Conclave c = new Conclave(LocalDate.parse(inicio), LocalDate.parse(fim), cardeais, escrutinios, PAPAS.get(papa));
        c.setId(id);
        CONCLAVES.put(id, c);
    }

    private static void enciclica(long id, String titulo, String traducao, String data, long papa) {
        Enciclica en = new Enciclica(titulo, traducao, LocalDate.parse(data), PAPAS.get(papa));
        en.setId(id);
        ENCICLICAS.put(id, en);
    }

    private static void concilio(long id, String nome, String inicio, String fim, long... papas) {
        List<Papa> lista = new ArrayList<>();
        for (long p : papas) {
            lista.add(PAPAS.get(p));
        }
        Concilio c = new Concilio(nome, "Basílica de São Pedro, Vaticano", LocalDate.parse(inicio), LocalDate.parse(fim), lista);
        c.setId(id);
        CONCILIOS.put(id, c);
    }

    private static void santo(long id, String nome, String pais, String data, long papa) {
        Santo s = new Santo(nome, pais, LocalDate.parse(data), PAPAS.get(papa));
        s.setId(id);
        SANTOS.put(id, s);
    }
}
