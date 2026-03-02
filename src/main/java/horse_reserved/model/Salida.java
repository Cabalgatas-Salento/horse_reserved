package horse_reserved.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "salidas")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Clase que representa las salidas turisticas sobre las que se realizan reservaciones
 */
public class Salida {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Define una relacion de muchos a 1 entre ruta y Salidas
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="ruta_id", nullable=false)
    private Ruta ruta;

    @Column(name="fecha_programada", nullable=false)
    private LocalDate fechaProgramada;

    @Column(name="tiempo_inicio", nullable = false)
    private LocalTime tiempoInicio;

    @Column(name="tiempo_fin", nullable = false)
    private LocalTime tiempoFin;

    @Column(name="estado", nullable = false, length = 50)
    private String estado;

    /**
     * Define la relacion de 1 a muchos entre Salida y reservas
     */
    @Builder.Default
    @OneToMany(mappedBy = "salida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reserva> reservaciones = new ArrayList<>();

    /**
     * Define la relacion de muchos a muchos entre salidas y caballos
     */
    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "salida_caballos",
            joinColumns = @JoinColumn(name = "salida_id"),
            inverseJoinColumns = @JoinColumn(name = "horse_id")
    )
    private List<Caballo> caballos = new ArrayList<>();

    /**
     * Define la relacion de muchos a muchos entre salidas y guias
     */
    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "salida_guias",
            joinColumns = @JoinColumn(name = "salida_id"),
            inverseJoinColumns = @JoinColumn(name = "guia_id")
    )
    private List<Guia> guias = new ArrayList<>();

    /**
     * Metodo para agregar una reserva en una salida
     * @param reserva
     * @return
     */
    public boolean agregarReserva(Reserva reserva){
        if(!reservaciones.contains(reserva)){
            reservaciones.add(reserva);
            reserva.setSalida(this);
            return true;
        }
        return false;
    }

    /**
     * Metodo para eliminar una reserva en una salida
     * @param reserva
     * @return
     */
    public boolean eliminarReserva(Reserva reserva){
        return reservaciones.remove(reserva);
    }

    /**
     * Metodo para agregar un caballo en una salida
     * @param caballo
     * @return
     */
    public boolean agregarCaballo(Caballo caballo) {
        if (!caballos.contains(caballo)) {
            caballos.add(caballo);
            caballo.getSalidas().add(this);
            return true;
        }
        return false;
    }

    /**
     * Metodo para eliminar un caballo de una salida
     * @param caballo
     * @return
     */
    public boolean eliminarCaballo(Caballo caballo) {
        if (caballos.remove(caballo)) {
            caballo.getSalidas().remove(this);
            return true;
        }
        return false;
    }

    /**
     * Metodo para agregar un guia a una salida
     * @param guia
     * @return
     */
    public boolean agregarGuia(Guia guia) {
        if (!guias.contains(guia)) {
            guias.add(guia);
            guia.getSalidas().add(this);
            return true;
        }
        return false;
    }

    /**
     * Metodo para eliminar un guia de una salida
     * @param guia
     * @return
     */
    public boolean eliminarGuia(Guia guia) {
        if (guias.remove(guia)) {
            guia.getSalidas().remove(this);
            return true;
        }
        return false;
    }
}
