package horse_reserved.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservaciones")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Clase que representa las reservas realizadas sobre las diferentes salidas en cada ruta
 */
public class Reserva {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Define la relacion de muchos a 1 entre Salida y reservas
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="salida_id", nullable=false)
    private Salida salida;

    /**
     * Define la relacion de muchos a 1 entre Cliente y reservas
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="client_id", nullable=true)
    private Usuario cliente;

    /**
     * Define la relacion de muchos a 1 entre Operador y reservas
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="operator_id")
    private Usuario operador;

    @Positive
    @Column(name="num_people", nullable = false)
    private int cantPersonas; //cantidad de personas que participan en una reserva

    @Positive
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Positive
    @Column(name = "total", nullable = false, precision = 20, scale = 2)
    private BigDecimal precioTotal;

    @Column(name="estado", nullable = false, length = 50)
    private String estado;

    /**
     * Define la relacion de 1 a muchos entre Reserva y participantes
     */
    @Builder.Default
    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participante> participantes = new ArrayList<>();


    /**
     * Metodo para agregar un participante en una reserva
     * @param participante
     * @return
     */
    public void agregarParticipante(Participante participante) {
        participantes.add(participante);
        participante.setReserva(this);
    }

    /**
     * Metodo para eliminar un participante de una reserva
     * @param participante
     * @return
     */
    public boolean eliminarParticipante(Participante participante) {
        return participantes.remove(participante);
    }
}
