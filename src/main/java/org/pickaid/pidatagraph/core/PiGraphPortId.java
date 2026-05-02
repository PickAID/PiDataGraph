package org.pickaid.pidatagraph.core;

import java.util.Objects;

public record PiGraphPortId(String value) implements Comparable<PiGraphPortId> {
    public PiGraphPortId {
        value = clean(value);
    }

    @Override
    public int compareTo(PiGraphPortId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }

    private static String clean(String value) {
        String cleaned = Objects.requireNonNull(value, "port id").trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("port id must not be blank");
        }
        return cleaned;
    }
}
