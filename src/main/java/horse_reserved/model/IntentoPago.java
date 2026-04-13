package horse_reserved.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "intentos_pago")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
/**
 * Clase que representa un intento de pago asociado a una reserva
 */
public class IntentoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservacion_id", nullable = false)
    private Reserva reserva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PagoEstado estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 20)
    private MetodoPago metodoPago;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    private String moneda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pagado_por_usuario_id")
    private Usuario pagadoPorUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pagado_por_operador_id")
    private Usuario pagadoPorOperador;

    @Column(name = "referencia_simulada", nullable = false, unique = true, length = 120)
    private String referenciaSimulada;

    @Column(name = "fecha_intento", nullable = false)
    private LocalDateTime fechaIntento;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "intentoPago", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaccion> transacciones;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (fechaIntento == null) fechaIntento = LocalDateTime.now();
        if (moneda == null) moneda = "COP";
    }
}