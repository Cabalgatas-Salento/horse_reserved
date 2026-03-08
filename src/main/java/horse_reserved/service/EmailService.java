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
}
