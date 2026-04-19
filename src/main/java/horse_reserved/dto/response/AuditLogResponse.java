package horse_reserved.dto.response;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private Instant ocurridoEn;
    private Long usuarioId;
    private String usuarioEmail;
    private String categoria;
    private String accion;
    private String resultado;
    private String detalle;
    private String entidadTipo;
    private Long entidadId;
    private String ipOrigen;
}
