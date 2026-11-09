package senac.tsi.apipapas.validacao;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

import java.time.LocalDate;

public class PeriodoValidoValidator implements ConstraintValidator<PeriodoValido, Object> {

    private String inicio;
    private String fim;

    @Override
    public void initialize(PeriodoValido anotacao) {
        this.inicio = anotacao.inicio();
        this.fim = anotacao.fim();
    }

    @Override
    public boolean isValid(Object objeto, ConstraintValidatorContext context) {
        if (objeto == null) {
            return true;
        }
        BeanWrapperImpl bean = new BeanWrapperImpl(objeto);
        Object valorInicio = bean.getPropertyValue(inicio);
        Object valorFim = bean.getPropertyValue(fim);
        if (!(valorInicio instanceof LocalDate dataInicio) || !(valorFim instanceof LocalDate dataFim)) {
            return true;
        }
        if (!dataFim.isBefore(dataInicio)) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode(fim)
                .addConstraintViolation();
        return false;
    }
}
