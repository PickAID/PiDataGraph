package org.pickaid.pidatagraph.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PiGraphNodeExecution {
    private final PiGraphDefinition graph;
    private final PiGraphNode node;
    private final Map<PiGraphPortId, Object> inputs;
    private final Map<PiGraphPortId, Object> outputs = new LinkedHashMap<>();

    PiGraphNodeExecution(
            PiGraphDefinition graph,
            PiGraphNode node,
            Map<PiGraphPortId, Object> inputs
    ) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.node = Objects.requireNonNull(node, "node");
        this.inputs = Map.copyOf(Objects.requireNonNull(inputs, "inputs"));
    }

    public PiGraphDefinition graph() {
        return graph;
    }

    public PiGraphNode node() {
        return node;
    }

    public Object input(PiGraphPortId port) {
        Objects.requireNonNull(port, "port");
        if (!node.hasInput(port)) {
            throw new IllegalArgumentException("node " + node.id() + " has no input port " + port);
        }
        if (!inputs.containsKey(port)) {
            throw new IllegalStateException("missing input value: " + node.id() + "." + port);
        }
        return inputs.get(port);
    }

    public <T> T input(PiGraphPortId port, Class<T> type) {
        Object value = input(port);
        if (!type.isInstance(value)) {
            throw new IllegalStateException("input " + node.id() + "." + port
                    + " expected " + type.getName()
                    + " but got " + value.getClass().getName());
        }
        return type.cast(value);
    }

    public void output(PiGraphPortId port, Object value) {
        Objects.requireNonNull(port, "port");
        Objects.requireNonNull(value, "value");
        if (!node.hasOutput(port)) {
            throw new IllegalArgumentException("node " + node.id() + " has no output port " + port);
        }
        if (outputs.putIfAbsent(port, value) != null) {
            throw new IllegalStateException("duplicate output value: " + node.id() + "." + port);
        }
    }

    Map<PiGraphPortId, Object> outputs() {
        return Map.copyOf(outputs);
    }
}
