package senac.tsi.apipapas.exception;

/** Dado de entrada inválido detectado no controller (fora do Bean Validation). Vira 400 com o campo indicado. */
public class DadosInvalidosException extends RuntimeException {

    private final String campo;

    public DadosInvalidosException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
