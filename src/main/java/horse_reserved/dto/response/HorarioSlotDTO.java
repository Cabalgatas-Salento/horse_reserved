package horse_reserved.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
/**
 * dto para representar un rango horario disponible para reservar
 */
public class HorarioSlotDTO {

    private final LocalTime horaInicio;
    private final LocalTime horaFin;
    private final int cuposDisponibles;

}