package dev.uday.aijavadevs.document;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service 
public class DocumentService {

    private final VectorStore vectorStore;

    public DocumentService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void ingestDocument(String content) {
        Document document = new Document(content);
        
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(List.of(document));
        
        vectorStore.accept(chunks);
    }

    public DocumentUploadResponse ingestFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty");
        }

        // 1. Extract content and metadata using Apache Tika (supports PDF, DOCX, PPTX, TXT, etc.)
        TikaDocumentReader reader = new TikaDocumentReader(file.getResource());
        List<Document> rawDocuments = reader.get();

        // 2. Enrich metadata with file information
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded_document";
        for (Document doc : rawDocuments) {
            doc.getMetadata().put("file_name", filename);
            doc.getMetadata().put("file_size", file.getSize());
            if (file.getContentType() != null) {
                doc.getMetadata().put("content_type", file.getContentType());
            }
        }

        // 3. Transform & split the extracted text into overlapping semantic token chunks
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(rawDocuments);

        // 4. Load the vector embeddings into PostgreSQL pgvector
        vectorStore.accept(chunks);

        return new DocumentUploadResponse(
                filename,
                chunks.size(),
                file.getSize(),
                "File parsed, split into " + chunks.size() + " chunks, and indexed in pgvector!"
        );
    }
}
