package org.pickaid.pidatagraph.engine.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.action.PiEngineActions;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;

public final class PiLevelDimensionPredicate implements PiEnginePredicate {
    static final Codec<PiLevelDimensionPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("level").forGetter(PiLevelDimensionPredicate::level),
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(PiLevelDimensionPredicate::dimension)
    ).apply(instance, PiLevelDimensionPredicate::new));

    private final String level;
    private final ResourceLocation dimension;

    public PiLevelDimensionPredicate(String level, ResourceLocation dimension) {
        this.level = PiEngineActions.checkVariableName(Objects.requireNonNull(level, "level"));
        this.dimension = Objects.requireNonNull(dimension, "dimension");
    }

    public PiLevelDimensionPredicate(PiEngineContextKey<? extends Level> level, ResourceLocation dimension) {
        this(Objects.requireNonNull(level, "level").name(), dimension);
    }

    public String level() {
        return level;
    }

    public ResourceLocation dimension() {
        return dimension;
    }

    @Override
    public PiEnginePredicateType<?> type() {
        return PiEnginePredicates.LEVEL_DIMENSION;
    }

    @Override
    public boolean test(PiEngineContext context) {
        Optional<Level> resolved = context.object(level, Level.class);
        return resolved.isPresent() && resolved.get().dimension().location().equals(dimension);
    }

    @Override
    public PiEngineContextContract contextContract() {
        return PiEngineContextContract.builder().object(level, Level.class).build();
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        PiEngineActions.verifyObject(path + ".level", context, level, Level.class);
    }
}
