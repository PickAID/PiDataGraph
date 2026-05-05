package org.pickaid.pidatagraph.data;

import java.util.List;
import java.util.Objects;

public record PiDataValidation(List<PiDataIssue> issues) {
    public PiDataValidation {
        issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
    }

    public static PiDataValidation success() {
        return new PiDataValidation(List.of());
    }

    public boolean ok() {
        return issues.isEmpty();
    }
}
