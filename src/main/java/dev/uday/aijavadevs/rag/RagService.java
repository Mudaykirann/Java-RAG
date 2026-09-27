package dev.uday.aijavadevs.rag;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
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

    public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    public AskResponse askQuestion(String question) {
        // 1. Retrieve the top 3 most similar document chunks from pgvector
        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(3)
                        .build()
        );

        // 2. Extract and concatenate text from all matched chunks
        String context = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // 3. Define the prompt template with context and strict instructions
        String templateString = """
                You are a knowledgeable assistant. Use the following retrieved CONTEXT to answer the QUESTION.
                If the answer is not present in the CONTEXT, respond honestly: "I don't have enough information in my knowledge base to answer that."
                Do not make up facts or extrapolate beyond the provided CONTEXT.

                CONTEXT:
                {context}

                QUESTION:
                {question}
                """;

        PromptTemplate promptTemplate = new PromptTemplate(templateString);
        Prompt prompt = promptTemplate.create(Map.of(
                "context", context.isBlank() ? "No relevant documents found." : context,
                "question", question
        ));

        // 4. Send the prompt to the LLM and return the grounded answer
        String answer = chatClient.prompt(prompt)
                .call()
                .content();

        return new AskResponse(answer);
    }
}
