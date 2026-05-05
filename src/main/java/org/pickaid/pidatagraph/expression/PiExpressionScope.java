package org.pickaid.pidatagraph.expression;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class PiExpressionScope {
    private static final PiExpressionScope EMPTY = new PiExpressionScope(Set.of());

    private final Set<String> variables;

    private PiExpressionScope(Collection<String> variables) {
        LinkedHashSet<String> checked = new LinkedHashSet<>();
        for (String variable : variables) {
            checked.add(checkVariableName(variable));
        }
        this.variables = Collections.unmodifiableSet(checked);
    }

    public static PiExpressionScope empty() {
        return EMPTY;
    }

    public static PiExpressionScope of(String... variables) {
        if (variables.length == 0) {
            return EMPTY;
        }
        return new PiExpressionScope(Arrays.asList(variables));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Set<String> variables() {
        return variables;
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

    public static final class Builder {
        private final LinkedHashSet<String> variables = new LinkedHashSet<>();

        public Builder variable(String variable) {
            variables.add(checkVariableName(variable));
            return this;
        }

        public Builder variables(String... variables) {
            for (String variable : variables) {
                variable(variable);
            }
            return this;
        }

        public Builder variables(Collection<String> variables) {
            for (String variable : variables) {
                variable(variable);
            }
            return this;
        }

        public PiExpressionScope build() {
            if (variables.isEmpty()) {
                return EMPTY;
            }
            return new PiExpressionScope(variables);
        }
    }
}
