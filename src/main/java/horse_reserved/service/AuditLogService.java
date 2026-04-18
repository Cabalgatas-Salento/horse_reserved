package horse_reserved.service;

import horse_reserved.model.AuditAccion;
import horse_reserved.model.AuditCategoria;
import horse_reserved.model.AuditLog;
import horse_reserved.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    private void registrar(
            Long usuarioId,
            String usuarioEmail,
            AuditCategoria categoria,
            AuditAccion accion,
            String resultado,
            String detalle,
            String entidadTipo,
            Long entidadId,
            String ipOrigen
    ) {
        try {
            AuditLog entry = AuditLog.builder()
                    .ocurridoEn(LocalDateTime.now(ZoneId.of("America/Bogota")))
                    .usuarioId(usuarioId)
                    .usuarioEmail(usuarioEmail)
                    .categoria(categoria)
                    .accion(accion)
                    .resultado(resultado)
                    .detalle(detalle)
                    .entidadTipo(entidadTipo)
                    .entidadId(entidadId)
                    .ipOrigen(ipOrigen)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Error al persistir audit log — accion={}, usuario={}: {}",
                    accion, usuarioEmail, e.getMessage());
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarExito(Long usuarioId, String email,
                                AuditCategoria categoria, AuditAccion accion,
                                String entidadTipo, Long entidadId, String ip) {
        registrar(usuarioId, email, categoria, accion, "EXITO", null, entidadTipo, entidadId, ip);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallo(Long usuarioId, String email,
                                AuditCategoria categoria, AuditAccion accion,
                                String detalle, String ip) {
        registrar(usuarioId, email, categoria, accion, "FALLO", detalle, null, null, ip);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarErrorSistema(String detalle, String ip) {
        registrar(null, null, AuditCategoria.SISTEMA, AuditAccion.ERROR_INTERNO,
                "ERROR_SISTEMA", detalle, null, null, ip);
    }
}
