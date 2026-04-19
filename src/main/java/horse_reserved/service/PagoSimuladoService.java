package horse_reserved.service;

import horse_reserved.dto.request.CrearIntentoPagoRequest;
import horse_reserved.dto.request.ReembolsarPagoRequest;
import horse_reserved.dto.response.IntentoPagoResponse;
import horse_reserved.dto.response.MetricasPagosResponse;
import horse_reserved.dto.response.TransaccionResponse;
import horse_reserved.exception.InvalidCredentialsException;
import horse_reserved.repository.IntentoPagoRepository;
import horse_reserved.repository.ReservaRepository;
import horse_reserved.repository.TransaccionRepository;
import horse_reserved.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import horse_reserved.model.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Random;
import java.util.UUID;

/**
 * Servicio encargado de gestionar el flujo de pagos simulados.
 * Permite crear intentos de pago, procesar reembolsos y consultar
 * transacciones y métricas asociadas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoSimuladoService {

    private final IntentoPagoRepository intentoRepo;
    private final TransaccionRepository transaccionRepo;
    private final ReservaRepository reservaRepo;
    private final UsuarioRepository usuarioRepo;

    /**
     * Crea un intento de pago asociado a una reserva.
     * Valida la identidad del pagador y evita pagos duplicados.
     *
     * @param req datos del intento de pago
     * @return información del intento creado
     */
    @Transactional
    public IntentoPagoResponse crearIntento(CrearIntentoPagoRequest req) {

        log.info("Iniciando intento de pago | reservaId={} usuarioId={} operadorId={} monto={}",
                req.reservaId(), req.usuarioId(), req.operadorId(), req.monto());

        boolean tieneUsuario = req.usuarioId() != null;
        boolean tieneOperador = req.operadorId() != null;

        if (tieneUsuario == tieneOperador) {
            log.warn("Intento inválido: múltiples o ningún pagador | reservaId={} usuarioId={} operadorId={}",
                    req.reservaId(), req.usuarioId(), req.operadorId());
            throw new IllegalArgumentException(
                    "Debe especificarse exactamente uno: usuarioId u operadorId.");
        }

        Reserva reserva = reservaRepo.findById(req.reservaId())
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada: " + req.reservaId()));

        Usuario pagadoPorUsuario = null;
        Usuario pagadoPorOperador = null;

        if (tieneUsuario) {
            pagadoPorUsuario = usuarioRepo.findById(req.usuarioId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + req.usuarioId()));

            if (!reserva.getCliente().getId().equals(pagadoPorUsuario.getId())) {
                log.warn("Intento de pago rechazado: usuario no propietario | reservaId={} usuarioId={}",
                        reserva.getId(), pagadoPorUsuario.getId());
                throw new SecurityException("El usuario no es propietario de esta reserva.");
            }
        } else {
            pagadoPorOperador = usuarioRepo.findById(req.operadorId())
                    .orElseThrow(() -> new IllegalArgumentException("Operador no encontrado: " + req.operadorId()));
        }

        if (intentoRepo.existsByReservaIdAndEstado(reserva.getId(), PagoEstado.REALIZADO)) {
            log.warn("Pago duplicado bloqueado | reservaId={}", reserva.getId());
            throw new IllegalStateException(
                    "Ya existe un pago REALIZADO para la reserva " + reserva.getId());
        }

        IntentoPago intento = IntentoPago.builder()
                .reserva(reserva)
                .estado(PagoEstado.REALIZADO)
                .metodoPago(req.metodoPago())
                .monto(req.monto())
                .moneda("COP")
                .pagadoPorUsuario(pagadoPorUsuario)
                .pagadoPorOperador(pagadoPorOperador)
                .referenciaSimulada("SIM-" + UUID.randomUUID())
                .fechaIntento(LocalDateTime.now())
                .build();

        intento = intentoRepo.save(intento);

        log.info("Intento de pago creado | intentoId={} referencia={} estado={}",
                intento.getId(), intento.getReferenciaSimulada(), intento.getEstado());

        Transaccion tx = Transaccion.builder()
                .intentoPago(intento)
                .tipoMovimiento(TipoMovimientoTransaccion.PAGO)
                .estado(PagoEstado.REALIZADO)
                .monto(req.monto())
                .moneda("COP")
                .detalle("Pago exitoso")
                .fechaTransaccion(LocalDateTime.now())
                .build();

        transaccionRepo.save(tx);

        log.info("Transacción registrada | transaccionId={} tipo={} estado={} monto={}",
                tx.getId(), tx.getTipoMovimiento(), tx.getEstado(), tx.getMonto());

        return IntentoPagoResponse.from(intento);
    }

    /**
     * Procesa el reembolso de un intento de pago previamente realizado.
     *
     * @param req datos del reembolso
     * @return información actualizada del intento
     */
    @Transactional
    public IntentoPagoResponse reembolsar(ReembolsarPagoRequest req) {

        log.info("Iniciando reembolso | intentoPagoId={} motivo={}",
                req.intentoPagoId(), req.motivo());

        IntentoPago intento = intentoRepo.findById(req.intentoPagoId())
                .orElseThrow(() -> new IllegalArgumentException("Intento no encontrado: " + req.intentoPagoId()));

        if (intento.getEstado() != PagoEstado.REALIZADO) {
            log.warn("Reembolso rechazado | intentoPagoId={} estadoActual={}",
                    intento.getId(), intento.getEstado());
            throw new IllegalStateException("El intento no puede ser reembolsado.");
        }

        intento.setEstado(PagoEstado.REEMBOLSADO);
        intentoRepo.save(intento);

        log.info("Reembolso realizado | intentoPagoId={} monto={}",
                intento.getId(), intento.getMonto());

        Transaccion tx = Transaccion.builder()
                .intentoPago(intento)
                .tipoMovimiento(TipoMovimientoTransaccion.REEMBOLSO)
                .estado(PagoEstado.REEMBOLSADO)
                .monto(intento.getMonto())
                .moneda("COP")
                .detalle(req.motivo() != null ? req.motivo() : "Reembolso")
                .fechaTransaccion(LocalDateTime.now())
                .build();

        transaccionRepo.save(tx);

        log.info("Transacción de reembolso registrada | transaccionId={} monto={}",
                tx.getId(), tx.getMonto());

        return IntentoPagoResponse.from(intento);
    }

    /**
     * Obtiene un intento de pago por su identificador.
     * (mensaje de excepcion usado para evitar exponer IDs reales de no existentes)
     */
    @Transactional(readOnly = true)
    public IntentoPagoResponse obtenerIntento(Long id) {
        Usuario usuario = usuarioAutenticado();
        log.debug("Consulta intento pago | id={} | solicitante={}", id, usuario.getId());

        IntentoPago intento = intentoRepo.findById(id)
                .orElseThrow(() -> new SecurityException("No tienes permiso para consultar este recurso."));

        validarAcceso(intento, usuario.getId());

        return IntentoPagoResponse.from(intento);
    }

    /**
     * Obtiene una transacción por su identificador.
     * (mensaje de excepcion usado para evitar exponer IDs reales de no existentes)
     */
    @Transactional(readOnly = true)
    public TransaccionResponse obtenerTransaccion(Long id) {
        Usuario usuario = usuarioAutenticado();
        log.debug("Consulta transacción | id={} | solicitante={}", id, usuario.getId());

        Transaccion transaccion = transaccionRepo.findById(id)
                .orElseThrow(() -> new SecurityException("No tienes permiso para consultar este recurso."));

        validarAcceso(transaccion.getIntentoPago(), usuario.getId());

        return TransaccionResponse.from(transaccion);
    }

    /**
     * Valida que el solicitante sea el cliente dueño de la reserva o el operador que registró el intento.
     */
    private void validarAcceso(IntentoPago intento, Long usuarioSolicitanteId) {
        boolean esDuenio = intento.getReserva().getCliente().getId().equals(usuarioSolicitanteId);

        boolean esOperador = intento.getPagadoPorOperador() != null
                && intento.getPagadoPorOperador().getId().equals(usuarioSolicitanteId);

        if (!esDuenio && !esOperador) {
            log.warn("Acceso denegado a intento pago | id={} | solicitante={}",
                    intento.getId(), usuarioSolicitanteId);
            throw new SecurityException("No tienes permiso para consultar este recurso.");
        }
    }

    /**
     * Calcula métricas agregadas de pagos en un rango de fechas.
     */
    @Transactional(readOnly = true)
    public MetricasPagosResponse calcularMetricas(LocalDate desde, LocalDate hasta) {

        log.info("Calculando métricas de pagos | desde={} hasta={}", desde, hasta);

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(LocalTime.MAX);

        BigDecimal ingresosBrutos = transaccionRepo.sumarMontosPorTipoYEstado(
                TipoMovimientoTransaccion.PAGO, PagoEstado.REALIZADO, inicio, fin);

        BigDecimal totalReembolsos = transaccionRepo.sumarMontosPorTipoYEstado(
                TipoMovimientoTransaccion.REEMBOLSO, PagoEstado.REEMBOLSADO, inicio, fin);

        BigDecimal ingresosNetos = ingresosBrutos.subtract(totalReembolsos);

        long cantidad = transaccionRepo.contarPorTipoYEstado(
                TipoMovimientoTransaccion.PAGO, PagoEstado.REALIZADO, inicio, fin);

        BigDecimal ticketPromedio = cantidad > 0
                ? ingresosBrutos.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        log.info("Métricas calculadas | ingresosBrutos={} reembolsos={} neto={} cantidad={}",
                ingresosBrutos, totalReembolsos, ingresosNetos, cantidad);

        return new MetricasPagosResponse(desde, hasta,
                ingresosBrutos, totalReembolsos, ingresosNetos, ticketPromedio, cantidad);
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
        return usuarioRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new InvalidCredentialsException("Usuario autenticado no encontrado"));
    }
}