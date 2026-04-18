package horse_reserved.service.chatbot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IntentScorerTest {

    private IntentScorer scorer;
    private TextNormalizer normalizer;

    @BeforeEach
    void setUp() {
        scorer = new IntentScorer();
        normalizer = new TextNormalizer();
    }

    // ── scoreByKeywords ───────────────────────────────────────────────────

    @Test
    void scoreByKeywords_coincidenciaTotal() {
        double score = scorer.scoreByKeywords("como me registro", List.of("registro", "como"));
        assertThat(score).isEqualTo(1.0);
    }

    @Test
    void scoreByKeywords_sinCoincidencias() {
        double score = scorer.scoreByKeywords("ver mis reservas", List.of("registro", "login"));
        assertThat(score).isEqualTo(0.0);
    }

    @Test
    void scoreByKeywords_coincidenciaParcial() {
        double score = scorer.scoreByKeywords("cancelar reserva hoy", List.of("cancelar", "anular", "reserva"));
        assertThat(score).isCloseTo(2.0 / 3.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void scoreByKeywords_textoVacioRetornaCero() {
        double score = scorer.scoreByKeywords("", List.of("registro"));
        assertThat(score).isEqualTo(0.0);
    }

    @Test
    void scoreByKeywords_keywordsVaciasRetornaCero() {
        double score = scorer.scoreByKeywords("quiero registrarme", List.of());
        assertThat(score).isEqualTo(0.0);
    }

    // ── scoreByUtterances ─────────────────────────────────────────────────

    @Test
    void scoreByUtterances_coincidenciaExacta() {
        double score = scorer.scoreByUtterances(
                "como me registro",
                List.of("como me registro", "crear cuenta"),
                normalizer);
        assertThat(score).isEqualTo(1.0);
    }

    @Test
    void scoreByUtterances_similitudParcialJaccard() {
        // "como registro" vs utterance "como me registro" → intersección {como, registro} / unión {como, me, registro} = 2/3
        double score = scorer.scoreByUtterances(
                "como registro",
                List.of("como me registro"),
                normalizer);
        assertThat(score).isCloseTo(2.0 / 3.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void scoreByUtterances_sinSimilitud() {
        double score = scorer.scoreByUtterances(
                "ver rutas disponibles",
                List.of("cambiar contrasena", "recuperar clave"),
                normalizer);
        assertThat(score).isEqualTo(0.0);
    }

    @Test
    void scoreByUtterances_retornaMejorScoreEntreUtterances() {
        double score = scorer.scoreByUtterances(
                "ver mis reservas",
                List.of("cambiar contrasena", "ver mis reservas", "historial reservas"),
                normalizer);
        assertThat(score).isEqualTo(1.0);
    }
}
