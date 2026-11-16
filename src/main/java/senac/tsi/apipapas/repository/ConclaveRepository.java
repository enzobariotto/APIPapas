package senac.tsi.apipapas.repository;

import senac.tsi.apipapas.model.Conclave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConclaveRepository extends JpaRepository<Conclave, Long> {

    Page<Conclave> findByNumeroEscrutiniosLessThanEqual(Integer maxEscrutinios, Pageable pageable);

    Optional<Conclave> findByPapaId(Long papaId);
}
