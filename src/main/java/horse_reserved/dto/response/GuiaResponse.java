package horse_reserved.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuiaResponse {
    private Long id;
    private String nombre;
    private String telefono;
    private String email;
    private boolean activo;
}
