package jorg.jorg;

public class JorgWriteException extends Exception {

    public JorgWriteException() {
    }

    public JorgWriteException(String message) {
        super(message);
    }

    public JorgWriteException(String message, Throwable cause) {
        super(message, cause);
    }

    public JorgWriteException(Throwable cause) {
        super(cause);
    }

    public JorgWriteException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
