package horse_reserved.dto.response;

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
    private String dificultad;
    private int duracionMinutos;
    private String urlImagen;
}
