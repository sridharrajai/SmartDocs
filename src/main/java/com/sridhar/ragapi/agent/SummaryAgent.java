package com.sridhar.ragapi.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface SummaryAgent {

    @SystemMessage("You are a summarisation assistant. Summarise the conversation below in exactly 3 sentences, retaining all key facts.")
    String summarise(@UserMessage String query);
}
