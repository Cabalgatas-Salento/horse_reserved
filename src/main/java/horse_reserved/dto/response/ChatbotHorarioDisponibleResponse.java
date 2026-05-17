package horse_reserved.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotHorarioDisponibleResponse {
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private int duracionMinutos;
    private int cuposDisponiblesEstimados;
    private BigDecimal precioPorPersona;
}