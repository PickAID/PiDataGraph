package org.pickaid.pidatagraph.expression;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.DoubleSupplier;

public final class PiExpressionContext {
    private static final DoubleSupplier DEFAULT_RANDOM = Math::random;

    private final Map<String, Double> variables;
    private final DoubleSupplier random;

    private PiExpressionContext(Map<String, Double> variables, DoubleSupplier random) {
        this.variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables));
        this.random = Objects.requireNonNull(random, "random");
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, Double> variables() {
        return variables;
    }

    public double random() {
        return random.getAsDouble();
    }

    public static final class Builder {
        private final LinkedHashMap<String, Double> variables = new LinkedHashMap<>();
        private DoubleSupplier random = DEFAULT_RANDOM;

        public Builder variable(String name, double value) {
            variables.put(checkVariableName(name), value);
            return this;
        }

        public Builder variables(Map<String, Double> variables) {
            variables.forEach(this::variable);
            return this;
        }

        public Builder random(DoubleSupplier random) {
            this.random = Objects.requireNonNull(random, "random");
            return this;
        }

        public PiExpressionContext build() {
            return new PiExpressionContext(variables, random);
        }

        private static String checkVariableName(String variable) {
            String checked = Objects.requireNonNull(variable, "variable").trim();
            if (checked.isEmpty()) {
                throw new IllegalArgumentException("expression variable must not be blank");
            }
            char first = checked.charAt(0);
            if (!Character.isLetter(first) && first != '_') {
                throw new IllegalArgumentException("invalid expression variable: " + variable);
            }
            for (int index = 1; index < checked.length(); index++) {
                char next = checked.charAt(index);
                if (!Character.isLetterOrDigit(next) && next != '_') {
                    throw new IllegalArgumentException("invalid expression variable: " + variable);
                }
            }
            return checked;
        }
    }
}
