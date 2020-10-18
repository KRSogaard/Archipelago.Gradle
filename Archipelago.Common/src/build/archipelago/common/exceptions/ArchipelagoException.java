package build.archipelago.common.exceptions;

public class ArchipelagoException extends Exception {
    public ArchipelagoException() {
        super();
    }
    public ArchipelagoException(String message) {
        super(message);
    }
    public ArchipelagoException(String message, Throwable cause) {
        super(message, cause);
    }
    public ArchipelagoException(Throwable cause) {
        super(cause);
    }
    protected ArchipelagoException(String message, Throwable cause,
                        boolean enableSuppression,
                        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
