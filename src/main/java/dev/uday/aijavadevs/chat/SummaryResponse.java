package dev.uday.aijavadevs.chat;

import java.util.List;

public record SummaryResponse(String title, String summary, List<String> keyPoints) {
}
