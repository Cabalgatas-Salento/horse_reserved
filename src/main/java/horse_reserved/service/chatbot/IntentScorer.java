package horse_reserved.service.chatbot;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Encargado de calcular métricas de similitud entre
 * la consulta del usuario y los intents definidos.
 */
@Component
public class IntentScorer {

    /**
     * Calcula la proporción de keywords presentes en el texto.
     * detección por frase con límites de palabra, lo que permite que keywords
     * multi-palabra ("iniciar sesion", "mis reservas") sean detectadas.
     */
    public double scoreByKeywords(String normalizedText, List<String> keywords) {
        if (normalizedText == null || normalizedText.isBlank() || keywords == null || keywords.isEmpty()) {
            return 0.0;
        }

        // Padding de espacios para detección de límites de palabra sin regex costosa
        String paddedText = " " + normalizedText + " ";

        int hits = 0;
        int validKeywords = 0;

        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) continue;
            validKeywords++;

            String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
            String paddedKeyword = " " + normalizedKeyword + " ";

            // Detecta tanto tokens únicos como frases multi-palabra con límite de palabra
            if (paddedText.contains(paddedKeyword)) {
                hits++;
            }
        }

        if (validKeywords == 0) return 0.0;
        return (double) hits / (double) validKeywords;
    }

    /**
     * Calcula la similitud del texto frente a un conjunto de utterances.
     *
     * detección de contenimiento (containment) entre
     * la query y las utterances, antes de caer al Jaccard. Esto cubre
     * casos donde el usuario escribe variaciones más largas o más cortas
     * de una utterance definida en el JSON.
     */
    public double scoreByUtterances(String normalizedText, List<String> utterances, TextNormalizer normalizer) {
        if (normalizedText == null || normalizedText.isBlank() || utterances == null || utterances.isEmpty()) {
            return 0.0;
        }

        double best = 0.0;

        for (String utterance : utterances) {
            if (utterance == null || utterance.isBlank()) continue;
            String u = normalizer.normalizeBasic(utterance);

            // Prioridad 1: coincidencia exacta -> score perfecto
            if (normalizedText.equals(u)) {
                return 1.0;
            }

            // Prioridad 2: contenimiento con límites de palabra
            double containment = containmentSimilarity(normalizedText, u);
            if (containment > best) best = containment;

            // Prioridad 3: similitud Jaccard como respaldo
            double jaccard = jaccardTokenSimilarity(normalizedText, u);
            if (jaccard > best) best = jaccard;
        }

        return best;
    }

    /**
     * Detecta si uno de los dos textos está contenido en el otro
     * con respeto de límites de palabra (word-boundary safe).
     *
     * - utterance contenida en query: el usuario amplió la frase -> score alto (0.82–0.92)
     * - query contenida en utterance: el usuario acortó la frase -> score medio-alto (0.75–0.85)
     */
    private double containmentSimilarity(String query, String utterance) {
        String pQuery = " " + query + " ";
        String pUtterance = " " + utterance + " ";

        // La utterance del JSON está completamente en la query del usuario
        if (pQuery.contains(pUtterance)) {
            double ratio = (double) utterance.length() / (double) query.length();
            return 0.82 + 0.10 * ratio; // entre 0.82 y 0.92
        }

        // La query del usuario está completamente en la utterance del JSON
        if (pUtterance.contains(pQuery)) {
            double ratio = (double) query.length() / (double) utterance.length();
            return 0.75 + 0.10 * ratio; // entre 0.75 y 0.85
        }

        return 0.0;
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