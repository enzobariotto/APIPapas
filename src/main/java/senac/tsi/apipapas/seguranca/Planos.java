package senac.tsi.apipapas.seguranca;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import senac.tsi.apipapas.model.Plano;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Tabela de preços e limites de cada plano. Os valores vêm de application.properties (app.planos.*), então
 * dá para mudar preços e cotas sem mexer no código.
 *
 * Cada plano tem:
 * - preço mensal (em reais);
 * - limite por minuto (rate limiting, ver RateLimitFilter);
 * - cota mensal: total de requisições no mês do calendário (horário de Brasília). A cota renova no dia 1º.
 *
 * O limite por minuto do plano Gratuito é o mesmo de app.rate-limit.limite-com-chave.
 */
@Component
public class Planos {

    /** Fuso usado para saber em que mês estamos (a cota renova à meia-noite do dia 1º, horário de Brasília). */
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final Map<Plano, Limites> limites = new EnumMap<>(Plano.class);

    Planos(@Value("${app.rate-limit.limite-com-chave:60}") int porMinutoGratuito,
           @Value("${app.planos.gratuito.cota-mensal:3000}") long cotaGratuito,
           @Value("${app.planos.basico.preco:19.90}") BigDecimal precoBasico,
           @Value("${app.planos.basico.por-minuto:120}") int porMinutoBasico,
           @Value("${app.planos.basico.cota-mensal:50000}") long cotaBasico,
           @Value("${app.planos.pro.preco:49.90}") BigDecimal precoPro,
           @Value("${app.planos.pro.por-minuto:300}") int porMinutoPro,
           @Value("${app.planos.pro.cota-mensal:300000}") long cotaPro) {
        limites.put(Plano.GRATUITO, new Limites(Plano.GRATUITO, BigDecimal.ZERO, porMinutoGratuito, cotaGratuito));
        limites.put(Plano.BASICO, new Limites(Plano.BASICO, precoBasico, porMinutoBasico, cotaBasico));
        limites.put(Plano.PRO, new Limites(Plano.PRO, precoPro, porMinutoPro, cotaPro));
    }

    public Limites de(Plano plano) {
        return limites.get(plano == null ? Plano.GRATUITO : plano);
    }

    public List<Limites> todos() {
        return List.copyOf(limites.values());
    }

    public static YearMonth mesAtual() {
        return YearMonth.now(FUSO);
    }

    /** Dia em que a cota do mês atual renova (dia 1º do mês seguinte). */
    public static LocalDate renovacao() {
        return mesAtual().plusMonths(1).atDay(1);
    }

    public record Limites(Plano plano, BigDecimal precoMensal, int porMinuto, long cotaMensal) {
    }
}
