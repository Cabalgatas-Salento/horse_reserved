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

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Persiste el log en una transacción independiente (REQUIRES_NEW) y de forma
     * asíncrona (@Async). REQUIRES_NEW garantiza que el log persiste aunque la
     * transacción del llamador haga rollback. El try-catch interno impide que un
     * fallo de auditoría afecte el flujo principal.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(
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
                    .ocurridoEn(Instant.now())
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

    public void registrarExito(Long usuarioId, String email,
                                AuditCategoria categoria, AuditAccion accion,
                                String entidadTipo, Long entidadId, String ip) {
        registrar(usuarioId, email, categoria, accion, "EXITO", null, entidadTipo, entidadId, ip);
    }

    public void registrarFallo(Long usuarioId, String email,
                                AuditCategoria categoria, AuditAccion accion,
                                String detalle, String ip) {
        registrar(usuarioId, email, categoria, accion, "FALLO", detalle, null, null, ip);
    }

    public void registrarErrorSistema(String detalle, String ip) {
        registrar(null, null, AuditCategoria.SISTEMA, AuditAccion.ERROR_INTERNO,
                "ERROR_SISTEMA", detalle, null, null, ip);
    }
}
