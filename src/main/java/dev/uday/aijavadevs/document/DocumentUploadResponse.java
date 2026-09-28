package dev.uday.aijavadevs.document;

public record DocumentUploadResponse(
        String fileName,
        int chunkCount,
        long sizeBytes,
        String message
) {
}
