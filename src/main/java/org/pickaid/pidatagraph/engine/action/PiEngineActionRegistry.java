package org.pickaid.pidatagraph.engine.action;

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
import org.pickaid.pidatagraph.engine.predicate.PiEnginePredicate;
import org.pickaid.pidatagraph.engine.predicate.PiEnginePredicateRegistry;
import org.pickaid.pidatagraph.engine.predicate.PiEnginePredicateType;
import org.pickaid.pidatagraph.engine.predicate.PiEnginePredicates;

public final class PiEngineActionRegistry {
    private final Map<ResourceLocation, PiEngineActionType<? extends PiEngineAction>> types;
    private final PiEnginePredicateRegistry predicates;
    private final Codec<PiEngineAction> codec;

    private PiEngineActionRegistry(
            List<PiEngineActionType<? extends PiEngineAction>> types,
            List<PiEnginePredicateType<? extends PiEnginePredicate>> predicateTypes
    ) {
        LinkedHashMap<ResourceLocation, PiEngineActionType<? extends PiEngineAction>> checked = new LinkedHashMap<>();
        for (PiEngineActionType<? extends PiEngineAction> type : types) {
            PiEngineActionType<? extends PiEngineAction> previous = checked.putIfAbsent(type.id(), type);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate engine action type: " + type.id());
            }
        }
        this.types = Map.copyOf(checked);
        this.predicates = PiEnginePredicateRegistry.builder().install(predicateTypes).build();
        this.codec = createCodec();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static PiEngineActionRegistry standard() {
        return builder()
                .install(PiEngineActions.core())
                .installPredicates(PiEnginePredicates.core())
                .build();
    }

    public Codec<PiEngineAction> codec() {
        return codec;
    }

    public PiEngineActionType<? extends PiEngineAction> require(ResourceLocation id) {
        PiEngineActionType<? extends PiEngineAction> type = types.get(Objects.requireNonNull(id, "id"));
        if (type == null) {
            throw new IllegalArgumentException("unknown engine action type: " + id);
        }
        return type;
    }

    public Codec<PiEnginePredicate> predicateCodec() {
        return predicates.codec();
    }

    private Codec<PiEngineAction> createCodec() {
        Codec<PiEngineActionType<? extends PiEngineAction>> typeCodec = ResourceLocation.CODEC.comapFlatMap(
                id -> {
                    PiEngineActionType<? extends PiEngineAction> type = types.get(id);
                    if (type == null) {
                        return DataResult.error(() -> "unknown engine action type: " + id);
                    }
                    return DataResult.success(type);
                },
                PiEngineActionType::id
        );
        return ExtraCodecs.lazyInitializedCodec(() -> typeCodec.dispatch(
                "type",
                PiEngineAction::type,
                type -> cast(type).codec(codec, predicates.codec())
        ));
    }

    @SuppressWarnings("unchecked")
    private static <T extends PiEngineAction> PiEngineActionType<T> cast(PiEngineActionType<? extends PiEngineAction> type) {
        return (PiEngineActionType<T>) type;
    }

    public static final class Builder {
        private final List<PiEngineActionType<? extends PiEngineAction>> types = new ArrayList<>();
        private final List<PiEnginePredicateType<? extends PiEnginePredicate>> predicateTypes = new ArrayList<>();

        public Builder add(PiEngineActionType<? extends PiEngineAction> type) {
            types.add(Objects.requireNonNull(type, "type"));
            return this;
        }

        public Builder install(Collection<PiEngineActionType<? extends PiEngineAction>> types) {
            for (PiEngineActionType<? extends PiEngineAction> type : types) {
                add(type);
            }
            return this;
        }

        public Builder addPredicate(PiEnginePredicateType<? extends PiEnginePredicate> type) {
            predicateTypes.add(Objects.requireNonNull(type, "type"));
            return this;
        }

        public Builder installPredicates(Collection<PiEnginePredicateType<? extends PiEnginePredicate>> types) {
            for (PiEnginePredicateType<? extends PiEnginePredicate> type : types) {
                addPredicate(type);
            }
            return this;
        }

        public PiEngineActionRegistry build() {
            List<PiEnginePredicateType<? extends PiEnginePredicate>> resolvedPredicates =
                    predicateTypes.isEmpty() ? PiEnginePredicates.core() : predicateTypes;
            return new PiEngineActionRegistry(types, resolvedPredicates);
        }
    }
}
