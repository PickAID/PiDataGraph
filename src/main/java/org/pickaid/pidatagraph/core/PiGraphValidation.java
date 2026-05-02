package org.pickaid.pidatagraph.core;

import java.util.List;
import java.util.Objects;

public record PiGraphValidation(List<PiGraphIssue> issues) {
    public PiGraphValidation {
        issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
    }

    public boolean ok() {
        return issues.isEmpty();
    }

    public String describe() {
        if (ok()) {
            return "graph is valid";
        }
        return issues.stream()
                .map(issue -> issue.code() + ": " + issue.message())
                .reduce((left, right) -> left + "; " + right)
                .orElse("graph is invalid");
    }
}
