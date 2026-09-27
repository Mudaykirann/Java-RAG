package dev.uday.aijavadevs.document;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/documents")
    public String ingestDocument(@RequestBody DocumentRequest request) {
        documentService.ingestDocument(request.content());
        return "Document ingested successfully!";
    }
}
