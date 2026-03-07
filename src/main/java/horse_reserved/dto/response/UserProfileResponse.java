package horse_reserved.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {

    private Long userId;
    private String email;
    private String primerNombre;
    private String primerApellido;
    private String tipoDocumento;
    private String documento;
    private String telefono;
    private String role;
    private Boolean isActive;
    private Boolean habeasDataConsented;
}