package horse_reserved.service;

import horse_reserved.dto.response.ParticipanteResponse;
import horse_reserved.dto.response.ReservaResponse;
import horse_reserved.model.Participante;
import horse_reserved.model.Reserva;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
/**
 * Clase que permite mapear los datos en respuestas
 */
public class ReservaMapper {

    public ReservaResponse toResponse(Reserva reserva) {
        List<ParticipanteResponse> participantes = reserva.getParticipantes().stream()
                .map(this::toParticipanteResponse)
                .toList();

        return ReservaResponse.builder()
                .id(reserva.getId())
                .estado(reserva.getEstado())
                .cantPersonas(reserva.getCantPersonas())
                .salidaId(reserva.getSalida().getId())
                .rutaId(reserva.getSalida().getRuta().getId())
                .fechaProgramada(reserva.getSalida().getFechaProgramada())
                .tiempoInicio(reserva.getSalida().getTiempoInicio())
                .tiempoFin(reserva.getSalida().getTiempoFin())
                .salidaEstado(reserva.getSalida().getEstado())
                .rutaNombre(reserva.getSalida().getRuta().getNombre())
                .clienteId(reserva.getCliente() != null ? reserva.getCliente().getId() : null)
                .clienteEmail(reserva.getCliente() != null ? reserva.getCliente().getEmail() : null)
                .operadorId(reserva.getOperador() != null ? reserva.getOperador().getId() : null)
                .participantes(participantes)
                .build();
    }

    private ParticipanteResponse toParticipanteResponse(Participante p) {
        return ParticipanteResponse.builder()
                .id(p.getId())
                .primerNombre(p.getPrimerNombre())
                .primerApellido(p.getPrimerApellido())
                .tipoDocumento(p.getTipoDocumento().name())
                .documento(p.getDocumento())
                .edad(p.getEdad())
                .cmAltura(p.getCmAltura())
                .kgPeso(p.getKgPeso())
                .build();
    }
}