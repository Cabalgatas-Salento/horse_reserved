package horse_reserved.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;

    @InjectMocks EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "no-reply@test.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:4200");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    @Test
    void sendPasswordResetEmail_invocaMailSenderSend() {
        emailService.sendPasswordResetEmail("user@test.com", "Juan", "token-uuid");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendReservaConfirmacionEmail_invocaMailSender() {
        emailService.sendReservaConfirmacionEmail(
                "user@test.com", "Juan", 1L, "Ruta Bosque",
                LocalDate.now().plusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0),
                2, BigDecimal.valueOf(50_000), BigDecimal.valueOf(100_000),
                List.of("Ana García", "Luis Pérez"));

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendReservaCancelacionEmail_invocaMailSender() {
        emailService.sendReservaCancelacionEmail(
                "user@test.com", "Juan", 1L, "Ruta Bosque",
                LocalDate.now().plusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0));

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendReservaActualizacionEmail_invocaMailSender() {
        emailService.sendReservaActualizacionEmail(
                "user@test.com", "Juan", 1L, "Ruta Bosque",
                LocalDate.now().plusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0),
                2, BigDecimal.valueOf(50_000), BigDecimal.valueOf(100_000),
                List.of("Ana García"));

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendProgramacionSalidaEmail_invocaMailSender() {
        emailService.sendProgramacionSalidaEmail(
                "user@test.com", "Juan", 1L, "Ruta Bosque", "Hermosa ruta",
                LocalDate.now().plusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0),
                60, List.of("Carlos Guía"), List.of("Tornado"));

        verify(mailSender).send(any(MimeMessage.class));
    }
}
