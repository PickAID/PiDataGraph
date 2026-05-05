package org.pickaid.pidatagraph.engine.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.PiEngineFrame;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.expression.PiIntExpression;

public final class PiRepeatAction implements PiEngineAction {
    private final PiIntExpression times;
    private final String index;
    private final PiEngineAction child;

    public PiRepeatAction(PiIntExpression times, String index, PiEngineAction child) {
        this.times = Objects.requireNonNull(times, "times");
        this.index = index == null ? "" : index.trim();
        if (!this.index.isEmpty()) {
            PiEngineActions.checkVariableName(this.index);
        }
        this.child = Objects.requireNonNull(child, "child");
    }

    static Codec<PiRepeatAction> codec(Codec<PiEngineAction> actionCodec) {
        return RecordCodecBuilder.create(instance -> instance.group(
                PiIntExpression.CODEC.fieldOf("times").forGetter(PiRepeatAction::times),
                Codec.STRING.optionalFieldOf("index", "").forGetter(PiRepeatAction::index),
                actionCodec.fieldOf("child").forGetter(PiRepeatAction::child)
        ).apply(instance, PiRepeatAction::new));
    }

    public PiIntExpression times() {
        return times;
    }

    public String index() {
        return index;
    }

    public PiEngineAction child() {
        return child;
    }

    @Override
    public PiEngineActionType<?> type() {
        return PiEngineActions.REPEAT;
    }

    @Override
    public PiEngineFrame execute(PiEngineContext context) {
        int count = Math.max(0, context.evaluate(times));
        PiEngineFrame frame = PiEngineFrame.empty();
        for (int i = 0; i < count; i++) {
            PiEngineContext childContext = index.isEmpty()
                    ? context
                    : context.derive().number(index, i).build();
            frame = frame.merge(childContext.execute(child));
        }
        return frame;
    }

    @Override
    public PiEngineContextContract contextContract() {
        PiEngineContextContract childContract = PiEngineActions.contextContract("repeat child", child);
        return index.isEmpty() ? childContract : childContract.withoutNumber(index);
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        PiEngineActions.verify(path + ".times", context, times);
        PiDataBuildContext childContext = index.isEmpty() ? context : context.withVariable(index);
        child.verify(childContext, path + ".child");
    }
}
