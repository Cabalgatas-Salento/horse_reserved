package horse_reserved.service;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import horse_reserved.dto.request.CrearPreferenciaMpRequest;
import horse_reserved.dto.response.IntentoPagoResponse;
import horse_reserved.dto.response.PreferenciaMpResponse;
import horse_reserved.exception.AccessDeniedBusinessException;
import horse_reserved.exception.BusinessRuleException;
import horse_reserved.exception.InvalidCredentialsException;
import horse_reserved.exception.ResourceNotFoundException;
import horse_reserved.model.IntentoPago;
import horse_reserved.model.MetodoPago;
import horse_reserved.model.PagoEstado;
import horse_reserved.model.Reserva;
import horse_reserved.model.TipoMovimientoTransaccion;
import horse_reserved.model.Transaccion;
import horse_reserved.model.Usuario;
import horse_reserved.repository.IntentoPagoRepository;
import horse_reserved.repository.ReservaRepository;
import horse_reserved.repository.TransaccionRepository;
import horse_reserved.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoMercadoPagoService {

    private final IntentoPagoRepository intentoRepo;
    private final TransaccionRepository transaccionRepo;
    private final ReservaRepository reservaRepo;
    private final UsuarioRepository usuarioRepo;

    @Value("${mercadopago.success-url}")
    private String successUrl;

    @Value("${mercadopago.failure-url}")
    private String failureUrl;

    @Value("${mercadopago.pending-url}")
    private String pendingUrl;

    @Value("${mercadopago.webhook-url:}")
    private String notificationUrl;

    @Transactional
    public PreferenciaMpResponse crearPreferencia(CrearPreferenciaMpRequest req) {
        Usuario usuario = usuarioAutenticado();
        log.info("Creando preferencia MP | reservaId={} usuarioId={}", req.reservaId(), usuario.getId());

        Reserva reserva = reservaRepo.findById(req.reservaId())
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + req.reservaId()));

        if (reserva.getCliente() == null || !reserva.getCliente().getId().equals(usuario.getId())) {
            throw new AccessDeniedBusinessException("No tienes permiso para pagar esta reserva.");
        }

        if (!"reservado".equalsIgnoreCase(reserva.getEstado())) {
            throw new BusinessRuleException("La reserva no está en estado 'reservado'.");
        }

        if (intentoRepo.existsByReservaIdAndEstado(reserva.getId(), PagoEstado.REALIZADO)) {
            throw new BusinessRuleException("Esta reserva ya fue pagada.");
        }

        // Guardar intento primero para obtener el ID y usarlo en la successUrl
        IntentoPago intento = IntentoPago.builder()
                .reserva(reserva)
                .estado(PagoEstado.PENDIENTE)
                .metodoPago(MetodoPago.TARJETA)
                .monto(reserva.getPrecioTotal())
                .moneda("COP")
                .pagadoPorUsuario(usuario)
                .referenciaSimulada("MP-" + UUID.randomUUID())
                .fechaIntento(LocalDateTime.now())
                .build();

        intento = intentoRepo.save(intento);

        Preference preference = crearPreferenciaEnMp(reserva, intento.getId());

        intento.setMpPreferenceId(preference.getId());
        intentoRepo.save(intento);

        log.info("Preferencia MP creada | intentoId={} preferenceId={}", intento.getId(), preference.getId());

        return new PreferenciaMpResponse(
                intento.getId(),
                preference.getId(),
                preference.getInitPoint(),
                preference.getSandboxInitPoint()
        );
    }

    @Transactional
    public void procesarWebhook(String paymentId) {
        log.info("Webhook MP | paymentId={}", paymentId);
        try {
            Payment payment = new PaymentClient().get(Long.parseLong(paymentId));
            String realStatus = payment.getStatus();
            String externalRef = payment.getExternalReference();
            log.info("Pago MP consultado | paymentId={} status={} externalRef={}", paymentId, realStatus, externalRef);

            Long reservaId = Long.parseLong(externalRef);
            Optional<IntentoPago> opt = intentoRepo.findByMpPaymentId(paymentId);
            if (opt.isEmpty()) {
                opt = intentoRepo.findByReservaIdAndEstado(reservaId, PagoEstado.PENDIENTE);
            }

            opt.ifPresentOrElse(intento -> {
                if (intento.getMpPaymentId() == null) {
                    intento.setMpPaymentId(paymentId);
                }
                intento.setMpPaymentStatus(realStatus);

                switch (realStatus) {
                    case "approved" -> {
                        intento.setEstado(PagoEstado.REALIZADO);
                        Transaccion tx = Transaccion.builder()
                                .intentoPago(intento)
                                .tipoMovimiento(TipoMovimientoTransaccion.PAGO)
                                .estado(PagoEstado.REALIZADO)
                                .monto(intento.getMonto())
                                .moneda("COP")
                                .detalle("Pago MP aprobado | paymentId=" + paymentId)
                                .fechaTransaccion(LocalDateTime.now())
                                .build();
                        transaccionRepo.save(tx);
                        log.info("Pago MP aprobado | intentoId={}", intento.getId());
                    }
                    case "rejected" -> {
                        intento.setEstado(PagoEstado.CANCELADO);
                        log.info("Pago MP rechazado | intentoId={}", intento.getId());
                    }
                    default -> log.info("Estado MP intermedio | status={}", realStatus);
                }
                intentoRepo.save(intento);
            }, () -> log.warn("Webhook MP sin intento asociado | paymentId={} reservaId={}", paymentId, reservaId));

        } catch (Exception e) {
            log.error("Error procesando webhook MP | paymentId={} error={}", paymentId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public IntentoPagoResponse consultarEstado(Long intentoId) {
        Usuario usuario = usuarioAutenticado();
        IntentoPago intento = intentoRepo.findById(intentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Intento de pago no encontrado: " + intentoId));

        if (intento.getPagadoPorUsuario() == null ||
                !intento.getPagadoPorUsuario().getId().equals(usuario.getId())) {
            throw new AccessDeniedBusinessException("No tienes permiso para consultar este recurso.");
        }

        return IntentoPagoResponse.from(intento);
    }

    @Transactional
    public void asociarPaymentId(Long intentoId, String mpPaymentId) {
        IntentoPago intento = intentoRepo.findById(intentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Intento de pago no encontrado: " + intentoId));

        if (intento.getMpPaymentId() == null) {
            intento.setMpPaymentId(mpPaymentId);
            intentoRepo.save(intento);
            log.info("mpPaymentId asociado | intentoId={} mpPaymentId={}", intentoId, mpPaymentId);
        }
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    private Preference crearPreferenciaEnMp(Reserva reserva, Long intentoId) {
        try {
            String nombreRuta = Optional.ofNullable(reserva.getSalida())
                    .map(s -> s.getRuta())
                    .map(r -> r.getNombre())
                    .orElse("sin nombre");

            // COP no acepta decimales en unit_price
            var precioSinDecimales = reserva.getPrecioTotal().setScale(0, RoundingMode.HALF_UP);

            log.info("Creando preferencia MP | reservaId={} monto={} intentoId={}", 
                    reserva.getId(), precioSinDecimales, intentoId);

            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title("Reserva de Cabalgata #" + reserva.getId() + " - " + nombreRuta)
                    .quantity(1)
                    .unitPrice(precioSinDecimales)
                    .currencyId("COP")
                    .build();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(successUrl + "?intentoId=" + intentoId)
                    .failure(failureUrl)
                    .pending(pendingUrl)
                    .build();

            log.debug("URLs de retorno configuradas | success={} failure={} pending={}", 
                    successUrl, failureUrl, pendingUrl);

            PreferencePayerRequest payer = PreferencePayerRequest.builder()
                    .email(reserva.getCliente().getEmail())
                    .build();

            // auto_return requiere URL pública con HTTPS — no funciona en localhost
            boolean esUrlPublica = !successUrl.contains("localhost") && !successUrl.contains("127.0.0.1");

            PreferenceRequest.PreferenceRequestBuilder requestBuilder = PreferenceRequest.builder()
                    .items(List.of(item))
                    .backUrls(backUrls)
                    .payer(payer)
                    .externalReference(String.valueOf(reserva.getId()))
                    .statementDescriptor("Horse Reserved")
                    .binaryMode(false);

            if (esUrlPublica) {
                requestBuilder.autoReturn("approved");
            }

            // Solo agregar notification_url si está configurado
            if (notificationUrl != null && !notificationUrl.isBlank()) {
                requestBuilder.notificationUrl(notificationUrl);
                log.debug("Notification URL configurada: {}", notificationUrl);
            }

            PreferenceRequest preferenceRequest = requestBuilder.build();

            log.debug("Enviando preferencia a MP | items={} backUrls.success={} externalRef={} notifUrl={}",
                    preferenceRequest.getItems().size(),
                    preferenceRequest.getBackUrls().getSuccess(),
                    preferenceRequest.getExternalReference(),
                    notificationUrl.isBlank() ? "(no configurada)" : notificationUrl);

            Preference preference = new PreferenceClient().create(preferenceRequest);
            log.info("Preferencia MP creada exitosamente | preferenceId={} initPoint={}", 
                    preference.getId(), preference.getInitPoint());
            
            return preference;

        } catch (MPApiException e) {
            String statusCode = String.valueOf(e.getStatusCode());
            String body = (e.getApiResponse() != null && e.getApiResponse().getContent() != null)
                    ? e.getApiResponse().getContent()
                    : "(sin body)";
            log.error("Error API MP | status={} body={} sdkMsg={}", statusCode, body, e.getMessage());
            throw new RuntimeException("Error MP [" + statusCode + "]: " + (body.isBlank() ? e.getMessage() : body), e);
        } catch (MPException e) {
            log.error("Error SDK MP | mensaje={}", e.getMessage());
            throw new RuntimeException("Error de comunicación con MercadoPago: " + e.getMessage(), e);
        }
    }

    private Usuario usuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new InvalidCredentialsException("Usuario no autenticado");
        }
        return usuarioRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new InvalidCredentialsException("Usuario autenticado no encontrado"));
    }
}
