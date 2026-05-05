package org.pickaid.pidatagraph.engine.predicate;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import org.pickaid.pidatagraph.expression.PiBooleanExpression;

public final class PiEnginePredicateRegistry {
    private final Map<ResourceLocation, PiEnginePredicateType<? extends PiEnginePredicate>> types;
    private final Codec<PiEnginePredicate> codec;

    private PiEnginePredicateRegistry(List<PiEnginePredicateType<? extends PiEnginePredicate>> types) {
        LinkedHashMap<ResourceLocation, PiEnginePredicateType<? extends PiEnginePredicate>> checked = new LinkedHashMap<>();
        for (PiEnginePredicateType<? extends PiEnginePredicate> type : types) {
            PiEnginePredicateType<? extends PiEnginePredicate> previous = checked.putIfAbsent(type.id(), type);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate engine predicate type: " + type.id());
            }
        }
        this.types = Map.copyOf(checked);
        this.codec = createCodec();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Codec<PiEnginePredicate> codec() {
        return codec;
    }

    private Codec<PiEnginePredicate> createCodec() {
        Codec<PiEnginePredicateType<? extends PiEnginePredicate>> typeCodec = ResourceLocation.CODEC.comapFlatMap(
                id -> {
                    PiEnginePredicateType<? extends PiEnginePredicate> type = types.get(id);
                    if (type == null) {
                        return DataResult.error(() -> "unknown engine predicate type: " + id);
                    }
                    return DataResult.success(type);
                },
                PiEnginePredicateType::id
        );
        Codec<PiEnginePredicate> dispatched = ExtraCodecs.lazyInitializedCodec(() -> typeCodec.dispatch(
                "type",
                PiEnginePredicate::type,
                type -> cast(type).codec(codec)
        ));
        return Codec.either(PiBooleanExpression.CODEC, dispatched).xmap(
                either -> either.map(PiExpressionPredicate::new, predicate -> predicate),
                predicate -> predicate instanceof PiExpressionPredicate expression
                        ? Either.left(expression.expression())
                        : Either.right(predicate)
        );
    }

    @SuppressWarnings("unchecked")
    private static <T extends PiEnginePredicate> PiEnginePredicateType<T> cast(PiEnginePredicateType<? extends PiEnginePredicate> type) {
        return (PiEnginePredicateType<T>) type;
    }

    public static final class Builder {
        private final List<PiEnginePredicateType<? extends PiEnginePredicate>> types = new ArrayList<>();

        public Builder add(PiEnginePredicateType<? extends PiEnginePredicate> type) {
            types.add(Objects.requireNonNull(type, "type"));
            return this;
        }

        public Builder install(Collection<PiEnginePredicateType<? extends PiEnginePredicate>> types) {
            for (PiEnginePredicateType<? extends PiEnginePredicate> type : types) {
                add(type);
            }
            return this;
        }

        public PiEnginePredicateRegistry build() {
            return new PiEnginePredicateRegistry(types);
        }
    }
}
