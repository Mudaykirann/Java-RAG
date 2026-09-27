package dev.uday.aijavadevs.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String answer = chatService.generateAnswer(request.message());
        return new ChatResponse(answer);
    }

    @PostMapping("/summarize")
    public SummaryResponse summarize(@RequestBody SummaryRequest request) {
        return chatService.summarizeText(request.text());
    }
}
