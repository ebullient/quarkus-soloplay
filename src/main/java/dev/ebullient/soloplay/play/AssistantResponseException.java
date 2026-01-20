package dev.ebullient.soloplay.play;

public class AssistantResponseException extends RuntimeException {

    final boolean retryable;

    public AssistantResponseException(String msg, boolean retryable) {
        super(msg);
        this.retryable = retryable;
    }

    public AssistantResponseException(String msg, Exception e, boolean retryable) {
        super(msg, e);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
