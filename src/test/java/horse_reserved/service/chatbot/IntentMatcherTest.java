package horse_reserved.service.chatbot;

import horse_reserved.model.chatbot.FaqIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IntentMatcherTest {

    private IntentMatcher matcher;

    @BeforeEach
    void setUp() {
        TextNormalizer normalizer = new TextNormalizer();
        IntentScorer scorer = new IntentScorer();
        matcher = new IntentMatcher(normalizer, scorer);
    }

    private FaqIntent intent(String id, List<String> utterances, List<String> keywords) {
        return FaqIntent.builder()
                .id(id)
                .threshold(0.70)
                .utterances(utterances)
                .keywords(keywords)
                .build();
    }

    private FaqIntent intentWithSynonyms(String id, List<String> utterances, List<String> keywords,
                                          Map<String, List<String>> synonyms) {
        return FaqIntent.builder()
                .id(id)
                .threshold(0.70)
                .utterances(utterances)
                .keywords(keywords)
                .synonyms(synonyms)
                .build();
    }

    @Test
    void findBestMatch_retornaIntentCorrecto() {
        FaqIntent registro = intent("registro_usuario",
                List.of("cómo me registro", "crear cuenta"),
                List.of("registro", "cuenta"));
        FaqIntent reserva = intent("crear_reserva",
                List.of("cómo reservo", "agendar cabalgata"),
                List.of("reserva", "agendar"));

        IntentMatcher.MatchResult result = matcher.findBestMatch("como me registro", List.of(registro, reserva));

        assertThat(result.intent()).isNotNull();
        assertThat(result.intent().getId()).isEqualTo("registro_usuario");
        assertThat(result.score()).isGreaterThan(0.70);
    }

    @Test
    void findBestMatch_listaVaciaRetornaNulo() {
        IntentMatcher.MatchResult result = matcher.findBestMatch("quiero reservar", List.of());
        assertThat(result.intent()).isNull();
        assertThat(result.score()).isEqualTo(0.0);
    }

    @Test
    void findBestMatch_sinCoincidenciaRetornaScoreBajo() {
        FaqIntent registro = intent("registro_usuario",
                List.of("crear cuenta nueva"),
                List.of("registro"));

        IntentMatcher.MatchResult result = matcher.findBestMatch("cuanto cuesta la cabalgata", List.of(registro));

        assertThat(result.score()).isLessThan(0.70);
    }

    @Test
    void findBestMatch_coincidenciaExactaDeUtteranceNoEsPenalizadaPorKeywords() {
        // Reproduce el bug: utteranceScore=1.0 pero keywordScore bajo → finalScore < threshold
        // Con la corrección, utteranceScore==1.0 debe ganar independientemente de keywords.
        FaqIntent registro = intentWithSynonyms("registro_usuario",
                List.of("como me registro", "crear cuenta"),
                List.of("registro", "registrar", "cuenta", "crear usuario", "sign up"),
                Map.of("registrar", List.of("crear", "abrir"), "cuenta", List.of("usuario", "perfil")));

        IntentMatcher.MatchResult result = matcher.findBestMatch("como me registro", List.of(registro));

        assertThat(result.intent()).isNotNull();
        assertThat(result.intent().getId()).isEqualTo("registro_usuario");
        assertThat(result.score()).isEqualTo(1.0);
    }

    @Test
    void findBestMatch_expandeSinonimosAntesDeScoring() {
        // El usuario escribe "abrir cuenta" — "abrir" es sinónimo de "registrar"
        FaqIntent registro = intentWithSynonyms("registro_usuario",
                List.of("registrar cuenta", "crear cuenta"),
                List.of("registrar", "cuenta"),
                Map.of("registrar", List.of("abrir", "crear")));

        IntentMatcher.MatchResult result = matcher.findBestMatch("abrir cuenta", List.of(registro));

        assertThat(result.intent()).isNotNull();
        assertThat(result.intent().getId()).isEqualTo("registro_usuario");
        assertThat(result.score()).isGreaterThan(0.70);
    }
}
