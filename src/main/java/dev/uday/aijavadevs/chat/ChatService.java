package dev.uday.aijavadevs.chat;

import dev.uday.aijavadevs.weather.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final WeatherTools weatherTools;

    public ChatService(ChatClient.Builder chatClientBuilder, WeatherTools weatherTools) {
        this.chatClient = chatClientBuilder.build();
        this.weatherTools = weatherTools;
    }

    public String generateAnswer(String message) {
        return chatClient.prompt()
                .user(message)
                .tools(weatherTools)
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
