package horse_reserved.controller;

import horse_reserved.dto.request.AuditLogFiltroRequest;
import horse_reserved.dto.response.AuditLogResponse;
import horse_reserved.model.AuditLog;
import horse_reserved.repository.AuditLogRepository;
import horse_reserved.repository.AuditLogSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMINISTRADOR')")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> listar(
            @ModelAttribute AuditLogFiltroRequest filtro) {

        int safeSize = Math.min(filtro.getSize(), 200);
        PageRequest pageable = PageRequest.of(filtro.getPage(), safeSize,
                Sort.by(Sort.Direction.DESC, "ocurridoEn"));

        Page<AuditLog> pagina = auditLogRepository.findAll(
                AuditLogSpec.withFiltros(
                        filtro.getCategoria(),
                        filtro.getUsuarioEmail(),
                        filtro.getResultado(),
                        toLocalStart(filtro.getDesde()),
                        toLocalEnd(filtro.getHasta())),
                pageable
        );

        return ResponseEntity.ok(pagina.map(this::toResponse));
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .ocurridoEn(a.getOcurridoEn().atZone(ZoneId.of("America/Bogota")).toInstant())
                .usuarioId(a.getUsuarioId())
                .usuarioEmail(a.getUsuarioEmail())
                .categoria(a.getCategoria().name())
                .accion(a.getAccion().name())
                .resultado(a.getResultado())
                .detalle(a.getDetalle())
                .entidadTipo(a.getEntidadTipo())
                .entidadId(a.getEntidadId())
                .ipOrigen(a.getIpOrigen())
                .build();
    }

    private LocalDateTime toLocalStart(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime toLocalEnd(LocalDate date) {
        return date == null ? null : date.atTime(23, 59, 59);
    }
}
