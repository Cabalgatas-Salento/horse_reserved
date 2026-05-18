package horse_reserved.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
@Data
@Builder
@AllArgsConstructor
/**
 * dto para la obtencion de los horarios disponibles para reservar una ruta en una fecha
 */
public class HorariosDisponiblesResponse {

    private final Long            rutaId;
    private final LocalDate        fecha;
    private final int              cantPersonasEvaluadas;
    private final List<HorarioSlotDTO> horariosDisponibles;

}