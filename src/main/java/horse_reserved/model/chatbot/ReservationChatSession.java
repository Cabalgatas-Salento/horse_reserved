package horse_reserved.model.chatbot;

import horse_reserved.dto.request.ParticipanteRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationChatSession {

    private String id;
    private String userEmail;
    private ReservationFlowStep step;

    // datos recopilados en el flujo
    private Long rutaId;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private Integer cantPersonas;

    @Builder.Default
    private List<ParticipanteRequest> participantes = new ArrayList<>();

    private Instant updatedAt;
}