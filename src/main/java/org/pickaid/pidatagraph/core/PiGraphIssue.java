package org.pickaid.pidatagraph.core;

import java.util.Objects;

public record PiGraphIssue(
        Code code,
        String message
) {
    public PiGraphIssue {
        Objects.requireNonNull(code, "code");
        message = Objects.requireNonNull(message, "message").trim();
        if (message.isEmpty()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }

    public enum Code {
        MISSING_SOURCE_NODE,
        MISSING_TARGET_NODE,
        MISSING_OUTPUT_PORT,
        MISSING_INPUT_PORT,
        CYCLE
    }
}
