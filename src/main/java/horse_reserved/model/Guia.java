package horse_reserved.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "guias")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Clase que representa los guias que se asignan a las reservaciones
 */
public class Guia {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name="telefono", nullable = false, length = 100)
    private String telefono;

    @Column(name="email", nullable = false, length = 100)
    private String email;

    @Column(name="is_active", nullable = false)
    private boolean activo;

    /**
     * Definicion de relacion de muchos a muchos entre salidas y guias
     */
    @Builder.Default
    @ManyToMany(mappedBy = "guias")
    private List<Salida> salidas =  new ArrayList<>();
}
