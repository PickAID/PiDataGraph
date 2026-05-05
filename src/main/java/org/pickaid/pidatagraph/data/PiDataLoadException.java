package org.pickaid.pidatagraph.data;

public final class PiDataLoadException extends IllegalStateException {
    public PiDataLoadException(String message) {
        super(message);
    }

    public PiDataLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
