package org.pickaid.pidatagraph.graphcontext;

import java.util.Objects;
import org.pickaid.pidatagraph.engine.context.PiEngineKeyNames;

public record PiGraphContextField(String name, Kind kind, Class<?> type) {
    public PiGraphContextField {
        name = PiEngineKeyNames.variable(name);
        kind = Objects.requireNonNull(kind, "kind");
        type = Objects.requireNonNull(type, "type");
    }

    public static PiGraphContextField number(String name) {
        return new PiGraphContextField(name, Kind.NUMBER, Number.class);
    }

    public static PiGraphContextField object(String name, Class<?> type) {
        return new PiGraphContextField(name, Kind.OBJECT, type);
    }

    public enum Kind {
        NUMBER,
        OBJECT
    }
}
