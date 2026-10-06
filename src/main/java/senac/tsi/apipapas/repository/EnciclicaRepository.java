package senac.tsi.apipapas.repository;

import senac.tsi.apipapas.model.Enciclica;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnciclicaRepository extends JpaRepository<Enciclica, Long> {

    Page<Enciclica> findByPapaId(Long papaId, Pageable pageable);
}
