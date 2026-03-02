package horse_reserved.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * Clase que sirve para mapear la informacion necesaria para las respuestas relacionadas
 * con los participantes
 */
public class ParticipanteResponse {
    private Long id;
    private String primerNombre;
    private String primerApellido;
    private String tipoDocumento;
    private String documento;
    private int edad;
    private int cmAltura;
    private BigDecimal kgPeso;
}
