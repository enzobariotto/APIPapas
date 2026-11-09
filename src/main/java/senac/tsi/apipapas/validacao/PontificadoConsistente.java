package senac.tsi.apipapas.validacao;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import senac.tsi.apipapas.model.Papa;
import senac.tsi.apipapas.model.SituacaoPontificado;

import java.lang.annotation.*;

/**
 * Validação de classe do Papa: a situação precisa combinar com a data de fim do pontificado.
 * EM_EXERCICIO não pode ter data de fim; RENUNCIA e FALECIMENTO precisam ter.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PontificadoConsistente.Validador.class)
public @interface PontificadoConsistente {

    String message() default "A situação não combina com a data de fim do pontificado";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<PontificadoConsistente, Papa> {

        @Override
        public boolean isValid(Papa papa, ConstraintValidatorContext context) {
            if (papa == null || papa.getSituacao() == null) {
                return true;
            }
            boolean emExercicio = papa.getSituacao() == SituacaoPontificado.EM_EXERCICIO;
            boolean temFim = papa.getFimPontificado() != null;
            if (emExercicio == !temFim) {
                return true;
            }
            String mensagem = emExercicio
                    ? "Um papa EM_EXERCICIO não pode ter data de fim do pontificado"
                    : "Informe a data de fim do pontificado para a situação " + papa.getSituacao();
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(mensagem)
                    .addPropertyNode("fimPontificado")
                    .addConstraintViolation();
            return false;
        }
    }
}
