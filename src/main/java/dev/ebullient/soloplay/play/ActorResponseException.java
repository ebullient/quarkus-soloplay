package dev.ebullient.soloplay.play;

public class ActorResponseException extends RuntimeException {

    public ActorResponseException(String msg) {
        super(msg);
    }

    public ActorResponseException(String msg, Exception e) {
        super(msg, e);
    }

}
