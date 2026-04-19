package horse_reserved.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacciones")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
/**
 * Clase que representa una transaccion realizada en la aplicacion
 */
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intento_pago_id", nullable = false)
    private IntentoPago intentoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 20)
    private TipoMovimientoTransaccion tipoMovimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PagoEstado estado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(length = 255)
    private String detalle;

    @Column(name = "fecha_transaccion", nullable = false)
    private LocalDateTime fechaTransaccion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (fechaTransaccion == null) fechaTransaccion = LocalDateTime.now();
        if (moneda == null) moneda = "COP";
    }
}