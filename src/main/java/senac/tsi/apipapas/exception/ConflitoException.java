package senac.tsi.apipapas.exception;

/**
 * Os dados são válidos, mas entram em conflito com registros que já existem (ex.: número de ordem repetido,
 * dois papas ao mesmo tempo). Vira 409 com o campo indicado.
 */
public class ConflitoException extends RuntimeException {

    private final String campo;

    public ConflitoException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
