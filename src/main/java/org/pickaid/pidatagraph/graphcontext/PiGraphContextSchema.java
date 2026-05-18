package org.pickaid.pidatagraph.graphcontext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.engine.context.PiEngineKeyNames;

public final class PiGraphContextSchema {
    private final String id;
    private final List<PiGraphContextField> fields;

    private PiGraphContextSchema(String id, List<PiGraphContextField> fields) {
        this.id = PiEngineKeyNames.variable(id);
        this.fields = List.copyOf(fields);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static PiGraphContextSchema fromEngineContract(String id, PiEngineContextContract contract) {
        Objects.requireNonNull(contract, "contract");
        Builder builder = builder(id);
        contract.numbers().forEach(builder::number);
        contract.objects().forEach(builder::object);
        return builder.build();
    }

    public String id() {
        return id;
    }

    public List<PiGraphContextField> fields() {
        return fields;
    }

    public Set<String> numbers() {
        LinkedHashSet<String> numbers = new LinkedHashSet<>();
        for (PiGraphContextField field : fields) {
            if (field.kind() == PiGraphContextField.Kind.NUMBER) {
                numbers.add(field.name());
            }
        }
        return Collections.unmodifiableSet(numbers);
    }

    public Map<String, Class<?>> objects() {
        LinkedHashMap<String, Class<?>> objects = new LinkedHashMap<>();
        for (PiGraphContextField field : fields) {
            if (field.kind() == PiGraphContextField.Kind.OBJECT) {
                objects.put(field.name(), field.type());
            }
        }
        return Collections.unmodifiableMap(objects);
    }

    public PiEngineContextContract toEngineContract() {
        return PiEngineContextContract.builder()
                .numbers(numbers())
                .objects(objects())
                .build();
    }

    public static final class Builder {
        private final String id;
        private final LinkedHashMap<String, PiGraphContextField> fields = new LinkedHashMap<>();

        private Builder(String id) {
            this.id = PiEngineKeyNames.variable(id);
        }

        public Builder number(String name) {
            add(PiGraphContextField.number(name));
            return this;
        }

        public Builder object(String name, Class<?> type) {
            add(PiGraphContextField.object(name, type));
            return this;
        }

        public Builder object(PiGraphContextKey<?> key) {
            Objects.requireNonNull(key, "key");
            return object(key.name(), key.type());
        }

        private void add(PiGraphContextField field) {
            PiGraphContextField previous = fields.putIfAbsent(field.name(), field);
            if (previous == null) {
                return;
            }
            if (previous.kind() != field.kind()) {
                throw new IllegalArgumentException("conflicting graph context field kind for `" + field.name() + "`");
            }
            if (previous.kind() == PiGraphContextField.Kind.OBJECT) {
                fields.put(field.name(), PiGraphContextField.object(field.name(),
                        mergeObjectType(field.name(), previous.type(), field.type())));
            }
        }

        public PiGraphContextSchema build() {
            return new PiGraphContextSchema(id, new ArrayList<>(fields.values()));
        }

        private static Class<?> mergeObjectType(String name, Class<?> existing, Class<?> next) {
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
            throw new IllegalArgumentException("conflicting graph context object type for `" + name + "`: "
                    + existing.getName() + " vs " + next.getName());
        }
    }
}
