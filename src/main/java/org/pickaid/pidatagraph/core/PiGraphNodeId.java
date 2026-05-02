package org.pickaid.pidatagraph.core;

import java.util.Objects;

public record PiGraphNodeId(String value) implements Comparable<PiGraphNodeId> {
    public PiGraphNodeId {
        value = clean(value, "node id");
    }

    @Override
    public int compareTo(PiGraphNodeId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }

    private static String clean(String value, String label) {
        String cleaned = Objects.requireNonNull(value, label).trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return cleaned;
    }
}
