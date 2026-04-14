package com.sridhar.ragapi.council;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouncilOrchestratorTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ChatClient chatClient;

    private CouncilOrchestrator councilOrchestrator;

    @Mock
    private ChatResponseMetadata chatResponseMetadata;

    @Mock
    private Usage usage;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        councilOrchestrator = new CouncilOrchestrator(chatClientBuilder, objectMapper);
    }
    @Mock
    private ChatClient.ChatClientRequestSpec promptSpec;

    @Mock
    private ChatClient.CallResponseSpec callSpec;

    @Mock
    private ChatResponse chatResponse;

    @Mock
    private Generation generation;
    @Mock
    private ChatResponse criticResponse;

    @Mock
    private ChatResponse refinerResponse;
    @Mock
    private Generation refinerGeneration;

    @Test
    void whenVerdictAcceptable_returnsDraftWithoutRefiner() {
        // Chain stub — each call returns the next mock in the chain
        when(chatClient.prompt(anyString())).thenReturn(promptSpec);
        when(promptSpec.call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(new AssistantMessage("{\"verdict\": \"ACCEPTABLE\"}"));
        when(chatResponse.getMetadata()).thenReturn(chatResponseMetadata);
        when(chatResponseMetadata.getUsage()).thenReturn(usage);
        when(usage.getTotalTokens()).thenReturn(Math.toIntExact(100L));
        // ACT
        String result = councilOrchestrator.refine("what is RAG?", "chunk1", "draft answer");

        // ASSERT
        assertThat(result).isEqualTo("draft answer");
    }

    @Test
    void whenVerdictUnacceptable_returnsDraftWithRefiner() {
        // Chain stub — each call returns the next mock in the chain
        when(chatClient.prompt(anyString())).thenReturn(promptSpec);
        when(promptSpec.call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(criticResponse).thenReturn(refinerResponse);
        when(criticResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(new AssistantMessage("{\"verdict\": \"NEEDS_IMPROVEMENT\"}"));
        when(criticResponse.getMetadata()).thenReturn(chatResponseMetadata);
        when(refinerResponse.getResult()).thenReturn(refinerGeneration);
        when(refinerGeneration.getOutput()).thenReturn(new AssistantMessage("refined answer"));
        when(refinerResponse.getMetadata()).thenReturn(chatResponseMetadata);
        when(chatResponseMetadata.getUsage()).thenReturn(usage);
        when(usage.getTotalTokens()).thenReturn(Math.toIntExact(100L));
        // ACT
        String result = councilOrchestrator.refine("what is RAG?", "chunk1", "refined answer");

        // ASSERT
        assertThat(result).isEqualTo("refined answer");
    }
}
