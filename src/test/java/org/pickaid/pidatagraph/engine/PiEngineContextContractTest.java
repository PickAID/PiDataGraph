package org.pickaid.pidatagraph.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.data.PiDataVerificationException;
import org.pickaid.pidatagraph.engine.action.PiEngineAction;
import org.pickaid.pidatagraph.engine.action.PiEngineActionType;
import org.pickaid.pidatagraph.engine.action.PiEngineActions;
import org.pickaid.pidatagraph.engine.action.PiWithContextAction;
import org.pickaid.pidatagraph.engine.action.PiWithNumberAction;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
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

    private static class Target {
    }

    private static final class WrongTarget {
    }

    private static final class DamageSourceToken {
    }
}
