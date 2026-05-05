package org.pickaid.pidatagraph.engine.context;

import java.util.Objects;

/**
 * Typed Java-side handle for a value emitted by an engine frame.
 *
 * <p>Use this when the output name is stable enough to share between the action
 * that emits it and the Java code that reads it. Unlike expression variables,
 * frame value names may use dotted paths, for example {@code hud.mana_fill}.</p>
 *
 * @param name frame output name
 * @param type expected Java type
 * @param <T> expected Java type
 */
public record PiEngineValueKey<T>(String name, Class<T> type) {
    public PiEngineValueKey {
        name = PiEngineKeyNames.frameValue(name);
        type = Objects.requireNonNull(type, "type");
    }

    public static PiEngineValueKey<Number> number(String name) {
        return new PiEngineValueKey<>(name, Number.class);
    }

    public static PiEngineValueKey<Boolean> flag(String name) {
        return new PiEngineValueKey<>(name, Boolean.class);
    }

    public static <T> PiEngineValueKey<T> object(String name, Class<T> type) {
        return new PiEngineValueKey<>(name, type);
    }
}
