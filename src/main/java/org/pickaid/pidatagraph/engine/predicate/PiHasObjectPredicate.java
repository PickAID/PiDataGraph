package org.pickaid.pidatagraph.engine.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.action.PiEngineActions;
import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;

public final class PiHasObjectPredicate implements PiEnginePredicate {
    static final Codec<PiHasObjectPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("key").forGetter(PiHasObjectPredicate::key)
    ).apply(instance, PiHasObjectPredicate::new));

    private final String key;

    public PiHasObjectPredicate(String key) {
        this.key = PiEngineActions.checkVariableName(Objects.requireNonNull(key, "key"));
    }

    public PiHasObjectPredicate(PiEngineContextKey<?> key) {
        this(Objects.requireNonNull(key, "key").name());
    }

    public String key() {
        return key;
    }

    @Override
    public PiEnginePredicateType<?> type() {
        return PiEnginePredicates.HAS_OBJECT;
    }

    @Override
    public boolean test(PiEngineContext context) {
        return context.hasObject(key);
    }
}
