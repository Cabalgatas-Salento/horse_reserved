package horse_reserved.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaballoResponse {
    private Long id;
    private String nombre;
    private String raza;
    private boolean activo;
}
