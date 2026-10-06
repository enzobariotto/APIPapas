package senac.tsi.apipapas.repository;

import senac.tsi.apipapas.model.Conclave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConclaveRepository extends JpaRepository<Conclave, Long> {

    Page<Conclave> findByNumeroEscrutiniosLessThanEqual(Integer maxEscrutinios, Pageable pageable);
}
