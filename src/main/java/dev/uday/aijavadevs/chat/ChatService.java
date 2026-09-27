package dev.uday.aijavadevs.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String generateAnswer(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    public String greet(String name) {
        return chatClient.prompt()
                .user("Hello, " + name + " how are you?")
                .call()
                .content();
    }

    public SummaryResponse summarizeText(String text) {
        return chatClient.prompt()
                .system("You are an expert technical writer. Summarize the provided text.")
                .user(text)
                .call()
                .entity(SummaryResponse.class);
    }
}
