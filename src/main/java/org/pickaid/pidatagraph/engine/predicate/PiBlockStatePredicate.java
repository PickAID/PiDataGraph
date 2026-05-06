package org.pickaid.pidatagraph.engine.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.action.PiEngineActions;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;

public final class PiBlockStatePredicate implements PiEnginePredicate {
    static final Codec<PiBlockStatePredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("level").forGetter(PiBlockStatePredicate::level),
            Codec.STRING.fieldOf("pos").forGetter(PiBlockStatePredicate::pos),
            ResourceLocation.CODEC.optionalFieldOf("block").forGetter(PiBlockStatePredicate::block),
            ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(PiBlockStatePredicate::tag)
    ).apply(instance, PiBlockStatePredicate::new));

    private final String level;
    private final String pos;
    private final Optional<ResourceLocation> block;
    private final Optional<ResourceLocation> tag;

    public PiBlockStatePredicate(String level, String pos, Optional<ResourceLocation> block, Optional<ResourceLocation> tag) {
        this.level = PiEngineActions.checkVariableName(Objects.requireNonNull(level, "level"));
        this.pos = PiEngineActions.checkVariableName(Objects.requireNonNull(pos, "pos"));
        this.block = Objects.requireNonNull(block, "block");
        this.tag = Objects.requireNonNull(tag, "tag");
    }

    public PiBlockStatePredicate(
            PiEngineContextKey<? extends Level> level,
            PiEngineContextKey<BlockPos> pos,
            Optional<ResourceLocation> block,
            Optional<ResourceLocation> tag
    ) {
        this(Objects.requireNonNull(level, "level").name(), Objects.requireNonNull(pos, "pos").name(), block, tag);
    }

    public String level() {
        return level;
    }

    public String pos() {
        return pos;
    }

    public Optional<ResourceLocation> block() {
        return block;
    }

    public Optional<ResourceLocation> tag() {
        return tag;
    }

    @Override
    public PiEnginePredicateType<?> type() {
        return PiEnginePredicates.BLOCK_STATE;
    }

    @Override
    public boolean test(PiEngineContext context) {
        Optional<Level> resolvedLevel = context.object(level, Level.class);
        Optional<BlockPos> resolvedPos = context.object(pos, BlockPos.class);
        if (resolvedLevel.isEmpty() || resolvedPos.isEmpty()) {
            return false;
        }
        BlockState state = resolvedLevel.get().getBlockState(resolvedPos.get());
        if (block.isPresent() && BuiltInRegistries.BLOCK.getOptional(block.get()).filter(state::is).isEmpty()) {
            return false;
        }
        return tag.isEmpty() || state.is(TagKey.create(Registries.BLOCK, tag.get()));
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
