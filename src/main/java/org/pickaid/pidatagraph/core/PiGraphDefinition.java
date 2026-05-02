package org.pickaid.pidatagraph.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record PiGraphDefinition(
        ResourceLocation id,
        List<PiGraphNode> nodes,
        List<PiGraphEdge> edges
) {
    public PiGraphDefinition {
        Objects.requireNonNull(id, "id");
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        edges = List.copyOf(Objects.requireNonNull(edges, "edges"));
        Map<PiGraphNodeId, PiGraphNode> byId = new LinkedHashMap<>();
        for (PiGraphNode node : nodes) {
            PiGraphNode checked = Objects.requireNonNull(node, "node");
            if (byId.putIfAbsent(checked.id(), checked) != null) {
                throw new IllegalArgumentException("duplicate node: " + checked.id());
            }
        }
        edges.forEach(edge -> Objects.requireNonNull(edge, "edge"));
    }

    public Map<PiGraphNodeId, PiGraphNode> nodesById() {
        Map<PiGraphNodeId, PiGraphNode> byId = new LinkedHashMap<>();
        for (PiGraphNode node : nodes) {
            byId.put(node.id(), node);
        }
        return Map.copyOf(byId);
    }
}
