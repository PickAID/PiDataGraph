package org.pickaid.pidatagraph.engine.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;

public final class PiChancePredicate implements PiEnginePredicate {
    static final Codec<PiChancePredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PiDoubleExpression.CODEC.fieldOf("chance").forGetter(PiChancePredicate::chance)
    ).apply(instance, PiChancePredicate::new));

    private final PiDoubleExpression chance;

    public PiChancePredicate(PiDoubleExpression chance) {
        this.chance = Objects.requireNonNull(chance, "chance");
    }

    public PiDoubleExpression chance() {
        return chance;
    }

    @Override
    public PiEnginePredicateType<?> type() {
        return PiEnginePredicates.CHANCE;
    }

    @Override
    public boolean test(PiEngineContext context) {
        double value = Math.max(0.0, Math.min(1.0, context.evaluate(chance)));
        return context.random() < value;
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        PiEnginePredicates.verify(path + ".chance", context, chance);
    }
}
