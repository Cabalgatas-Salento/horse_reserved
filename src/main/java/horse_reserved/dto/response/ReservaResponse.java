package horse_reserved.dto.response;

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
 * Clase usada para mapear los datos necesarios para responder a la peticion de reserva
 */
public class ReservaResponse {

    private Long id;
    private String estado;
    private int cantPersonas;

    private Long salidaId;
    private Long rutaId;
    private LocalDate fechaProgramada;
    private LocalTime tiempoInicio;
    private LocalTime tiempoFin;
    private String salidaEstado;
    private String rutaNombre;

    private Long clienteId;
    private String clienteEmail;

    private Long operadorId; // nullable
    private List<ParticipanteResponse> participantes;
}
