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

public final class PiEmitFlagAction implements PiEngineAction {
    private final String name;
    private final PiEnginePredicate predicate;

    public PiEmitFlagAction(String name, PiEnginePredicate predicate) {
        this.name = PiEngineActions.checkFrameValueName(Objects.requireNonNull(name, "name"));
        this.predicate = Objects.requireNonNull(predicate, "predicate");
    }

    static Codec<PiEmitFlagAction> codec(Codec<PiEngineAction> actionCodec, Codec<PiEnginePredicate> predicateCodec) {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(PiEmitFlagAction::name),
                predicateCodec.fieldOf("predicate").forGetter(PiEmitFlagAction::predicate)
        ).apply(instance, PiEmitFlagAction::new));
    }

    public String name() {
        return name;
    }

    public PiEnginePredicate predicate() {
        return predicate;
    }

    @Override
    public PiEngineActionType<?> type() {
        return PiEngineActions.EMIT_FLAG;
    }

    @Override
    public PiEngineFrame execute(PiEngineContext context) {
        return PiEngineFrame.builder().flag(name, predicate.test(context)).build();
    }

    @Override
    public PiEngineContextContract contextContract() {
        return PiEnginePredicates.contextContract("emit_flag predicate", predicate);
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        predicate.verify(context, path + ".predicate");
    }
}
