package senac.tsi.apipapas.repository;

import senac.tsi.apipapas.model.Papa;
import senac.tsi.apipapas.model.SituacaoPontificado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PapaRepository extends JpaRepository<Papa, Long> {

    Page<Papa> findBySituacao(SituacaoPontificado situacao, Pageable pageable);
}
