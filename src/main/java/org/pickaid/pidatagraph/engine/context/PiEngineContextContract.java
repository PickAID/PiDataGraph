package org.pickaid.pidatagraph.engine.context;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.data.PiDataVerificationException;

public final class PiEngineContextContract {
    private static final PiEngineContextContract EMPTY = new PiEngineContextContract(Set.of(), Map.of());

    private final Set<String> numbers;
    private final Map<String, Class<?>> objects;

    private PiEngineContextContract(Set<String> numbers, Map<String, Class<?>> objects) {
        this.numbers = Collections.unmodifiableSet(new LinkedHashSet<>(numbers));
        this.objects = Collections.unmodifiableMap(new LinkedHashMap<>(objects));
    }

    public static PiEngineContextContract empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Set<String> numbers() {
        return numbers;
    }

    public Map<String, Class<?>> objects() {
        return objects;
    }

    public Optional<Class<?>> objectType(String name) {
        return Optional.ofNullable(objects.get(checkName(name)));
    }

    public PiEngineContextContract merge(PiEngineContextContract other) {
        Objects.requireNonNull(other, "other");
        if (numbers.isEmpty() && objects.isEmpty()) {
            return other;
        }
        if (other.numbers.isEmpty() && other.objects.isEmpty()) {
            return this;
        }
        return builder().merge(this).merge(other).build();
    }

    public PiEngineContextContract withoutNumber(String number) {
        return withoutNumbers(Set.of(number));
    }

    public PiEngineContextContract withoutNumber(PiEngineNumberKey number) {
        return withoutNumber(Objects.requireNonNull(number, "number").name());
    }

    public PiEngineContextContract withoutNumbers(Collection<String> providedNumbers) {
        Objects.requireNonNull(providedNumbers, "providedNumbers");
        if (providedNumbers.isEmpty() || numbers.isEmpty()) {
            return this;
        }
        LinkedHashSet<String> remaining = new LinkedHashSet<>(numbers);
        providedNumbers.forEach(value -> remaining.remove(checkName(value)));
        return new PiEngineContextContract(remaining, objects);
    }

    public PiEngineContextContract withoutObject(String object) {
        return withoutObjects(Set.of(object));
    }

    public PiEngineContextContract withoutObjects(Collection<String> providedObjects) {
        Objects.requireNonNull(providedObjects, "providedObjects");
        if (providedObjects.isEmpty() || objects.isEmpty()) {
            return this;
        }
        LinkedHashMap<String, Class<?>> remaining = new LinkedHashMap<>(objects);
        providedObjects.forEach(value -> remaining.remove(checkName(value)));
        return new PiEngineContextContract(numbers, remaining);
    }

    public void verify(PiDataBuildContext context, String path) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(path, "path");
        for (String number : numbers) {
            if (!context.hasVariable(number)) {
                throw new PiDataVerificationException(path + ".numbers." + number,
                        "missing engine context number `" + number + "`");
            }
        }
        for (Map.Entry<String, Class<?>> entry : objects.entrySet()) {
            String name = entry.getKey();
            Class<?> type = entry.getValue();
            if (!context.hasObject(name)) {
                throw new PiDataVerificationException(path + ".objects." + name,
                        "missing engine context object `" + name + "` of type " + type.getName());
            }
            if (!context.hasObject(name, type)) {
                throw new PiDataVerificationException(path + ".objects." + name,
                        "engine context object `" + name + "` must be " + type.getName()
                                + ", but validation context provides " + context.objectType(name).orElseThrow().getName());
            }
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PiEngineContextContract other)) {
            return false;
        }
        return numbers.equals(other.numbers) && objects.equals(other.objects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numbers, objects);
    }

    @Override
    public String toString() {
        return "PiEngineContextContract[numbers=" + numbers + ", objects=" + objects + "]";
    }

    static Class<?> mergeObjectType(String name, Class<?> existing, Class<?> next) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(existing, "existing");
        Objects.requireNonNull(next, "next");
        if (existing.equals(next)) {
            return existing;
        }
        if (existing == Object.class) {
            return next;
        }
        if (next == Object.class) {
            return existing;
        }
        if (existing.isAssignableFrom(next)) {
            return next;
        }
        if (next.isAssignableFrom(existing)) {
            return existing;
        }
        throw new IllegalArgumentException("conflicting engine context object type for `" + name + "`: "
                + existing.getName() + " vs " + next.getName());
    }

    private static String checkName(String name) {
        return PiEngineKeyNames.variable(name);
    }

    public static final class Builder {
        private final LinkedHashSet<String> numbers = new LinkedHashSet<>();
        private final LinkedHashMap<String, Class<?>> objects = new LinkedHashMap<>();

        public Builder number(String name) {
            numbers.add(checkName(name));
            return this;
        }

        public Builder number(PiEngineNumberKey key) {
            return number(Objects.requireNonNull(key, "key").name());
        }

        public Builder numbers(Collection<String> names) {
            Objects.requireNonNull(names, "names").forEach(this::number);
            return this;
        }

        public Builder object(String name, Class<?> type) {
            String checked = checkName(name);
            Class<?> checkedType = Objects.requireNonNull(type, "type");
            objects.merge(checked, checkedType, (existing, next) -> mergeObjectType(checked, existing, next));
            return this;
        }

        public Builder object(PiEngineContextKey<?> key) {
            Objects.requireNonNull(key, "key");
            return object(key.name(), key.type());
        }

        public Builder objects(Map<String, Class<?>> values) {
            Objects.requireNonNull(values, "values").forEach(this::object);
            return this;
        }

        public Builder merge(PiEngineContextContract contract) {
            Objects.requireNonNull(contract, "contract");
            numbers(contract.numbers());
            objects(contract.objects());
            return this;
        }

        public PiEngineContextContract build() {
            if (numbers.isEmpty() && objects.isEmpty()) {
                return EMPTY;
            }
            return new PiEngineContextContract(numbers, objects);
        }
    }
}
