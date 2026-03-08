package horse_reserved.dto.response;

import horse_reserved.model.Dificultad;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RutaResponse {
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private String descripcion;
    private Dificultad dificultad;
    private int duracionMinutos;
    private String urlImagen;
}
