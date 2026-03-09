package horse_reserved.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import horse_reserved.exception.RecaptchaVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class RecaptchaService {

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    @Value("${recaptcha.secret-key}")
    private String secretKey;

    private final RestClient restClient = RestClient.create();

    public void verify(String token) {
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
