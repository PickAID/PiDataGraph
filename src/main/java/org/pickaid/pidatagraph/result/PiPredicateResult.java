package org.pickaid.pidatagraph.result;

import java.util.Objects;

public record PiPredicateResult(boolean passed, String reason) implements PiGraphResult {
    public PiPredicateResult {
        reason = Objects.requireNonNull(reason, "reason");
    }

    public static PiPredicateResult pass() {
        return new PiPredicateResult(true, "");
    }

    public static PiPredicateResult fail(String reason) {
        return new PiPredicateResult(false, reason);
    }
}
