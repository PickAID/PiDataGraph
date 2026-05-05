package org.pickaid.pidatagraph.data;

import java.util.Objects;

public record PiDataIssue(String path, String message) {
    public PiDataIssue {
        path = Objects.requireNonNull(path, "path");
        message = Objects.requireNonNull(message, "message");
    }
}
