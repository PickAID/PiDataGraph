package org.pickaid.pidatagraph.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.action.PiEngineAction;
import org.pickaid.pidatagraph.engine.action.PiEngineActionRegistry;
import org.pickaid.pidatagraph.engine.action.PiEngineActionType;
import org.pickaid.pidatagraph.engine.action.PiEngineActions;
import org.pickaid.pidatagraph.engine.action.PiEmitFlagAction;
import org.pickaid.pidatagraph.engine.action.PiEmitNumberAction;
import org.pickaid.pidatagraph.engine.action.PiEmitObjectAction;
import org.pickaid.pidatagraph.engine.action.PiEmitRandomNumberAction;
import org.pickaid.pidatagraph.engine.action.PiFailAction;
import org.pickaid.pidatagraph.engine.action.PiForEachObjectAction;
import org.pickaid.pidatagraph.engine.action.PiGuardAction;
import org.pickaid.pidatagraph.engine.action.PiNoopAction;
import org.pickaid.pidatagraph.engine.action.PiSelectObjectAction;
import org.pickaid.pidatagraph.engine.action.PiSequenceAction;
import org.pickaid.pidatagraph.engine.action.PiWithContextAction;
import org.pickaid.pidatagraph.engine.action.PiWithNumberAction;
import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.engine.context.PiEngineNumberKey;
import org.pickaid.pidatagraph.engine.context.PiEngineValueKey;
import org.pickaid.pidatagraph.engine.predicate.PiAllPredicate;
import org.pickaid.pidatagraph.engine.predicate.PiBiomePredicate;
import org.pickaid.pidatagraph.engine.predicate.PiBlockStatePredicate;
import org.pickaid.pidatagraph.engine.predicate.PiEntityTypePredicate;
import org.pickaid.pidatagraph.engine.predicate.PiExpressionPredicate;
import org.pickaid.pidatagraph.engine.predicate.PiHasObjectPredicate;
import org.pickaid.pidatagraph.engine.predicate.PiHasNumberPredicate;
import org.pickaid.pidatagraph.engine.predicate.PiItemEnchantmentPredicate;
import org.pickaid.pidatagraph.engine.predicate.PiLevelDimensionPredicate;
import org.pickaid.pidatagraph.engine.predicate.PiNotPredicate;
import org.pickaid.pidatagraph.engine.predicate.PiChancePredicate;
import org.pickaid.pidatagraph.engine.predicate.PiEnginePredicate;
import org.pickaid.pidatagraph.engine.predicate.PiEnginePredicateType;
import org.pickaid.pidatagraph.engine.predicate.PiEnginePredicates;
import org.pickaid.pidatagraph.engine.predicate.PiNumberRangePredicate;
import org.pickaid.pidatagraph.engine.predicate.PiObjectEqualsPredicate;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;
import org.pickaid.pidatagraph.expression.PiExpressionScope;
import org.pickaid.pidatagraph.expression.PiIntExpression;

class PiEnginePredicateActionTest {
    @Test
    void coreActionsAndPredicatesCoverQuestScaleConditionAndRewardBreadth() {
        List<ResourceLocation> actionIds = PiEngineActions.core().stream()
                .map(PiEngineActionType::id)
                .toList();
        List<ResourceLocation> predicateIds = PiEnginePredicates.core().stream()
                .map(PiEnginePredicateType::id)
                .toList();

        assertTrue(actionIds.size() >= 13, "core actions should cover at least reward-scale breadth");
        assertTrue(predicateIds.size() >= 14, "core predicates should cover at least task-scale breadth");
        assertTrue(actionIds.contains(new ResourceLocation("pidatagraph", "emit_random_number")));
        assertTrue(actionIds.contains(new ResourceLocation("pidatagraph", "select_object")));
        assertTrue(predicateIds.contains(new ResourceLocation("pidatagraph", "entity_type")));
        assertTrue(predicateIds.contains(new ResourceLocation("pidatagraph", "level_dimension")));
        assertTrue(predicateIds.contains(new ResourceLocation("pidatagraph", "biome")));
    }

    @Test
    void predicateDrivenActionChainCanGateAndIterateRuntimeTargets() {
        PiEngineActionRegistry registry = PiEngineActionRegistry.builder()
                .install(PiEngineActions.core())
                .installPredicates(PiEnginePredicates.core())
                .add(TargetProbeAction.TYPE)
                .build();
        PiEngineAction action = new PiGuardAction(
                new PiAllPredicate(List.of(
                        PiExpressionPredicate.of("mana >= cost"),
                        new PiHasObjectPredicate("targets")
                )),
                new PiForEachObjectAction(
                        "targets",
                        "target",
                        "targetIndex",
                        new TargetProbeAction(PiDoubleExpression.of("base + targetIndex"))
                )
        );

        JsonElement encoded = registry.codec()
                .encodeStart(JsonOps.INSTANCE, action)
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                });

        assertEquals(JsonParser.parseString("""
                {
                  "type": "pidatagraph:guard",
                  "predicate": {
                    "type": "pidatagraph:all",
                    "predicates": [
                      "mana >= cost",
                      {
                        "type": "pidatagraph:has_object",
                        "key": "targets"
                      }
                    ]
                  },
                  "child": {
                    "type": "pidatagraph:for_each_object",
                    "list": "targets",
                    "item": "target",
                    "index": "targetIndex",
                    "child": {
                      "type": "example:target_probe",
                      "amount": "base + targetIndex"
                    }
                  }
                }
                """), encoded);

        TargetLog log = new TargetLog();
        action.execute(PiEngineContext.builder()
                .number("mana", 10)
                .number("cost", 4)
                .number("base", 2)
                .object("targets", List.of("zombie", "skeleton"))
                .object("targetLog", log)
                .build());

        assertEquals(List.of(
                new TargetHit("zombie", 0, 2.0),
                new TargetHit("skeleton", 1, 3.0)
        ), log.hits());
    }

    @Test
    void guardActionStopsTheChainWhenPredicateFails() {
        TargetLog log = new TargetLog();
        PiEngineAction action = new PiGuardAction(
                new PiAllPredicate(List.of(
                        PiExpressionPredicate.of("mana >= cost"),
                        new PiHasObjectPredicate("targets")
                )),
                new PiForEachObjectAction(
                        "targets",
                        "target",
                        "targetIndex",
                        new TargetProbeAction(PiDoubleExpression.of("base"))
                )
        );

        action.execute(PiEngineContext.builder()
                .number("mana", 1)
                .number("cost", 4)
                .number("base", 2)
                .object("targets", List.of("zombie"))
                .object("targetLog", log)
                .build());

        assertTrue(log.hits().isEmpty());
    }

    @Test
    void predicateVerificationIncludesVariablesIntroducedByForEach() {
        PiEngineAction action = new PiForEachObjectAction(
                "targets",
                "target",
                "targetIndex",
                new TargetProbeAction(PiDoubleExpression.of("base + targetIndex"))
        );

        action.verify(PiDataBuildContext.builder()
                .expressionScope(PiExpressionScope.of("base"))
                .object("targets", Iterable.class)
                .build(), "root");
    }

    @Test
    void typedContextKeysKeepCommonSlotsExplicitWithoutForcingSpellNames() {
        PiEngineContextKey<String> actor = PiEngineContextKey.of("actor", String.class);
        PiEngineContextKey<String> target = PiEngineContextKey.of("target", String.class);
        PiEngineContext context = PiEngineContext.builder()
                .object(actor, "player-a")
                .object(target, "zombie")
                .build();

        assertEquals("player-a", context.object(actor).orElseThrow());
        assertEquals("zombie", context.object(target).orElseThrow());
        assertTrue(context.hasObject(actor));
    }

    @Test
    void corePredicatesAndContextActionsAcceptTypedJavaKeys() {
        PiEngineNumberKey base = PiEngineNumberKey.of("base");
        PiEngineNumberKey scaled = PiEngineNumberKey.of("scaled");
        PiEngineNumberKey targetIndex = PiEngineNumberKey.of("targetIndex");
        PiEngineContextKey<List> targets = PiEngineContextKey.of("targets", List.class);
        PiEngineContextKey<String> target = PiEngineContextKey.of("target", String.class);
        PiEngineValueKey<Number> selectedIndex = PiEngineValueKey.number("selected.index");

        PiEngineAction action = new PiForEachObjectAction(
                targets,
                target,
                targetIndex,
                new PiWithNumberAction(
                        scaled,
                        PiDoubleExpression.of("base + targetIndex"),
                        new PiGuardAction(
                                new PiAllPredicate(List.of(
                                        new PiHasNumberPredicate(scaled),
                                        new PiHasObjectPredicate(target),
                                        new PiNumberRangePredicate(
                                                scaled,
                                                java.util.Optional.of(PiDoubleExpression.of("3")),
                                                java.util.Optional.of(PiDoubleExpression.of("3")))
                                )),
                                new PiEmitNumberAction(selectedIndex, PiDoubleExpression.of("scaled")))));

        PiEngineFrame frame = action.execute(PiEngineContext.builder()
                .number(base, 2)
                .object(targets, List.of("zombie", "skeleton"))
                .build());

        assertEquals(3.0, frame.number(selectedIndex), 0.0001);
    }

    @Test
    void genericRewardStyleActionsEncodeAndRunWithDeterministicRandomAndSelection() {
        PiEngineContextKey<List> rewardPool = PiEngineContextKey.of("rewardPool", List.class);
        PiEngineValueKey<Number> rolledValue = PiEngineValueKey.number("rolled.value");
        PiEngineValueKey<Object> selectedReward = PiEngineValueKey.object("selected.reward", Object.class);
        PiEngineAction action = new PiSequenceAction(List.of(
                new PiNoopAction(),
                new PiEmitRandomNumberAction(
                        rolledValue,
                        PiDoubleExpression.of("minReward"),
                        PiDoubleExpression.of("maxReward")),
                new PiSelectObjectAction(selectedReward, rewardPool, PiIntExpression.of("selectedIndex"))));

        JsonElement encoded = PiEngineActionRegistry.standard().codec()
                .encodeStart(JsonOps.INSTANCE, action)
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                });

        assertEquals(JsonParser.parseString("""
                {
                  "type": "pidatagraph:sequence",
                  "children": [
                    {
                      "type": "pidatagraph:noop"
                    },
                    {
                      "type": "pidatagraph:emit_random_number",
                      "name": "rolled.value",
                      "min": "minReward",
                      "max": "maxReward"
                    },
                    {
                      "type": "pidatagraph:select_object",
                      "name": "selected.reward",
                      "list": "rewardPool",
                      "index": "selectedIndex"
                    }
                  ]
                }
                """), encoded);

        PiEngineFrame frame = action.execute(PiEngineContext.builder()
                .number("minReward", 10)
                .number("maxReward", 18)
                .number("selectedIndex", 1)
                .random(() -> 0.25)
                .object(rewardPool, List.of("diamond", "emerald", "netherite"))
                .build());

        assertEquals(12.0, frame.number(rolledValue), 0.0001);
        assertEquals("emerald", frame.object(selectedReward).orElseThrow());
    }

    @Test
    void failActionCanStopInvalidDataDrivenBranchesWithAReadableMessage() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                new PiFailAction("missing required target").execute(PiEngineContext.builder().build()));

        assertEquals("engine fail action: missing required target", error.getMessage());
    }

    @Test
    void objectEqualsPredicateSupportsTypedKeysAndIdentityMode() {
        PiEngineContextKey<String> left = PiEngineContextKey.of("leftTarget", String.class);
        PiEngineContextKey<String> right = PiEngineContextKey.of("rightTarget", String.class);
        PiObjectEqualsPredicate equals = new PiObjectEqualsPredicate(left, right, false);
        PiObjectEqualsPredicate identity = new PiObjectEqualsPredicate(left, right, true);
        PiEngineContext context = PiEngineContext.builder()
                .object(left, new String("zombie"))
                .object(right, new String("zombie"))
                .build();

        assertTrue(equals.test(context));
        assertFalse(identity.test(context));
    }

    @Test
    void minecraftPredicatesExposeTypedConstructorsAndCodecShapes() {
        PiEngineContextKey<net.minecraft.world.entity.Entity> entity = PiEngineContextKey.of("target", net.minecraft.world.entity.Entity.class);
        PiEngineContextKey<net.minecraft.world.level.Level> level = PiEngineContextKey.of("level", net.minecraft.world.level.Level.class);
        PiEngineContextKey<net.minecraft.core.BlockPos> pos = PiEngineContextKey.of("pos", net.minecraft.core.BlockPos.class);

        List<PiEnginePredicate> predicates = List.of(
                new PiEntityTypePredicate(entity, java.util.Optional.of(new ResourceLocation("minecraft", "zombie")), java.util.Optional.empty()),
                new PiLevelDimensionPredicate(level, new ResourceLocation("minecraft", "overworld")),
                new PiBlockStatePredicate(level, pos, java.util.Optional.of(new ResourceLocation("minecraft", "stone")), java.util.Optional.empty()),
                new PiBiomePredicate(level, pos, java.util.Optional.of(new ResourceLocation("minecraft", "plains")), java.util.Optional.empty()));

        JsonElement encoded = Codec.list(PiEnginePredicates.standardCodec())
                .encodeStart(JsonOps.INSTANCE, predicates)
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                });

        assertEquals(JsonParser.parseString("""
                [
                  {
                    "type": "pidatagraph:entity_type",
                    "entity": "target",
                    "entity_type": "minecraft:zombie"
                  },
                  {
                    "type": "pidatagraph:level_dimension",
                    "level": "level",
                    "dimension": "minecraft:overworld"
                  },
                  {
                    "type": "pidatagraph:block_state",
                    "level": "level",
                    "pos": "pos",
                    "block": "minecraft:stone"
                  },
                  {
                    "type": "pidatagraph:biome",
                    "level": "level",
                    "pos": "pos",
                    "biome": "minecraft:plains"
                  }
                ]
                """), encoded);
    }

    @Test
    void predicatesSupportNegationAndDeterministicChance() {
        PiEngineContext context = PiEngineContext.builder()
                .number("mana", 10)
                .random(() -> 0.25)
                .build();

        assertTrue(new PiNotPredicate(PiExpressionPredicate.of("mana < 5")).test(context));
        assertTrue(new PiChancePredicate(PiDoubleExpression.of("0.5")).test(context));
        assertFalse(new PiChancePredicate(PiDoubleExpression.of("0.1")).test(context));
    }

    @Test
    void contextActionBuildsAGenericMinecraftStyleActionChainWithoutSpellSpecificSlots() {
        PiEngineActionRegistry registry = PiEngineActionRegistry.standard();
        PiEngineAction action = new PiWithContextAction(
                java.util.Map.of(
                        "finalDamage", PiDoubleExpression.of("baseDamage + enchantmentLevel * 0.5"),
                        "resourceAfter", PiDoubleExpression.of("resource - cost")
                ),
                java.util.Map.of("target", "hitEntity"),
                new PiGuardAction(
                        new PiAllPredicate(List.of(
                                new PiHasNumberPredicate("finalDamage"),
                                new PiNumberRangePredicate("enchantmentLevel", java.util.Optional.of(PiDoubleExpression.of("1")), java.util.Optional.empty()),
                                PiExpressionPredicate.of("resourceAfter >= 0")
                        )),
                        new PiSequenceAction(List.of(
                                new PiEmitNumberAction("damage", PiDoubleExpression.of("finalDamage")),
                                new PiEmitNumberAction("resourceLeft", PiDoubleExpression.of("resourceAfter")),
                                new PiEmitObjectAction("selectedTarget", "target"),
                                new PiEmitFlagAction("accepted", PiExpressionPredicate.of("finalDamage > 0"))
                        ))
                )
        );

        JsonElement encoded = registry.codec()
                .encodeStart(JsonOps.INSTANCE, action)
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                });

        assertEquals(JsonParser.parseString("""
                {
                  "type": "pidatagraph:with_context",
                  "numbers": {
                    "finalDamage": "baseDamage + enchantmentLevel * 0.5",
                    "resourceAfter": "resource - cost"
                  },
                  "objects": {
                    "target": "hitEntity"
                  },
                  "child": {
                    "type": "pidatagraph:guard",
                    "predicate": {
                      "type": "pidatagraph:all",
                      "predicates": [
                        {
                          "type": "pidatagraph:has_number",
                          "key": "finalDamage"
                        },
                        {
                          "type": "pidatagraph:number_range",
                          "key": "enchantmentLevel",
                          "min": "1"
                        },
                        "resourceAfter >= 0"
                      ]
                    },
                    "child": {
                      "type": "pidatagraph:sequence",
                      "children": [
                        {
                          "type": "pidatagraph:emit_number",
                          "name": "damage",
                          "value": "finalDamage"
                        },
                        {
                          "type": "pidatagraph:emit_number",
                          "name": "resourceLeft",
                          "value": "resourceAfter"
                        },
                        {
                          "type": "pidatagraph:emit_object",
                          "name": "selectedTarget",
                          "source": "target"
                        },
                        {
                          "type": "pidatagraph:emit_flag",
                          "name": "accepted",
                          "predicate": "finalDamage > 0"
                        }
                      ]
                    }
                  }
                }
                """), encoded);

        PiEngineFrame frame = action.execute(PiEngineContext.builder()
                .number("baseDamage", 8)
                .number("enchantmentLevel", 2)
                .number("resource", 20)
                .number("cost", 7)
                .object("hitEntity", "zombie")
                .build());

        assertEquals(9.0, frame.number("damage"), 0.0001);
        assertEquals(13.0, frame.number("resourceLeft"), 0.0001);
        assertEquals("zombie", frame.object("selectedTarget", String.class).orElseThrow());
        assertTrue(frame.flag("accepted"));
    }

    @Test
    void contextActionVerificationExposesDerivedNumbersToChildren() {
        PiEngineAction action = new PiWithContextAction(
                java.util.Map.of("distanceScale", PiDoubleExpression.of("distance / maxDistance")),
                java.util.Map.of(),
                new PiEmitFlagAction("inRange", PiExpressionPredicate.of("distanceScale <= 1"))
        );

        action.verify(PiDataBuildContext.builder()
                .expressionScope(PiExpressionScope.of("distance", "maxDistance"))
                .build(), "root");
    }

    @Test
    void emitObjectActionReportsMissingSourceObjectWithType() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                new PiEmitObjectAction("selectedTarget", "target")
                        .execute(PiEngineContext.builder().build()));

        assertEquals("missing engine context object `target` of type " + Object.class.getName(), error.getMessage());
    }

    @Test
    void forEachObjectActionReportsMissingIterableObjectWithType() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                new PiForEachObjectAction("targets", "target", "", new PiEmitObjectAction("selectedTarget", "target"))
                        .execute(PiEngineContext.builder().build()));

        assertEquals("missing engine context object `targets` of type " + Iterable.class.getName(), error.getMessage());
    }

    @Test
    void forEachObjectActionReportsNullElementsWithListAndIndex() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                new PiForEachObjectAction("targets", "target", "targetIndex", new PiEmitObjectAction("selectedTarget", "target"))
                        .execute(PiEngineContext.builder()
                                .object("targets", java.util.Arrays.asList("zombie", null))
                                .build()));

        assertEquals("engine context object list `targets` contains null for `target` at index 1", error.getMessage());
    }

    @Test
    void numberPredicatesCanGateCooldownResourceDistanceOrEnchantmentVariables() {
        PiEngineContext context = PiEngineContext.builder()
                .number("cooldown", 0)
                .number("resource", 12)
                .number("distance", 5)
                .number("range", 8)
                .build();

        assertTrue(new PiHasNumberPredicate("cooldown").test(context));
        assertTrue(new PiNumberRangePredicate("resource", java.util.Optional.of(PiDoubleExpression.of("8")), java.util.Optional.empty()).test(context));
        assertTrue(new PiNumberRangePredicate("distance", java.util.Optional.empty(), java.util.Optional.of(PiDoubleExpression.of("range"))).test(context));
        assertFalse(new PiHasNumberPredicate("missing").test(context));
        assertFalse(new PiNumberRangePredicate("distance", java.util.Optional.empty(), java.util.Optional.of(PiDoubleExpression.of("4"))).test(context));
    }

    @Test
    void itemEnchantmentPredicateIsADataDrivenVanillaItemStackCondition() {
        PiItemEnchantmentPredicate predicate = new PiItemEnchantmentPredicate(
                "weapon",
                new ResourceLocation("minecraft", "sharpness"),
                java.util.Optional.of(PiDoubleExpression.of("requiredLevel")),
                java.util.Optional.of(PiDoubleExpression.of("4"))
        );

        JsonElement encoded = PiEnginePredicates.standardCodec()
                .encodeStart(JsonOps.INSTANCE, predicate)
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                });

        assertEquals(JsonParser.parseString("""
                {
                  "type": "pidatagraph:item_enchantment",
                  "stack": "weapon",
                  "enchantment": "minecraft:sharpness",
                  "min": "requiredLevel",
                  "max": "4"
                }
                """), encoded);

        PiEngineContext context = PiEngineContext.builder()
                .number("requiredLevel", 2)
                .build();

        assertFalse(predicate.test(context));
    }

    @Test
    void defaultPredicateVerificationReportsNullContractWithPath() {
        NullPointerException error = assertThrows(NullPointerException.class, () ->
                new NullContractPredicate().verify(PiDataBuildContext.builder().build(), "spell.condition"));

        assertEquals("engine predicate returned null context contract at spell.condition", error.getMessage());
    }

    @Test
    void compositePredicateContractReportsNullChildContractWithParentSlot() {
        NullPointerException error = assertThrows(NullPointerException.class, () ->
                new PiAllPredicate(List.of(new NullContractPredicate())).contextContract());

        assertEquals("all predicate[0] predicate example:null_contract_predicate returned null context contract", error.getMessage());
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("example", path);
    }

    private record NullContractPredicate() implements PiEnginePredicate {
        private static final PiEnginePredicateType<NullContractPredicate> TYPE = PiEnginePredicateType.of(
                id("null_contract_predicate"),
                ignored -> Codec.unit(new NullContractPredicate())
        );

        @Override
        public PiEnginePredicateType<?> type() {
            return TYPE;
        }

        @Override
        public boolean test(PiEngineContext context) {
            return true;
        }

        @Override
        public PiEngineContextContract contextContract() {
            return null;
        }
    }

    private record TargetProbeAction(PiDoubleExpression amount) implements PiEngineAction {
        private static final PiEngineActionType<TargetProbeAction> TYPE = PiEngineActionType.of(
                id("target_probe"),
                actionCodec -> RecordCodecBuilder.create(instance -> instance.group(
                        PiDoubleExpression.CODEC.fieldOf("amount").forGetter(TargetProbeAction::amount)
                ).apply(instance, TargetProbeAction::new))
        );

        @Override
        public PiEngineActionType<?> type() {
            return TYPE;
        }

        @Override
        public PiEngineFrame execute(PiEngineContext context) {
            context.object("targetLog", TargetLog.class)
                    .orElseThrow()
                    .add(
                            context.object("target", String.class).orElseThrow(),
                            (int) context.number("targetIndex"),
                            context.evaluate(amount)
                    );
            return PiEngineFrame.empty();
        }

        @Override
        public void verify(PiDataBuildContext context, String path) {
            PiEngineActions.verify(path + ".amount", context, amount);
        }
    }

    private record TargetHit(String target, int index, double amount) {
    }

    private static final class TargetLog {
        private final List<TargetHit> hits = new ArrayList<>();

        private void add(String target, int index, double amount) {
            hits.add(new TargetHit(target, index, amount));
        }

        private List<TargetHit> hits() {
            return List.copyOf(hits);
        }
    }
}
