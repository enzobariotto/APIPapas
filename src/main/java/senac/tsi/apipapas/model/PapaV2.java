package senac.tsi.apipapas.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Representação do papa na versão 2 da API (X-API-Version: 2).
 * Agrupa os nomes e os dados do pontificado e acrescenta campos calculados.
 */
@Schema(name = "PapaV2", description = "Papa na versão 2 da API")
public record PapaV2(
        Long id,
        Nome nome,
        @Schema(example = "266") Integer numeroOrdem,
        @Schema(example = "Argentina") String paisNascimento,
        Pontificado pontificado) {

    public record Nome(
            @Schema(example = "Francisco") String papal,
            @Schema(example = "Jorge Mario Bergoglio") String batismo) {
    }

    public record Pontificado(
            @Schema(example = "2013-03-13") LocalDate inicio,
            @Schema(example = "2025-04-21") LocalDate fim,
            @Schema(example = "FALECIMENTO") SituacaoPontificado situacao,
            @Schema(description = "Duração do pontificado em dias (até hoje, se ainda estiver em exercício)", example = "4422")
            long duracaoDias,
            @Schema(example = "false") boolean emExercicio) {
    }

    public static PapaV2 de(Papa papa) {
        LocalDate fim = papa.getFimPontificado() != null ? papa.getFimPontificado() : LocalDate.now();
        return new PapaV2(
                papa.getId(),
                new Nome(papa.getNomePapal(), papa.getNomeBatismo()),
                papa.getNumeroOrdem(),
                papa.getPaisNascimento(),
                new Pontificado(papa.getInicioPontificado(), papa.getFimPontificado(), papa.getSituacao(),
                        ChronoUnit.DAYS.between(papa.getInicioPontificado(), fim),
                        papa.getSituacao() == SituacaoPontificado.EM_EXERCICIO));
    }
}
