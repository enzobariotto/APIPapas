package senac.tsi.apipapas.validacao;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validação de classe (Bean Validation): a data do campo {@link #fim()} não pode ser anterior à do campo
 * {@link #inicio()}. O erro é apontado no campo de fim. Se alguma das datas estiver vazia, não valida.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PeriodoValidoValidator.class)
public @interface PeriodoValido {

    String inicio();

    String fim();

    String message() default "A data de fim não pode ser anterior à data de início";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
