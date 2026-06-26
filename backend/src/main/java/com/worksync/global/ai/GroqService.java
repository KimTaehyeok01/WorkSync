// Groq REST API(OpenAI 호환)를 호출하여 텍스트 생성을 처리하는 서비스
package com.worksync.global.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GroqService {

    @Value("${groq.api-key}")
    private String apiKey;

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("unchecked")
    public String generate(String prompt) {
        Map<String, Object> body = Map.of(
                "model", MODEL,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                API_URL, new HttpEntity<>(body, headers), Map.class);

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) response.getBody().get("choices");
        Map<String, Object> message =
                (Map<String, Object>) choices.get(0).get("message");

        return (String) message.get("content");
    }
}
