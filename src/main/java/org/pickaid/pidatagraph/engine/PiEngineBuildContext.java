package org.pickaid.pidatagraph.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.engine.context.PiEngineKeyNames;
import org.pickaid.pidatagraph.expression.PiExpressionLanguage;
import org.pickaid.pidatagraph.expression.PiExpressionScope;

public final class PiEngineBuildContext {
    private final PiExpressionLanguage expressionLanguage;
    private final PiExpressionScope expressionScope;
    private final Map<String, Class<?>> objectTypes;

    private PiEngineBuildContext(PiExpressionLanguage expressionLanguage, PiExpressionScope expressionScope, Map<String, Class<?>> objectTypes) {
        this.expressionLanguage = Objects.requireNonNull(expressionLanguage, "expressionLanguage");
        this.expressionScope = Objects.requireNonNull(expressionScope, "expressionScope");
        this.objectTypes = Collections.unmodifiableMap(new LinkedHashMap<>(objectTypes));
    }

    public static PiEngineBuildContext standard() {
        return builder().build();
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

    public PiEngineBuildContext withScope(PiExpressionScope scope) {
        return new PiEngineBuildContext(expressionLanguage, scope, objectTypes);
    }

    public PiEngineBuildContext withObject(String name, Class<?> type) {
        return builder()
                .expressionLanguage(expressionLanguage)
                .expressionScope(expressionScope)
                .objects(objectTypes)
                .object(name, type)
                .build();
    }

    public PiEngineBuildContext withContextContract(PiEngineContextContract contract) {
        Objects.requireNonNull(contract, "contract");
        return builder()
                .expressionLanguage(expressionLanguage)
                .expressionScope(PiExpressionScope.builder()
                        .variables(expressionScope.variables())
                        .variables(contract.numbers())
                        .build())
                .objects(objectTypes)
                .objects(contract.objects())
                .build();
    }

    public PiDataBuildContext dataContext() {
        return PiDataBuildContext.builder()
                .expressionLanguage(expressionLanguage)
                .expressionScope(expressionScope)
                .objects(objectTypes)
                .build();
    }

    private static String checkName(String name) {
        return PiEngineKeyNames.object(name);
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

        public PiEngineBuildContext build() {
            return new PiEngineBuildContext(expressionLanguage, expressionScope, objectTypes);
        }
    }
}
