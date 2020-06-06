package jorg.jorg;

public class JorgReadException extends Exception {

    public JorgReadException() {
    }

    public JorgReadException(String message) {
        super(message);
    }

    public JorgReadException(String message, Throwable cause) {
        super(message, cause);
    }

    public JorgReadException(Throwable cause) {
        super(cause);
    }

    public JorgReadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
