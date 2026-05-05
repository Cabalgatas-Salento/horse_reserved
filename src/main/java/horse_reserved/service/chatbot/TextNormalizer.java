package horse_reserved.service.chatbot;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Proporciona utilidades para normalizar texto
 * y facilitar comparaciones consistentes.
 */
@Component
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
     * Aplica normalización incluyendo reemplazo de sinónimos.
     *
     * Aspectos clave:
     * 1. Usa regex con lookbehind/lookahead para límites de palabra,
     *    evitando reemplazar dentro de otras palabras (ej: "log" dentro de "logistica").
     * 2. Ordena las variantes de mayor a menor longitud antes de aplicarlas,
     *    evitando que una variante corta consuma parte de una más larga.
     * 3. Elimina la dependencia del orden de iteración del Map (no determinista en HashMap).
     */
    public String normalizeWithSynonyms(String text, Map<String, List<String>> synonymsByCanonical) {
        String normalized = normalizeBasic(text);
        if (normalized.isBlank() || synonymsByCanonical == null || synonymsByCanonical.isEmpty()) {
            return normalized;
        }

        // Construir lista plana de (variante -> canónico) para poder ordenar globalmente
        List<Map.Entry<String, String>> replacements = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : synonymsByCanonical.entrySet()) {
            String canonical = normalizeBasic(entry.getKey());
            List<String> variants = entry.getValue() == null ? Collections.emptyList() : entry.getValue();

            for (String variant : variants) {
                String normalizedVariant = normalizeBasic(variant);
                if (!normalizedVariant.isBlank() && !normalizedVariant.equals(canonical)) {
                    replacements.add(Map.entry(normalizedVariant, canonical));
                }
            }
        }

        // Ordenar de mayor a menor longitud: procesar primero las frases más específicas
        replacements.sort((a, b) -> b.getKey().length() - a.getKey().length());

        for (Map.Entry<String, String> replacement : replacements) {
            String variant = replacement.getKey();
            String canonical = replacement.getValue();

            // Límites de palabra seguros para texto ya normalizado [a-z0-9\s]
            String wordBoundaryPattern = "(?<![a-z0-9])" + Pattern.quote(variant) + "(?![a-z0-9])";
            normalized = normalized.replaceAll(wordBoundaryPattern, Matcher.quoteReplacement(canonical));
        }

        return normalized;
    }

    public String expandWithSynonyms(String normalizedText, Map<String, List<String>> synonyms) {
        if (normalizedText == null || normalizedText.isBlank() || synonyms == null || synonyms.isEmpty()) {
            return normalizedText;
        }

        StringBuilder expanded = new StringBuilder(normalizedText);

        // Padding para evitar falsos positivos parciales
        String paddedText = " " + normalizedText + " ";

        for (Map.Entry<String, List<String>> entry : synonyms.entrySet()) {
            String key = entry.getKey();

            if (key == null || key.isBlank()) continue;

            String normalizedKey = key.trim().toLowerCase(Locale.ROOT);
            String paddedKey = " " + normalizedKey + " ";

            if (paddedText.contains(paddedKey)) {

                for (String synonym : entry.getValue()) {
                    if (synonym == null || synonym.isBlank()) continue;

                    String normalizedSynonym = synonym.trim().toLowerCase(Locale.ROOT);

                    if (!paddedText.contains(" " + normalizedSynonym + " ")) {
                        expanded.append(" ").append(normalizedSynonym);
                    }
                }
            }
        }

        return expanded.toString();
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