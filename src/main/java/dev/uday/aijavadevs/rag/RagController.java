package dev.uday.aijavadevs.rag;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public AskResponse ask(@RequestBody AskRequest request) {
        return ragService.askQuestion(request.question(), request.conversationId());
    }

    @DeleteMapping("/ask/{conversationId}")
    public void clearConversation(@PathVariable String conversationId) {
        ragService.clearConversation(conversationId);
    }
}
