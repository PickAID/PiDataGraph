package org.pickaid.pidatagraph.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PiGraphExecutor {
    private PiGraphExecutor() {
    }

    public static PiGraphExecutionResult execute(
            PiGraphDefinition graph,
            PiGraphNodeTypeRegistry registry
    ) {
        Objects.requireNonNull(registry, "registry");
        PiGraphPlan plan = PiGraphPlanner.plan(graph);
        Map<PiGraphNodeId, PiGraphNode> nodes = graph.nodesById();
        Map<PiGraphNodeId, Map<PiGraphPortId, Object>> outputs = new LinkedHashMap<>();

        for (PiGraphNodeId nodeId : plan.nodeOrder()) {
            PiGraphNode node = nodes.get(nodeId);
            Map<PiGraphPortId, Object> inputs = routedInputs(graph, node, outputs);
            PiGraphNodeExecution execution = new PiGraphNodeExecution(graph, node, inputs);
            registry.require(node.type()).action().execute(execution);
            outputs.put(node.id(), execution.outputs());
        }
        return new PiGraphExecutionResult(plan, outputs);
    }

    private static Map<PiGraphPortId, Object> routedInputs(
            PiGraphDefinition graph,
            PiGraphNode node,
            Map<PiGraphNodeId, Map<PiGraphPortId, Object>> outputs
    ) {
        Map<PiGraphPortId, Object> inputs = new LinkedHashMap<>();
        for (PiGraphEdge edge : graph.edges()) {
            if (!edge.to().nodeId().equals(node.id())) {
                continue;
            }
            Map<PiGraphPortId, Object> sourceOutputs = outputs.get(edge.from().nodeId());
            if (sourceOutputs == null || !sourceOutputs.containsKey(edge.from().portId())) {
                throw new IllegalStateException("missing output value: " + edge.from());
            }
            if (inputs.putIfAbsent(edge.to().portId(), sourceOutputs.get(edge.from().portId())) != null) {
                throw new IllegalStateException("duplicate input value: " + edge.to());
            }
        }
        return inputs;
    }
}
