package senac.tsi.apipapas.repository;

import senac.tsi.apipapas.model.ChaveApi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChaveApiRepository extends JpaRepository<ChaveApi, Long> {

    Optional<ChaveApi> findByHashAndAtivaTrue(String hash);
}
