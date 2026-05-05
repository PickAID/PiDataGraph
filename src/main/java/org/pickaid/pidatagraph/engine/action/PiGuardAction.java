package org.pickaid.pidatagraph.engine.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.PiEngineFrame;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.engine.predicate.PiEnginePredicate;
import org.pickaid.pidatagraph.engine.predicate.PiEnginePredicates;

public final class PiGuardAction implements PiEngineAction {
    private final PiEnginePredicate predicate;
    private final PiEngineAction child;

    public PiGuardAction(PiEnginePredicate predicate, PiEngineAction child) {
        this.predicate = Objects.requireNonNull(predicate, "predicate");
        this.child = Objects.requireNonNull(child, "child");
    }

    static Codec<PiGuardAction> codec(Codec<PiEngineAction> actionCodec, Codec<PiEnginePredicate> predicateCodec) {
        return RecordCodecBuilder.create(instance -> instance.group(
                predicateCodec.fieldOf("predicate").forGetter(PiGuardAction::predicate),
                actionCodec.fieldOf("child").forGetter(PiGuardAction::child)
        ).apply(instance, PiGuardAction::new));
    }

    public PiEnginePredicate predicate() {
        return predicate;
    }

    public PiEngineAction child() {
        return child;
    }

    @Override
    public PiEngineActionType<?> type() {
        return PiEngineActions.GUARD;
    }

    @Override
    public PiEngineFrame execute(PiEngineContext context) {
        if (!predicate.test(context)) {
            return PiEngineFrame.empty();
        }
        return context.execute(child);
    }

    @Override
    public PiEngineContextContract contextContract() {
        return PiEnginePredicates.contextContract("guard predicate", predicate)
                .merge(PiEngineActions.contextContract("guard child", child));
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        predicate.verify(context, path + ".predicate");
        child.verify(context, path + ".child");
    }
}
