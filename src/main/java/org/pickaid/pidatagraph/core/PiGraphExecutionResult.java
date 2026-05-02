package org.pickaid.pidatagraph.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record PiGraphExecutionResult(
        PiGraphPlan plan,
        Map<PiGraphNodeId, Map<PiGraphPortId, Object>> outputs
) {
    public PiGraphExecutionResult {
        Objects.requireNonNull(plan, "plan");
        Map<PiGraphNodeId, Map<PiGraphPortId, Object>> copy = new LinkedHashMap<>();
        Objects.requireNonNull(outputs, "outputs").forEach((nodeId, ports) ->
                copy.put(Objects.requireNonNull(nodeId, "nodeId"), Map.copyOf(Objects.requireNonNull(ports, "ports"))));
        outputs = Map.copyOf(copy);
    }

    public Object output(PiGraphNodeId nodeId, PiGraphPortId port) {
        Map<PiGraphPortId, Object> nodeOutputs = outputs.get(Objects.requireNonNull(nodeId, "nodeId"));
        if (nodeOutputs == null || !nodeOutputs.containsKey(port)) {
            throw new IllegalStateException("missing output value: " + nodeId + "." + port);
        }
        return nodeOutputs.get(port);
    }

    public <T> T output(PiGraphNodeId nodeId, PiGraphPortId port, Class<T> type) {
        Object value = output(nodeId, port);
        if (!type.isInstance(value)) {
            throw new IllegalStateException("output " + nodeId + "." + port
                    + " expected " + type.getName()
                    + " but got " + value.getClass().getName());
        }
        return type.cast(value);
    }
}
