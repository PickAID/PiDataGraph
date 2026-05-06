package org.pickaid.pidatagraph.engine.action;

import com.mojang.serialization.Codec;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.PiEngineFrame;

public final class PiNoopAction implements PiEngineAction {
    static final Codec<PiNoopAction> CODEC = Codec.unit(new PiNoopAction());

    @Override
    public PiEngineActionType<?> type() {
        return PiEngineActions.NOOP;
    }

    @Override
    public PiEngineFrame execute(PiEngineContext context) {
        return PiEngineFrame.empty();
    }
}
