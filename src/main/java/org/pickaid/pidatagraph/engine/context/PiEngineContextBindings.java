package org.pickaid.pidatagraph.engine.context;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.pickaid.pidatagraph.engine.PiEngineContext;

/**
 * Small helpers for building a stable engine context from common Minecraft
 * runtime objects.
 */
public final class PiEngineContextBindings {
    private PiEngineContextBindings() {
    }

    public static PiEngineContext.Builder level(PiEngineContext.Builder builder, Level level) {
        Objects.requireNonNull(level, "level");
        return Objects.requireNonNull(builder, "builder")
                .object(PiEngineContextKeys.LEVEL, level)
                .number("gameTime", level.getGameTime())
                .number("dayTime", level.getDayTime())
                .number("clientSide", level.isClientSide() ? 1.0D : 0.0D);
    }

    public static PiEngineContext.Builder random(PiEngineContext.Builder builder, RandomSource random) {
        Objects.requireNonNull(random, "random");
        return Objects.requireNonNull(builder, "builder")
                .object(PiEngineContextKeys.RANDOM, random)
                .random(random);
    }

    public static PiEngineContext.Builder actor(PiEngineContext.Builder builder, Entity actor) {
        return entity(builder, PiEngineContextKeys.ACTOR.name(), actor);
    }

    public static PiEngineContext.Builder target(PiEngineContext.Builder builder, Entity target) {
        return entity(builder, PiEngineContextKeys.TARGET.name(), target);
    }

    public static PiEngineContext.Builder livingTarget(PiEngineContext.Builder builder, LivingEntity target) {
        return living(builder, PiEngineContextKeys.LIVING_TARGET.name(), target);
    }

    public static <T extends Entity> PiEngineContext.Builder entity(PiEngineContext.Builder builder, PiEngineContextKey<T> key, T entity) {
        return entity(builder, Objects.requireNonNull(key, "key").name(), entity);
    }

    public static PiEngineContext.Builder entity(PiEngineContext.Builder builder, String key, Entity entity) {
        Objects.requireNonNull(entity, "entity");
        String checkedKey = checkedKey(key);
        builder = Objects.requireNonNull(builder, "builder")
                .object(checkedKey, entity)
                .number(checkedKey + "X", entity.getX())
                .number(checkedKey + "Y", entity.getY())
                .number(checkedKey + "Z", entity.getZ())
                .number(checkedKey + "YRot", entity.getYRot())
                .number(checkedKey + "XRot", entity.getXRot())
                .number(checkedKey + "Tick", entity.tickCount)
                .number(checkedKey + "OnGround", entity.onGround() ? 1.0D : 0.0D);
        return vector(builder, checkedKey + "Delta", entity.getDeltaMovement());
    }

    public static <T extends LivingEntity> PiEngineContext.Builder living(PiEngineContext.Builder builder, PiEngineContextKey<T> key, T entity) {
        return living(builder, Objects.requireNonNull(key, "key").name(), entity);
    }

    public static PiEngineContext.Builder living(PiEngineContext.Builder builder, String key, LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        String checkedKey = checkedKey(key);
        return entity(builder, checkedKey, entity)
                .number(checkedKey + "Health", entity.getHealth())
                .number(checkedKey + "MaxHealth", entity.getMaxHealth())
                .number(checkedKey + "Absorption", entity.getAbsorptionAmount())
                .number(checkedKey + "Armor", entity.getArmorValue());
    }

    public static PiEngineContext.Builder position(PiEngineContext.Builder builder, Vec3 position) {
        return vector(builder, PiEngineContextKeys.POSITION.name(), position);
    }

    public static PiEngineContext.Builder origin(PiEngineContext.Builder builder, Vec3 origin) {
        return vector(builder, PiEngineContextKeys.ORIGIN.name(), origin);
    }

    public static PiEngineContext.Builder direction(PiEngineContext.Builder builder, Vec3 direction) {
        return vector(builder, PiEngineContextKeys.DIRECTION.name(), direction);
    }

    public static PiEngineContext.Builder vector(PiEngineContext.Builder builder, PiEngineContextKey<Vec3> key, Vec3 value) {
        return vector(builder, Objects.requireNonNull(key, "key").name(), value);
    }

    public static PiEngineContext.Builder vector(PiEngineContext.Builder builder, String key, Vec3 value) {
        Objects.requireNonNull(value, "value");
        String checkedKey = checkedKey(key);
        return Objects.requireNonNull(builder, "builder")
                .object(checkedKey, value)
                .number(checkedKey + "X", value.x)
                .number(checkedKey + "Y", value.y)
                .number(checkedKey + "Z", value.z)
                .number(checkedKey + "Length", value.length());
    }

    public static PiEngineContext.Builder blockPos(PiEngineContext.Builder builder, BlockPos pos) {
        return blockPos(builder, PiEngineContextKeys.BLOCK_POS.name(), pos);
    }

    public static PiEngineContext.Builder blockPos(PiEngineContext.Builder builder, PiEngineContextKey<BlockPos> key, BlockPos pos) {
        return blockPos(builder, Objects.requireNonNull(key, "key").name(), pos);
    }

    public static PiEngineContext.Builder blockPos(PiEngineContext.Builder builder, String key, BlockPos pos) {
        Objects.requireNonNull(pos, "pos");
        String checkedKey = checkedKey(key);
        return Objects.requireNonNull(builder, "builder")
                .object(checkedKey, pos)
                .number(checkedKey + "X", pos.getX())
                .number(checkedKey + "Y", pos.getY())
                .number(checkedKey + "Z", pos.getZ());
    }

    public static PiEngineContext.Builder itemStack(PiEngineContext.Builder builder, PiEngineContextKey<ItemStack> key, ItemStack stack) {
        return itemStack(builder, Objects.requireNonNull(key, "key").name(), stack);
    }

    public static PiEngineContext.Builder itemStack(PiEngineContext.Builder builder, String key, ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        String checkedKey = checkedKey(key);
        int maxDamage = stack.getMaxDamage();
        double damageRatio = maxDamage <= 0 ? 0.0D : (double) stack.getDamageValue() / (double) maxDamage;
        return Objects.requireNonNull(builder, "builder")
                .object(checkedKey, stack)
                .number(checkedKey + "Count", stack.getCount())
                .number(checkedKey + "Damage", stack.getDamageValue())
                .number(checkedKey + "MaxDamage", maxDamage)
                .number(checkedKey + "DamageRatio", damageRatio)
                .number(checkedKey + "Empty", stack.isEmpty() ? 1.0D : 0.0D);
    }

    public static PiEngineContext.Builder damageSource(PiEngineContext.Builder builder, DamageSource damageSource) {
        return damageSource(builder, PiEngineContextKeys.DAMAGE_SOURCE, damageSource);
    }

    public static PiEngineContext.Builder damageSource(PiEngineContext.Builder builder, PiEngineContextKey<DamageSource> key, DamageSource damageSource) {
        return Objects.requireNonNull(builder, "builder")
                .object(Objects.requireNonNull(key, "key"), Objects.requireNonNull(damageSource, "damageSource"));
    }

    public static PiEngineContext.Builder hand(PiEngineContext.Builder builder, InteractionHand hand) {
        return hand(builder, PiEngineContextKeys.HAND, hand);
    }

    public static PiEngineContext.Builder hand(PiEngineContext.Builder builder, PiEngineContextKey<InteractionHand> key, InteractionHand hand) {
        return Objects.requireNonNull(builder, "builder")
                .object(Objects.requireNonNull(key, "key"), Objects.requireNonNull(hand, "hand"));
    }

    private static String checkedKey(String key) {
        return PiEngineContextKey.of(key, Object.class).name();
    }
}
