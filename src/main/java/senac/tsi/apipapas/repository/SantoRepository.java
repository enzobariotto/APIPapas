package senac.tsi.apipapas.repository;

import senac.tsi.apipapas.model.Santo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SantoRepository extends JpaRepository<Santo, Long> {

    Page<Santo> findByPaisOrigemIgnoreCase(String pais, Pageable pageable);

    List<Santo> findByCanonizadoPorId(Long papaId);
}
