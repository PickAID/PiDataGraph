package org.pickaid.pidatagraph.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

final class PiGraphTopology {
    private PiGraphTopology() {
    }

    static List<PiGraphNodeId> sort(PiGraphDefinition graph) {
        Map<PiGraphNodeId, Integer> indegree = new LinkedHashMap<>();
        Map<PiGraphNodeId, List<PiGraphNodeId>> outgoing = new LinkedHashMap<>();
        for (PiGraphNode node : graph.nodes()) {
            indegree.put(node.id(), 0);
            outgoing.put(node.id(), new ArrayList<>());
        }
        for (PiGraphEdge edge : graph.edges()) {
            outgoing.get(edge.from().nodeId()).add(edge.to().nodeId());
            indegree.compute(edge.to().nodeId(), (ignored, count) -> count == null ? 1 : count + 1);
        }

        PriorityQueue<PiGraphNodeId> ready = new PriorityQueue<>();
        indegree.forEach((nodeId, count) -> {
            if (count == 0) {
                ready.add(nodeId);
            }
        });

        List<PiGraphNodeId> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            PiGraphNodeId current = ready.remove();
            order.add(current);
            for (PiGraphNodeId next : outgoing.getOrDefault(current, List.of())) {
                int count = indegree.compute(next, (ignored, value) -> value == null ? 0 : value - 1);
                if (count == 0) {
                    ready.add(next);
                }
            }
        }
        return List.copyOf(order);
    }
}
