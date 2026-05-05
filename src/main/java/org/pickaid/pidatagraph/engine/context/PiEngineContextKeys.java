package org.pickaid.pidatagraph.engine.context;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class PiEngineContextKeys {
    public static final PiEngineContextKey<Entity> ACTOR = PiEngineContextKey.of("actor", Entity.class);
    public static final PiEngineContextKey<Player> PLAYER = PiEngineContextKey.of("player", Player.class);
    public static final PiEngineContextKey<ServerPlayer> SERVER_PLAYER = PiEngineContextKey.of("serverPlayer", ServerPlayer.class);
    public static final PiEngineContextKey<Entity> SOURCE = PiEngineContextKey.of("source", Entity.class);
    public static final PiEngineContextKey<Entity> TARGET = PiEngineContextKey.of("target", Entity.class);
    public static final PiEngineContextKey<LivingEntity> LIVING_TARGET = PiEngineContextKey.of("livingTarget", LivingEntity.class);
    public static final PiEngineContextKey<Level> LEVEL = PiEngineContextKey.of("level", Level.class);
    public static final PiEngineContextKey<Vec3> ORIGIN = PiEngineContextKey.of("origin", Vec3.class);
    public static final PiEngineContextKey<Vec3> POSITION = PiEngineContextKey.of("position", Vec3.class);
    public static final PiEngineContextKey<Vec3> DIRECTION = PiEngineContextKey.of("direction", Vec3.class);
    public static final PiEngineContextKey<BlockPos> BLOCK_POS = PiEngineContextKey.of("blockPos", BlockPos.class);
    public static final PiEngineContextKey<DamageSource> DAMAGE_SOURCE = PiEngineContextKey.of("damageSource", DamageSource.class);
    public static final PiEngineContextKey<ItemStack> ITEM_STACK = PiEngineContextKey.of("itemStack", ItemStack.class);
    public static final PiEngineContextKey<InteractionHand> HAND = PiEngineContextKey.of("hand", InteractionHand.class);
    public static final PiEngineContextKey<RandomSource> RANDOM = PiEngineContextKey.of("random", RandomSource.class);

    private PiEngineContextKeys() {
    }
}
