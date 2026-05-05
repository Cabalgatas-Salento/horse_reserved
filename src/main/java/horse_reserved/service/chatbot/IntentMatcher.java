package horse_reserved.service.chatbot;

import horse_reserved.model.chatbot.FaqIntent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
/**
 * Responsable de encontrar el intent más relevante
 * para una consulta del usuario.
 */
public class IntentMatcher {

    private final TextNormalizer normalizer;
    private final IntentScorer scorer;
    private static final double EPS = 1e-6;

    public IntentMatcher(TextNormalizer normalizer, IntentScorer scorer) {
        this.normalizer = normalizer;
        this.scorer = scorer;
    }

    /**
     * Evalúa todos los intents y retorna el mejor candidato
     * junto con su score.
     * Aplica una combinación ponderada de similitud por utterances
     * y coincidencia de keywords, incluyendo lógica de desempate.
     * La query se expande con los sinónimos de cada intent antes del scoring.
     */
    public MatchResult findBestMatch(String userQuestion, List<FaqIntent> intents) {
        if (intents == null || intents.isEmpty()) {
            return new MatchResult(null, 0.0);
        }

        String normalized = normalizer.normalizeBasic(userQuestion);

        FaqIntent bestIntent = null;
        double bestScore = 0.0;
        double bestUtteranceScore = 0.0;
        double bestKeywordScore = 0.0;

        for (FaqIntent intent : intents) {

            String expanded = normalizer.expandWithSynonyms(normalized, intent.getSynonyms());

            //evaluar ambas versiones
            double utteranceScoreOriginal = scorer.scoreByUtterances(
                    normalized, intent.getUtterances(), normalizer);

            double utteranceScoreExpanded = scorer.scoreByUtterances(
                    expanded, intent.getUtterances(), normalizer);

            double utteranceScore = Math.max(utteranceScoreOriginal, utteranceScoreExpanded);

            double keywordScoreOriginal = scorer.scoreByKeywords(
                    normalized, normalizeList(intent.getKeywords()));

            double keywordScoreExpanded = scorer.scoreByKeywords(
                    expanded, normalizeList(intent.getKeywords()));

            double keywordScore = Math.max(keywordScoreOriginal, keywordScoreExpanded);

            //combinación adaptativa
            double finalScore;

            if (utteranceScore >= 0.9) {
                finalScore = utteranceScore;
            } else if (keywordScore >= 0.8) {
                finalScore = keywordScore;
            } else {
                finalScore = Math.max(
                        utteranceScore * 0.7 + keywordScore * 0.3,
                        Math.max(utteranceScore, keywordScore)
                );
            }

            //desempate
            boolean betterScore = finalScore > bestScore;
            boolean tieScore = Math.abs(finalScore - bestScore) < EPS;

            boolean betterUtterance = utteranceScore > bestUtteranceScore;
            boolean tieUtterance = Math.abs(utteranceScore - bestUtteranceScore) < EPS;

            boolean betterKeyword = keywordScore > bestKeywordScore;

            if (
                    betterScore ||
                            (tieScore && betterUtterance) ||
                            (tieScore && tieUtterance && betterKeyword)
            ) {
                bestScore = finalScore;
                bestIntent = intent;
                bestUtteranceScore = utteranceScore;
                bestKeywordScore = keywordScore;
            }
        }

        return new MatchResult(bestIntent, bestScore);
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) return List.of();
        return values.stream()
                .map(v -> normalizer.normalizeBasic(v))
                .toList();
    }

    public record MatchResult(FaqIntent intent, double score) {
    }

}