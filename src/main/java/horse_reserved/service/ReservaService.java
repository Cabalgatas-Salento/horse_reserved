package horse_reserved.service;

import horse_reserved.dto.request.CreateReservaRequest;
import horse_reserved.dto.request.ParticipanteRequest;
import horse_reserved.dto.request.UpdateReservaRequest;
import horse_reserved.dto.response.ReservaResponse;
import horse_reserved.exception.*;
import horse_reserved.model.*;
import horse_reserved.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * Servicio de implementacion de gestion de reservas
 */
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final SalidaRepository salidaRepository;
    private final RutaRepository rutaRepository;
    private final CaballoRepository caballoRepository;
    private final GuiaRepository guiaRepository;
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

        Usuario autenticado = usuarioAutenticado();

        Usuario cliente;
        Usuario operador;
        if (esOperador(autenticado)) {
            if (request.getClienteId() != null) {
                cliente = usuarioRepository.findById(request.getClienteId())
                        .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + request.getClienteId()));
                if (cliente.getRole() != Rol.CLIENTE) {
                    throw new BusinessRuleException("El usuario especificado no es un cliente");
                }
            } else {
                cliente = null; // reserva de invitado
            }
            operador = autenticado;
        } else {
            cliente = autenticado;
            operador = null;
        }

        Salida salida = salidaRepository
                .findProgramadaByRutaAndFechaAndHora(request.getRutaId(), request.getFecha(), request.getHoraInicio())
                .orElseGet(() -> crearNuevaSalida(
                        request.getRutaId(), request.getFecha(), request.getHoraInicio(), request.getCantPersonas()));

        validarCupoDisponible(salida, request.getCantPersonas());

        long totalPersonas = reservaRepository.sumPersonasReservadasActivasBySalida(salida.getId())
                + request.getCantPersonas();
        asignarGuiasSalida(salida, totalPersonas);

        Reserva reserva = Reserva.builder()
                .salida(salida)
                .cliente(cliente)
                .operador(operador)
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
        List<Reserva> reservas = esOperador(actual)
                ? reservaRepository.findByOperadorIdOrderByIdDesc(actual.getId())
                : reservaRepository.findByClienteIdOrderByIdDesc(actual.getId());
        return reservas.stream().map(reservaMapper::toResponse).toList();
    }

    /**
     * Actualiza ruta, fecha/hora y participantes de una reserva existente.
     * Solo se puede actualizar si el estado es "reservado".
     */
    @Transactional
    public ReservaResponse actualizarReserva(Long reservaId, UpdateReservaRequest request) {
        if (request.getCantPersonas() != request.getParticipantes().size()) {
            throw new BusinessRuleException("cantPersonas debe coincidir con el número de participantes");
        }

        Usuario actual = usuarioAutenticado();

        Reserva reserva = reservaRepository.findDetailedById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + reservaId));

        if (!puedeGestionarReserva(actual, reserva)) {
            throw new AccessDeniedBusinessException("No tienes permisos para actualizar esta reserva");
        }
        if ("cancelado".equalsIgnoreCase(reserva.getEstado())) {
            throw new BusinessRuleException("No puedes actualizar una reserva cancelada");
        }
        if ("completado".equalsIgnoreCase(reserva.getEstado())) {
            throw new BusinessRuleException("No puedes actualizar una reserva completada");
        }

        Salida salidaActual = reserva.getSalida();
        boolean salidaCambia = !salidaActual.getRuta().getId().equals(request.getRutaId())
                || !salidaActual.getFechaProgramada().equals(request.getFecha())
                || !salidaActual.getTiempoInicio().equals(request.getHoraInicio());

        Salida nuevaSalida;
        if (salidaCambia) {
            nuevaSalida = salidaRepository
                    .findProgramadaByRutaAndFechaAndHora(request.getRutaId(), request.getFecha(), request.getHoraInicio())
                    .orElseGet(() -> crearNuevaSalida(
                            request.getRutaId(), request.getFecha(), request.getHoraInicio(), request.getCantPersonas()));
            validarCupoDisponible(nuevaSalida, request.getCantPersonas());
            long total = reservaRepository.sumPersonasReservadasActivasBySalida(nuevaSalida.getId())
                    + request.getCantPersonas();
            asignarGuiasSalida(nuevaSalida, total);
        } else {
            nuevaSalida = salidaActual;
            // Desconta la reserva actual para no doble-contarla en la validación de cupo
            long ocupadosNetos = reservaRepository.sumPersonasReservadasActivasBySalida(nuevaSalida.getId())
                    - reserva.getCantPersonas();
            int maximo = nuevaSalida.getCaballos().size();
            if (maximo == 0) {
                throw new BusinessRuleException("La salida no tiene caballos asignados");
            }
            if (ocupadosNetos + request.getCantPersonas() > maximo) {
                throw new BusinessRuleException(
                        "Cupo insuficiente. Disponibles: " + (maximo - ocupadosNetos) + ", solicitados: " + request.getCantPersonas());
            }
            asignarGuiasSalida(nuevaSalida, ocupadosNetos + request.getCantPersonas());
        }

        reserva.getParticipantes().clear();
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

        reserva.setSalida(nuevaSalida);
        reserva.setCantPersonas(request.getCantPersonas());

        return reservaMapper.toResponse(reservaRepository.save(reserva));
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> listarTodas() {
        return reservaRepository.findAllOrderByIdDesc()
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
     * Crea una nueva salida para la ruta, fecha y hora indicadas,
     * asignando todos los caballos disponibles y los guias necesarios.
     */
    private Salida crearNuevaSalida(Long rutaId, LocalDate fecha, LocalTime horaInicio, int cantPersonas) {
        Ruta ruta = rutaRepository.findById(rutaId)
                .orElseThrow(() -> new ResourceNotFoundException("Ruta no encontrada: " + rutaId));

        LocalTime horaFin = horaInicio.plusMinutes(ruta.getDuracionMinutos());

        List<Caballo> caballos = caballoRepository.findDisponibles(fecha, horaInicio, horaFin);
        if (caballos.isEmpty()) {
            throw new BusinessRuleException("No hay caballos disponibles para esa fecha y hora");
        }

        Salida nueva = Salida.builder()
                .ruta(ruta)
                .fechaProgramada(fecha)
                .tiempoInicio(horaInicio)
                .tiempoFin(horaFin)
                .estado("programado")
                .build();

        caballos.forEach(nueva::agregarCaballo);
        asignarGuiasSalida(nueva, (long) cantPersonas);

        return salidaRepository.save(nueva);
    }

    /**
     * Asigna los guias necesarios a una salida segun el total de personas.
     * Regla: <= 8 personas -> 1 guia, > 8 personas -> 2 guias.
     * Si faltan guias y la salida es hoy o manana, lanza excepcion.
     * Si la salida es mas adelante, crea la reserva aunque no haya guia extra.
     */
    private void asignarGuiasSalida(Salida salida, long totalPersonas) {
        int guidesNeeded  = totalPersonas > 8 ? 2 : 1;
        int guidesAssigned = salida.getGuias().size();
        int guidesToAdd   = guidesNeeded - guidesAssigned;

        if (guidesToAdd <= 0) return;

        List<Guia> disponibles = guiaRepository.findDisponibles(
                salida.getFechaProgramada(), salida.getTiempoInicio(), salida.getTiempoFin());

        if (disponibles.size() < guidesToAdd && esSalidaInminente(salida.getFechaProgramada())) {
            throw new BusinessRuleException(
                    "No hay guías disponibles para cubrir esta salida en la fecha indicada");
        }

        disponibles.stream().limit(guidesToAdd).forEach(salida::agregarGuia);
    }

    private boolean esSalidaInminente(LocalDate fecha) {
        return !fecha.isAfter(LocalDate.now().plusDays(1));
    }

    /**
     * Validacion para determinar si hay suficientes cupos en la salida para realizar
     * una reserva. El cupo maximo es el numero de caballos asignados a la salida.
     * @param salida
     * @param nuevosCupos
     */
    private void validarCupoDisponible(Salida salida, int nuevosCupos) {
        long ocupados = reservaRepository.sumPersonasReservadasActivasBySalida(salida.getId());
        int maximo = salida.getCaballos().size();

        if (maximo == 0) {
            throw new BusinessRuleException("La salida no tiene caballos asignados");
        }

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
        if (reserva.getCliente() == null) return false;
        return reserva.getCliente().getId().equals(actual.getId());
    }

    /**
     * Metodo que determian si el usuario puede gestionar una reserva determinada
     * @param actual
     * @param reserva
     * @return
     */
    private boolean puedeGestionarReserva(Usuario actual, Reserva reserva) {
        if (esOperador(actual)) return true;
        if (reserva.getCliente() == null) return false;
        return reserva.getCliente().getId().equals(actual.getId());
    }

    private boolean esAdmin(Usuario u) {
        return u.getRole() == Rol.ADMINISTRADOR;
    }

    private boolean esOperador(Usuario u) {
        return u.getRole() == Rol.OPERADOR;
    }
}