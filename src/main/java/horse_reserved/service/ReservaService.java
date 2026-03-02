package horse_reserved.service;

import horse_reserved.dto.request.CreateReservaRequest;
import horse_reserved.dto.request.ParticipanteRequest;
import horse_reserved.dto.response.ReservaResponse;
import horse_reserved.exception.*;
import horse_reserved.model.*;
import horse_reserved.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * Servicio de implementacion de gestion de reservas
 */
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final SalidaRepository salidaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReservaMapper reservaMapper;

    /**
     * Metodo para realizar una reserva nueva
     * Supone que ya existen todos los recursos necesarios para una reserva
     * incluidos rutas, salidas, caballos, guias
     * @param request
     * @return
     */
    @Transactional
    public ReservaResponse crearReserva(CreateReservaRequest request) {
        validarRequestCrear(request);

        Usuario cliente = usuarioAutenticado();

        Salida salida = salidaRepository.findWithRutaById(request.getSalidaId())
                .orElseThrow(() -> new ResourceNotFoundException("Salida no encontrada: " + request.getSalidaId()));

        validarSalidaDisponible(salida);
        validarCupoDisponible(salida, request.getCantPersonas());

        Reserva reserva = Reserva.builder()
                .salida(salida)
                .cliente(cliente)
                .operador(null) // reserva directa de cliente
                .cantPersonas(request.getCantPersonas())
                .estado("reservado")
                .build();

        for (ParticipanteRequest pReq : request.getParticipantes()) {
            Participante p = Participante.builder()
                    .primerNombre(pReq.getPrimerNombre().trim())
                    .primerApellido(pReq.getPrimerApellido().trim())
                    .tipoDocumento(TipoDocumento.fromString(pReq.getTipoDocumento()))
                    .documento(pReq.getDocumento().trim())
                    .edad(pReq.getEdad())
                    .cmAltura(pReq.getCmAltura())
                    .kgPeso(pReq.getKgPeso())
                    .build();

            reserva.agregarParticipante(p);
        }

        Reserva saved = reservaRepository.save(reserva);
        return reservaMapper.toResponse(saved);
    }

    /**
     * Metodo para obtener todas las reservas del usuario
     * @return
     */
    @Transactional(readOnly = true)
    public List<ReservaResponse> listarMisReservas() {
        Usuario actual = usuarioAutenticado();
        return reservaRepository.findByClienteIdOrderByIdDesc(actual.getId())
                .stream()
                .map(reservaMapper::toResponse)
                .toList();
    }

    /**
     * Metodo para buscar una reserva por su id
     * @param reservaId
     * @return
     */
    @Transactional(readOnly = true)
    public ReservaResponse obtenerPorId(Long reservaId) {
        Usuario actual = usuarioAutenticado();

        Reserva reserva = reservaRepository.findDetailedById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + reservaId));

        if (!puedeVerReserva(actual, reserva)) {
            throw new AccessDeniedBusinessException("No tienes permisos para ver esta reserva");
        }

        return reservaMapper.toResponse(reserva);
    }

    /**
     * Metodo para cancelar una reserva
     * @param reservaId
     * @return
     */
    @Transactional
    public ReservaResponse cancelarReserva(Long reservaId) {
        Usuario actual = usuarioAutenticado();

        Reserva reserva = reservaRepository.findDetailedById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + reservaId));

        if (!puedeGestionarReserva(actual, reserva)) {
            throw new AccessDeniedBusinessException("No tienes permisos para cancelar esta reserva");
        }

        if ("cancelado".equalsIgnoreCase(reserva.getEstado())) {
            throw new BusinessRuleException("La reserva ya está cancelada");
        }

        if ("completado".equalsIgnoreCase(reserva.getEstado())) {
            throw new BusinessRuleException("No puedes cancelar una reserva completada");
        }

        reserva.setEstado("cancelado");
        return reservaMapper.toResponse(reservaRepository.save(reserva));
    }

    // ===================== VALIDACIONES =====================

    /**
     * Validacion para la peticion de crear una reserva
     * @param request
     */
    private void validarRequestCrear(CreateReservaRequest request) {
        if (request.getParticipantes() == null || request.getParticipantes().isEmpty()) {
            throw new BusinessRuleException("Debes enviar al menos un participante");
        }
        if (request.getCantPersonas() != request.getParticipantes().size()) {
            throw new BusinessRuleException("cantPersonas debe coincidir con el número de participantes");
        }
    }

    /**
     * Validacion para saber si una salida esta disponible para reservar
     * @param salida
     */
    private void validarSalidaDisponible(Salida salida) {
        String estado = salida.getEstado() == null ? "" : salida.getEstado().toLowerCase();
        if (!"programado".equals(estado)) {
            throw new BusinessRuleException("Solo se puede reservar en salidas en estado 'programado'");
        }
    }

    /**
     * Validacion para determinar si hay suficientes cupos en la salida para realizar
     * una reserva
     * @param salida
     * @param nuevosCupos
     */
    private void validarCupoDisponible(Salida salida, int nuevosCupos) {
        long ocupados = reservaRepository.sumPersonasReservadasActivasBySalida(salida.getId());
        int maximo = salida.getRuta().getMaxCaballos();

        if (ocupados + nuevosCupos > maximo) {
            throw new BusinessRuleException(
                    "Cupo insuficiente. Disponibles: " + (maximo - ocupados) + ", solicitados: " + nuevosCupos
            );
        }
    }

    /**
     * Metodo para obtener el usuario actual
     * @return
     */
    private Usuario usuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new InvalidCredentialsException("Usuario no autenticado");
        }
        return usuarioRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new InvalidCredentialsException("Usuario autenticado no encontrado"));
    }

    /**
     * Metodo que determina si el usuario puede ver una reserva determinada
     * @param actual
     * @param reserva
     * @return
     */
    private boolean puedeVerReserva(Usuario actual, Reserva reserva) {
        if (esAdmin(actual) || esOperador(actual)) return true;
        return reserva.getCliente().getId().equals(actual.getId());
    }

    /**
     * Metodo que determian si el usuario puede gestionar una reserva determinada
     * @param actual
     * @param reserva
     * @return
     */
    private boolean puedeGestionarReserva(Usuario actual, Reserva reserva) {
        if (esAdmin(actual) || esOperador(actual)) return true;
        return reserva.getCliente().getId().equals(actual.getId());
    }

    private boolean esAdmin(Usuario u) {
        return u.getRole() == Rol.ADMINISTRADOR;
    }

    private boolean esOperador(Usuario u) {
        return u.getRole() == Rol.OPERADOR;
    }
}