package org.pickaid.pidatagraph.engine.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.action.PiEngineActions;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;

public final class PiItemEnchantmentPredicate implements PiEnginePredicate {
    static final Codec<PiItemEnchantmentPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("stack").forGetter(PiItemEnchantmentPredicate::stack),
            ResourceLocation.CODEC.fieldOf("enchantment").forGetter(PiItemEnchantmentPredicate::enchantment),
            PiDoubleExpression.CODEC.optionalFieldOf("min").forGetter(PiItemEnchantmentPredicate::min),
            PiDoubleExpression.CODEC.optionalFieldOf("max").forGetter(PiItemEnchantmentPredicate::max)
    ).apply(instance, PiItemEnchantmentPredicate::new));

    private final String stack;
    private final ResourceLocation enchantment;
    private final Optional<PiDoubleExpression> min;
    private final Optional<PiDoubleExpression> max;

    public PiItemEnchantmentPredicate(
            String stack,
            ResourceLocation enchantment,
            Optional<PiDoubleExpression> min,
            Optional<PiDoubleExpression> max
    ) {
        this.stack = PiEngineActions.checkVariableName(Objects.requireNonNull(stack, "stack"));
        this.enchantment = Objects.requireNonNull(enchantment, "enchantment");
        this.min = Objects.requireNonNull(min, "min");
        this.max = Objects.requireNonNull(max, "max");
    }

    public String stack() {
        return stack;
    }

    public ResourceLocation enchantment() {
        return enchantment;
    }

    public Optional<PiDoubleExpression> min() {
        return min;
    }

    public Optional<PiDoubleExpression> max() {
        return max;
    }

    @Override
    public PiEnginePredicateType<?> type() {
        return PiEnginePredicates.ITEM_ENCHANTMENT;
    }

    @Override
    public boolean test(PiEngineContext context) {
        Optional<ItemStack> itemStack = context.object(stack, ItemStack.class);
        if (itemStack.isEmpty()) {
            return false;
        }
        Optional<Enchantment> resolved = BuiltInRegistries.ENCHANTMENT.getOptional(enchantment);
        if (resolved.isEmpty()) {
            return false;
        }
        int level = EnchantmentHelper.getItemEnchantmentLevel(resolved.get(), itemStack.get());
        if (level <= 0) {
            return false;
        }
        if (min.isPresent() && level < context.evaluate(min.get())) {
            return false;
        }
        return max.isEmpty() || level <= context.evaluate(max.get());
    }

    @Override
    public PiEngineContextContract contextContract() {
        return PiEngineContextContract.builder().object(stack, ItemStack.class).build();
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        PiEngineActions.verifyObject(path + ".stack", context, stack, ItemStack.class);
        min.ifPresent(expression -> PiEnginePredicates.verify(path + ".min", context, expression));
        max.ifPresent(expression -> PiEnginePredicates.verify(path + ".max", context, expression));
    }
}
