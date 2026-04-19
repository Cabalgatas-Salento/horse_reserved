package horse_reserved.service.chatbot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizerTest {

    private TextNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new TextNormalizer();
    }

    @Test
    void normalizeBasic_eliminaAcentos() {
        assertThat(normalizer.normalizeBasic("cómo me registro")).isEqualTo("como me registro");
        assertThat(normalizer.normalizeBasic("olvidé mi contraseña")).isEqualTo("olvide mi contrasena");
        assertThat(normalizer.normalizeBasic("cancelación")).isEqualTo("cancelacion");
    }

    @Test
    void normalizeBasic_convierteAMinusculas() {
        assertThat(normalizer.normalizeBasic("RESERVAR")).isEqualTo("reservar");
        assertThat(normalizer.normalizeBasic("Cabalgata Salento")).isEqualTo("cabalgata salento");
    }

    @Test
    void normalizeBasic_eliminaCaracteresEspeciales() {
        assertThat(normalizer.normalizeBasic("¿cómo reservo?")).isEqualTo("como reservo");
        assertThat(normalizer.normalizeBasic("login/register")).isEqualTo("login register");
    }

    @Test
    void normalizeBasic_normalizaEspaciosMultiples() {
        assertThat(normalizer.normalizeBasic("ver  mis   reservas")).isEqualTo("ver mis reservas");
    }

    @Test
    void normalizeBasic_textoVacioRetornaCadenaVacia() {
        assertThat(normalizer.normalizeBasic("")).isEqualTo("");
        assertThat(normalizer.normalizeBasic("   ")).isEqualTo("");
    }

    @Test
    void normalizeBasic_textoNullRetornaCadenaVacia() {
        assertThat(normalizer.normalizeBasic(null)).isEqualTo("");
    }
}
