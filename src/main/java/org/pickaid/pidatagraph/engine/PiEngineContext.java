package org.pickaid.pidatagraph.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.DoubleSupplier;
import net.minecraft.util.RandomSource;
import org.pickaid.pidatagraph.expression.PiCompiledBooleanExpression;
import org.pickaid.pidatagraph.expression.PiCompiledDoubleExpression;
import org.pickaid.pidatagraph.expression.PiCompiledIntExpression;
import org.pickaid.pidatagraph.expression.PiBooleanExpression;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;
import org.pickaid.pidatagraph.expression.PiExpressionContext;
import org.pickaid.pidatagraph.expression.PiExpressionLanguage;
import org.pickaid.pidatagraph.expression.PiExpressionScope;
import org.pickaid.pidatagraph.expression.PiIntExpression;
import org.pickaid.pidatagraph.engine.action.PiEngineAction;
import org.pickaid.pidatagraph.engine.action.PiEngineActionType;
import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.engine.context.PiEngineNumberKey;

public final class PiEngineContext {
    private final PiExpressionLanguage language;
    private final PiExpressionContext expressions;
    private final Map<String, Double> numbers;
    private final Map<String, Object> objects;

    private PiEngineContext(
            PiExpressionLanguage language,
            PiExpressionContext expressions,
            Map<String, Double> numbers,
            Map<String, Object> objects
    ) {
        this.language = Objects.requireNonNull(language, "language");
        this.expressions = Objects.requireNonNull(expressions, "expressions");
        this.numbers = Collections.unmodifiableMap(new LinkedHashMap<>(numbers));
        this.objects = Collections.unmodifiableMap(new LinkedHashMap<>(objects));
    }

    public static Builder builder() {
        return new Builder();
    }

    public PiExpressionContext expressions() {
        return expressions;
    }

    public PiExpressionLanguage language() {
        return language;
    }

    public Map<String, Double> numbers() {
        return numbers;
    }

    public double number(String key) {
        Double value = numbers.get(Objects.requireNonNull(key, "key"));
        if (value == null) {
            throw new IllegalArgumentException("missing engine context number: " + key);
        }
        return value;
    }

    public double number(PiEngineNumberKey key) {
        return number(Objects.requireNonNull(key, "key").name());
    }

    public boolean hasNumber(String key) {
        return numbers.containsKey(Objects.requireNonNull(key, "key"));
    }

    public boolean hasNumber(PiEngineNumberKey key) {
        return hasNumber(Objects.requireNonNull(key, "key").name());
    }

    public PiExpressionScope expressionScope() {
        return PiExpressionScope.builder().variables(numbers.keySet()).build();
    }

    public Builder derive() {
        return new Builder().language(language).numbers(numbers).objects(objects);
    }

    public double evaluate(PiCompiledDoubleExpression expression) {
        return expression.evaluate(expressions);
    }

    public int evaluate(PiCompiledIntExpression expression) {
        return expression.evaluate(expressions);
    }

    public boolean evaluate(PiCompiledBooleanExpression expression) {
        return expression.evaluate(expressions);
    }

    public double evaluate(PiDoubleExpression expression) {
        return evaluate(expression.compile(language, expressionScope()));
    }

    public int evaluate(PiIntExpression expression) {
        return evaluate(expression.compile(language, expressionScope()));
    }

    public boolean evaluate(PiBooleanExpression expression) {
        return evaluate(expression.compile(language, expressionScope()));
    }

    public double random() {
        return expressions.random();
    }

    public PiEngineFrame execute(PiEngineAction action) {
        PiEngineAction checked = Objects.requireNonNull(action, "action");
        PiEngineActionType<?> type = Objects.requireNonNull(checked.type(), "action returned null type");
        return Objects.requireNonNull(checked.execute(this), "action " + type.id() + " returned null frame");
    }

    public void verifyContract(PiEngineContextContract contract, String owner) {
        Objects.requireNonNull(contract, "contract");
        Objects.requireNonNull(owner, "owner");
        StringBuilder issues = new StringBuilder();
        for (String number : contract.numbers()) {
            if (!hasNumber(number)) {
                appendIssue(issues, "missing number `" + number + "`");
            }
        }
        for (Map.Entry<String, Class<?>> entry : contract.objects().entrySet()) {
            Object value = objects.get(entry.getKey());
            if (value == null) {
                appendIssue(issues, "missing object `" + entry.getKey() + "` of type " + entry.getValue().getName());
            } else if (!entry.getValue().isInstance(value)) {
                appendIssue(issues, "object `" + entry.getKey() + "` is " + value.getClass().getName()
                        + ", not " + entry.getValue().getName());
            }
        }
        if (issues.length() > 0) {
            throw new PiEngineContractViolation("engine context contract failed for " + owner + ": "
                    + issues
                    + "; available objects: " + objects.keySet()
                    + "; available numbers: " + numbers.keySet());
        }
    }

    public Optional<Object> object(String key) {
        return Optional.ofNullable(objects.get(Objects.requireNonNull(key, "key")));
    }

    public boolean hasObject(String key) {
        return objects.containsKey(Objects.requireNonNull(key, "key"));
    }

    public <T> Optional<T> object(String key, Class<T> type) {
        Object value = objects.get(Objects.requireNonNull(key, "key"));
        if (value == null) {
            return Optional.empty();
        }
        if (!type.isInstance(value)) {
            throw new ClassCastException("engine context object `" + key + "` is " + value.getClass().getName() + ", not " + type.getName());
        }
        return Optional.of(type.cast(value));
    }

    public <T> Optional<T> object(PiEngineContextKey<T> key) {
        Objects.requireNonNull(key, "key");
        return object(key.name(), key.type());
    }

    public <T> boolean hasObject(PiEngineContextKey<T> key) {
        return hasObject(Objects.requireNonNull(key, "key").name());
    }

    private static void appendIssue(StringBuilder issues, String issue) {
        if (issues.length() > 0) {
            issues.append("; ");
        }
        issues.append(issue);
    }

    public static final class Builder {
        private final PiExpressionContext.Builder expressions = PiExpressionContext.builder();
        private final LinkedHashMap<String, Double> numbers = new LinkedHashMap<>();
        private final LinkedHashMap<String, Object> objects = new LinkedHashMap<>();
        private PiExpressionLanguage language = PiExpressionLanguage.standard();

        public Builder language(PiExpressionLanguage language) {
            this.language = Objects.requireNonNull(language, "language");
            return this;
        }

        public Builder number(String key, double value) {
            String checked = checkKey(key);
            numbers.put(checked, value);
            expressions.variable(checked, value);
            return this;
        }

        public Builder number(PiEngineNumberKey key, double value) {
            return number(Objects.requireNonNull(key, "key").name(), value);
        }

        public Builder numbers(Map<String, Double> values) {
            values.forEach(this::number);
            return this;
        }

        public Builder objects(Map<String, Object> values) {
            values.forEach(this::object);
            return this;
        }

        public Builder random(DoubleSupplier random) {
            expressions.random(random);
            return this;
        }

        public Builder random(RandomSource random) {
            Objects.requireNonNull(random, "random");
            return random(random::nextDouble);
        }

        public Builder object(String key, Object value) {
            objects.put(checkKey(key), Objects.requireNonNull(value, "value"));
            return this;
        }

        public <T> Builder object(PiEngineContextKey<T> key, T value) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
            if (!key.type().isInstance(value)) {
                throw new ClassCastException("engine context object `" + key.name() + "` is " + value.getClass().getName() + ", not " + key.type().getName());
            }
            return object(key.name(), value);
        }

        public PiEngineContext build() {
            return new PiEngineContext(language, expressions.build(), numbers, objects);
        }

        private static String checkKey(String key) {
            String checked = Objects.requireNonNull(key, "key").trim();
            PiExpressionScope.of(checked);
            return checked;
        }
    }
}
