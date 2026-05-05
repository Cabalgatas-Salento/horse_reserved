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

    // ── scoreByKeywords — tests existentes (sin cambios) ─────────────────

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

    @Test
    void scoreByKeywords_fraseMultipalabra_detectaIniciarSesion() {
        double score = scorer.scoreByKeywords(
                "no puedo iniciar sesion hoy",
                List.of("login", "iniciar sesion", "acceder", "entrar", "autenticar"));
        assertThat(score).isGreaterThan(0.0);
        assertThat(score).isCloseTo(1.0 / 5.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void scoreByKeywords_fraseMultipalabra_detectaMisReservas() {
        double score = scorer.scoreByKeywords(
                "ver mis reservas activas",
                List.of("mis reservas", "mias", "historial", "reservaciones"));
        assertThat(score).isCloseTo(1.0 / 4.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void scoreByKeywords_fraseMultipalabra_noMatchaParcial() {
        double score = scorer.scoreByKeywords(
                "ver reservas",
                List.of("mis reservas"));
        assertThat(score).isEqualTo(0.0);
    }

    @Test
    void scoreByKeywords_fraseMultipalabra_noMatchaDentroDeOtraPalabra() {
        double score = scorer.scoreByKeywords(
                "necesito ayuda con logistica",
                List.of("log"));
        assertThat(score).isEqualTo(0.0);
    }

    @Test
    void scoreByKeywords_variosKeywordsMultipalabra_detectaCorrectamente() {
        double score = scorer.scoreByKeywords(
                "quiero crear usuario nuevo",
                List.of("registro", "registrar", "cuenta", "crear usuario", "sign up"));
        assertThat(score).isCloseTo(1.0 / 5.0, org.assertj.core.data.Offset.offset(0.001));
    }

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

    @Test
    void scoreByUtterances_queryContieneUtterance_retornaScoreAlto() {
        double score = scorer.scoreByUtterances(
                "quiero cancelar mi reserva de cabalgata",
                List.of("cancelar reserva", "anular reservacion", "quiero cancelar mi reserva"),
                normalizer);
        assertThat(score).isGreaterThan(0.80);
    }

    @Test
    void scoreByUtterances_utteranceContieneQuery_retornaScoreMedioAlto() {
        double score = scorer.scoreByUtterances(
                "cancelar",
                List.of("cancelar reserva", "anular reservacion"),
                normalizer);
        assertThat(score).isGreaterThan(0.75);
    }

    @Test
    void scoreByUtterances_containmentSuperaJaccard_cuandoQueryEsLarga() {
        double score = scorer.scoreByUtterances(
                "hola buenos dias como me registro en la aplicacion",
                List.of("como me registro"),
                normalizer);
        assertThat(score).isGreaterThan(0.80);
    }
}