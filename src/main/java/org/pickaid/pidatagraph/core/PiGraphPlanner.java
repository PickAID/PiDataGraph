package org.pickaid.pidatagraph.core;

import java.util.Objects;

public final class PiGraphPlanner {
    private PiGraphPlanner() {
    }

    public static PiGraphPlan plan(PiGraphDefinition graph) {
        Objects.requireNonNull(graph, "graph");
        PiGraphValidation validation = PiGraphValidator.validate(graph);
        if (!validation.ok()) {
            throw new IllegalStateException(validation.describe());
        }
        return new PiGraphPlan(PiGraphTopology.sort(graph));
    }
}
