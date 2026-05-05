package org.pickaid.pidatagraph.engine.action;

import java.util.Objects;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.PiEngineFrame;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;

/**
 * A codec-backed gameplay node that can run against one engine context.
 */
public interface PiEngineAction {
    PiEngineActionType<?> type();

    PiEngineFrame execute(PiEngineContext context);

    default PiEngineContextContract contextContract() {
        return PiEngineContextContract.empty();
    }

    default void verify(PiDataBuildContext context, String path) {
        Objects.requireNonNull(contextContract(), "engine action returned null context contract at " + path)
                .verify(context, path + ".context");
    }
}
