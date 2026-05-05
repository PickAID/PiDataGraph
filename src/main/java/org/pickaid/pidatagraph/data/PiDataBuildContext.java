package org.pickaid.pidatagraph.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.pickaid.pidatagraph.expression.PiExpressionLanguage;
import org.pickaid.pidatagraph.expression.PiExpressionScope;

public final class PiDataBuildContext {
    private final PiExpressionLanguage expressionLanguage;
    private final PiExpressionScope expressionScope;
    private final Map<String, Class<?>> objectTypes;

    private PiDataBuildContext(PiExpressionLanguage expressionLanguage, PiExpressionScope expressionScope, Map<String, Class<?>> objectTypes) {
        this.expressionLanguage = Objects.requireNonNull(expressionLanguage, "expressionLanguage");
        this.expressionScope = Objects.requireNonNull(expressionScope, "expressionScope");
        this.objectTypes = Collections.unmodifiableMap(new LinkedHashMap<>(objectTypes));
    }

    public static Builder builder() {
        return new Builder();
    }

    public PiExpressionLanguage expressionLanguage() {
        return expressionLanguage;
    }

    public PiExpressionScope expressionScope() {
        return expressionScope;
    }

    public Map<String, Class<?>> objectTypes() {
        return objectTypes;
    }

    public boolean hasVariable(String variable) {
        return expressionScope.variables().contains(checkName(variable));
    }

    public boolean hasObject(String name) {
        return objectTypes.containsKey(checkName(name));
    }

    public boolean hasObject(String name, Class<?> requiredType) {
        Class<?> providedType = objectTypes.get(checkName(name));
        if (providedType == null) {
            return false;
        }
        return providedType == Object.class || Objects.requireNonNull(requiredType, "requiredType").isAssignableFrom(providedType);
    }

    public Optional<Class<?>> objectType(String name) {
        return Optional.ofNullable(objectTypes.get(checkName(name)));
    }

    public PiDataBuildContext withVariable(String variable) {
        return builder()
                .expressionLanguage(expressionLanguage)
                .expressionScope(PiExpressionScope.builder()
                        .variables(expressionScope.variables())
                        .variable(variable)
                        .build())
                .objects(objectTypes)
                .build();
    }

    public PiDataBuildContext withObject(String name, Class<?> type) {
        return builder()
                .expressionLanguage(expressionLanguage)
                .expressionScope(expressionScope)
                .objects(objectTypes)
                .object(name, type)
                .build();
    }

    private static String checkName(String name) {
        String checked = Objects.requireNonNull(name, "name").trim();
        PiExpressionScope.of(checked);
        return checked;
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
        throw new IllegalArgumentException("conflicting engine context object type for `" + name + "`: "
                + existing.getName() + " vs " + next.getName());
    }

    public static final class Builder {
        private PiExpressionLanguage expressionLanguage = PiExpressionLanguage.standard();
        private PiExpressionScope expressionScope = PiExpressionScope.empty();
        private final LinkedHashMap<String, Class<?>> objectTypes = new LinkedHashMap<>();

        public Builder expressionLanguage(PiExpressionLanguage expressionLanguage) {
            this.expressionLanguage = Objects.requireNonNull(expressionLanguage, "expressionLanguage");
            return this;
        }

        public Builder expressionScope(PiExpressionScope expressionScope) {
            this.expressionScope = Objects.requireNonNull(expressionScope, "expressionScope");
            return this;
        }

        public Builder object(String name, Class<?> type) {
            String checked = checkName(name);
            Class<?> checkedType = Objects.requireNonNull(type, "type");
            objectTypes.merge(checked, checkedType, (existing, next) -> mergeObjectType(checked, existing, next));
            return this;
        }

        public Builder objects(Map<String, Class<?>> values) {
            Objects.requireNonNull(values, "values").forEach(this::object);
            return this;
        }

        public PiDataBuildContext build() {
            return new PiDataBuildContext(expressionLanguage, expressionScope, objectTypes);
        }
    }
}
