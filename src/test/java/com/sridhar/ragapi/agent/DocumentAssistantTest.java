package com.sridhar.ragapi.agent;

import ch.qos.logback.core.BasicStatusManager;
import com.sridhar.ragapi.council.CouncilOrchestrator;
import com.sridhar.ragapi.repository.ChatMessageRepository;
import com.sridhar.ragapi.service.AgentService;
import com.sridhar.ragapi.service.PostGresChatMemoryService;
import com.sridhar.ragapi.service.SummaryService;
import com.sridhar.ragapi.service.TokenAwareMemoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentAssistantTest {

    @Mock
    private DocumentAssistant documentAssistant;

    @Mock
    private SummaryService summaryService;

    @Mock
    private PostGresChatMemoryService postGresChatMemoryService;

    @Mock
    private CouncilOrchestrator councilOrchestrator;

    @Mock
    private TokenAwareMemoryService tokenAwareMemoryService;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private AgentService agentService;

    @BeforeEach
    public void setUp(){
        AgentContextHolder.clearContext();
    }

    @Test
    void councilInvokedwhenChunksPresent(){

        when(documentAssistant.chat(anyString(), anyString())).thenReturn("draft answer");
        AgentContextHolder.setContext("chunk1, chunk2");
        when(councilOrchestrator.refine(anyString(), anyString(), anyString())).thenReturn("refined answer");
        // ACT
        String result = agentService.agentChat("session-1", "user-1", "what is RAG?");

        // ASSERT
        verify(councilOrchestrator).refine(anyString(), anyString(), anyString());
    }

    @Test
    void whenNoRagContext_councilShouldBeSkipped() {
        when(documentAssistant.chat(anyString(), anyString())).thenReturn("draft answer");
        String result = agentService.agentChat("session-1", "user-1", "what is RAG?");
        verify(councilOrchestrator, never()).refine(anyString(), anyString(), anyString());
    }
}
