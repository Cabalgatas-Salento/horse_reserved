package horse_reserved.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * Clase para transformar los objetos en información necesaria para actualizar una reserva.
 * Permite cambiar ruta, fecha/hora y participantes.
 */
public class UpdateReservaRequest {

    @NotNull
    private Long rutaId;

    @NotNull
    private LocalDate fecha;

    @NotNull
    private LocalTime horaInicio;

    @Min(1)
    private int cantPersonas;

    @NotEmpty
    @Valid
    private List<ParticipanteRequest> participantes;
}
