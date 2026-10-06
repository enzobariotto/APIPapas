package senac.tsi.apipapas.repository;

import senac.tsi.apipapas.model.Concilio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcilioRepository extends JpaRepository<Concilio, Long> {

    Page<Concilio> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}
