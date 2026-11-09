package senac.tsi.apipapas.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Tratamento global de exceções: converte cada erro num status HTTP apropriado e numa resposta
 * no formato ErroApi, com mensagem em português explicando o problema.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // ------------------------------------------------------------------ 404

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ErroApi> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErroApi> rotaInexistente(NoResourceFoundException ex, HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, "A rota " + request.getMethod() + " " + request.getRequestURI()
                + " não existe. Consulte a documentação em /swagger-ui.html.", request);
    }

    // ------------------------------------------------------------------ 400: corpo da requisição

    /** Bean Validation do corpo (@Valid): devolve a mensagem de cada campo inválido. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroApi> validacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> campos = new TreeMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.merge(erro.getField(), erro.getDefaultMessage(), (a, b) -> a + "; " + b);
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(erro -> campos.merge(erro.getObjectName(), erro.getDefaultMessage(), (a, b) -> a + "; " + b));
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos. Corrija os campos indicados.", request, campos);
    }

    @ExceptionHandler(DadosInvalidosException.class)
    ResponseEntity<ErroApi> dadosInvalidos(DadosInvalidosException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos. Corrija os campos indicados.", request,
                Map.of(ex.getCampo(), ex.getMessage()));
    }

    /** Corpo ausente, JSON malformado ou valor com tipo errado (texto em campo numérico, enum ou data inválidos). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroApi> corpoIlegivel(HttpMessageNotReadableException ex, HttpServletRequest request) {
        Throwable causa = ex.getCause();
        if (causa instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            String campo = caminhoDoCampo(mismatch);
            return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos. Corrija os campos indicados.", request,
                    Map.of(campo, mensagemDeTipo(mismatch)));
        }
        if (causa instanceof StreamReadException || causa instanceof JacksonException) {
            return resposta(HttpStatus.BAD_REQUEST,
                    "JSON malformado. Verifique a sintaxe do corpo da requisição (aspas, vírgulas e chaves).", request);
        }
        return resposta(HttpStatus.BAD_REQUEST, "O corpo da requisição é obrigatório e deve ser um JSON válido.", request);
    }

    // ------------------------------------------------------------------ 400: parâmetros da URL e headers

    /** Path variable ou query param com tipo errado, ex.: /papas/abc ou /papas/situacao/XYZ. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErroApi> tipoDoParametro(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        Class<?> tipo = ex.getRequiredType();
        String esperado;
        if (tipo != null && tipo.isEnum()) {
            esperado = "um dos valores " + valoresDoEnum(tipo);
        } else if (tipo != null && Number.class.isAssignableFrom(tipo)) {
            esperado = "um número inteiro";
        } else {
            esperado = "do tipo " + (tipo != null ? tipo.getSimpleName() : "esperado");
        }
        return resposta(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + ex.getName() + "'.", request,
                Map.of(ex.getName(), "Valor '" + ex.getValue() + "' inválido: deve ser " + esperado));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErroApi> parametroAusente(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente.", request,
                Map.of(ex.getParameterName(), "O parâmetro '" + ex.getParameterName() + "' é obrigatório"));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ErroApi> headerAusente(MissingRequestHeaderException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "O header " + ex.getHeaderName() + " é obrigatório.", request);
    }

    /** Bean Validation nos parâmetros dos métodos (ex.: @Min em query param). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErroApi> validacaoDeParametros(HandlerMethodValidationException ex, HttpServletRequest request) {
        Map<String, String> campos = new TreeMap<>();
        ex.getParameterValidationResults().forEach(resultado -> {
            String nome = resultado.getMethodParameter().getParameterName();
            resultado.getResolvableErrors().forEach(erro ->
                    campos.merge(nome != null ? nome : "parametro", erro.getDefaultMessage(), (a, b) -> a + "; " + b));
        });
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetros inválidos. Corrija os campos indicados.", request, campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErroApi> restricao(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> campos = new TreeMap<>();
        ex.getConstraintViolations().forEach(v ->
                campos.merge(v.getPropertyPath().toString(), v.getMessage(), (a, b) -> a + "; " + b));
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos. Corrija os campos indicados.", request, campos);
    }

    /** Ordenação por propriedade inexistente, ex.: ?sort=xyz. */
    @ExceptionHandler(PropertyReferenceException.class)
    ResponseEntity<ErroApi> ordenacaoInvalida(PropertyReferenceException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Não é possível ordenar por '" + ex.getPropertyName()
                + "': o campo não existe neste recurso.", request, Map.of("sort", "Campo '" + ex.getPropertyName() + "' inexistente"));
    }

    @ExceptionHandler(InvalidApiVersionException.class)
    ResponseEntity<ErroApi> versaoInvalida(InvalidApiVersionException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "A versão '" + request.getHeader("X-API-Version")
                + "' não é suportada. Envie no header X-API-Version uma das versões disponíveis: 1 ou 2.", request);
    }

    // ------------------------------------------------------------------ 405, 406, 415

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErroApi> metodoNaoPermitido(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String permitidos = ex.getSupportedHttpMethods() == null ? "" : ex.getSupportedHttpMethods().stream()
                .map(Object::toString).collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(ex.getHeaders())
                .body(ErroApi.de(HttpStatus.METHOD_NOT_ALLOWED, "O método " + ex.getMethod()
                        + " não é permitido nesta rota. Métodos permitidos: " + permitidos + ".", request));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErroApi> tipoNaoSuportado(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .headers(ex.getHeaders())
                .body(ErroApi.de(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type '" + ex.getContentType()
                        + "' não suportado. Envie o corpo como JSON com o header Content-Type: application/json.", request));
    }

    /** O cliente não aceita nenhum formato que a API produz; por isso a resposta vai sem corpo. */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<Void> naoAceitavel(HttpMediaTypeNotAcceptableException ex) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).headers(ex.getHeaders()).build();
    }

    // ------------------------------------------------------------------ 409

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErroApi> conflito(DataIntegrityViolationException ex, HttpServletRequest request) {
        ConstraintKind tipo = ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException cv
                ? cv.getKind() : ConstraintKind.OTHER;
        String mensagem = switch (tipo) {
            case UNIQUE -> "Registro duplicado: já existe um registro com estes dados "
                    + "(por exemplo, o papa informado já possui um conclave).";
            case FOREIGN_KEY -> "Operação não permitida: existem registros vinculados a este recurso. "
                    + "Exclua ou altere os registros vinculados antes.";
            default -> "Operação não permitida: os dados entram em conflito com registros existentes.";
        };
        return resposta(HttpStatus.CONFLICT, mensagem, request);
    }

    // ------------------------------------------------------------------ demais erros

    /**
     * Erro não previsto: registra no log e devolve 500 sem expor detalhes internos.
     * Exceções do Spring que já carregam um status HTTP (ErrorResponse) mantêm esse status.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroApi> inesperado(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse erro) {
            HttpStatus status = HttpStatus.valueOf(erro.getStatusCode().value());
            String detalhe = erro.getBody().getDetail();
            return ResponseEntity.status(status).headers(erro.getHeaders())
                    .body(ErroApi.de(status, detalhe != null ? detalhe : status.getReasonPhrase(), request));
        }
        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno inesperado. Tente novamente; se persistir, entre em contato com o responsável pela API.", request);
    }

    // ------------------------------------------------------------------ auxiliares

    private static ResponseEntity<ErroApi> resposta(HttpStatus status, String mensagem, HttpServletRequest request) {
        return resposta(status, mensagem, request, null);
    }

    private static ResponseEntity<ErroApi> resposta(HttpStatus status, String mensagem, HttpServletRequest request,
                                                    Map<String, String> campos) {
        return ResponseEntity.status(status).body(ErroApi.de(status, mensagem, request, campos));
    }

    /** Monta o nome do campo a partir do caminho do Jackson, ex.: "papa.id" ou "papas[0].id". */
    private static String caminhoDoCampo(JacksonException ex) {
        StringBuilder campo = new StringBuilder();
        for (JacksonException.Reference ref : ex.getPath()) {
            if (ref.getPropertyName() != null) {
                if (!campo.isEmpty()) {
                    campo.append('.');
                }
                campo.append(ref.getPropertyName());
            } else if (ref.getIndex() >= 0) {
                campo.append('[').append(ref.getIndex()).append(']');
            }
        }
        return campo.isEmpty() ? "corpo" : campo.toString();
    }

    private static String mensagemDeTipo(MismatchedInputException ex) {
        Class<?> tipo = ex.getTargetType();
        String recebido = ex instanceof InvalidFormatException formato ? "Valor '" + formato.getValue() + "' inválido: "
                : "Valor inválido: ";
        if (tipo == null) {
            return recebido + "tipo incompatível";
        }
        if (tipo.isEnum()) {
            return recebido + "use um dos valores " + valoresDoEnum(tipo);
        }
        if (tipo == LocalDate.class) {
            return recebido + "use uma data no formato AAAA-MM-DD";
        }
        if (Number.class.isAssignableFrom(tipo) || tipo == int.class || tipo == long.class) {
            return recebido + "deve ser um número inteiro";
        }
        if (tipo == String.class) {
            return recebido + "deve ser um texto";
        }
        return recebido + "deve ser um objeto/valor do tipo " + tipo.getSimpleName()
                + (tipo.getSimpleName().equals("Papa") ? ", ex.: {\"id\": 6}" : "");
    }

    private static String valoresDoEnum(Class<?> tipo) {
        return Arrays.stream(tipo.getEnumConstants()).map(Object::toString).collect(Collectors.joining(", "));
    }
}
