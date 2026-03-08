package horse_reserved.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rutas")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Clase que representa las rutas turisticas sobre las que se realizan salidas en caballo
 */
public class Ruta {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="nombre", nullable=false, length=150)
    private String nombre;

    @Column(name="descripcion", nullable=false)
    private String descripcion;

    @Positive
    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(name="dificultad", nullable = false, length= 50)
    private Dificultad dificultad;

    @Positive
    @Column(name="duracion_minutos", nullable = false)
    private int duracionMinutos;

    @Column(name="image_url", length = 500)
    private String urlImagen;

    @Column(name="is_active", nullable = false)
    private boolean activa;

    /**
     * Define la relacion de 1 a muchos entre Ruta y salidas
     */
    @Builder.Default
    @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Salida> salidas = new ArrayList<>();

    /**
     * Metodo para agregar una salida a una ruta
     * @param salida
     * @return
     */
    public boolean agregarSalida(Salida salida){
        if(!salidas.contains(salida)){
            salidas.add(salida);
            salida.setRuta(this);
            return true;
        }
        return false;
    }

    /**
     * Metodo para eliminar una salida de una ruta
     * @param salida
     * @return
     */
    public boolean eliminarSalida(Salida salida){
        return salidas.remove(salida);
    }
}
