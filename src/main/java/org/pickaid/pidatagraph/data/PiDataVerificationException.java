package org.pickaid.pidatagraph.data;

import java.util.Objects;

public final class PiDataVerificationException extends RuntimeException {
    private final String path;

    public PiDataVerificationException(String path, String message) {
        super(message);
        this.path = Objects.requireNonNull(path, "path");
    }

    public PiDataVerificationException(String path, Throwable cause) {
        super(cause == null ? null : cause.getMessage(), cause);
        this.path = Objects.requireNonNull(path, "path");
    }

    public String path() {
        return path;
    }
}
