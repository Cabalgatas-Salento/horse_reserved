package horse_reserved.service.chatbot;

import horse_reserved.dto.response.ChatbotHorarioDisponibleResponse;
import horse_reserved.exception.BusinessRuleException;
import horse_reserved.exception.ResourceNotFoundException;
import horse_reserved.model.Caballo;
import horse_reserved.model.Ruta;
import horse_reserved.model.Salida;
import horse_reserved.repository.CaballoRepository;
import horse_reserved.repository.GuiaRepository;
import horse_reserved.repository.ReservaRepository;
import horse_reserved.repository.RutaRepository;
import horse_reserved.repository.SalidaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula horarios disponibles para una ruta y fecha dadas.
 * Genera slots cada 30 minutos entre 08:30 y 14:30 (igual que HoraInicioValidator).
 * Solo devuelve slots con al menos un cupo estimado Y al menos un guía disponible.
 */
@Service
@RequiredArgsConstructor
public class ChatbotReservationAvailabilityService {

    private static final LocalTime HORA_MIN = LocalTime.of(8, 30);
    private static final LocalTime HORA_MAX = LocalTime.of(14, 30);

    private final RutaRepository rutaRepository;
    private final SalidaRepository salidaRepository;
    private final ReservaRepository reservaRepository;
    private final CaballoRepository caballoRepository;
    private final GuiaRepository guiaRepository;

    @Transactional(readOnly = true)
    public List<ChatbotHorarioDisponibleResponse> horariosDisponibles(Long rutaId, LocalDate fecha) {
        if (fecha == null || !fecha.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("La fecha de reserva debe ser al menos 1 día después de hoy");
        }

        Ruta ruta = rutaRepository.findById(rutaId)
                .filter(Ruta::isActiva)
                .orElseThrow(() -> new ResourceNotFoundException("Ruta no encontrada o inactiva: " + rutaId));

        List<ChatbotHorarioDisponibleResponse> result = new ArrayList<>();

        for (LocalTime inicio = HORA_MIN; !inicio.isAfter(HORA_MAX); inicio = inicio.plusMinutes(30)) {
            LocalTime fin = inicio.plusMinutes(ruta.getDuracionMinutos());

            int cupos = cuposDisponiblesEstimados(rutaId, fecha, inicio, fin);
            boolean hayGuia = !guiaRepository.findDisponibles(fecha, inicio, fin).isEmpty();

            if (cupos > 0 && hayGuia) {
                result.add(ChatbotHorarioDisponibleResponse.builder()
                        .horaInicio(inicio)
                        .horaFin(fin)
                        .duracionMinutos(ruta.getDuracionMinutos())
                        .cuposDisponiblesEstimados(cupos)
                        .precioPorPersona(ruta.getPrecio())
                        .build());
            }
        }
        return result;
    }

    // ── Privados ─────────────────────────────────────────────────────────────

    private int cuposDisponiblesEstimados(Long rutaId, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        return salidaRepository
                .findProgramadaByRutaAndFechaAndHora(rutaId, fecha, inicio)
                .map(salida -> cuposEnSalidaExistente(salida, fecha, inicio, fin))
                .orElseGet(() -> caballoRepository.findDisponibles(fecha, inicio, fin).size());
    }

    /**
     * Si ya existe una salida programada:
     * cupos = caballos activos asignados − ocupados + caballos adicionales disponibles
     * (refleja que ReservaService puede expandir caballos si es necesario).
     */
    private int cuposEnSalidaExistente(Salida salida, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        long ocupados = reservaRepository.sumPersonasReservadasActivasBySalida(salida.getId());
        int asignadosActivos = (int) salida.getCaballos().stream()
                .filter(Caballo::isActivo)
                .count();
        int libresAsignados = Math.max(0, asignadosActivos - (int) ocupados);
        int adicionalesDisponibles = caballoRepository.findDisponibles(fecha, inicio, fin).size();
        return libresAsignados + adicionalesDisponibles;
    }
}