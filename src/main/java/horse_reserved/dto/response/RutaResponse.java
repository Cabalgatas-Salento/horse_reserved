package horse_reserved.dto.response;

import horse_reserved.model.Dificultad;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RutaResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private Dificultad dificultad;
    private int duracionMinutos;
    private String urlImagen;
}
