package horse_reserved.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import horse_reserved.exception.RecaptchaVerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Arrays;

@Service
public class RecaptchaService {

    private static final Logger log = LoggerFactory.getLogger(RecaptchaService.class);
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    @Value("${recaptcha.secret-key}")
    private String secretKey;

    @Value("${recaptcha.enabled:true}")
    private boolean enabled;

    private final RestClient restClient = RestClient.create();

    public void verify(String token) {
        if (!enabled) return;
        if (token == null || token.isBlank()) {
            throw new RecaptchaVerificationException("El token de reCAPTCHA es obligatorio");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secretKey);
        form.add("response", token);

        RecaptchaResponse response = restClient.post()
                .uri(VERIFY_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(RecaptchaResponse.class);

        if (response == null || !response.success()) {
            String errorCodes = (response != null && response.errorCodes() != null)
                    ? Arrays.toString(response.errorCodes()) : "none";
            String hostname = (response != null) ? response.hostname() : "null";
            log.warn("reCAPTCHA verification failed — errorCodes: {}, hostname: {}", errorCodes, hostname);
            throw new RecaptchaVerificationException("Verificación reCAPTCHA fallida. Por favor, inténtalo de nuevo.");
        }

    }

    private record RecaptchaResponse(
            boolean success,
            @JsonProperty("challenge_ts") String challengeTs,
            String hostname,
            @JsonProperty("error-codes") String[] errorCodes
    ) {}
}
