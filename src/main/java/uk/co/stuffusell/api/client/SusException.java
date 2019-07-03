package uk.co.stuffusell.api.client;

public class SusException extends RuntimeException {

    public SusException(String message) {
        super(message);
    }

    public SusException(Throwable e) {
        super(e);
    }
}
