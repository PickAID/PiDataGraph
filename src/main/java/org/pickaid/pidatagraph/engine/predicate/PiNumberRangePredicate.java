package org.pickaid.pidatagraph.engine.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.action.PiEngineActions;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.engine.context.PiEngineNumberKey;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;

public final class PiNumberRangePredicate implements PiEnginePredicate {
    static final Codec<PiNumberRangePredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("key").forGetter(PiNumberRangePredicate::key),
            PiDoubleExpression.CODEC.optionalFieldOf("min").forGetter(PiNumberRangePredicate::min),
            PiDoubleExpression.CODEC.optionalFieldOf("max").forGetter(PiNumberRangePredicate::max)
    ).apply(instance, PiNumberRangePredicate::new));

    private final String key;
    private final Optional<PiDoubleExpression> min;
    private final Optional<PiDoubleExpression> max;

    public PiNumberRangePredicate(String key, Optional<PiDoubleExpression> min, Optional<PiDoubleExpression> max) {
        this.key = PiEngineActions.checkVariableName(Objects.requireNonNull(key, "key"));
        this.min = Objects.requireNonNull(min, "min");
        this.max = Objects.requireNonNull(max, "max");
    }

    public PiNumberRangePredicate(PiEngineNumberKey key, Optional<PiDoubleExpression> min, Optional<PiDoubleExpression> max) {
        this(Objects.requireNonNull(key, "key").name(), min, max);
    }

    public String key() {
        return key;
    }

    public Optional<PiDoubleExpression> min() {
        return min;
    }

    public Optional<PiDoubleExpression> max() {
        return max;
    }

    @Override
    public PiEnginePredicateType<?> type() {
        return PiEnginePredicates.NUMBER_RANGE;
    }

    @Override
    public boolean test(PiEngineContext context) {
        if (!context.hasNumber(key)) {
            return false;
        }
        double value = context.number(key);
        if (min.isPresent() && value < context.evaluate(min.get())) {
            return false;
        }
        return max.isEmpty() || value <= context.evaluate(max.get());
    }

    @Override
    public PiEngineContextContract contextContract() {
        return PiEngineContextContract.builder().number(key).build();
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        PiEngineActions.verifyNumber(path + ".key", context, key);
        min.ifPresent(expression -> PiEnginePredicates.verify(path + ".min", context, expression));
        max.ifPresent(expression -> PiEnginePredicates.verify(path + ".max", context, expression));
    }
}
