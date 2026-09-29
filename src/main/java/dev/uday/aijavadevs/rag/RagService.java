package dev.uday.aijavadevs.rag;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import dev.uday.aijavadevs.weather.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ChatMemory chatMemory;
    private final WeatherTools weatherTools;

    public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, ChatMemory chatMemory, WeatherTools weatherTools) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.chatMemory = chatMemory;
        this.weatherTools = weatherTools;
    }

    public AskResponse askQuestion(String question) {
        return askQuestion(question, null);
    }

    public AskResponse askQuestion(String question, String conversationId) {
        // Ensure a valid conversationId is present
        String convId = (conversationId != null && !conversationId.isBlank())
                ? conversationId
                : UUID.randomUUID().toString();

        // 1. Fetch previous conversation history for this conversationId
        List<Message> history = chatMemory.get(convId);

        // 2. Query Contextualization: If history exists, reformulate follow-up into a standalone search query
        String searchQuery = contextualizeQuery(question, history);

        // 3. Retrieve Top-3 most similar document chunks from pgvector using the search query
        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(searchQuery)
                        .topK(3)
                        .build()
        );

        // 4. Extract and concatenate text from all matched chunks
        String context = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // Format history for the prompt
        String historyString = formatHistory(history);

        // 5. Define the grounded prompt template with Context + Conversation History + Current Question + Tools
        String templateString = """
                You are a knowledgeable assistant. Use the retrieved CONTEXT, CONVERSATION HISTORY, and any available live TOOLS (such as weather) to answer the user's QUESTION.
                If the answer is not present in the CONTEXT, CONVERSATION HISTORY, or available TOOLS, respond honestly: "I don't have enough information in my knowledge base to answer that."
                Do not make up facts or extrapolate beyond the provided CONTEXT or tool outputs.

                CONTEXT:
                {context}

                CONVERSATION HISTORY:
                {history}

                QUESTION:
                {question}
                """;

        PromptTemplate promptTemplate = new PromptTemplate(templateString);
        Prompt prompt = promptTemplate.create(Map.of(
                "context", context.isBlank() ? "No relevant documents found." : context,
                "history", historyString.isBlank() ? "No previous history." : historyString,
                "question", question
        ));

        // 6. Send the prompt to the LLM with available tools and return the answer
        String answer = chatClient.prompt(prompt)
                .tools(weatherTools)
                .call()
                .content();

        // 7. Save this turn into ChatMemory
        chatMemory.add(convId, List.of(new UserMessage(question), new AssistantMessage(answer)));

        return new AskResponse(answer, convId);
    }

    public void clearConversation(String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            chatMemory.clear(conversationId);
        }
    }

    private String contextualizeQuery(String question, List<Message> history) {
        if (history == null || history.isEmpty()) {
            return question;
        }

        String historyString = formatHistory(history);

        String promptString = """
                Given the following chat history and a follow-up user question, rephrase the follow-up question into a standalone search query that contains all necessary subject names and context.
                Do NOT answer the question. Only output the standalone search query without preamble. If the question is already standalone, return it unchanged.

                Chat History:
                {history}

                Follow-up Question:
                {question}

                Standalone Search Query:
                """;

        PromptTemplate template = new PromptTemplate(promptString);
        Prompt prompt = template.create(Map.of(
                "history", historyString,
                "question", question
        ));

        try {
            String rewritten = chatClient.prompt(prompt).call().content();
            if (rewritten != null && !rewritten.isBlank()) {
                return rewritten.trim();
            }
        } catch (Exception e) {
            // Fallback to original question if reformulation encounters any issue
        }

        return question;
    }

    private String formatHistory(List<Message> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        return history.stream()
                .map(m -> m.getMessageType() + ": " + m.getText())
                .collect(Collectors.joining("\n"));
    }
}
