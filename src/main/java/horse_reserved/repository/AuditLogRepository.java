package horse_reserved.repository;

import horse_reserved.model.AuditCategoria;
import horse_reserved.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:categoria IS NULL OR a.categoria = :categoria)
          AND (:usuarioEmail IS NULL OR LOWER(a.usuarioEmail) LIKE LOWER(CONCAT('%', :usuarioEmail, '%')))
          AND (:resultado IS NULL OR a.resultado = :resultado)
          AND (:desde IS NULL OR a.ocurridoEn >= :desde)
          AND (:hasta IS NULL OR a.ocurridoEn <= :hasta)
        ORDER BY a.ocurridoEn DESC
        """)
    Page<AuditLog> buscarConFiltros(
            @Param("categoria") AuditCategoria categoria,
            @Param("usuarioEmail") String usuarioEmail,
            @Param("resultado") String resultado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );
}
