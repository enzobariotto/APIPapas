package senac.tsi.apipapas.seguranca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Chave mestra do administrador, lida da variável de ambiente API_ADMIN_KEY.
 * Se ela não estiver definida, uma chave aleatória é gerada e exibida no log da inicialização.
 */
@Component
public class ChaveAdmin {

    private static final Logger log = LoggerFactory.getLogger(ChaveAdmin.class);

    private final byte[] valor;

    ChaveAdmin(@Value("${app.seguranca.admin-key:}") String configurada) {
        String chave = configurada;
        if (chave == null || chave.isBlank()) {
            chave = "admin_" + GeradorChave.aleatorio();
            log.warn("API_ADMIN_KEY não definida. Chave de admin gerada para esta execução: {}", chave);
        }
        this.valor = chave.getBytes(StandardCharsets.UTF_8);
    }

    public boolean confere(String chave) {
        return chave != null && MessageDigest.isEqual(valor, chave.getBytes(StandardCharsets.UTF_8));
    }
}
