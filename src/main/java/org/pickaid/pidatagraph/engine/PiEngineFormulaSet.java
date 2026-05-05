package org.pickaid.pidatagraph.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.pickaid.pidatagraph.expression.PiBooleanExpression;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;
import org.pickaid.pidatagraph.expression.PiExpressionLanguage;
import org.pickaid.pidatagraph.expression.PiExpressionScope;
import org.pickaid.pidatagraph.expression.PiIntExpression;

public final class PiEngineFormulaSet {
    private final List<SourceEntry> entries;

    private PiEngineFormulaSet(List<SourceEntry> entries) {
        this.entries = List.copyOf(entries);
    }

    public static Builder builder() {
        return new Builder();
    }

    public PiCompiledFormulaSet compile(PiExpressionLanguage language, PiExpressionScope scope) {
        List<PiCompiledFormulaSet.Entry<?>> compiled = new ArrayList<>();
        for (SourceEntry entry : entries) {
            compiled.add(entry.compile(language, scope));
        }
        return new PiCompiledFormulaSet(compiled);
    }

    public static final class Builder {
        private final List<SourceEntry> entries = new ArrayList<>();

        public Builder number(String key, PiDoubleExpression expression) {
            entries.add(new NumberEntry(key, expression));
            return this;
        }

        public Builder integer(String key, PiIntExpression expression) {
            entries.add(new IntegerEntry(key, expression));
            return this;
        }

        public Builder flag(String key, PiBooleanExpression expression) {
            entries.add(new FlagEntry(key, expression));
            return this;
        }

        public PiEngineFormulaSet build() {
            return new PiEngineFormulaSet(entries);
        }
    }

    private sealed interface SourceEntry permits NumberEntry, IntegerEntry, FlagEntry {
        PiCompiledFormulaSet.Entry<?> compile(PiExpressionLanguage language, PiExpressionScope scope);
    }

    private record NumberEntry(String key, PiDoubleExpression expression) implements SourceEntry {
        private NumberEntry {
            key = Objects.requireNonNull(key, "key");
            expression = Objects.requireNonNull(expression, "expression");
        }

        @Override
        public PiCompiledFormulaSet.Entry<Double> compile(PiExpressionLanguage language, PiExpressionScope scope) {
            var compiled = expression.compile(language, scope);
            return new PiCompiledFormulaSet.Entry<>(key, context -> context.evaluate(compiled));
        }
    }

    private record IntegerEntry(String key, PiIntExpression expression) implements SourceEntry {
        private IntegerEntry {
            key = Objects.requireNonNull(key, "key");
            expression = Objects.requireNonNull(expression, "expression");
        }

        @Override
        public PiCompiledFormulaSet.Entry<Integer> compile(PiExpressionLanguage language, PiExpressionScope scope) {
            var compiled = expression.compile(language, scope);
            return new PiCompiledFormulaSet.Entry<>(key, context -> context.evaluate(compiled));
        }
    }

    private record FlagEntry(String key, PiBooleanExpression expression) implements SourceEntry {
        private FlagEntry {
            key = Objects.requireNonNull(key, "key");
            expression = Objects.requireNonNull(expression, "expression");
        }

        @Override
        public PiCompiledFormulaSet.Entry<Boolean> compile(PiExpressionLanguage language, PiExpressionScope scope) {
            var compiled = expression.compile(language, scope);
            return new PiCompiledFormulaSet.Entry<>(key, context -> context.evaluate(compiled));
        }
    }
}
