package horse_reserved.dto.request;

import horse_reserved.model.AuditCategoria;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class AuditLogFiltroRequest {
    private AuditCategoria categoria;
    private String usuarioEmail;
    private String resultado;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate desde;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate hasta;

    private int page = 0;
    private int size = 50;
}
