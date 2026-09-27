package dev.uday.aijavadevs.rag;

public record AskResponse(String answer, String conversationId) {
    public AskResponse(String answer) {
        this(answer, null);
    }
}
