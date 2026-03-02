package horse_reserved.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "participantes")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Clase de usuario que permite el login e identificar a las personas que usan el sistema
 */

public class Participante {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Define la relacion de muchos a 1 entre participantes y reserva
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservacion_id", nullable = false)
    private Reserva reserva;

    @NotBlank
    @Column(name = "primer_nombre", nullable = false, length = 100)
    private String primerNombre;

    @NotBlank
    @Column(name = "primer_apellido", nullable = false, length = 100)
    private String primerApellido;

    @Column(name = "tipo_documento", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private TipoDocumento tipoDocumento;

    @NotBlank
    @Column(name = "documento", nullable = false, length = 50)
    private String documento;

    @Positive
    @Column(name = "edad", nullable = false)
    private short edad;

    @Positive
    @Column(name = "altura_cm", nullable = false)
    private short cmAltura;

    @Positive
    @Digits(integer = 3, fraction = 2)
    @Column(name = "peso_kg", nullable = false)
    private BigDecimal kgPeso;


}
