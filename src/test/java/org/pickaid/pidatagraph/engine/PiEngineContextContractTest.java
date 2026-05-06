package org.pickaid.pidatagraph.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.data.PiDataVerificationException;
import org.pickaid.pidatagraph.engine.action.PiEngineAction;
import org.pickaid.pidatagraph.engine.action.PiEngineActionType;
import org.pickaid.pidatagraph.engine.action.PiEngineActions;
import org.pickaid.pidatagraph.engine.action.PiForEachObjectAction;
import org.pickaid.pidatagraph.engine.action.PiWithContextAction;
import org.pickaid.pidatagraph.engine.action.PiWithNumberAction;
import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.engine.context.PiEngineNumberKey;
import org.pickaid.pidatagraph.engine.context.PiEngineValueKey;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;
import org.pickaid.pidatagraph.expression.PiExpressionScope;

class PiEngineContextContractTest {
    @Test
    void leafActionsCanDeclareTheRuntimeObjectsTheyNeed() {
        DamageProbeAction action = new DamageProbeAction(PiDoubleExpression.of("finalDamage"));

        PiDataVerificationException error = assertThrows(PiDataVerificationException.class, () ->
                action.verify(PiDataBuildContext.builder()
                        .expressionScope(PiExpressionScope.of("finalDamage"))
                        .build(), "root"));

        assertEquals("root.context.objects.target", error.path());
        assertEquals("missing engine context object `target` of type " + Target.class.getName(), error.getMessage());
    }

    @Test
    void declaredRuntimeObjectsAreCheckedBeforeExecution() {
        DamageProbeAction action = new DamageProbeAction(PiDoubleExpression.of("finalDamage"));

        action.verify(PiDataBuildContext.builder()
                .expressionScope(PiExpressionScope.of("finalDamage"))
                .object("target", Target.class)
                .object("damageSource", DamageSourceToken.class)
                .build(), "root");
    }

    @Test
    void leafActionsCanDeclareTheRuntimeNumbersTheyNeed() {
        NumberProbeAction action = new NumberProbeAction("finalDamage");

        PiDataVerificationException error = assertThrows(PiDataVerificationException.class, () ->
                action.verify(PiDataBuildContext.builder().build(), "root"));

        assertEquals("root.context.numbers.finalDamage", error.path());
        assertEquals("missing engine context number `finalDamage`", error.getMessage());
    }

    @Test
    void withNumberSatisfiesAChildNumberRequirement() {
        PiEngineAction action = new PiWithNumberAction(
                "finalDamage",
                PiDoubleExpression.of("baseDamage + power"),
                new NumberProbeAction("finalDamage")
        );

        assertEquals(PiEngineContextContract.empty(), action.contextContract());
        action.verify(PiDataBuildContext.builder()
                .expressionScope(PiExpressionScope.of("baseDamage", "power"))
                .build(), "root");
    }

    @Test
    void objectTypeMismatchFailsAtValidationTime() {
        DamageProbeAction action = new DamageProbeAction(PiDoubleExpression.of("finalDamage"));

        PiDataVerificationException error = assertThrows(PiDataVerificationException.class, () ->
                action.verify(PiDataBuildContext.builder()
                        .expressionScope(PiExpressionScope.of("finalDamage"))
                        .object("target", WrongTarget.class)
                        .object("damageSource", DamageSourceToken.class)
                        .build(), "root"));

        assertEquals("root.context.objects.target", error.path());
        assertEquals("engine context object `target` must be " + Target.class.getName()
                + ", but validation context provides " + WrongTarget.class.getName(), error.getMessage());
    }

    @Test
    void withContextTurnsChildObjectRequirementsIntoSourceObjectRequirements() {
        PiEngineAction action = new PiWithContextAction(
                Map.of("finalDamage", PiDoubleExpression.of("baseDamage + power")),
                Map.of("target", "hitEntity"),
                new DamageProbeAction(PiDoubleExpression.of("finalDamage"))
        );

        PiEngineContextContract contract = action.contextContract();

        assertEquals(Map.of(
                "hitEntity", Target.class,
                "damageSource", DamageSourceToken.class
        ), contract.objects());
    }

    @Test
    void withContextValidationUsesTheChildRequiredTypeForObjectAliases() {
        PiEngineAction action = new PiWithContextAction(
                Map.of("finalDamage", PiDoubleExpression.of("baseDamage + power")),
                Map.of("target", "hitEntity"),
                new DamageProbeAction(PiDoubleExpression.of("finalDamage"))
        );

        action.verify(PiDataBuildContext.builder()
                .expressionScope(PiExpressionScope.of("baseDamage", "power"))
                .object("hitEntity", Target.class)
                .object("damageSource", DamageSourceToken.class)
                .build(), "root");
    }

    @Test
    void forEachObjectValidationUsesTheChildRequiredTypeForTheLoopItem() {
        PiEngineAction action = new PiForEachObjectAction(
                "targets",
                "target",
                "",
                new TypedTargetAction()
        );

        action.verify(PiDataBuildContext.builder()
                .object("targets", Iterable.class)
                .build(), "root");
    }

    @Test
    void engineBuildContextCanAbsorbAnActionContractForLoadTimeChecks() {
        PiEngineAction action = new DamageProbeAction(PiDoubleExpression.of("finalDamage"));

        PiEngineBuildContext context = PiEngineBuildContext.standard()
                .withScope(PiExpressionScope.of("baseDamage"))
                .withContextContract(action.contextContract())
                .withObject("target", Target.class);

        assertEquals(PiExpressionScope.of("baseDamage").variables(), context.expressionScope().variables());
        assertEquals(Map.of(
                "target", Target.class,
                "damageSource", DamageSourceToken.class
        ), context.objectTypes());
    }

    @Test
    void engineBuildContextAcceptsTypedKeysForGeneratedGlueValidation() {
        PiEngineNumberKey baseDamage = PiEngineNumberKey.of("baseDamage");
        PiEngineContextKey<Target> target = PiEngineContextKey.of("target", Target.class);

        PiEngineBuildContext context = PiEngineBuildContext.standard()
                .withNumber(baseDamage)
                .withObject(target);

        assertEquals(PiExpressionScope.of("baseDamage").variables(), context.expressionScope().variables());
        assertEquals(Map.of("target", Target.class), context.objectTypes());
    }

    @Test
    void dataBuildContextAcceptsTypedKeysForGeneratedGlueValidation() {
        PiEngineNumberKey baseDamage = PiEngineNumberKey.of("baseDamage");
        PiEngineContextKey<Target> target = PiEngineContextKey.of("target", Target.class);

        PiDataBuildContext context = PiDataBuildContext.builder()
                .number(baseDamage)
                .object(target)
                .build();
        PiDataBuildContext derived = PiDataBuildContext.builder().build()
                .withVariable(baseDamage)
                .withObject(target);

        assertEquals(PiExpressionScope.of("baseDamage").variables(), context.expressionScope().variables());
        assertEquals(Map.of("target", Target.class), context.objectTypes());
        assertEquals(PiExpressionScope.of("baseDamage").variables(), derived.expressionScope().variables());
        assertEquals(Map.of("target", Target.class), derived.objectTypes());
    }

    @Test
    void dataBuildContextCanQueryTypedKeysFromGeneratedGlueValidation() {
        PiEngineNumberKey baseDamage = PiEngineNumberKey.of("baseDamage");
        PiEngineContextKey<Target> target = PiEngineContextKey.of("target", Target.class);

        PiDataBuildContext context = PiDataBuildContext.builder()
                .number(baseDamage)
                .object(target)
                .build();

        assertEquals(true, context.hasVariable(baseDamage));
        assertEquals(true, context.hasObject(target));
        assertEquals(Target.class, context.objectType(target).orElseThrow());
    }

    @Test
    void contextContractCanQueryAndRemoveTypedObjectKeys() {
        PiEngineContextKey<Target> target = PiEngineContextKey.of("target", Target.class);

        PiEngineContextContract contract = PiEngineContextContract.builder()
                .object(target)
                .build();

        assertEquals(Target.class, contract.objectType(target).orElseThrow());
        assertEquals(PiEngineContextContract.empty(), contract.withoutObject(target));
    }

    @Test
    void objectContractsAndKeysRejectPrimitiveTypes() {
        IllegalArgumentException contextKeyError = assertThrows(IllegalArgumentException.class, () ->
                PiEngineContextKey.of("count", int.class));
        assertEquals("engine context object key `count` type must not be primitive: int", contextKeyError.getMessage());

        IllegalArgumentException valueKeyError = assertThrows(IllegalArgumentException.class, () ->
                PiEngineValueKey.object("count", int.class));
        assertEquals("engine frame value key `count` type must not be primitive: int", valueKeyError.getMessage());

        IllegalArgumentException contractError = assertThrows(IllegalArgumentException.class, () ->
                PiEngineContextContract.builder().object("count", int.class));
        assertEquals("engine context object `count` type must not be primitive: int", contractError.getMessage());

        IllegalArgumentException engineBuildError = assertThrows(IllegalArgumentException.class, () ->
                PiEngineBuildContext.standard().withObject("count", int.class));
        assertEquals("engine build context object `count` type must not be primitive: int", engineBuildError.getMessage());

        IllegalArgumentException dataBuildError = assertThrows(IllegalArgumentException.class, () ->
                PiDataBuildContext.builder().object("count", int.class));
        assertEquals("data build context object `count` type must not be primitive: int", dataBuildError.getMessage());
    }

    @Test
    void contextExecuteChecksActionContractBeforeCallingActionLogic() {
        ContractOnlyAction action = new ContractOnlyAction();

        PiEngineContractViolation error = assertThrows(PiEngineContractViolation.class, () ->
                PiEngineContext.builder().build().execute(action));

        assertFalse(action.executed);
        assertEquals("engine context contract failed for action example:contract_only: missing object `target` of type "
                + Target.class.getName()
                + "; available objects: []; available numbers: []", error.getMessage());
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("example", path);
    }

    private record DamageProbeAction(PiDoubleExpression amount) implements PiEngineAction {
        private static final PiEngineActionType<DamageProbeAction> TYPE = PiEngineActionType.of(
                id("contract_damage_probe"),
                ignored -> com.mojang.serialization.Codec.unit(new DamageProbeAction(PiDoubleExpression.of("0")))
        );

        @Override
        public PiEngineActionType<?> type() {
            return TYPE;
        }

        @Override
        public PiEngineFrame execute(PiEngineContext context) {
            return PiEngineFrame.empty();
        }

        @Override
        public PiEngineContextContract contextContract() {
            return PiEngineContextContract.builder()
                    .object("target", Target.class)
                    .object("damageSource", DamageSourceToken.class)
                    .build();
        }

        @Override
        public void verify(PiDataBuildContext context, String path) {
            PiEngineAction.super.verify(context, path);
            PiEngineActions.verify(path + ".amount", context, amount);
        }
    }

    private record NumberProbeAction(String number) implements PiEngineAction {
        private static final PiEngineActionType<NumberProbeAction> TYPE = PiEngineActionType.of(
                id("contract_number_probe"),
                ignored -> com.mojang.serialization.Codec.unit(new NumberProbeAction("value"))
        );

        @Override
        public PiEngineActionType<?> type() {
            return TYPE;
        }

        @Override
        public PiEngineFrame execute(PiEngineContext context) {
            return PiEngineFrame.builder().number("observed", context.number(number)).build();
        }

        @Override
        public PiEngineContextContract contextContract() {
            return PiEngineContextContract.builder().number(number).build();
        }
    }

    private record TypedTargetAction() implements PiEngineAction {
        private static final PiEngineActionType<TypedTargetAction> TYPE = PiEngineActionType.of(
                id("typed_target"),
                ignored -> com.mojang.serialization.Codec.unit(new TypedTargetAction())
        );

        @Override
        public PiEngineActionType<?> type() {
            return TYPE;
        }

        @Override
        public PiEngineFrame execute(PiEngineContext context) {
            return PiEngineFrame.empty();
        }

        @Override
        public PiEngineContextContract contextContract() {
            return PiEngineContextContract.builder().object("target", Target.class).build();
        }
    }

    private static final class ContractOnlyAction implements PiEngineAction {
        private static final PiEngineActionType<ContractOnlyAction> TYPE = PiEngineActionType.of(
                id("contract_only"),
                ignored -> com.mojang.serialization.Codec.unit(new ContractOnlyAction())
        );
        private boolean executed;

        @Override
        public PiEngineActionType<?> type() {
            return TYPE;
        }

        @Override
        public PiEngineFrame execute(PiEngineContext context) {
            executed = true;
            return PiEngineFrame.empty();
        }

        @Override
        public PiEngineContextContract contextContract() {
            return PiEngineContextContract.builder().object("target", Target.class).build();
        }
    }

    private static class Target {
    }

    private static final class WrongTarget {
    }

    private static final class DamageSourceToken {
    }
}
