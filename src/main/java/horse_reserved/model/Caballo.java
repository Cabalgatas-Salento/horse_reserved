package horse_reserved.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "caballos")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Clase que representa los caballos que se asignan a las reservaciones
 */
public class Caballo {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name="raza", nullable = false, length = 100)
    private String raza;

    @Column(name="is_active", nullable = false)
    private boolean activo;

    /**
     * Definicion de relacion de muchos a muchos entre salidas y caballos
     */
    @Builder.Default
    @ManyToMany(mappedBy = "caballos")
    private List<Salida> salidas =  new ArrayList<>();

}
