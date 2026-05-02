package org.pickaid.pidatagraph.core;

import java.util.Objects;

public record PiGraphEndpoint(
        PiGraphNodeId nodeId,
        PiGraphPortId portId
) {
    public PiGraphEndpoint {
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(portId, "portId");
    }

    @Override
    public String toString() {
        return nodeId + "." + portId;
    }
}
