package senac.tsi.apipapas.exception;

public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Long id) {
        super("Não foi encontrado(a) " + recurso + " com id " + id);
    }
}
