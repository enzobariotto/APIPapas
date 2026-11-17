package senac.tsi.apipapas.seguranca;

import senac.tsi.apipapas.model.ChaveApi;
import senac.tsi.apipapas.model.Plano;
import senac.tsi.apipapas.repository.ChaveApiRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class ChaveApiService {

    private final ChaveApiRepository repository;
    private final Planos planos;

    ChaveApiService(ChaveApiRepository repository, Planos planos) {
        this.repository = repository;
        this.planos = planos;
    }

    /** Gera uma nova chave. O valor completo só existe neste retorno: no banco fica apenas o hash. */
    public ChaveGerada gerar(String nome, Plano plano) {
        String valor = "papas_" + GeradorChave.aleatorio();
        ChaveApi chave = repository.save(new ChaveApi(nome, valor.substring(0, 11), GeradorChave.sha256(valor), plano));
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

    /**
     * Conta uma requisição na cota mensal da chave. Sincronizado para que requisições simultâneas da mesma
     * chave não se percam na contagem (ler, somar e gravar acontecem juntos).
     */
    public synchronized Cota consumirCota(Long chaveId) {
        ChaveApi chave = repository.findById(chaveId).orElseThrow();
        long cotaMensal = planos.de(chave.getPlano()).cotaMensal();
        boolean liberada = chave.consumirCota(cotaMensal);
        if (liberada) {
            repository.save(chave);
        }
        return new Cota(liberada, chave.getPlano(), cotaMensal, chave.getRequisicoesNoMes());
    }

    /** Resultado da contagem: se a requisição foi liberada e quanto da cota do mês já foi usado. */
    public record Cota(boolean liberada, Plano plano, long cotaMensal, long usadas) {

        public long restantes() {
            return Math.max(0, cotaMensal - usadas);
        }
    }

    public record ChaveGerada(ChaveApi chave, String valor) {
    }
}
