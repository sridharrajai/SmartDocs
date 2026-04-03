package com.sridhar.ragapi.agent;

public class AgentContextHolder {
    private static final ThreadLocal<String> context = new ThreadLocal<String>();

    public static String getContext() {
        return context.get() != null ? context.get() : "";
    }

    ;

    public static void setContext(String context) {
        AgentContextHolder.context.set(context);
    }

    public static void clearContext() {
        AgentContextHolder.context.remove();
    }
}
