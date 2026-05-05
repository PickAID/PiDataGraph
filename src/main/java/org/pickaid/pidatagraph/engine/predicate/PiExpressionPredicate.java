package org.pickaid.pidatagraph.engine.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.expression.PiBooleanExpression;

public final class PiExpressionPredicate implements PiEnginePredicate {
    static final Codec<PiExpressionPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PiBooleanExpression.CODEC.fieldOf("value").forGetter(PiExpressionPredicate::expression)
    ).apply(instance, PiExpressionPredicate::new));

    private final PiBooleanExpression expression;

    public PiExpressionPredicate(PiBooleanExpression expression) {
        this.expression = Objects.requireNonNull(expression, "expression");
    }

    public static PiExpressionPredicate of(String expression) {
        return new PiExpressionPredicate(PiBooleanExpression.of(expression));
    }

    public PiBooleanExpression expression() {
        return expression;
    }

    @Override
    public PiEnginePredicateType<?> type() {
        return PiEnginePredicates.EXPRESSION;
    }

    @Override
    public boolean test(PiEngineContext context) {
        return context.evaluate(expression);
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        PiEnginePredicates.verify(path, context, expression);
    }
}
