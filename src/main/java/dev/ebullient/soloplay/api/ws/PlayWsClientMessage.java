package dev.ebullient.soloplay.api.ws;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Messages sent from client to server over the Play WebSocket.
 *
 * Protocol:
 * - {@link HistoryRequest}: Request conversation history
 * - {@link UserMessage}: Send a player message to the GM
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PlayWsClientMessage.HistoryRequest.class, name = "history_request"),
        @JsonSubTypes.Type(value = PlayWsClientMessage.UserMessage.class, name = "user_message")
})
public sealed interface PlayWsClientMessage {

    /**
     * Request conversation history for this story thread.
     *
     * @param limit Maximum number of messages to return (default: 100)
     */
    record HistoryRequest(Integer limit) implements PlayWsClientMessage {
        public HistoryRequest {
            if (limit == null) {
                limit = 100;
            }
        }
    }

    /**
     * Player message to the GM.
     *
     * @param text The player's message text
     */
    record UserMessage(String text) implements PlayWsClientMessage {
    }
}
