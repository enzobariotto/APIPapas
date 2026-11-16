package senac.tsi.apipapas.validacao;

import org.springframework.stereotype.Component;
import senac.tsi.apipapas.exception.ConflitoException;
import senac.tsi.apipapas.exception.DadosInvalidosException;
import senac.tsi.apipapas.model.Concilio;
import senac.tsi.apipapas.model.Conclave;
import senac.tsi.apipapas.model.Enciclica;
import senac.tsi.apipapas.model.Papa;
import senac.tsi.apipapas.model.Santo;
import senac.tsi.apipapas.repository.ConcilioRepository;
import senac.tsi.apipapas.repository.ConclaveRepository;
import senac.tsi.apipapas.repository.EnciclicaRepository;
import senac.tsi.apipapas.repository.PapaRepository;
import senac.tsi.apipapas.repository.SantoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Regras de coerência histórica, que dependem de outros registros (por isso não cabem no Bean Validation):
 * - não há dois papas com o mesmo número de ordem nem dois pontificados ao mesmo tempo (409);
 * - encíclicas e canonizações acontecem durante o pontificado do papa (400);
 * - o conclave termina no início do pontificado do papa eleito (até 30 dias antes) (400);
 * - cada papa de um concílio era papa durante o concílio (400);
 * - ao alterar um papa, os registros ligados a ele precisam continuar coerentes (409).
 */
@Component
public class RegrasHistoricas {

    /** Máximo de dias entre o fim do conclave e o início do pontificado do papa eleito. */
    static final int DIAS_ENTRE_CONCLAVE_E_PONTIFICADO = 30;

    private final PapaRepository papas;
    private final EnciclicaRepository enciclicas;
    private final SantoRepository santos;
    private final ConclaveRepository conclaves;
    private final ConcilioRepository concilios;

    RegrasHistoricas(PapaRepository papas, EnciclicaRepository enciclicas, SantoRepository santos,
                     ConclaveRepository conclaves, ConcilioRepository concilios) {
        this.papas = papas;
        this.enciclicas = enciclicas;
        this.santos = santos;
        this.conclaves = conclaves;
        this.concilios = concilios;
    }

    // ------------------------------------------------------------ papa

    /** Cadastro (id nulo) ou alteração (id do papa alterado) de um papa. */
    public void validarPapa(Papa dados, Long id) {
        for (Papa outro : papas.findAll()) {
            if (outro.getId().equals(id)) {
                continue;
            }
            if (outro.getNumeroOrdem().equals(dados.getNumeroOrdem())) {
                throw new ConflitoException("numeroOrdem", "Já existe um papa com o número de ordem "
                        + dados.getNumeroOrdem() + ": " + outro.getNomePapal() + ".");
            }
            if (!dados.getInicioPontificado().isAfter(fim(outro)) && !outro.getInicioPontificado().isAfter(fim(dados))) {
                throw new ConflitoException("inicioPontificado", "O pontificado informado (" + periodo(dados)
                        + ") coincide com o de " + outro.getNomePapal() + " (" + periodo(outro)
                        + "). Não pode haver dois papas ao mesmo tempo.");
            }
        }
        if (id != null) {
            validarRegistrosLigados(dados, id);
        }
    }

    /** Ao mudar as datas de um papa, nada do que está ligado a ele pode ficar fora do novo pontificado. */
    private void validarRegistrosLigados(Papa dados, Long id) {
        String aviso = " ficaria fora do novo pontificado (" + periodo(dados) + "). Altere ou exclua esse registro antes.";
        for (Enciclica e : enciclicas.findByPapaId(id)) {
            if (!dentro(dados, e.getDataPublicacao())) {
                throw new ConflitoException("inicioPontificado", "A encíclica " + e.getTitulo()
                        + " (" + e.getDataPublicacao() + ")" + aviso);
            }
        }
        for (Santo s : santos.findByCanonizadoPorId(id)) {
            if (!dentro(dados, s.getDataCanonizacao())) {
                throw new ConflitoException("inicioPontificado", "A canonização de " + s.getNome()
                        + " (" + s.getDataCanonizacao() + ")" + aviso);
            }
        }
        conclaves.findByPapaId(id).ifPresent(c -> {
            if (!conclaveCombina(c.getDataFim(), dados)) {
                throw new ConflitoException("inicioPontificado", "O conclave que elegeu este papa terminou em "
                        + c.getDataFim() + ". O pontificado deve começar nesse dia ou até "
                        + DIAS_ENTRE_CONCLAVE_E_PONTIFICADO + " dias depois. Altere ou exclua o conclave antes.");
            }
        });
        for (Concilio c : concilios.findByPapasId(id)) {
            if (!coincide(dados, c.getDataInicio(), c.getDataFim())) {
                throw new ConflitoException("inicioPontificado", "O papa participou do " + c.getNome()
                        + " (" + c.getDataInicio() + " a " + c.getDataFim() + "), que" + aviso);
            }
        }
    }

    // ------------------------------------------------------------ registros ligados a um papa

    /** A data (de publicação, de canonização...) precisa estar dentro do pontificado do papa. */
    public void validarDentroDoPontificado(Papa papa, LocalDate data, String campo, String descricao) {
        if (data != null && !dentro(papa, data)) {
            throw new DadosInvalidosException(campo, descricao + " (" + data + ") precisa estar dentro do pontificado de "
                    + papa.getNomePapal() + " (" + periodo(papa) + ")");
        }
    }

    public void validarConclave(Conclave conclave, Papa eleito) {
        if (conclave.getDataFim() != null && !conclaveCombina(conclave.getDataFim(), eleito)) {
            throw new DadosInvalidosException("dataFim", "O conclave deve terminar no dia em que começa o pontificado de "
                    + eleito.getNomePapal() + " (" + eleito.getInicioPontificado() + ") ou até "
                    + DIAS_ENTRE_CONCLAVE_E_PONTIFICADO + " dias antes");
        }
    }

    public void validarConcilio(Concilio concilio, List<Papa> participantes) {
        if (concilio.getDataInicio() == null || concilio.getDataFim() == null) {
            return;
        }
        for (int i = 0; i < participantes.size(); i++) {
            Papa papa = participantes.get(i);
            if (!coincide(papa, concilio.getDataInicio(), concilio.getDataFim())) {
                throw new DadosInvalidosException("papas[" + i + "]", papa.getNomePapal() + " (" + periodo(papa)
                        + ") não era papa durante o concílio (" + concilio.getDataInicio() + " a " + concilio.getDataFim() + ")");
            }
        }
        if (participantes.stream().map(Papa::getId).distinct().count() < participantes.size()) {
            throw new DadosInvalidosException("papas", "O mesmo papa foi informado mais de uma vez");
        }
    }

    // ------------------------------------------------------------ auxiliares

    /** Fim do pontificado; para o papa em exercício, hoje. */
    private static LocalDate fim(Papa papa) {
        return Objects.requireNonNullElseGet(papa.getFimPontificado(), LocalDate::now);
    }

    private static boolean dentro(Papa papa, LocalDate data) {
        return !data.isBefore(papa.getInicioPontificado()) && !data.isAfter(fim(papa));
    }

    private static boolean coincide(Papa papa, LocalDate inicio, LocalDate fim) {
        return !papa.getInicioPontificado().isAfter(fim) && !inicio.isAfter(fim(papa));
    }

    private static boolean conclaveCombina(LocalDate fimDoConclave, Papa eleito) {
        LocalDate inicio = eleito.getInicioPontificado();
        return !fimDoConclave.isAfter(inicio) && !fimDoConclave.isBefore(inicio.minusDays(DIAS_ENTRE_CONCLAVE_E_PONTIFICADO));
    }

    private static String periodo(Papa papa) {
        return papa.getFimPontificado() == null
                ? "desde " + papa.getInicioPontificado()
                : papa.getInicioPontificado() + " a " + papa.getFimPontificado();
    }
}
