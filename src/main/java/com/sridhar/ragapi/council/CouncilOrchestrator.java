package com.sridhar.ragapi.council;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CouncilOrchestrator {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public CouncilOrchestrator(ChatClient.Builder chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient.build();
        this.objectMapper = objectMapper;
    }

    public String refine(String originalQuery, String retrievedChunks,String draftResponse){
        // STEP 1: CRITIC -- audits draft against retrieved context
        String criticPrompt =
                "You are a factual accuracy critic reviewing an AI-generated answer.\n\n" +
        "RULES: The ONLY valid source of facts is the RETRIEVED CONTEXT.\n" +
                "If a claim is not in the context, it is a hallucination. Quote it.\n" +
                "Respond ONLY as JSON. No preamble.\n\n" +
                "QUESTION:\n" + originalQuery + "\n\nCONTEXT:\n" + retrievedChunks +
                "\n\nDRAFT:\n" + draftResponse + "\n\n" +
                "{\"hallucination\": true/false, \"hallucination_detail\": \"...\"," +
                "\"missing_info\": \"...\", \"clarity_issue\": \"...\"," +
                "\"verdict\": \"ACCEPTABLE\" | \"NEEDS_IMPROVEMENT\"}";
        String critiqueJson = chatClient.prompt().user(criticPrompt).call().content();
        log.debug("Council critique JSON : {}", critiqueJson);
        // FAST-PATH: strip Markdown fences, parse verdict via Jackson -- never use String.contains()
        String cleaned =
                critiqueJson.replaceAll("(?s)```json\\s*","").replaceAll("(?s)```\\s*","").trim();
        log.info("Council critique Cleaned : {}", cleaned);
        int b = cleaned.indexOf('{'), e = cleaned.lastIndexOf('}');
        if (b >= 0 && e > b) cleaned = cleaned.substring(b, e + 1);
        String verdict = "NEEDS_IMPROVEMENT";
        try { verdict =
                objectMapper.readTree(cleaned).path("verdict").asText("NEEDS_IMPROVEMENT"); }
        catch (Exception ex) { log.warn("Council: unparseable Critic JSON, fast-path taken"); verdict = "ACCEPTABLE"; }
            if ("ACCEPTABLE".equalsIgnoreCase(verdict)) return draftResponse;

        // STEP 2: REFINER -- surgical corrections only
        String refinerPrompt =
                "You are a senior technical writer improving an AI answer.\n\n" +
                        "CONSTRAINTS: Stay grounded in RETRIEVED CONTEXT only.\n" +
                        "Fix only what the critic flagged. Do not rewrite what was correct.\n" +
                        "QUESTION:\n" + originalQuery + "\n\nCONTEXT:\n" + retrievedChunks +
                        "\n\nDRAFT:\n" + draftResponse +
                        "\n\nCRITIC FEEDBACK:\n" + critiqueJson + "\n\nWrite the improved answer now:";
        return chatClient.prompt().user(refinerPrompt).call().content();
    }
}
