package senac.tsi.apipapas.repository;

import senac.tsi.apipapas.model.RegistroIdempotencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegistroIdempotenciaRepository extends JpaRepository<RegistroIdempotencia, Long> {

    Optional<RegistroIdempotencia> findByClienteAndChave(String cliente, String chave);
}
