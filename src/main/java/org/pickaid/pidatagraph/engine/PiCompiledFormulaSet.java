package org.pickaid.pidatagraph.engine;

import java.util.List;
import java.util.Objects;

public final class PiCompiledFormulaSet {
    private final List<Entry<?>> entries;

    PiCompiledFormulaSet(List<Entry<?>> entries) {
        this.entries = List.copyOf(entries);
    }

    public PiEngineFrame evaluate(PiEngineContext context) {
        Objects.requireNonNull(context, "context");
        PiEngineFrame.Builder frame = PiEngineFrame.builder();
        for (Entry<?> entry : entries) {
            frame.value(entry.key(), entry.evaluate(context));
        }
        return frame.build();
    }

    record Entry<T>(String key, PiEngineFormula<T> formula) {
        Entry {
            key = Objects.requireNonNull(key, "key");
            formula = Objects.requireNonNull(formula, "formula");
        }

        T evaluate(PiEngineContext context) {
            return formula.evaluate(context);
        }
    }
}
