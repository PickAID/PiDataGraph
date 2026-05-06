package org.pickaid.pidatagraph.engine.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.action.PiEngineActions;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;

public final class PiObjectEqualsPredicate implements PiEnginePredicate {
    static final Codec<PiObjectEqualsPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("left").forGetter(PiObjectEqualsPredicate::left),
            Codec.STRING.fieldOf("right").forGetter(PiObjectEqualsPredicate::right),
            Codec.BOOL.optionalFieldOf("identity", false).forGetter(PiObjectEqualsPredicate::identity)
    ).apply(instance, PiObjectEqualsPredicate::new));

    private final String left;
    private final String right;
    private final boolean identity;

    public PiObjectEqualsPredicate(String left, String right, boolean identity) {
        this.left = PiEngineActions.checkVariableName(Objects.requireNonNull(left, "left"));
        this.right = PiEngineActions.checkVariableName(Objects.requireNonNull(right, "right"));
        this.identity = identity;
    }

    public PiObjectEqualsPredicate(PiEngineContextKey<?> left, PiEngineContextKey<?> right, boolean identity) {
        this(Objects.requireNonNull(left, "left").name(), Objects.requireNonNull(right, "right").name(), identity);
    }

    public String left() {
        return left;
    }

    public String right() {
        return right;
    }

    public boolean identity() {
        return identity;
    }

    @Override
    public PiEnginePredicateType<?> type() {
        return PiEnginePredicates.OBJECT_EQUALS;
    }

    @Override
    public boolean test(PiEngineContext context) {
        Optional<Object> leftObject = context.object(left, Object.class);
        Optional<Object> rightObject = context.object(right, Object.class);
        if (leftObject.isEmpty() || rightObject.isEmpty()) {
            return false;
        }
        return identity ? leftObject.get() == rightObject.get() : leftObject.get().equals(rightObject.get());
    }

    @Override
    public PiEngineContextContract contextContract() {
        return PiEngineContextContract.builder()
                .object(left, Object.class)
                .object(right, Object.class)
                .build();
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        PiEngineActions.verifyObject(path + ".left", context, left, Object.class);
        PiEngineActions.verifyObject(path + ".right", context, right, Object.class);
    }
}
