package horse_reserved.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

@Entity
@Table(name = "audit_log")
@Immutable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ocurrido_en", nullable = false, updatable = false)
    private Instant ocurridoEn;

    @Column(name = "usuario_id", updatable = false)
    private Long usuarioId;

    @Column(name = "usuario_email", length = 200, updatable = false)
    private String usuarioEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 50, updatable = false)
    private AuditCategoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 100, updatable = false)
    private AuditAccion accion;

    @Column(name = "resultado", nullable = false, length = 20, updatable = false)
    private String resultado;

    @Column(name = "detalle", columnDefinition = "TEXT", updatable = false)
    private String detalle;

    @Column(name = "entidad_tipo", length = 50, updatable = false)
    private String entidadTipo;

    @Column(name = "entidad_id", updatable = false)
    private Long entidadId;

    @Column(name = "ip_origen", length = 45, updatable = false)
    private String ipOrigen;
}
