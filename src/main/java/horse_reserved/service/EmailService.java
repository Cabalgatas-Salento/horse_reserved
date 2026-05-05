package horse_reserved.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Servicio para el envío de correos electrónicos transaccionales
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Envía el correo de restablecimiento de contraseña de forma asíncrona.
     * El @Async evita bloquear el hilo HTTP mientras el SMTP responde.
     *
     * @param toEmail      Dirección del destinatario
     * @param primerNombre Nombre del usuario para personalizar el saludo
     * @param token        UUID del token de restablecimiento
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String primerNombre, String token) {
        String resetLink = frontendUrl + "/auth/reset-password?token=" + token;
        String subject = "Restablecer contraseña - Horse Reserved";
        String htmlBody = buildResetEmailBody(primerNombre, resetLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Correo de restablecimiento enviado a: {}", toEmail);
        } catch (MessagingException | MailException e) {
            // Se loguea el error pero NO se propaga: el endpoint ya devolvió 200.
            // Un fallo de SMTP no debe revelar al cliente si el email existe.
            log.error("Error al enviar correo de restablecimiento a {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendReservaConfirmacionEmail(
            String toEmail, String clienteNombre, Long reservaId,
            String rutaNombre, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
            int cantPersonas, BigDecimal precioUnitario, BigDecimal precioTotal,
            List<String> participantesNombres) {
        String subject = "Reserva confirmada #" + reservaId + " - Horse Reserved";
        String htmlBody = buildResumenEmailBody("¡Tu reserva está confirmada!", clienteNombre, reservaId,
                rutaNombre, fecha, horaInicio, horaFin, cantPersonas, precioUnitario, precioTotal,
                participantesNombres);
        sendHtml(toEmail, subject, htmlBody, "confirmación de reserva");
    }

    @Async
    public void sendReservaActualizacionEmail(
            String toEmail, String clienteNombre, Long reservaId,
            String rutaNombre, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
            int cantPersonas, BigDecimal precioUnitario, BigDecimal precioTotal,
            List<String> participantesNombres) {
        String subject = "Reserva actualizada #" + reservaId + " - Horse Reserved";
        String htmlBody = buildResumenEmailBody("Tu reserva ha sido actualizada", clienteNombre, reservaId,
                rutaNombre, fecha, horaInicio, horaFin, cantPersonas, precioUnitario, precioTotal,
                participantesNombres);
        sendHtml(toEmail, subject, htmlBody, "actualización de reserva");
    }

    @Async
    public void sendProgramacionSalidaEmail(
            String toEmail, String clienteNombre, Long reservaId,
            String rutaNombre, String rutaDescripcion,
            LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, int duracionMinutos,
            List<String> guiasNombres, List<String> caballosNombres) {
        String subject = "Programación de tu salida #" + reservaId + " - Horse Reserved";
        String htmlBody = buildProgramacionEmailBody("Programación de tu salida", clienteNombre, reservaId,
                rutaNombre, rutaDescripcion, fecha, horaInicio, horaFin, duracionMinutos,
                guiasNombres, caballosNombres);
        sendHtml(toEmail, subject, htmlBody, "programación de salida");
    }

    @Async
    public void sendProgramacionActualizadaEmail(
            String toEmail, String clienteNombre, Long reservaId,
            String rutaNombre, String rutaDescripcion,
            LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, int duracionMinutos,
            List<String> guiasNombres, List<String> caballosNombres) {
        String subject = "Programación actualizada de tu salida #" + reservaId + " - Horse Reserved";
        String htmlBody = buildProgramacionEmailBody("Programación actualizada de tu salida", clienteNombre,
                reservaId, rutaNombre, rutaDescripcion, fecha, horaInicio, horaFin, duracionMinutos,
                guiasNombres, caballosNombres);
        sendHtml(toEmail, subject, htmlBody, "programación actualizada de salida");
    }

    @Async
    public void sendReservaCancelacionEmail(
            String toEmail, String clienteNombre, Long reservaId,
            String rutaNombre, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        String subject = "Reserva cancelada #" + reservaId + " - Horse Reserved";
        String htmlBody = buildCancelacionEmailBody(clienteNombre, reservaId, rutaNombre, fecha, horaInicio, horaFin);
        sendHtml(toEmail, subject, htmlBody, "cancelación de reserva");
    }

    private void sendHtml(String toEmail, String subject, String htmlBody, String tipo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Correo de {} enviado a: {}", tipo, toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Error al enviar correo de {} a {}: {}", tipo, toEmail, e.getMessage());
        }
    }

    /**
     * Metodo que permite enviar una notificacion al administrador cuando la cantidad de caballos es baja
     * @param toEmail
     * @param adminNombre
     * @param cantidadActual
     * @param umbralMinimo
     */
    @Async
    public void sendAlertaCaballosEmail(String toEmail, String adminNombre, int cantidadActual, int umbralMinimo) {
        String subject = "⚠️ Alerta: Baja cantidad de caballos disponibles - Horse Reserved";
        String htmlBody = buildAlertaCaballosEmailBody(adminNombre, cantidadActual, umbralMinimo);
        sendHtml(toEmail, subject, htmlBody, "alerta de caballos");
    }

    private String buildAlertaCaballosEmailBody(String adminNombre, int cantidadActual, int umbralMinimo) {
        DateTimeFormatter fechaHoraFmt = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es-CO"));
        String fechaHora = java.time.LocalDateTime.now().format(fechaHoraFmt);

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"><title>Alerta - Horse Reserved</title></head>
            <body style="font-family:Arial,sans-serif;background-color:#f4f4f4;margin:0;padding:20px;">
              <div style="max-width:600px;margin:auto;">
                <div style="background-color:#2c3e50;padding:24px;border-radius:8px 8px 0 0;text-align:center;">
                  <h1 style="color:#ffffff;margin:0;font-size:22px;letter-spacing:1px;">Horse Reserved</h1>
                </div>
                <div style="background-color:#ffffff;padding:40px;border-radius:0 0 8px 8px;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                  <div style="background-color:#fef9e7;border-left:4px solid #f39c12;padding:16px 20px;border-radius:4px;margin-bottom:28px;">
                    <h2 style="color:#d35400;margin:0;font-size:18px;">⚠️ Alerta de disponibilidad</h2>
                  </div>
                  <p style="color:#555;font-size:16px;">
                    Hola, <strong>%s</strong>. Se ha detectado que la cantidad de caballos
                    disponibles está por debajo del umbral mínimo establecido.
                  </p>
                  <table style="width:100%%;border-collapse:collapse;margin:24px 0;font-size:15px;">
                    <tr style="background-color:#fdf2f2;">
                      <td style="padding:12px 14px;color:#888;font-weight:bold;width:50%%;">Caballos disponibles</td>
                      <td style="padding:12px 14px;color:#c0392b;font-weight:bold;font-size:20px;">%d</td>
                    </tr>
                    <tr>
                      <td style="padding:12px 14px;color:#888;font-weight:bold;">Umbral mínimo configurado</td>
                      <td style="padding:12px 14px;color:#2c3e50;font-weight:bold;font-size:20px;">%d</td>
                    </tr>
                    <tr style="background-color:#f8f9fa;">
                      <td style="padding:12px 14px;color:#888;font-weight:bold;">Fecha y hora de alerta</td>
                      <td style="padding:12px 14px;color:#555;">%s</td>
                    </tr>
                  </table>
                  <div style="background-color:#eaf4fb;border-left:4px solid #2980b9;padding:16px 20px;border-radius:4px;margin:24px 0;">
                    <h3 style="color:#2c3e50;margin-top:0;font-size:15px;">Acciones recomendadas</h3>
                    <ul style="color:#555;font-size:14px;padding-left:18px;margin:0;">
                      <li style="margin-bottom:6px;">Verificar el estado de salud de los caballos registrados.</li>
                      <li style="margin-bottom:6px;">Revisar si hay caballos marcados como inactivos o no disponibles.</li>
                      <li>Considerar ajustar la disponibilidad de reservas hasta normalizar el inventario.</li>
                    </ul>
                  </div>
                  <p style="color:#888;font-size:13px;">
                    Este es un correo automático generado por el sistema. Por favor no respondas a este mensaje.
                  </p>
                  <hr style="border:none;border-top:1px solid #eee;margin:32px 0;">
                  <p style="color:#aaa;font-size:12px;text-align:center;">
                    © 2026 Horse Reserved. Todos los derechos reservados.
                  </p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(adminNombre, cantidadActual, umbralMinimo, fechaHora);
    }

    private String buildResetEmailBody(String primerNombre, String resetLink) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Restablecer contraseña</title>
                </head>
                <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px;">
                    <div style="max-width: 600px; margin: auto; background-color: #ffffff;
                                border-radius: 8px; padding: 40px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                        <h2 style="color: #2c3e50;">Hola, %s</h2>
                        <p style="color: #555; font-size: 16px;">
                            Recibimos una solicitud para restablecer la contraseña de tu cuenta en
                            <strong>Horse Reserved</strong>.
                        </p>
                        <p style="color: #555; font-size: 16px;">
                            Haz clic en el siguiente botón para crear una nueva contraseña.
                            Este enlace es válido por <strong>5 minutos</strong>.
                        </p>
                        <div style="text-align: center; margin: 32px 0;">
                            <a href="%s"
                               style="background-color: #2980b9; color: #ffffff; padding: 14px 28px;
                                      text-decoration: none; border-radius: 6px; font-size: 16px;
                                      display: inline-block;">
                                Restablecer contraseña
                            </a>
                        </div>
                        <p style="color: #888; font-size: 13px;">
                            Si no solicitaste este cambio, puedes ignorar este correo.
                            Tu contraseña permanecerá sin cambios.
                        </p>
                        <p style="color: #888; font-size: 13px;">
                            O copia y pega este enlace en tu navegador:<br>
                            <a href="%s" style="color: #2980b9;">%s</a>
                        </p>
                        <hr style="border: none; border-top: 1px solid #eee; margin: 32px 0;">
                        <p style="color: #aaa; font-size: 12px; text-align: center;">
                            © 2026 Horse Reserved. Todos los derechos reservados.
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(primerNombre, resetLink, resetLink, resetLink);
    }

    private String buildResumenEmailBody(
            String encabezado, String clienteNombre, Long reservaId, String rutaNombre,
            LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
            int cantPersonas, BigDecimal precioUnitario, BigDecimal precioTotal,
            List<String> participantesNombres) {

        DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"));
        NumberFormat precioFmt = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO"));

        StringBuilder participantesHtml = new StringBuilder();
        for (String nombre : participantesNombres) {
            participantesHtml.append("<li style=\"color:#555;font-size:15px;margin-bottom:6px;\">")
                    .append(nombre).append("</li>");
        }

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"><title>Reserva - Horse Reserved</title></head>
                <body style="font-family:Arial,sans-serif;background-color:#f4f4f4;margin:0;padding:20px;">
                  <div style="max-width:600px;margin:auto;">
                    <div style="background-color:#2c3e50;padding:24px;border-radius:8px 8px 0 0;text-align:center;">
                      <h1 style="color:#ffffff;margin:0;font-size:22px;letter-spacing:1px;">Horse Reserved</h1>
                    </div>
                    <div style="background-color:#ffffff;padding:40px;border-radius:0 0 8px 8px;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                      <h2 style="color:#2c3e50;margin-top:0;">%s</h2>
                      <p style="color:#555;font-size:16px;">
                        Hola, <strong>%s</strong>. Los detalles de tu reserva <strong>#%d</strong> son los siguientes.
                      </p>
                      <table style="width:100%%;border-collapse:collapse;margin:24px 0;font-size:15px;">
                        <tr style="background-color:#f8f9fa;">
                          <td style="padding:10px 14px;color:#888;font-weight:bold;width:40%%;">Ruta</td>
                          <td style="padding:10px 14px;color:#2c3e50;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:10px 14px;color:#888;font-weight:bold;">Fecha</td>
                          <td style="padding:10px 14px;color:#2c3e50;">%s</td>
                        </tr>
                        <tr style="background-color:#f8f9fa;">
                          <td style="padding:10px 14px;color:#888;font-weight:bold;">Horario</td>
                          <td style="padding:10px 14px;color:#2c3e50;">%s – %s</td>
                        </tr>
                        <tr>
                          <td style="padding:10px 14px;color:#888;font-weight:bold;">Personas</td>
                          <td style="padding:10px 14px;color:#2c3e50;">%d</td>
                        </tr>
                        <tr style="background-color:#f8f9fa;">
                          <td style="padding:10px 14px;color:#888;font-weight:bold;">Precio por persona</td>
                          <td style="padding:10px 14px;color:#2c3e50;">$ %s COP</td>
                        </tr>
                        <tr style="border-top:2px solid #2980b9;">
                          <td style="padding:12px 14px;color:#2c3e50;font-weight:bold;font-size:16px;">Total a pagar</td>
                          <td style="padding:12px 14px;color:#2980b9;font-weight:bold;font-size:16px;">$ %s COP</td>
                        </tr>
                      </table>
                      <h3 style="color:#2c3e50;font-size:16px;border-bottom:2px solid #2980b9;padding-bottom:8px;">Participantes</h3>
                      <ul style="padding-left:20px;margin:12px 0 24px 0;">
                        %s
                      </ul>
                      <p style="color:#888;font-size:13px;">
                        Si tienes alguna duda o necesitas hacer cambios, contáctanos con el número de reserva.
                      </p>
                      <hr style="border:none;border-top:1px solid #eee;margin:32px 0;">
                      <p style="color:#aaa;font-size:12px;text-align:center;">
                        © 2026 Horse Reserved. Todos los derechos reservados.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                encabezado,
                clienteNombre, reservaId,
                rutaNombre,
                fecha.format(fechaFmt),
                horaInicio.format(DateTimeFormatter.ofPattern("HH:mm")),
                horaFin.format(DateTimeFormatter.ofPattern("HH:mm")),
                cantPersonas,
                precioFmt.format(precioUnitario),
                precioFmt.format(precioTotal),
                participantesHtml.toString());
    }

    private String buildProgramacionEmailBody(
            String encabezado, String clienteNombre, Long reservaId,
            String rutaNombre, String rutaDescripcion,
            LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, int duracionMinutos,
            List<String> guiasNombres, List<String> caballosNombres) {

        DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"));

        String guiasHtml = guiasNombres.isEmpty()
                ? "<li style=\"color:#555;font-size:15px;\">Por asignar</li>"
                : guiasNombres.stream()
                        .map(g -> "<li style=\"color:#555;font-size:15px;margin-bottom:4px;\">" + g + "</li>")
                        .reduce("", String::concat);

        String caballosHtml = caballosNombres.stream()
                .map(c -> "<li style=\"color:#555;font-size:15px;margin-bottom:4px;\">" + c + "</li>")
                .reduce("", String::concat);

        String descripcionHtml = (rutaDescripcion != null && !rutaDescripcion.isBlank())
                ? "<p style=\"color:#555;font-size:15px;\">" + rutaDescripcion + "</p>"
                : "";

        int horas = duracionMinutos / 60;
        int minutos = duracionMinutos % 60;
        String duracionTexto = horas > 0
                ? horas + " h" + (minutos > 0 ? " " + minutos + " min" : "")
                : minutos + " min";

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"><title>Salida - Horse Reserved</title></head>
                <body style="font-family:Arial,sans-serif;background-color:#f4f4f4;margin:0;padding:20px;">
                  <div style="max-width:600px;margin:auto;">
                    <div style="background-color:#2c3e50;padding:24px;border-radius:8px 8px 0 0;text-align:center;">
                      <h1 style="color:#ffffff;margin:0;font-size:22px;letter-spacing:1px;">Horse Reserved</h1>
                    </div>
                    <div style="background-color:#ffffff;padding:40px;border-radius:0 0 8px 8px;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                      <h2 style="color:#2c3e50;margin-top:0;">%s</h2>
                      <p style="color:#555;font-size:16px;">
                        Hola, <strong>%s</strong>. Aquí están los detalles de tu salida para la reserva <strong>#%d</strong>.
                      </p>
                      %s
                      <h3 style="color:#2c3e50;font-size:16px;border-bottom:2px solid #2980b9;padding-bottom:8px;">Detalles de la salida</h3>
                      <table style="width:100%%;border-collapse:collapse;margin:16px 0;font-size:15px;">
                        <tr style="background-color:#f8f9fa;">
                          <td style="padding:10px 14px;color:#888;font-weight:bold;width:40%%;">Ruta</td>
                          <td style="padding:10px 14px;color:#2c3e50;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:10px 14px;color:#888;font-weight:bold;">Fecha</td>
                          <td style="padding:10px 14px;color:#2c3e50;">%s</td>
                        </tr>
                        <tr style="background-color:#f8f9fa;">
                          <td style="padding:10px 14px;color:#888;font-weight:bold;">Hora de inicio</td>
                          <td style="padding:10px 14px;color:#2c3e50;font-weight:bold;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:10px 14px;color:#888;font-weight:bold;">Hora de fin</td>
                          <td style="padding:10px 14px;color:#2c3e50;">%s</td>
                        </tr>
                        <tr style="background-color:#f8f9fa;">
                          <td style="padding:10px 14px;color:#888;font-weight:bold;">Duración</td>
                          <td style="padding:10px 14px;color:#2c3e50;">%s</td>
                        </tr>
                      </table>
                      <h3 style="color:#2c3e50;font-size:16px;border-bottom:2px solid #2980b9;padding-bottom:8px;">Guías asignados</h3>
                      <ul style="padding-left:20px;margin:12px 0 24px 0;">
                        %s
                      </ul>
                      <h3 style="color:#2c3e50;font-size:16px;border-bottom:2px solid #2980b9;padding-bottom:8px;">Caballos asignados</h3>
                      <ul style="padding-left:20px;margin:12px 0 24px 0;">
                        %s
                      </ul>
                      <div style="background-color:#eaf4fb;border-left:4px solid #2980b9;padding:16px 20px;border-radius:4px;margin:24px 0;">
                        <h3 style="color:#2c3e50;margin-top:0;font-size:15px;">Recomendaciones</h3>
                        <ul style="color:#555;font-size:14px;padding-left:18px;margin:0;">
                          <li style="margin-bottom:6px;">Llega <strong>15 minutos antes</strong> del horario de salida.</li>
                          <li style="margin-bottom:6px;">Usa ropa cómoda y calzado cerrado.</li>
                          <li style="margin-bottom:6px;">Lleva agua y protector solar.</li>
                          <li>No uses el teléfono durante el recorrido por seguridad.</li>
                        </ul>
                      </div>
                      <hr style="border:none;border-top:1px solid #eee;margin:32px 0;">
                      <p style="color:#aaa;font-size:12px;text-align:center;">
                        © 2026 Horse Reserved. Todos los derechos reservados.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                encabezado,
                clienteNombre, reservaId,
                descripcionHtml,
                rutaNombre,
                fecha.format(fechaFmt),
                horaInicio.format(DateTimeFormatter.ofPattern("HH:mm")),
                horaFin.format(DateTimeFormatter.ofPattern("HH:mm")),
                duracionTexto,
                guiasHtml,
                caballosHtml);
    }

    private String buildCancelacionEmailBody(
            String clienteNombre, Long reservaId, String rutaNombre,
            LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {

        DateTimeFormatter fechaFmt = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"));

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"><title>Reserva cancelada</title></head>
                <body style="font-family:Arial,sans-serif;background-color:#f4f4f4;margin:0;padding:20px;">
                  <div style="max-width:600px;margin:auto;">
                    <div style="background-color:#2c3e50;padding:24px;border-radius:8px 8px 0 0;text-align:center;">
                      <h1 style="color:#ffffff;margin:0;font-size:22px;letter-spacing:1px;">Horse Reserved</h1>
                    </div>
                    <div style="background-color:#ffffff;padding:40px;border-radius:0 0 8px 8px;box-shadow:0 2px 8px rgba(0,0,0,0.1);">
                      <h2 style="color:#2c3e50;margin-top:0;">Tu reserva ha sido cancelada</h2>
                      <p style="color:#555;font-size:16px;">
                        Hola, <strong>%s</strong>. Te informamos que tu reserva <strong>#%d</strong> ha sido cancelada.
                      </p>
                      <table style="width:100%%;border-collapse:collapse;margin:24px 0;font-size:15px;">
                        <tr style="background-color:#f8f9fa;">
                          <td style="padding:10px 14px;color:#888;font-weight:bold;width:40%%;">Ruta</td>
                          <td style="padding:10px 14px;color:#2c3e50;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:10px 14px;color:#888;font-weight:bold;">Fecha</td>
                          <td style="padding:10px 14px;color:#2c3e50;">%s</td>
                        </tr>
                        <tr style="background-color:#f8f9fa;">
                          <td style="padding:10px 14px;color:#888;font-weight:bold;">Horario</td>
                          <td style="padding:10px 14px;color:#2c3e50;">%s – %s</td>
                        </tr>
                        <tr>
                          <td style="padding:10px 14px;color:#888;font-weight:bold;">Estado</td>
                          <td style="padding:10px 14px;color:#c0392b;font-weight:bold;">Cancelada</td>
                        </tr>
                      </table>
                      <p style="color:#888;font-size:13px;">
                        Si crees que esto es un error o deseas hacer una nueva reserva, contáctanos o visita nuestra plataforma.
                      </p>
                      <hr style="border:none;border-top:1px solid #eee;margin:32px 0;">
                      <p style="color:#aaa;font-size:12px;text-align:center;">
                        © 2026 Horse Reserved. Todos los derechos reservados.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                clienteNombre, reservaId,
                rutaNombre,
                fecha.format(fechaFmt),
                horaInicio.format(DateTimeFormatter.ofPattern("HH:mm")),
                horaFin.format(DateTimeFormatter.ofPattern("HH:mm")));
    }
}
