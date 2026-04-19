package horse_reserved.service.chatbot;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
/**
 * Encargado de calcular métricas de similitud entre
 * la consulta del usuario y los intents definidos.
 */
public class IntentScorer {

    /**
     * Calcula la proporción de keywords presentes en el texto.
     * Se basa en coincidencias exactas de tokens.
     */
    public double scoreByKeywords(String normalizedText, List<String> keywords) {
        if (normalizedText == null || normalizedText.isBlank() || keywords == null || keywords.isEmpty()) {
            return 0.0;
        }

        Set<String> tokens = new HashSet<>(Arrays.asList(normalizedText.split("\\s+")));

        int hits = 0;
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) continue;

            String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
            if (tokens.contains(normalizedKeyword)) {
                hits++;
            }
        }

        return (double) hits / (double) keywords.size();
    }

    /**
     * Calcula la similitud del texto frente a un conjunto de utterances.
     * Prioriza coincidencias exactas y usa similitud de Jaccard como respaldo.
     */
    public double scoreByUtterances(String normalizedText, List<String> utterances, TextNormalizer normalizer) {
        if (normalizedText == null || normalizedText.isBlank() || utterances == null || utterances.isEmpty()) {
            return 0.0;
        }

        double best = 0.0;

        for (String utterance : utterances) {
            if (utterance == null || utterance.isBlank()) continue;
            String u = normalizer.normalizeBasic(utterance);

            if (normalizedText.equals(u)) {
                return 1.0;
            }

            double jaccard = jaccardTokenSimilarity(normalizedText, u);
            if (jaccard > best) best = jaccard;
        }

        return best;
    }

    /**
     * Calcula la similitud de Jaccard entre dos textos tokenizados.
     */
    private double jaccardTokenSimilarity(String a, String b) {
        Set<String> sa = Arrays.stream(a.split("\\s+"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        Set<String> sb = Arrays.stream(b.split("\\s+"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        if (sa.isEmpty() && sb.isEmpty()) return 1.0;
        if (sa.isEmpty() || sb.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(sa);
        intersection.retainAll(sb);

        Set<String> union = new HashSet<>(sa);
        union.addAll(sb);

        return (double) intersection.size() / (double) union.size();
    }
}