package org.pickaid.pidatagraph.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PiGraphValidator {
    private PiGraphValidator() {
    }

    public static PiGraphValidation validate(PiGraphDefinition graph) {
        Objects.requireNonNull(graph, "graph");
        Map<PiGraphNodeId, PiGraphNode> nodes = graph.nodesById();
        List<PiGraphIssue> issues = new ArrayList<>();

        for (PiGraphEdge edge : graph.edges()) {
            validateEdge(edge, nodes, issues);
        }
        if (issues.isEmpty() && PiGraphTopology.sort(graph).size() != graph.nodes().size()) {
            issues.add(new PiGraphIssue(PiGraphIssue.Code.CYCLE, "graph " + graph.id() + " contains a cycle"));
        }
        return new PiGraphValidation(issues);
    }

    private static void validateEdge(
            PiGraphEdge edge,
            Map<PiGraphNodeId, PiGraphNode> nodes,
            List<PiGraphIssue> issues
    ) {
        PiGraphNode from = nodes.get(edge.from().nodeId());
        if (from == null) {
            issues.add(new PiGraphIssue(
                    PiGraphIssue.Code.MISSING_SOURCE_NODE,
                    "edge source node does not exist: " + edge.from().nodeId()));
        } else if (!from.hasOutput(edge.from().portId())) {
            issues.add(new PiGraphIssue(
                    PiGraphIssue.Code.MISSING_OUTPUT_PORT,
                    "edge source port does not exist: " + edge.from()));
        }

        PiGraphNode to = nodes.get(edge.to().nodeId());
        if (to == null) {
            issues.add(new PiGraphIssue(
                    PiGraphIssue.Code.MISSING_TARGET_NODE,
                    "edge target node does not exist: " + edge.to().nodeId()));
        } else if (!to.hasInput(edge.to().portId())) {
            issues.add(new PiGraphIssue(
                    PiGraphIssue.Code.MISSING_INPUT_PORT,
                    "edge target port does not exist: " + edge.to()));
        }
    }
}
