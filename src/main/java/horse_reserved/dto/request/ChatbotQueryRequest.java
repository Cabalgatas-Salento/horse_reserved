package horse_reserved.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * DTO de entrada para consultas al chatbot.
 * Encapsula la pregunta del usuario con validaciones básicas.
 */
public class ChatbotQueryRequest {

    @NotBlank(message = "La pregunta es obligatoria")
    @Size(max=300)
    private String question;
}