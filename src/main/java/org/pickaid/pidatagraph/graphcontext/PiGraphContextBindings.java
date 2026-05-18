package org.pickaid.pidatagraph.graphcontext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Objects;

/**
 * Binder factories for typed graph inputs.
 */
public final class PiGraphContextBindings {
    private PiGraphContextBindings() {
    }

    public static <T> PiGraphContextBinder<T> record(String id, Class<T> type) {
        Objects.requireNonNull(type, "type");
        if (!type.isRecord()) {
            throw new IllegalArgumentException("graph context binder requires a record type: " + type.getName());
        }
        PiGraphContextSchema schema = schemaFromRecord(id, type);
        return new RecordBinder<>(schema, type.getRecordComponents());
    }

    private static PiGraphContextSchema schemaFromRecord(String id, Class<?> type) {
        PiGraphContextSchema.Builder builder = PiGraphContextSchema.builder(id);
        for (RecordComponent component : type.getRecordComponents()) {
            if (isNumberType(component.getType())) {
                builder.number(component.getName());
            } else {
                builder.object(component.getName(), component.getType());
            }
        }
        return builder.build();
    }

    private static boolean isNumberType(Class<?> type) {
        return type == byte.class
                || type == short.class
                || type == int.class
                || type == long.class
                || type == float.class
                || type == double.class
                || Number.class.isAssignableFrom(type);
    }

    private record RecordBinder<T>(PiGraphContextSchema schema, RecordComponent[] components)
            implements PiGraphContextBinder<T> {
        private RecordBinder {
            Objects.requireNonNull(schema, "schema");
            components = Objects.requireNonNull(components, "components").clone();
        }

        @Override
        public PiGraphContext bind(T input) {
            Objects.requireNonNull(input, "input");
            PiGraphContext.Builder builder = PiGraphContext.builder();
            for (RecordComponent component : components) {
                Object value = read(component, input);
                if (isNumberType(component.getType())) {
                    builder.number(component.getName(), ((Number) value).doubleValue());
                } else {
                    builder.object(component.getName(), value);
                }
            }
            return builder.build();
        }

        private static Object read(RecordComponent component, Object input) {
            try {
                component.getAccessor().setAccessible(true);
                return component.getAccessor().invoke(input);
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new IllegalStateException("failed to read graph context record component `"
                        + component.getName() + "`", exception);
            }
        }
    }
}
