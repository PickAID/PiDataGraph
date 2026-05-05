package org.pickaid.pidatagraph.engine.predicate;

import java.util.Objects;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;

public interface PiEnginePredicate {
    PiEnginePredicateType<?> type();

    boolean test(PiEngineContext context);

    default PiEngineContextContract contextContract() {
        return PiEngineContextContract.empty();
    }

    default void verify(PiDataBuildContext context, String path) {
        Objects.requireNonNull(contextContract(), "engine predicate returned null context contract at " + path)
                .verify(context, path + ".context");
    }
}
