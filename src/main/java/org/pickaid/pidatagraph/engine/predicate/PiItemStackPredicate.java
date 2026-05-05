package org.pickaid.pidatagraph.engine.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.action.PiEngineActions;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;

public final class PiItemStackPredicate implements PiEnginePredicate {
    static final Codec<PiItemStackPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("stack").forGetter(PiItemStackPredicate::stack),
            ResourceLocation.CODEC.optionalFieldOf("item").forGetter(PiItemStackPredicate::item),
            ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(PiItemStackPredicate::tag),
            Codec.BOOL.optionalFieldOf("empty").forGetter(PiItemStackPredicate::empty),
            PiDoubleExpression.CODEC.optionalFieldOf("min_count").forGetter(PiItemStackPredicate::minCount),
            PiDoubleExpression.CODEC.optionalFieldOf("max_count").forGetter(PiItemStackPredicate::maxCount),
            PiDoubleExpression.CODEC.optionalFieldOf("min_damage").forGetter(PiItemStackPredicate::minDamage),
            PiDoubleExpression.CODEC.optionalFieldOf("max_damage").forGetter(PiItemStackPredicate::maxDamage),
            PiDoubleExpression.CODEC.optionalFieldOf("max_damage_ratio").forGetter(PiItemStackPredicate::maxDamageRatio)
    ).apply(instance, PiItemStackPredicate::new));

    private final String stack;
    private final Optional<ResourceLocation> item;
    private final Optional<ResourceLocation> tag;
    private final Optional<Boolean> empty;
    private final Optional<PiDoubleExpression> minCount;
    private final Optional<PiDoubleExpression> maxCount;
    private final Optional<PiDoubleExpression> minDamage;
    private final Optional<PiDoubleExpression> maxDamage;
    private final Optional<PiDoubleExpression> maxDamageRatio;

    public PiItemStackPredicate(
            String stack,
            Optional<ResourceLocation> item,
            Optional<ResourceLocation> tag,
            Optional<Boolean> empty,
            Optional<PiDoubleExpression> minCount,
            Optional<PiDoubleExpression> maxCount,
            Optional<PiDoubleExpression> minDamage,
            Optional<PiDoubleExpression> maxDamage,
            Optional<PiDoubleExpression> maxDamageRatio
    ) {
        this.stack = PiEngineActions.checkVariableName(Objects.requireNonNull(stack, "stack"));
        this.item = Objects.requireNonNull(item, "item");
        this.tag = Objects.requireNonNull(tag, "tag");
        this.empty = Objects.requireNonNull(empty, "empty");
        this.minCount = Objects.requireNonNull(minCount, "minCount");
        this.maxCount = Objects.requireNonNull(maxCount, "maxCount");
        this.minDamage = Objects.requireNonNull(minDamage, "minDamage");
        this.maxDamage = Objects.requireNonNull(maxDamage, "maxDamage");
        this.maxDamageRatio = Objects.requireNonNull(maxDamageRatio, "maxDamageRatio");
    }

    public String stack() {
        return stack;
    }

    public Optional<ResourceLocation> item() {
        return item;
    }

    public Optional<ResourceLocation> tag() {
        return tag;
    }

    public Optional<Boolean> empty() {
        return empty;
    }

    public Optional<PiDoubleExpression> minCount() {
        return minCount;
    }

    public Optional<PiDoubleExpression> maxCount() {
        return maxCount;
    }

    public Optional<PiDoubleExpression> minDamage() {
        return minDamage;
    }

    public Optional<PiDoubleExpression> maxDamage() {
        return maxDamage;
    }

    public Optional<PiDoubleExpression> maxDamageRatio() {
        return maxDamageRatio;
    }

    @Override
    public PiEnginePredicateType<?> type() {
        return PiEnginePredicates.ITEM_STACK;
    }

    @Override
    public boolean test(PiEngineContext context) {
        Optional<ItemStack> resolvedStack = context.object(stack, ItemStack.class);
        if (resolvedStack.isEmpty()) {
            return false;
        }
        ItemStack value = resolvedStack.get();
        if (empty.isPresent() && value.isEmpty() != empty.get()) {
            return false;
        }
        if (item.isPresent() && !matchesItem(value, item.get())) {
            return false;
        }
        if (tag.isPresent() && !value.is(TagKey.create(Registries.ITEM, tag.get()))) {
            return false;
        }
        if (minCount.isPresent() && value.getCount() < context.evaluate(minCount.get())) {
            return false;
        }
        if (maxCount.isPresent() && value.getCount() > context.evaluate(maxCount.get())) {
            return false;
        }
        if (minDamage.isPresent() && value.getDamageValue() < context.evaluate(minDamage.get())) {
            return false;
        }
        if (maxDamage.isPresent() && value.getDamageValue() > context.evaluate(maxDamage.get())) {
            return false;
        }
        if (maxDamageRatio.isPresent()) {
            int max = value.getMaxDamage();
            double ratio = max <= 0 ? 0.0D : (double) value.getDamageValue() / (double) max;
            return ratio <= context.evaluate(maxDamageRatio.get());
        }
        return true;
    }

    @Override
    public PiEngineContextContract contextContract() {
        return PiEngineContextContract.builder().object(stack, ItemStack.class).build();
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        PiEngineActions.verifyObject(path + ".stack", context, stack, ItemStack.class);
        minCount.ifPresent(expression -> PiEnginePredicates.verify(path + ".min_count", context, expression));
        maxCount.ifPresent(expression -> PiEnginePredicates.verify(path + ".max_count", context, expression));
        minDamage.ifPresent(expression -> PiEnginePredicates.verify(path + ".min_damage", context, expression));
        maxDamage.ifPresent(expression -> PiEnginePredicates.verify(path + ".max_damage", context, expression));
        maxDamageRatio.ifPresent(expression -> PiEnginePredicates.verify(path + ".max_damage_ratio", context, expression));
    }

    private static boolean matchesItem(ItemStack stack, ResourceLocation id) {
        Optional<Item> expected = BuiltInRegistries.ITEM.getOptional(id);
        return expected.isPresent() && stack.is(expected.get());
    }
}
