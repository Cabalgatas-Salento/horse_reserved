package horse_reserved.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotQueryRequest {

    @Size(max = 300)
    @NotBlank
    private String question;

    private String sessionId;

    @Builder.Default
    private Map<String, Object> payload = Map.of();

    @AssertTrue(message = "La pregunta o el payload son obligatorios")
    public boolean hasMessageOrPayload() {
        return (question != null && !question.isBlank())
                || (payload != null && !payload.isEmpty());
    }
}