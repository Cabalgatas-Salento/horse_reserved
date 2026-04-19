package horse_reserved.service.chatbot;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Component
/**
 * Proporciona utilidades para normalizar texto
 * y facilitar comparaciones consistentes.
 */
public class TextNormalizer {

    /**
     * Normaliza texto eliminando acentos, caracteres especiales
     * y estandarizando formato.
     */
    public String normalizeBasic(String text) {
        if (text == null || text.isBlank()) return "";

        String lower = text.toLowerCase(Locale.ROOT).trim();

        String noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        return noAccents
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Aplica normalización incluyendo reemplazo de sinónimos
     * definidos por un mapa canonical -> variantes.
     */
    public String normalizeWithSynonyms(String text, Map<String, List<String>> synonymsByCanonical) {
        String normalized = normalizeBasic(text);
        if (normalized.isBlank() || synonymsByCanonical == null || synonymsByCanonical.isEmpty()) {
            return normalized;
        }

        for (Map.Entry<String, List<String>> entry : synonymsByCanonical.entrySet()) {
            String canonical = normalizeBasic(entry.getKey());

            for (String variant : entry.getValue()) {
                String normalizedVariant = normalizeBasic(variant);
                if (!normalizedVariant.isBlank()) {
                    normalized = normalized.replace(normalizedVariant, canonical);
                }
            }
        }

        return normalized;
    }

    /**
     * Construye un mapa inverso de sinónimos para acceso rápido.
     */
    private Map<String, String> buildReverseSynonymsMap(Map<String, List<String>> synonymsByCanonical) {
        Map<String, String> reverse = new HashMap<>();

        for (Map.Entry<String, List<String>> e : synonymsByCanonical.entrySet()) {
            String canonical = normalizeBasic(e.getKey());
            reverse.put(canonical, canonical);

            List<String> variants = e.getValue() == null ? Collections.emptyList() : e.getValue();
            for (String variant : variants) {
                String normalizedVariant = normalizeBasic(variant);
                if (!normalizedVariant.isBlank()) {
                    reverse.put(normalizedVariant, canonical);
                }
            }
        }

        return reverse;
    }
}