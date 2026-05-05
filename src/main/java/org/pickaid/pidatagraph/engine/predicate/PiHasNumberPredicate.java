package org.pickaid.pidatagraph.engine.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.action.PiEngineActions;

public final class PiHasNumberPredicate implements PiEnginePredicate {
    static final Codec<PiHasNumberPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("key").forGetter(PiHasNumberPredicate::key)
    ).apply(instance, PiHasNumberPredicate::new));

    private final String key;

    public PiHasNumberPredicate(String key) {
        this.key = PiEngineActions.checkVariableName(Objects.requireNonNull(key, "key"));
    }

    public String key() {
        return key;
    }

    @Override
    public PiEnginePredicateType<?> type() {
        return PiEnginePredicates.HAS_NUMBER;
    }

    @Override
    public boolean test(PiEngineContext context) {
        return context.hasNumber(key);
    }
}
