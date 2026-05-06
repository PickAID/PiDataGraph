package org.pickaid.pidatagraph.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;
import org.pickaid.pidatagraph.engine.context.PiEngineFlagKey;
import org.pickaid.pidatagraph.engine.context.PiEngineKeyNames;
import org.pickaid.pidatagraph.engine.context.PiEngineNumberKey;
import org.pickaid.pidatagraph.engine.context.PiEngineValueKey;

public final class PiEngineFrame {
    private static final PiEngineFrame EMPTY = new PiEngineFrame(Map.of());

    private final Map<String, Object> values;

    private PiEngineFrame(Map<String, Object> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static PiEngineFrame empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public double number(String key) {
        return value(key, Number.class).doubleValue();
    }

    public double number(PiEngineNumberKey key) {
        return number(Objects.requireNonNull(key, "key").name());
    }

    public double number(PiEngineValueKey<? extends Number> key) {
        return number(Objects.requireNonNull(key, "key").name());
    }

    public double numberOr(String key, double fallback) {
        return hasValue(key) ? number(key) : fallback;
    }

    public double numberOr(PiEngineNumberKey key, double fallback) {
        return numberOr(Objects.requireNonNull(key, "key").name(), fallback);
    }

    public double numberOr(PiEngineValueKey<? extends Number> key, double fallback) {
        return numberOr(Objects.requireNonNull(key, "key").name(), fallback);
    }

    public boolean hasValue(String key) {
        return values.containsKey(checkKey(key));
    }

    public boolean hasValue(PiEngineNumberKey key) {
        return hasValue(Objects.requireNonNull(key, "key").name());
    }

    public boolean hasValue(PiEngineFlagKey key) {
        return hasValue(Objects.requireNonNull(key, "key").name());
    }

    public boolean hasValue(PiEngineContextKey<?> key) {
        return hasValue(Objects.requireNonNull(key, "key").name());
    }

    public boolean hasValue(PiEngineValueKey<?> key) {
        return hasValue(Objects.requireNonNull(key, "key").name());
    }

    public int integer(String key) {
        return value(key, Number.class).intValue();
    }

    public int integer(PiEngineNumberKey key) {
        return integer(Objects.requireNonNull(key, "key").name());
    }

    public int integer(PiEngineValueKey<? extends Number> key) {
        return integer(Objects.requireNonNull(key, "key").name());
    }

    public int integerOr(String key, int fallback) {
        return hasValue(key) ? integer(key) : fallback;
    }

    public int integerOr(PiEngineNumberKey key, int fallback) {
        return integerOr(Objects.requireNonNull(key, "key").name(), fallback);
    }

    public int integerOr(PiEngineValueKey<? extends Number> key, int fallback) {
        return integerOr(Objects.requireNonNull(key, "key").name(), fallback);
    }

    public boolean flag(String key) {
        return value(key, Boolean.class);
    }

    public boolean flag(PiEngineFlagKey key) {
        return flag(Objects.requireNonNull(key, "key").name());
    }

    public boolean flag(PiEngineValueKey<Boolean> key) {
        return flag(Objects.requireNonNull(key, "key").name());
    }

    public boolean flagOr(String key, boolean fallback) {
        return hasValue(key) ? flag(key) : fallback;
    }

    public boolean flagOr(PiEngineFlagKey key, boolean fallback) {
        return flagOr(Objects.requireNonNull(key, "key").name(), fallback);
    }

    public boolean flagOr(PiEngineValueKey<Boolean> key, boolean fallback) {
        return flagOr(Objects.requireNonNull(key, "key").name(), fallback);
    }

    public <T> Optional<T> object(String key, Class<T> type) {
        String checkedKey = checkKey(key);
        Object value = values.get(checkedKey);
        if (value == null) {
            return Optional.empty();
        }
        if (!type.isInstance(value)) {
            throw new ClassCastException("engine frame value `" + checkedKey + "` is " + value.getClass().getName() + ", not " + type.getName());
        }
        return Optional.of(type.cast(value));
    }

    public <T> Optional<T> object(PiEngineContextKey<T> key) {
        Objects.requireNonNull(key, "key");
        return object(key.name(), key.type());
    }

    public <T> Optional<T> object(PiEngineValueKey<T> key) {
        Objects.requireNonNull(key, "key");
        return object(key.name(), key.type());
    }

    public <T> T objectOr(PiEngineContextKey<T> key, T fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return object(key).orElse(fallback);
    }

    public <T> T objectOr(PiEngineValueKey<T> key, T fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return object(key).orElse(fallback);
    }

    public <T> T value(String key, Class<T> type) {
        String checkedKey = checkKey(key);
        Object value = values.get(checkedKey);
        if (value == null) {
            throw new IllegalArgumentException("missing engine frame value: " + checkedKey);
        }
        if (!type.isInstance(value)) {
            throw new ClassCastException("engine frame value `" + checkedKey + "` is " + value.getClass().getName() + ", not " + type.getName());
        }
        return type.cast(value);
    }

    public <T> T value(PiEngineValueKey<T> key) {
        Objects.requireNonNull(key, "key");
        return value(key.name(), key.type());
    }

    public <T> T value(PiEngineContextKey<T> key) {
        Objects.requireNonNull(key, "key");
        return value(key.name(), key.type());
    }

    public Map<String, Object> values() {
        return values;
    }

    public PiEngineFrame merge(PiEngineFrame other) {
        Objects.requireNonNull(other, "other");
        if (values.isEmpty()) {
            return other;
        }
        if (other.values.isEmpty()) {
            return this;
        }
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>(values);
        for (Map.Entry<String, Object> entry : other.values.entrySet()) {
            Object previous = merged.putIfAbsent(entry.getKey(), entry.getValue());
            if (previous != null) {
                throw duplicateValue(entry.getKey(), previous, entry.getValue());
            }
        }
        return new PiEngineFrame(merged);
    }

    public PiEngineFrame mergeReplacing(PiEngineFrame other) {
        Objects.requireNonNull(other, "other");
        if (other.values.isEmpty()) {
            return this;
        }
        if (values.isEmpty()) {
            return other;
        }
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>(values);
        merged.putAll(other.values);
        return new PiEngineFrame(merged);
    }

    private static IllegalArgumentException duplicateValue(String key, Object previous, Object next) {
        return new IllegalArgumentException("duplicate engine frame value `" + key + "` while merging frames: "
                + previous.getClass().getName() + " already exists, cannot add " + next.getClass().getName());
    }

    public static final class Builder {
        private final LinkedHashMap<String, Object> values = new LinkedHashMap<>();

        public Builder number(String key, double value) {
            return value(key, value);
        }

        public Builder number(PiEngineNumberKey key, double value) {
            return number(Objects.requireNonNull(key, "key").name(), value);
        }

        public Builder number(PiEngineValueKey<? extends Number> key, double value) {
            return number(Objects.requireNonNull(key, "key").name(), value);
        }

        public Builder integer(String key, int value) {
            return value(key, value);
        }

        public Builder integer(PiEngineNumberKey key, int value) {
            return integer(Objects.requireNonNull(key, "key").name(), value);
        }

        public Builder integer(PiEngineValueKey<? extends Number> key, int value) {
            return integer(Objects.requireNonNull(key, "key").name(), value);
        }

        public Builder flag(String key, boolean value) {
            return value(key, value);
        }

        public Builder flag(PiEngineFlagKey key, boolean value) {
            return flag(Objects.requireNonNull(key, "key").name(), value);
        }

        public Builder flag(PiEngineValueKey<Boolean> key, boolean value) {
            return flag(Objects.requireNonNull(key, "key").name(), value);
        }

        public Builder object(String key, Object value) {
            return value(key, value);
        }

        public <T> Builder object(PiEngineContextKey<T> key, T value) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
            if (!key.type().isInstance(value)) {
                throw new ClassCastException("engine frame value `" + key.name() + "` is " + value.getClass().getName() + ", not " + key.type().getName());
            }
            return object(key.name(), value);
        }

        public <T> Builder object(PiEngineValueKey<T> key, T value) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
            if (!key.type().isInstance(value)) {
                throw new ClassCastException("engine frame value `" + key.name() + "` is " + value.getClass().getName() + ", not " + key.type().getName());
            }
            return object(key.name(), value);
        }

        public Builder value(String key, Object value) {
            String checkedKey = checkKey(key);
            Objects.requireNonNull(value, "value");
            Object previous = values.putIfAbsent(checkedKey, value);
            if (previous != null) {
                throw duplicateValue(checkedKey, previous, value);
            }
            return this;
        }

        public PiEngineFrame build() {
            if (values.isEmpty()) {
                return EMPTY;
            }
            return new PiEngineFrame(values);
        }
    }

    private static String checkKey(String key) {
        return PiEngineKeyNames.frameValue(key);
    }
}
