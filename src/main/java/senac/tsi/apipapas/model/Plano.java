package senac.tsi.apipapas.model;

/**
 * Plano de uso de uma chave de API. Preço, limite por minuto e cota mensal de cada plano ficam em
 * application.properties (app.planos.*) e são lidos pela classe Planos.
 */
public enum Plano {

    GRATUITO("Gratuito"),
    BASICO("Básico"),
    PRO("Pro");

    private final String nome;

    Plano(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }
}
