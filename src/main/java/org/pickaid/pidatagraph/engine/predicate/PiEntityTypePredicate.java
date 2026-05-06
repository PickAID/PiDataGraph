package org.pickaid.pidatagraph.engine.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.action.PiEngineActions;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;

public final class PiEntityTypePredicate implements PiEnginePredicate {
    static final Codec<PiEntityTypePredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("entity").forGetter(PiEntityTypePredicate::entity),
            ResourceLocation.CODEC.optionalFieldOf("entity_type").forGetter(PiEntityTypePredicate::entityType),
            ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(PiEntityTypePredicate::tag)
    ).apply(instance, PiEntityTypePredicate::new));

    private final String entity;
    private final Optional<ResourceLocation> entityType;
    private final Optional<ResourceLocation> tag;

    public PiEntityTypePredicate(String entity, Optional<ResourceLocation> entityType, Optional<ResourceLocation> tag) {
        this.entity = PiEngineActions.checkVariableName(Objects.requireNonNull(entity, "entity"));
        this.entityType = Objects.requireNonNull(entityType, "entityType");
        this.tag = Objects.requireNonNull(tag, "tag");
    }

    public PiEntityTypePredicate(
            PiEngineContextKey<? extends Entity> entity,
            Optional<ResourceLocation> entityType,
            Optional<ResourceLocation> tag
    ) {
        this(Objects.requireNonNull(entity, "entity").name(), entityType, tag);
    }

    public String entity() {
        return entity;
    }

    public Optional<ResourceLocation> entityType() {
        return entityType;
    }

    public Optional<ResourceLocation> tag() {
        return tag;
    }

    @Override
    public PiEnginePredicateType<?> type() {
        return PiEnginePredicates.ENTITY_TYPE;
    }

    @Override
    public boolean test(PiEngineContext context) {
        Optional<Entity> resolved = context.object(entity, Entity.class);
        if (resolved.isEmpty()) {
            return false;
        }
        EntityType<?> type = resolved.get().getType();
        if (entityType.isPresent() && BuiltInRegistries.ENTITY_TYPE.getOptional(entityType.get()).filter(type::equals).isEmpty()) {
            return false;
        }
        return tag.isEmpty() || type.is(TagKey.create(Registries.ENTITY_TYPE, tag.get()));
    }

    @Override
    public PiEngineContextContract contextContract() {
        return PiEngineContextContract.builder().object(entity, Entity.class).build();
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        PiEngineActions.verifyObject(path + ".entity", context, entity, Entity.class);
    }
}
