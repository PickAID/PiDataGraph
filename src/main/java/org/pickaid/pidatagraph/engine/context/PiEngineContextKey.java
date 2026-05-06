package org.pickaid.pidatagraph.engine.context;

import java.util.Objects;

public record PiEngineContextKey<T>(String name, Class<T> type) {
    public PiEngineContextKey {
        name = PiEngineKeyNames.object(name);
        type = Objects.requireNonNull(type, "type");
        if (type.isPrimitive()) {
            throw new IllegalArgumentException("engine context object key `" + name
                    + "` type must not be primitive: " + type.getName());
        }
    }

    public static <T> PiEngineContextKey<T> of(String name, Class<T> type) {
        return new PiEngineContextKey<>(name, type);
    }

}
