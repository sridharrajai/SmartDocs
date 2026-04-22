package com.sridhar.ragapi.util;

import com.sridhar.ragapi.exception.PromptInjectionException;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class PromptInjectionFilter {

    private static final List<String> INJECTION_PATTERNS = List.of(
            "ignore all previous instructions",
            "disregard your instructions",
            "forget your system prompt",
            "you are now",
            "act as if you are",
            "jailbreak"
    );

    public void validate (String request) throws PromptInjectionException {
        String lower = request.toLowerCase();
        boolean isInjection = INJECTION_PATTERNS.stream().anyMatch(lower::contains);
        if (isInjection)
            throw new PromptInjectionException("Input validation failed -- potential injection detected");
    }
}
