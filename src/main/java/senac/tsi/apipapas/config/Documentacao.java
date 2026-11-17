package senac.tsi.apipapas.config;

/** Endereços dos documentos do projeto, referenciados pelo Swagger e pelas mensagens de erro. */
public final class Documentacao {

    public static final String REPOSITORIO = "https://github.com/enzobariotto/APIPapas";
    public static final String TERMOS_DE_USO = REPOSITORIO + "/blob/main/TERMOS_DE_USO.md";
    public static final String TERMOS_CHAVES = TERMOS_DE_USO + "#3-chaves-de-api";
    public static final String TERMOS_LIMITES = TERMOS_DE_USO + "#4-limites-de-uso";
    public static final String TERMOS_PLANOS = TERMOS_DE_USO + "#planos-e-preços";
    public static final String TERMOS_VERSOES = TERMOS_DE_USO + "#6-versões-da-api";
    public static final String LICENCA = "https://opensource.org/license/mit";

    private Documentacao() {
    }
}
