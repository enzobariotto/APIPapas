package senac.tsi.apipapas.idempotencia;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import senac.tsi.apipapas.model.RegistroIdempotencia;
import senac.tsi.apipapas.repository.RegistroIdempotenciaRepository;
import senac.tsi.apipapas.seguranca.ApiKeyFilter;
import senac.tsi.apipapas.seguranca.GeradorChave;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Idempotência nos POSTs dos recursos (papas, conclaves, encíclicas, concílios e santos).
 *
 * O cliente envia o header X-Idempotency-Key com um valor único (ex.: um UUID) por operação:
 * - primeira vez: a requisição é processada e a resposta de sucesso fica guardada por 24 horas;
 * - repetição com a mesma chave e o mesmo corpo: a resposta guardada é devolvida sem processar de novo
 *   (com o header Idempotency-Replayed: true);
 * - mesma chave com outro corpo: 422;
 * - mesma chave enquanto a primeira requisição ainda está em processamento: 409.
 * Respostas de erro não são guardadas, então o cliente pode corrigir os dados e tentar de novo com a mesma chave.
 * Roda depois do ApiKeyFilter, para que cada cliente tenha o seu próprio espaço de chaves.
 */
@Component
@Order(3)
public class IdempotenciaFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Idempotency-Key";
    public static final String HEADER_REPETIDA = "Idempotency-Replayed";
    private static final int TAMANHO_MAXIMO_CHAVE = 100;
    private static final Duration VALIDADE = Duration.ofHours(24);

    private final RegistroIdempotenciaRepository repository;

    IdempotenciaFilter(RegistroIdempotenciaRepository repository) {
        this.repository = repository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !ApiKeyFilter.ehRecurso(ApiKeyFilter.caminho(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String chave = request.getHeader(HEADER);
        if (chave == null || chave.isBlank()) {
            ApiKeyFilter.erro(response, HttpServletResponse.SC_BAD_REQUEST,
                    "O header " + HEADER + " é obrigatório em requisições POST.");
            return;
        }
        if (chave.length() > TAMANHO_MAXIMO_CHAVE) {
            ApiKeyFilter.erro(response, HttpServletResponse.SC_BAD_REQUEST,
                    "O header " + HEADER + " deve ter no máximo " + TAMANHO_MAXIMO_CHAVE + " caracteres.");
            return;
        }

        CorpoEmCacheRequest requisicao = new CorpoEmCacheRequest(request);
        String cliente = String.valueOf(request.getAttribute(ApiKeyFilter.ATRIBUTO_CLIENTE));
        String hash = hashRequisicao(requisicao);

        Optional<RegistroIdempotencia> existente = repository.findByClienteAndChave(cliente, chave);
        if (existente.isPresent() && expirado(existente.get())) {
            repository.delete(existente.get());
            repository.flush();
            existente = Optional.empty();
        }
        if (existente.isPresent()) {
            RegistroIdempotencia registro = existente.get();
            if (!registro.getHashRequisicao().equals(hash)) {
                ApiKeyFilter.erro(response, 422,
                        "A chave " + HEADER + " informada já foi usada com uma requisição diferente.");
            } else if (registro.getSituacao() == RegistroIdempotencia.Situacao.PROCESSANDO) {
                ApiKeyFilter.erro(response, HttpServletResponse.SC_CONFLICT,
                        "Já existe uma requisição com esta chave " + HEADER + " em processamento.");
            } else {
                repetirResposta(registro, response);
            }
            return;
        }

        RegistroIdempotencia registro;
        try {
            registro = repository.saveAndFlush(new RegistroIdempotencia(cliente, chave, hash));
        } catch (DataIntegrityViolationException e) {
            // Outra requisição com a mesma chave chegou ao mesmo tempo e registrou primeiro.
            ApiKeyFilter.erro(response, HttpServletResponse.SC_CONFLICT,
                    "Já existe uma requisição com esta chave " + HEADER + " em processamento.");
            return;
        }

        ContentCachingResponseWrapper resposta = new ContentCachingResponseWrapper(response);
        boolean guardado = false;
        try {
            chain.doFilter(requisicao, resposta);
            int status = resposta.getStatus();
            if (status >= 200 && status < 300) {
                registro.concluir(status, resposta.getContentType(), resposta.getHeader(HttpHeaders.LOCATION),
                        new String(resposta.getContentAsByteArray(), StandardCharsets.UTF_8));
                repository.save(registro);
                guardado = true;
            }
        } finally {
            if (!guardado) {
                repository.deleteById(registro.getId());
            }
            resposta.copyBodyToResponse();
        }
    }

    private static boolean expirado(RegistroIdempotencia registro) {
        return registro.getCriadoEm().plus(VALIDADE).isBefore(Instant.now());
    }

    private static String hashRequisicao(CorpoEmCacheRequest request) throws IOException {
        ByteArrayOutputStream dados = new ByteArrayOutputStream();
        dados.write((request.getMethod() + " " + request.getRequestURI() + "\n").getBytes(StandardCharsets.UTF_8));
        dados.write(request.getCorpo());
        return GeradorChave.sha256(dados.toByteArray());
    }

    private static void repetirResposta(RegistroIdempotencia registro, HttpServletResponse response) throws IOException {
        response.setStatus(registro.getStatusHttp());
        response.setHeader(HEADER_REPETIDA, "true");
        if (registro.getLocation() != null) {
            response.setHeader(HttpHeaders.LOCATION, registro.getLocation());
        }
        if (registro.getContentType() != null) {
            response.setContentType(registro.getContentType());
        }
        if (registro.getCorpoResposta() != null) {
            response.getOutputStream().write(registro.getCorpoResposta().getBytes(StandardCharsets.UTF_8));
        }
    }
}
