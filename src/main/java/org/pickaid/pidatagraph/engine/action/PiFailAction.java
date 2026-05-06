package org.pickaid.pidatagraph.engine.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.PiEngineFrame;

public final class PiFailAction implements PiEngineAction {
    static final Codec<PiFailAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("message").forGetter(PiFailAction::message)
    ).apply(instance, PiFailAction::new));

    private final String message;

    public PiFailAction(String message) {
        this.message = Objects.requireNonNull(message, "message");
    }

    public String message() {
        return message;
    }

    @Override
    public PiEngineActionType<?> type() {
        return PiEngineActions.FAIL;
    }

    @Override
    public PiEngineFrame execute(PiEngineContext context) {
        throw new IllegalStateException("engine fail action: " + message);
    }
}
