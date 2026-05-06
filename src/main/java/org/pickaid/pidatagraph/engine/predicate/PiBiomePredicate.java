package org.pickaid.pidatagraph.engine.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.action.PiEngineActions;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;

public final class PiBiomePredicate implements PiEnginePredicate {
    static final Codec<PiBiomePredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("level").forGetter(PiBiomePredicate::level),
            Codec.STRING.fieldOf("pos").forGetter(PiBiomePredicate::pos),
            ResourceLocation.CODEC.optionalFieldOf("biome").forGetter(PiBiomePredicate::biome),
            ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(PiBiomePredicate::tag)
    ).apply(instance, PiBiomePredicate::new));

    private final String level;
    private final String pos;
    private final Optional<ResourceLocation> biome;
    private final Optional<ResourceLocation> tag;

    public PiBiomePredicate(String level, String pos, Optional<ResourceLocation> biome, Optional<ResourceLocation> tag) {
        this.level = PiEngineActions.checkVariableName(Objects.requireNonNull(level, "level"));
        this.pos = PiEngineActions.checkVariableName(Objects.requireNonNull(pos, "pos"));
        this.biome = Objects.requireNonNull(biome, "biome");
        this.tag = Objects.requireNonNull(tag, "tag");
    }

    public PiBiomePredicate(
            PiEngineContextKey<? extends Level> level,
            PiEngineContextKey<BlockPos> pos,
            Optional<ResourceLocation> biome,
            Optional<ResourceLocation> tag
    ) {
        this(Objects.requireNonNull(level, "level").name(), Objects.requireNonNull(pos, "pos").name(), biome, tag);
    }

    public String level() {
        return level;
    }

    public String pos() {
        return pos;
    }

    public Optional<ResourceLocation> biome() {
        return biome;
    }

    public Optional<ResourceLocation> tag() {
        return tag;
    }

    @Override
    public PiEnginePredicateType<?> type() {
        return PiEnginePredicates.BIOME;
    }

    @Override
    public boolean test(PiEngineContext context) {
        Optional<Level> resolvedLevel = context.object(level, Level.class);
        Optional<BlockPos> resolvedPos = context.object(pos, BlockPos.class);
        if (resolvedLevel.isEmpty() || resolvedPos.isEmpty()) {
            return false;
        }
        Holder<Biome> holder = resolvedLevel.get().getBiome(resolvedPos.get());
        if (biome.isPresent() && !holder.is(ResourceKey.create(Registries.BIOME, biome.get()))) {
            return false;
        }
        return tag.isEmpty() || holder.is(TagKey.create(Registries.BIOME, tag.get()));
    }

    @Override
    public PiEngineContextContract contextContract() {
        return PiEngineContextContract.builder()
                .object(level, Level.class)
                .object(pos, BlockPos.class)
                .build();
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        PiEngineActions.verifyObject(path + ".level", context, level, Level.class);
        PiEngineActions.verifyObject(path + ".pos", context, pos, BlockPos.class);
    }
}
