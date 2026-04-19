package horse_reserved.service;

import horse_reserved.repository.ReservaRepository;
import horse_reserved.repository.SalidaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalidaSchedulerService {

    private final SalidaRepository salidaRepository;
    private final ReservaRepository reservaRepository;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void completarSalidasVencidas() {
        LocalDateTime now = LocalDateTime.now();

        List<Long> ids = salidaRepository.findProgramadasHastaHoy(now.toLocalDate())
                .stream()
                .filter(s -> !LocalDateTime.of(s.getFechaProgramada(), s.getTiempoFin())
                        .plusMinutes(30).isAfter(now))
                .map(horse_reserved.model.Salida::getId)
                .toList();

        if (ids.isEmpty()) return;

        int salidas = salidaRepository.completarPorIds(ids);
        int reservas = reservaRepository.completarPorSalidaIds(ids);
        log.info("Auto-completado: {} salidas, {} reservas", salidas, reservas);
    }
}
