package org.pickaid.pidatagraph.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.pickaid.pidatagraph.expression.PiBooleanExpression;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;
import org.pickaid.pidatagraph.expression.PiExpressionLanguage;
import org.pickaid.pidatagraph.expression.PiExpressionScope;
import org.pickaid.pidatagraph.expression.PiIntExpression;

class PiEngineCompositionTest {
    @Test
    void contextCanBeLayeredForOneCalculationWithoutRebuildingTheRoot() {
        PiCompiledFormulaSet spellNumbers = PiEngineFormulaSet.builder()
                .number("damage", PiDoubleExpression.of("base + spellPower * 2 - targetArmor"))
                .integer("cooldown", PiIntExpression.of("baseCooldown - haste"))
                .flag("enabled", PiBooleanExpression.of("mana >= cost"))
                .build()
                .compile(PiExpressionLanguage.standard(),
                        PiExpressionScope.of("base", "spellPower", "targetArmor", "baseCooldown", "haste", "mana", "cost"));
        PiEngineContext playerContext = PiEngineContext.builder()
                .number("spellPower", 4)
                .number("haste", 5)
                .number("mana", 12)
                .object("caster", "player-a")
                .build();

        PiEngineContext castContext = playerContext.derive()
                .number("base", 3)
                .number("targetArmor", 2)
                .number("baseCooldown", 20)
                .number("cost", 8)
                .object("target", "zombie")
                .build();

        PiEngineFrame result = spellNumbers.evaluate(castContext);

        assertEquals(9.0, result.number("damage"), 0.0001);
        assertEquals(15, result.integer("cooldown"));
        assertEquals("player-a", castContext.object("caster", String.class).orElseThrow());
        assertEquals("zombie", castContext.object("target", String.class).orElseThrow());
    }

    @Test
    void engineCanWrapAnotherEngineAndMergeItsFrame() {
        PiCompiledFormulaSet costs = PiEngineFormulaSet.builder()
                .integer("cost.mana", PiIntExpression.of("cost"))
                .flag("cost.can_pay", PiBooleanExpression.of("mana >= cost"))
                .build()
                .compile(PiExpressionLanguage.standard(), PiExpressionScope.of("mana", "cost"));
        PiCompiledFormulaSet spell = PiEngineFormulaSet.builder()
                .number("spell.damage", PiDoubleExpression.of("base + spellPower"))
                .integer("spell.cooldown", PiIntExpression.of("baseCooldown - haste"))
                .build()
                .compile(PiExpressionLanguage.standard(), PiExpressionScope.of("base", "spellPower", "baseCooldown", "haste"));

        PiEnginePlan castPlan = PiEnginePlan.builder()
                .run(costs::evaluate)
                .run(spell::evaluate)
                .build();
        PiEngineFrame frame = castPlan.evaluate(PiEngineContext.builder()
                .number("mana", 12)
                .number("cost", 8)
                .number("base", 3)
                .number("spellPower", 4)
                .number("baseCooldown", 20)
                .number("haste", 5)
                .build());

        assertEquals(8, frame.integer("cost.mana"));
        assertEquals(7.0, frame.number("spell.damage"), 0.0001);
        assertEquals(15, frame.integer("spell.cooldown"));
    }

    @Test
    void routerFlowCanDeriveContextBeforeCallingNestedPlan() {
        PiEnginePlan tooltipPlan = PiEnginePlan.builder()
                .run(PiEngineFormulaSet.builder()
                        .number("tooltip.damage", PiDoubleExpression.of("base + spellPower * tooltipScale"))
                        .build()
                        .compile(PiExpressionLanguage.standard(),
                                PiExpressionScope.of("base", "spellPower", "tooltipScale"))::evaluate)
                .build();
        PiEngineRouter router = PiEngineRouter.builder()
                .route(id("spell_tooltip"), signal -> tooltipPlan.evaluate(signal.context().derive()
                        .number("tooltipScale", 0.5)
                        .build()))
                .build();

        PiEngineFrame frame = router.dispatch(PiEngineSignal.builder(id("spell_tooltip"))
                .number("base", 3)
                .number("spellPower", 8)
                .build());

        assertEquals(7.0, frame.number("tooltip.damage"), 0.0001);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("examplemod", path);
    }
}
