package org.pickaid.pidatagraph.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.data.PiDataDefinition;
import org.pickaid.pidatagraph.data.PiDataSet;
import org.pickaid.pidatagraph.data.PiDataValidation;
import org.pickaid.pidatagraph.engine.action.PiEmitFlagAction;
import org.pickaid.pidatagraph.engine.action.PiEmitNumberAction;
import org.pickaid.pidatagraph.engine.action.PiEmitObjectAction;
import org.pickaid.pidatagraph.engine.action.PiEngineAction;
import org.pickaid.pidatagraph.engine.action.PiEngineActionData;
import org.pickaid.pidatagraph.engine.action.PiEngineActionRegistry;
import org.pickaid.pidatagraph.engine.action.PiEngineActionType;
import org.pickaid.pidatagraph.engine.context.PiEngineContextBindings;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;
import org.pickaid.pidatagraph.engine.context.PiEngineFlagKey;
import org.pickaid.pidatagraph.engine.context.PiEngineNumberKey;
import org.pickaid.pidatagraph.engine.context.PiEngineValueKey;
import org.pickaid.pidatagraph.engine.predicate.PiEnginePredicate;
import org.pickaid.pidatagraph.engine.predicate.PiEnginePredicateType;
import org.pickaid.pidatagraph.engine.predicate.PiEnginePredicates;
import org.pickaid.pidatagraph.engine.predicate.PiHasNumberPredicate;
import org.pickaid.pidatagraph.engine.predicate.PiItemStackPredicate;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;
import org.pickaid.pidatagraph.expression.PiExpressionScope;

class PiEngineRuntimeUtilityTest {
    private static final PiEngineNumberKey BASE_DAMAGE = PiEngineNumberKey.of("baseDamage");
    private static final PiEngineNumberKey POWER = PiEngineNumberKey.of("power");
    private static final PiEngineNumberKey DAMAGE = PiEngineNumberKey.of("damage");
    private static final PiEngineNumberKey COOLDOWN = PiEngineNumberKey.of("cooldown");
    private static final PiEngineFlagKey ACCEPTED = PiEngineFlagKey.of("accepted");
    private static final PiEngineContextKey<Vec3> IMPACT = PiEngineContextKey.of("impact", Vec3.class);
    private static final PiEngineValueKey<Number> HUD_MANA_FILL = PiEngineValueKey.number("hud.mana_fill");
    private static final PiEngineValueKey<Boolean> HUD_READY = PiEngineValueKey.flag("hud.ready");
    private static final PiEngineValueKey<Vec3> HIT_IMPACT = PiEngineValueKey.object("hit.impact", Vec3.class);

    @Test
    void frameMergeReportsConflictingOutputKeysAndSupportsExplicitReplacement() {
        PiEngineFrame base = PiEngineFrame.builder().number("damage", 4).build();
        PiEngineFrame next = PiEngineFrame.builder().number("damage", 7).build();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> base.merge(next));
        assertEquals("duplicate engine frame value `damage` while merging frames: "
                + Double.class.getName() + " already exists, cannot add " + Double.class.getName(), error.getMessage());
        assertEquals(7.0D, base.mergeReplacing(next).number("damage"), 0.0001D);
    }

    @Test
    void frameKeysUseTheSameNameRulesAsEngineContext() {
        PiEngineFrame frame = PiEngineFrame.builder()
                .number(" damage ", 4)
                .number("hud.mana_fill", 0.5)
                .build();

        assertTrue(frame.hasValue("damage"));
        assertEquals(4.0D, frame.number(" damage "), 0.0001D);
        assertEquals(0.5D, frame.number("hud.mana_fill"), 0.0001D);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                PiEngineFrame.builder().flag("bad-key", true));
        assertEquals("invalid expression variable: bad-key", error.getMessage());
    }

    @Test
    void numberKeysCanBeSharedByContractsContextsAndFrames() {
        PiEngineContext context = PiEngineContext.builder()
                .number(BASE_DAMAGE, 5)
                .number(POWER, 4)
                .build();

        PiEngineContextContract contract = PiEngineContextContract.builder()
                .number(BASE_DAMAGE)
                .number(POWER)
                .build();

        contract.verify(PiDataBuildContext.builder()
                .expressionScope(context.expressionScope())
                .build(), "root");

        PiEngineFrame frame = PiEngineFrame.builder()
                .number(DAMAGE, context.number(BASE_DAMAGE) + context.number(POWER))
                .build();

        assertTrue(frame.hasValue(DAMAGE));
        assertEquals(9.0D, frame.number(DAMAGE), 0.0001D);
    }

    @Test
    void flagKeysCanBeUsedForFrameOutputs() {
        PiEngineFrame frame = PiEngineFrame.builder()
                .flag(ACCEPTED, true)
                .build();

        assertTrue(frame.hasValue(ACCEPTED));
        assertTrue(frame.flag(ACCEPTED));
    }

    @Test
    void valueKeysSupportDottedFrameOutputsWithoutRawStrings() {
        Vec3 impact = new Vec3(1, 2, 3);
        PiEngineFlagKey dottedFlag = PiEngineFlagKey.of("hud.cast_ready");

        PiEngineFrame frame = PiEngineFrame.builder()
                .number(HUD_MANA_FILL, 0.75D)
                .flag(HUD_READY, true)
                .flag(dottedFlag, false)
                .object(HIT_IMPACT, impact)
                .build();

        assertTrue(frame.hasValue(HUD_MANA_FILL));
        assertEquals(0.75D, frame.number(HUD_MANA_FILL), 0.0001D);
        assertTrue(frame.flag(HUD_READY));
        assertFalse(frame.flag(dottedFlag));
        assertSame(impact, frame.object(HIT_IMPACT).orElseThrow());
    }

    @Test
    void frameKeysSupportDefaultValuesForOptionalOutputs() {
        Vec3 fallbackImpact = new Vec3(0, 0, 0);
        PiEngineFrame frame = PiEngineFrame.builder()
                .number(DAMAGE, 9)
                .integer(COOLDOWN, 20)
                .build();

        assertEquals(9.0D, frame.numberOr(DAMAGE, 0), 0.0001D);
        assertEquals(0.25D, frame.numberOr(HUD_MANA_FILL, 0.25D), 0.0001D);
        assertEquals(20, frame.integer(COOLDOWN));
        assertEquals(20, frame.integerOr(COOLDOWN, 0));
        assertFalse(frame.flagOr(ACCEPTED, false));
        assertFalse(frame.flagOr(HUD_READY, false));
        assertSame(fallbackImpact, frame.objectOr(IMPACT, fallbackImpact));
        assertSame(fallbackImpact, frame.objectOr(HIT_IMPACT, fallbackImpact));
    }

    @Test
    void contextNumbersRemainExpressionVariablesWhileFrameValueKeysMayBeDotted() {
        IllegalArgumentException variableError = assertThrows(IllegalArgumentException.class, () ->
                PiEngineNumberKey.of("hud.mana_fill"));
        assertEquals("invalid expression variable: hud.mana_fill", variableError.getMessage());

        IllegalArgumentException frameError = assertThrows(IllegalArgumentException.class, () ->
                PiEngineValueKey.number("hud.bad-key"));
        assertEquals("invalid expression variable: bad-key", frameError.getMessage());
    }

    @Test
    void contextObjectKeysCanBeUsedForFrameObjectOutputs() {
        Vec3 impact = new Vec3(1, 2, 3);

        PiEngineFrame frame = PiEngineFrame.builder()
                .object(IMPACT, impact)
                .build();

        assertTrue(frame.hasValue(IMPACT));
        assertSame(impact, frame.object(IMPACT).orElseThrow());
    }

    @Test
    void minecraftContextBindingsExposeObjectsAndExpressionNumbersTogether() {
        Vec3 impact = new Vec3(3, 4, 0);
        BlockPos blockPos = new BlockPos(7, 8, 9);

        PiEngineContext context = PiEngineContextBindings.blockPos(
                PiEngineContextBindings.vector(PiEngineContext.builder(), "impact", impact),
                "hitBlock",
                blockPos
        ).build();

        assertSame(impact, context.object("impact", Vec3.class).orElseThrow());
        assertSame(blockPos, context.object("hitBlock", BlockPos.class).orElseThrow());
        assertEquals(3.0D, context.number("impactX"), 0.0001D);
        assertEquals(4.0D, context.number("impactY"), 0.0001D);
        assertEquals(5.0D, context.number("impactLength"), 0.0001D);
        assertEquals(7.0D, context.number("hitBlockX"), 0.0001D);
        assertEquals(8.0D, context.number("hitBlockY"), 0.0001D);
        assertEquals(9.0D, context.number("hitBlockZ"), 0.0001D);
    }

    @Test
    void contextStringReadsUseTheSameNameRulesAsContextBuilders() {
        Vec3 actor = new Vec3(1, 0, 0);

        PiEngineContext context = PiEngineContext.builder()
                .number(" baseDamage ", 5)
                .object(" actor ", actor)
                .build();

        assertTrue(context.hasNumber(" baseDamage "));
        assertEquals(5.0D, context.number(" baseDamage "), 0.0001D);
        assertTrue(context.hasObject(" actor "));
        assertSame(actor, context.object(" actor ", Vec3.class).orElseThrow());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                context.hasNumber("bad-key"));
        assertEquals("invalid expression variable: bad-key", error.getMessage());
    }

    @Test
    void emitActionsCanWriteDottedFrameOutputPaths() {
        Vec3 impact = new Vec3(1, 2, 3);
        PiEngineContext context = PiEngineContext.builder()
                .number(BASE_DAMAGE, 6)
                .object(IMPACT, impact)
                .build();

        assertEquals(12.0D, context.execute(new PiEmitNumberAction(
                "hud.mana_fill",
                PiDoubleExpression.of("baseDamage * 2")
        )).number(HUD_MANA_FILL), 0.0001D);

        assertTrue(context.execute(new PiEmitFlagAction(
                "hud.ready",
                new PiHasNumberPredicate("baseDamage")
        )).flag(HUD_READY));

        assertSame(impact, context.execute(new PiEmitObjectAction(
                "hit.impact",
                "impact"
        )).object(HIT_IMPACT).orElseThrow());
    }

    @Test
    void signalBuilderCanUseTheSameContextBindingsAsDirectExecution() {
        PiEngineSignal signal = PiEngineSignal.builder(id("projectile_hit"))
                .configureContext(builder -> PiEngineContextBindings.vector(builder, "impact", new Vec3(1, 2, 2)))
                .number("baseDamage", 5)
                .build();

        assertEquals(id("projectile_hit"), signal.type());
        assertEquals(3.0D, signal.context().number("impactLength"), 0.0001D);
        assertEquals(5.0D, signal.context().number("baseDamage"), 0.0001D);
    }

    @Test
    void itemStackPredicateCoversCommonItemChecksWithoutCustomLeafCode() {
        PiItemStackPredicate predicate = new PiItemStackPredicate(
                "weapon",
                Optional.of(new ResourceLocation("minecraft", "diamond_sword")),
                Optional.empty(),
                Optional.of(false),
                Optional.of(PiDoubleExpression.of("minCount")),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(PiDoubleExpression.of("0.5"))
        );

        assertEquals(JsonParser.parseString("""
                {
                  "type": "pidatagraph:item_stack",
                  "stack": "weapon",
                  "item": "minecraft:diamond_sword",
                  "empty": false,
                  "min_count": "minCount",
                  "max_damage_ratio": "0.5"
                }
                """), PiEnginePredicates.standardCodec()
                .encodeStart(JsonOps.INSTANCE, predicate)
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                }));

        PiEngineContext context = PiEngineContext.builder()
                .number("minCount", 1)
                .build();

        predicate.verify(PiDataBuildContext.builder()
                .expressionScope(context.expressionScope())
                .object("weapon", ItemStack.class)
                .build(), "root");
        assertFalse(predicate.test(context));
    }

    @Test
    void actionDataHelperBuildsVerifiedDataDefinitionsForActionChains() {
        PiDataDefinition<PiEngineAction> definition = PiEngineActionData.definition(
                id("actions"),
                "engine/action",
                PiEngineActionRegistry.standard()
        );
        PiDataSet<PiEngineAction> set = PiDataSet.builder(definition, "examplemod")
                .entry("fire_hit", new PiEmitNumberAction("damage", PiDoubleExpression.of("baseDamage + power")))
                .build();

        PiDataValidation broken = set.verify(PiDataBuildContext.builder()
                .expressionScope(PiExpressionScope.of("baseDamage"))
                .build());

        assertEquals(1, broken.issues().size());
        assertEquals("examplemod:fire_hit/root.value", broken.issues().get(0).path());

        PiEngineLibrary<PiEngineAction> library = PiEngineLibrary.compile(
                set,
                PiEngineBuildContext.standard(),
                PiEngineActionData.contentType(definition, PiExpressionScope.of("baseDamage", "power"))
        );
        PiEngineFrame frame = library.require(new ResourceLocation("examplemod", "fire_hit"))
                .execute(PiEngineContext.builder()
                        .number("baseDamage", 6)
                        .number("power", 3)
                        .build());

        assertEquals(9.0D, frame.number("damage"), 0.0001D);
    }

    @Test
    void actionTypeReportsNullCodecsWithTypeId() {
        PiEngineActionType<PiEngineAction> type = PiEngineActionType.of(id("bad_action"), ignored -> null);

        NullPointerException error = assertThrows(NullPointerException.class, () ->
                type.codec(Codec.unit(new PiEmitNumberAction("damage", PiDoubleExpression.of("1")))));

        assertEquals("engine action type test:bad_action returned null codec", error.getMessage());
    }

    @Test
    void predicateTypeReportsNullCodecsWithTypeId() {
        PiEnginePredicateType<PiEnginePredicate> type = PiEnginePredicateType.of(id("bad_predicate"), ignored -> null);

        NullPointerException error = assertThrows(NullPointerException.class, () ->
                type.codec(PiEnginePredicates.standardCodec()));

        assertEquals("engine predicate type test:bad_predicate returned null codec", error.getMessage());
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("test", path);
    }
}
