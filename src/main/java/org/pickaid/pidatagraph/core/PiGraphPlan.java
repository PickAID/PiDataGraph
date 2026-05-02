package org.pickaid.pidatagraph.core;

import java.util.List;
import java.util.Objects;

public record PiGraphPlan(List<PiGraphNodeId> nodeOrder) {
    public PiGraphPlan {
        nodeOrder = List.copyOf(Objects.requireNonNull(nodeOrder, "nodeOrder"));
    }
}
