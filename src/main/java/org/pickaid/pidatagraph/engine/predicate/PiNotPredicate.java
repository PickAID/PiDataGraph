package org.pickaid.pidatagraph.engine.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;

public final class PiNotPredicate implements PiEnginePredicate {
    private final PiEnginePredicate predicate;

    public PiNotPredicate(PiEnginePredicate predicate) {
        this.predicate = Objects.requireNonNull(predicate, "predicate");
    }

    static Codec<PiNotPredicate> codec(Codec<PiEnginePredicate> predicateCodec) {
        return RecordCodecBuilder.create(instance -> instance.group(
                predicateCodec.fieldOf("predicate").forGetter(PiNotPredicate::predicate)
        ).apply(instance, PiNotPredicate::new));
    }

    public PiEnginePredicate predicate() {
        return predicate;
    }

    @Override
    public PiEnginePredicateType<?> type() {
        return PiEnginePredicates.NOT;
    }

    @Override
    public boolean test(PiEngineContext context) {
        return !predicate.test(context);
    }

    @Override
    public PiEngineContextContract contextContract() {
        return PiEnginePredicates.contextContract("not predicate", predicate);
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        predicate.verify(context, path + ".predicate");
    }
}
