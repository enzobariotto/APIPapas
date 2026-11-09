package senac.tsi.apipapas.seguranca;

import senac.tsi.apipapas.model.ChaveApi;
import senac.tsi.apipapas.repository.ChaveApiRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class ChaveApiService {

    private final ChaveApiRepository repository;

    ChaveApiService(ChaveApiRepository repository) {
        this.repository = repository;
    }

    /** Gera uma nova chave. O valor completo só existe neste retorno: no banco fica apenas o hash. */
    public ChaveGerada gerar(String nome) {
        String valor = "papas_" + GeradorChave.aleatorio();
        ChaveApi chave = repository.save(new ChaveApi(nome, valor.substring(0, 11), GeradorChave.sha256(valor)));
        return new ChaveGerada(chave, valor);
    }

    /** Devolve a chave se ela existir e estiver ativa, registrando a data do último uso. */
    public Optional<ChaveApi> validar(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        Optional<ChaveApi> chave = repository.findByHashAndAtivaTrue(GeradorChave.sha256(valor));
        chave.ifPresent(c -> {
            c.setUltimoUsoEm(Instant.now());
            repository.save(c);
        });
        return chave;
    }

    /** Apenas identifica a chave (usado pelo rate limiting), sem registrar uso. */
    public Optional<ChaveApi> identificar(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        return repository.findByHashAndAtivaTrue(GeradorChave.sha256(valor));
    }

    public record ChaveGerada(ChaveApi chave, String valor) {
    }
}
