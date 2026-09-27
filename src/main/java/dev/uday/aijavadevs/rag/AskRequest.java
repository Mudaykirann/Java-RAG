package dev.uday.aijavadevs.rag;

public record AskRequest(String question, String conversationId) {
    public AskRequest(String question) {
        this(question, null);
    }
}