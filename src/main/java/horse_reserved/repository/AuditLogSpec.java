package horse_reserved.repository;

import horse_reserved.model.AuditCategoria;
import horse_reserved.model.AuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditLogSpec {

    public static Specification<AuditLog> withFiltros(
            AuditCategoria categoria,
            String usuarioEmail,
            String resultado,
            LocalDateTime desde,
            LocalDateTime hasta) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (categoria != null)
                predicates.add(cb.equal(root.get("categoria"), categoria));

            if (usuarioEmail != null && !usuarioEmail.isBlank())
                predicates.add(cb.like(cb.lower(root.get("usuarioEmail")),
                        "%" + usuarioEmail.toLowerCase() + "%"));

            if (resultado != null)
                predicates.add(cb.equal(root.get("resultado"), resultado));

            if (desde != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("ocurridoEn"), desde));

            if (hasta != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("ocurridoEn"), hasta));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
