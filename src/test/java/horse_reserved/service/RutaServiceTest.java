package horse_reserved.service;

import horse_reserved.model.Ruta;
import horse_reserved.repository.RutaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RutaServiceTest {

    @Mock RutaRepository rutaRepository;

    @InjectMocks RutaService rutaService;

    @Test
    void findActivas_retornaRutasActivas() {
        Ruta ruta = Ruta.builder().id(1L).nombre("Ruta del Bosque").activa(true).build();
        when(rutaRepository.findByActivaTrue()).thenReturn(List.of(ruta));

        List<Ruta> result = rutaService.findActivas();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNombre()).isEqualTo("Ruta del Bosque");
    }

    @Test
    void findActivas_sinRutas_retornaListaVacia() {
        when(rutaRepository.findByActivaTrue()).thenReturn(List.of());

        List<Ruta> result = rutaService.findActivas();

        assertThat(result).isEmpty();
    }
}
