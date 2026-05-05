package org.pickaid.pidatagraph.engine.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.PiEngineFrame;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;

public final class PiSequenceAction implements PiEngineAction {
    private final List<PiEngineAction> children;

    public PiSequenceAction(List<PiEngineAction> children) {
        this.children = List.copyOf(Objects.requireNonNull(children, "children"));
    }

    static Codec<PiSequenceAction> codec(Codec<PiEngineAction> actionCodec) {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.list(actionCodec).fieldOf("children").forGetter(PiSequenceAction::children)
        ).apply(instance, PiSequenceAction::new));
    }

    public List<PiEngineAction> children() {
        return children;
    }

    @Override
    public PiEngineActionType<?> type() {
        return PiEngineActions.SEQUENCE;
    }

    @Override
    public PiEngineFrame execute(PiEngineContext context) {
        PiEngineFrame frame = PiEngineFrame.empty();
        for (PiEngineAction child : children) {
            frame = frame.merge(context.execute(child));
        }
        return frame;
    }

    @Override
    public PiEngineContextContract contextContract() {
        PiEngineContextContract.Builder builder = PiEngineContextContract.builder();
        for (int index = 0; index < children.size(); index++) {
            builder.merge(PiEngineActions.contextContract("sequence child[" + index + "]", children.get(index)));
        }
        return builder.build();
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        for (int index = 0; index < children.size(); index++) {
            children.get(index).verify(context, path + ".children[" + index + "]");
        }
    }
}
