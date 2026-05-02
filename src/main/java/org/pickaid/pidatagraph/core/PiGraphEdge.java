package org.pickaid.pidatagraph.core;

import java.util.Objects;

public record PiGraphEdge(
        PiGraphEndpoint from,
        PiGraphEndpoint to
) {
    public PiGraphEdge {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
    }
}
