package org.pickaid.pidatagraph.engine.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.PiEngineFrame;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.expression.PiBooleanExpression;
import org.pickaid.pidatagraph.engine.predicate.PiEnginePredicate;
import org.pickaid.pidatagraph.engine.predicate.PiEnginePredicates;
import org.pickaid.pidatagraph.engine.predicate.PiExpressionPredicate;

public final class PiIfAction implements PiEngineAction {
    private final PiEnginePredicate condition;
    private final PiEngineAction thenAction;
    private final PiEngineAction elseAction;

    public PiIfAction(PiBooleanExpression condition, PiEngineAction thenAction, PiEngineAction elseAction) {
        this(new PiExpressionPredicate(Objects.requireNonNull(condition, "condition")), thenAction, elseAction);
    }

    public PiIfAction(PiEnginePredicate condition, PiEngineAction thenAction, PiEngineAction elseAction) {
        this.condition = Objects.requireNonNull(condition, "condition");
        this.thenAction = thenAction;
        this.elseAction = elseAction;
    }

    static Codec<PiIfAction> codec(Codec<PiEngineAction> actionCodec, Codec<PiEnginePredicate> predicateCodec) {
        return RecordCodecBuilder.create(instance -> instance.group(
                predicateCodec.fieldOf("condition").forGetter(PiIfAction::condition),
                actionCodec.optionalFieldOf("then").forGetter(action -> Optional.ofNullable(action.thenAction())),
                actionCodec.optionalFieldOf("else").forGetter(action -> Optional.ofNullable(action.elseAction()))
        ).apply(instance, (condition, thenAction, elseAction) -> new PiIfAction(
                condition,
                thenAction.orElse(null),
                elseAction.orElse(null)
        )));
    }

    public PiEnginePredicate condition() {
        return condition;
    }

    public PiEngineAction thenAction() {
        return thenAction;
    }

    public PiEngineAction elseAction() {
        return elseAction;
    }

    @Override
    public PiEngineActionType<?> type() {
        return PiEngineActions.IF;
    }

    @Override
    public PiEngineFrame execute(PiEngineContext context) {
        boolean accepted = condition.test(context);
        PiEngineAction selected = accepted ? thenAction : elseAction;
        return selected == null ? PiEngineFrame.empty() : context.execute(selected);
    }

    @Override
    public PiEngineContextContract contextContract() {
        PiEngineContextContract.Builder builder = PiEngineContextContract.builder()
                .merge(PiEnginePredicates.contextContract("if condition", condition));
        if (thenAction != null) {
            builder.merge(PiEngineActions.contextContract("if then", thenAction));
        }
        if (elseAction != null) {
            builder.merge(PiEngineActions.contextContract("if else", elseAction));
        }
        return builder.build();
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        condition.verify(context, path + ".condition");
        if (thenAction != null) {
            thenAction.verify(context, path + ".then");
        }
        if (elseAction != null) {
            elseAction.verify(context, path + ".else");
        }
    }
}
