package dev.ebullient.soloplay.ai;

import dev.langchain4j.model.output.structured.Description;

public record JsonChatResponse(
    @Description("Your complete answer in markdown format")
    String response
) {
}
