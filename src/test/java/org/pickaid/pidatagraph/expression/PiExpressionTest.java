package org.pickaid.pidatagraph.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class PiExpressionTest {
    @Test
    void evaluatesFormulasWithVariablesComparisonsAndFunctions() {
        PiExpressionLanguage language = PiExpressionLanguage.standard();
        PiExpressionScope scope = PiExpressionScope.builder()
                .variables("base", "level", "crit")
                .build();
        PiCompiledExpression expression = language.compile("max(base, 2) + level * 1.5 + (crit > 0.5) * 10", scope);
        PiExpressionContext context = PiExpressionContext.builder()
                .variable("base", 4)
                .variable("level", 3)
                .variable("crit", 1)
                .build();

        assertEquals(18.5, expression.evaluate(context), 0.0001);
    }

    @Test
    void supportsDeterministicRandomForDatagenAndTests() {
        PiExpressionLanguage language = PiExpressionLanguage.standard();
        PiCompiledExpression expression = language.compile("rand(4, 8)", PiExpressionScope.empty());
        PiExpressionContext context = PiExpressionContext.builder()
                .random(() -> 0.25)
                .build();

        assertEquals(5.0, expression.evaluate(context), 0.0001);
    }

    @Test
    void runtimeContextVariablesUseTheSameNameRulesAsCompileScope() {
        PiCompiledExpression expression = PiExpressionLanguage.standard()
                .compile("base + power", PiExpressionScope.of("base", "power"));

        PiExpressionContext context = PiExpressionContext.builder()
                .variable(" base ", 3)
                .variable("power", 4)
                .build();

        assertEquals(7.0, expression.evaluate(context), 0.0001);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                PiExpressionContext.builder().variable("bad-key", 1));
        assertEquals("invalid expression variable: bad-key", error.getMessage());
    }

    @Test
    void evaluationReportsMissingRuntimeVariablesBeforeCallingExpressionEngine() {
        PiCompiledExpression expression = PiExpressionLanguage.standard()
                .compile("base + power", PiExpressionScope.of("base", "power"));
        PiExpressionContext context = PiExpressionContext.builder()
                .variable("base", 3)
                .build();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                expression.evaluate(context));

        assertEquals("missing expression variable `power` while evaluating `base + power`; available variables: [base]",
                error.getMessage());
    }

    @Test
    void customLanguageCanExtendTheStandardFunctionSet() {
        PiExpressionLanguage language = PiExpressionLanguage.standardBuilder()
                .function("spell_scale", 2, (context, args) -> args[0] * context.variables().get("spellPower") + args[1])
                .build();
        PiCompiledExpression expression = language.compile("spell_scale(2, 3)", PiExpressionScope.of("spellPower"));
        PiExpressionContext context = PiExpressionContext.builder()
                .variable("spellPower", 4)
                .build();

        assertEquals(11.0, expression.evaluate(context), 0.0001);
    }

    @Test
    void languageBuilderRejectsDuplicateFunctionsAndOperators() {
        PiExpressionLanguage.Builder builder = PiExpressionLanguage.builder()
                .function("scale", 1, (context, args) -> args[0]);

        IllegalArgumentException functionError = assertThrows(IllegalArgumentException.class, () ->
                builder.function("scale", 1, (context, args) -> args[0]));
        assertEquals("duplicate expression function: scale", functionError.getMessage());

        PiExpressionLanguage.Builder operatorBuilder = PiExpressionLanguage.builder()
                .operator("%%", 100, false, (left, right) -> left);

        IllegalArgumentException operatorError = assertThrows(IllegalArgumentException.class, () ->
                operatorBuilder.operator("%%", 100, false, (left, right) -> right));
        assertEquals("duplicate expression operator: %%", operatorError.getMessage());
    }

    @Test
    void reportsInvalidExpressionWithAllowedVariables() {
        PiExpressionLanguage language = PiExpressionLanguage.standard();
        PiExpressionCompileException error = assertThrows(PiExpressionCompileException.class, () ->
                language.compile("base + missing", PiExpressionScope.of("base")));

        assertEquals("invalid expression `base + missing` for variables [base]", error.getMessage());
        assertTrue(error.getCause().getMessage().contains("missing"));
    }

    @Test
    void typedExpressionsKeepCodecShapeAsPlainStrings() {
        PiDoubleExpression damage = PiDoubleExpression.of("base + level * 2");
        PiIntExpression cooldown = PiIntExpression.of("20 - haste");
        PiBooleanExpression enabled = PiBooleanExpression.of("mana >= cost & cooldown == 0");

        assertEquals(new JsonPrimitive("base + level * 2"),
                PiDoubleExpression.CODEC.encodeStart(JsonOps.INSTANCE, damage).getOrThrow(false, message -> {
                }));
        assertEquals(new JsonPrimitive("20 - haste"),
                PiIntExpression.CODEC.encodeStart(JsonOps.INSTANCE, cooldown).getOrThrow(false, message -> {
                }));
        assertEquals(new JsonPrimitive("mana >= cost & cooldown == 0"),
                PiBooleanExpression.CODEC.encodeStart(JsonOps.INSTANCE, enabled).getOrThrow(false, message -> {
                }));
    }

    @Test
    void typedExpressionsEvaluateAgainstTheSameContextModel() {
        PiExpressionLanguage language = PiExpressionLanguage.standard();
        PiExpressionScope scope = PiExpressionScope.of("base", "level", "mana", "cost", "cooldown");
        PiExpressionContext context = PiExpressionContext.builder()
                .variable("base", 4)
                .variable("level", 3)
                .variable("mana", 12)
                .variable("cost", 8)
                .variable("cooldown", 0)
                .build();

        assertEquals(10.0, PiDoubleExpression.of("base + level * 2").compile(language, scope).evaluate(context), 0.0001);
        assertEquals(10, PiIntExpression.of("base + level * 2").compile(language, scope).evaluate(context));
        assertTrue(PiBooleanExpression.of("mana >= cost & cooldown == 0").compile(language, scope).evaluate(context));
    }
}
