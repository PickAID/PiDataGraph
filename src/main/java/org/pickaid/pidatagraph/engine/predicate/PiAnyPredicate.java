package org.pickaid.pidatagraph.engine.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;

public final class PiAnyPredicate implements PiEnginePredicate {
    private final List<PiEnginePredicate> predicates;

    public PiAnyPredicate(List<PiEnginePredicate> predicates) {
        this.predicates = List.copyOf(Objects.requireNonNull(predicates, "predicates"));
    }

    static Codec<PiAnyPredicate> codec(Codec<PiEnginePredicate> predicateCodec) {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.list(predicateCodec).fieldOf("predicates").forGetter(PiAnyPredicate::predicates)
        ).apply(instance, PiAnyPredicate::new));
    }

    public List<PiEnginePredicate> predicates() {
        return predicates;
    }

    @Override
    public PiEnginePredicateType<?> type() {
        return PiEnginePredicates.ANY;
    }

    @Override
    public boolean test(PiEngineContext context) {
        for (PiEnginePredicate predicate : predicates) {
            if (predicate.test(context)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public PiEngineContextContract contextContract() {
        PiEngineContextContract.Builder builder = PiEngineContextContract.builder();
        for (int index = 0; index < predicates.size(); index++) {
            builder.merge(PiEnginePredicates.contextContract("any predicate[" + index + "]", predicates.get(index)));
        }
        return builder.build();
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        for (int index = 0; index < predicates.size(); index++) {
            predicates.get(index).verify(context, path + ".predicates[" + index + "]");
        }
    }
}
