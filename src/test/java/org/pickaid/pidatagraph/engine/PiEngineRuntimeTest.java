package org.pickaid.pidatagraph.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.pickaid.pidatagraph.data.PiDataDefinition;
import org.pickaid.pidatagraph.data.PiDataSet;
import org.pickaid.pidatagraph.expression.PiBooleanExpression;
import org.pickaid.pidatagraph.expression.PiCompiledBooleanExpression;
import org.pickaid.pidatagraph.expression.PiCompiledDoubleExpression;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;
import org.pickaid.pidatagraph.expression.PiExpressionLanguage;
import org.pickaid.pidatagraph.expression.PiExpressionScope;
import org.pickaid.pidatagraph.expression.PiIntExpression;

class PiEngineRuntimeTest {
    @Test
    void compiledLibraryFeedsGameplayEventsDirectly() {
        PiDataSet<SpellSpec> data = PiDataSet.builder(spellDefinition(), "examplemod")
                .entry("fireball", new SpellSpec(
                        PiDoubleExpression.of("base + spellPower * 2 - targetArmor"),
                        PiIntExpression.of("20 - haste"),
                        PiBooleanExpression.of("mana >= cost")))
                .build();
        PiEngineContentType<SpellSpec, SpellRuntime> spellType = PiEngineContentType
                .builder(spellDefinition(), PiEngineRuntimeTest::compileSpell)
                .scope(PiExpressionScope.of("base", "spellPower", "targetArmor", "haste", "mana", "cost"))
                .build();

        PiEngineLibrary<SpellRuntime> spells = PiEngineLibrary.compile(data, PiEngineBuildContext.standard(), spellType);
        PiEngineSignal cast = PiEngineSignal.builder(id("spell_cast"))
                .number("base", 3)
                .number("spellPower", 4)
                .number("targetArmor", 2)
                .number("haste", 5)
                .number("mana", 12)
                .number("cost", 8)
                .build();

        SpellRuntime fireball = spells.require(id("fireball"));

        assertEquals(9.0, cast.context().evaluate(fireball.damage()), 0.0001);
        assertEquals(15, cast.context().evaluate(fireball.cooldown()));
        assertTrue(cast.context().evaluate(fireball.enabled()));
    }

    @Test
    void formulaSetCanDriveUiFramesFromTheSameEngineContext() {
        PiEngineFormulaSet hud = PiEngineFormulaSet.builder()
                .number("hud.mana_fill", PiDoubleExpression.of("mana / maxMana"))
                .flag("hud.cast_ready", PiBooleanExpression.of("cooldown == 0 & mana >= cost"))
                .integer("hud.cooldown_ticks", PiIntExpression.of("cooldown"))
                .build();
        PiCompiledFormulaSet runtimeHud = hud.compile(
                PiExpressionLanguage.standard(),
                PiExpressionScope.of("mana", "maxMana", "cooldown", "cost"));
        PiEngineContext context = PiEngineContext.builder()
                .number("mana", 60)
                .number("maxMana", 100)
                .number("cooldown", 12)
                .number("cost", 20)
                .build();

        PiEngineFrame frame = runtimeHud.evaluate(context);

        assertEquals(0.6, frame.number("hud.mana_fill"), 0.0001);
        assertFalse(frame.flag("hud.cast_ready"));
        assertEquals(12, frame.integer("hud.cooldown_ticks"));
    }

    @Test
    void routerLetsEventFlowsReturnTypedFrames() {
        PiCompiledFormulaSet damagePreview = PiEngineFormulaSet.builder()
                .number("preview.damage", PiDoubleExpression.of("base + spellPower"))
                .flag("preview.critical", PiBooleanExpression.of("crit > 0.5"))
                .build()
                .compile(PiExpressionLanguage.standard(), PiExpressionScope.of("base", "spellPower", "crit"));
        PiEngineRouter router = PiEngineRouter.builder()
                .route(id("spell_hovered"), signal -> damagePreview.evaluate(signal.context()))
                .build();

        PiEngineFrame frame = router.dispatch(PiEngineSignal.builder(id("spell_hovered"))
                .number("base", 4)
                .number("spellPower", 7)
                .number("crit", 1)
                .build());

        assertEquals(11.0, frame.number("preview.damage"), 0.0001);
        assertTrue(frame.flag("preview.critical"));
    }

    @Test
    void libraryFailsBeforeRuntimeWhenLoadedDataUsesUnknownVariables() {
        PiDataSet<SpellSpec> data = PiDataSet.builder(spellDefinition(), "examplemod")
                .entry("broken", new SpellSpec(
                        PiDoubleExpression.of("base + unknown"),
                        PiIntExpression.of("20"),
                        PiBooleanExpression.of("1")))
                .build();
        PiEngineContentType<SpellSpec, SpellRuntime> spellType = PiEngineContentType
                .builder(spellDefinition(), PiEngineRuntimeTest::compileSpell)
                .scope(PiExpressionScope.of("base"))
                .build();

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                PiEngineLibrary.compile(data, PiEngineBuildContext.standard(), spellType));

        assertEquals("engine data validation failed: examplemod:broken/damage: invalid expression `base + unknown` for variables [base]",
                error.getMessage());
    }

    @Test
    void libraryReportsEntryIdWhenDomainCompilerFails() {
        PiDataSet<SpellSpec> data = PiDataSet.builder(spellDefinition(), "examplemod")
                .entry("broken", new SpellSpec(PiDoubleExpression.of("1"), PiIntExpression.of("20"), PiBooleanExpression.of("1")))
                .build();
        PiEngineContentType<SpellSpec, SpellRuntime> spellType = PiEngineContentType
                .<SpellSpec, SpellRuntime>builder(spellDefinition(), (entry, context) -> {
                    throw new NullPointerException();
                })
                .build();

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                PiEngineLibrary.compile(data, PiEngineBuildContext.standard(), spellType));

        assertEquals("engine content compile failed: examplemod:broken: " + NullPointerException.class.getName(),
                error.getMessage());
    }

    @Test
    void libraryCanRepresentReloadedDataWithoutDatagen() {
        PiEngineLibrary<SpellRuntime> library = PiEngineLibrary.<SpellRuntime>builder()
                .entry(id("runtime_loaded"), new SpellRuntime(
                        id("runtime_loaded"),
                        PiDoubleExpression.of("power * 3").compile(PiExpressionLanguage.standard(), PiExpressionScope.of("power")),
                        PiIntExpression.of("10").compile(PiExpressionLanguage.standard(), PiExpressionScope.empty()),
                        PiBooleanExpression.of("1").compile(PiExpressionLanguage.standard(), PiExpressionScope.empty())))
                .build();

        Optional<SpellRuntime> spell = library.get(id("runtime_loaded"));

        assertTrue(spell.isPresent());
        assertEquals(12.0, PiEngineContext.builder().number("power", 4).build().evaluate(spell.get().damage()), 0.0001);
    }

    private static SpellRuntime compileSpell(org.pickaid.pidatagraph.data.PiDataEntry<SpellSpec> entry, PiEngineBuildContext context) {
        SpellSpec spec = entry.value();
        return new SpellRuntime(
                entry.id(),
                spec.damage().compile(context.expressionLanguage(), context.expressionScope()),
                spec.cooldown().compile(context.expressionLanguage(), context.expressionScope()),
                spec.enabled().compile(context.expressionLanguage(), context.expressionScope()));
    }

    private static PiDataDefinition<SpellSpec> spellDefinition() {
        return PiDataDefinition.<SpellSpec>builder(id("spell"), "spell", SpellSpec.CODEC)
                .verify("damage", (context, spec) -> spec.damage().compile(context.expressionLanguage(), context.expressionScope()))
                .verify("cooldown", (context, spec) -> spec.cooldown().compile(context.expressionLanguage(), context.expressionScope()))
                .verify("enabled", (context, spec) -> spec.enabled().compile(context.expressionLanguage(), context.expressionScope()))
                .build();
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("examplemod", path);
    }

    private record SpellSpec(PiDoubleExpression damage, PiIntExpression cooldown, PiBooleanExpression enabled) {
        private static final Codec<SpellSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PiDoubleExpression.CODEC.fieldOf("damage").forGetter(SpellSpec::damage),
                PiIntExpression.CODEC.fieldOf("cooldown").forGetter(SpellSpec::cooldown),
                PiBooleanExpression.CODEC.fieldOf("enabled").forGetter(SpellSpec::enabled)
        ).apply(instance, SpellSpec::new));
    }

    private record SpellRuntime(
            ResourceLocation id,
            PiCompiledDoubleExpression damage,
            org.pickaid.pidatagraph.expression.PiCompiledIntExpression cooldown,
            PiCompiledBooleanExpression enabled
    ) {
    }
}
