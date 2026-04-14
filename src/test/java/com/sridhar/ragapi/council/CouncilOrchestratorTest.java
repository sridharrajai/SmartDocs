package com.sridhar.ragapi.council;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouncilOrchestratorTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec promptSpec;

    @Mock
    private ChatClient.CallResponseSpec callSpec;

    @Mock
    private ChatResponse chatResponse;

    @Mock
    private Generation generation;

    @InjectMocks
    private CouncilOrchestrator councilOrchestrator;

    @Test
    void whenVerdictAcceptable_returnsDraftWithoutRefiner() {
        // Chain stub — each call returns the next mock in the chain
        when(chatClient.prompt(anyString())).thenReturn(promptSpec);
        when(promptSpec.call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(chatResponse);
        when(chatResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(new AssistantMessage("{\"verdict\": \"ACCEPTABLE\"}"));

        // ACT
        String result = councilOrchestrator.refine("what is RAG?", "chunk1", "draft answer");

        // ASSERT
        assertThat(result).isEqualTo("draft answer");
    }
}
