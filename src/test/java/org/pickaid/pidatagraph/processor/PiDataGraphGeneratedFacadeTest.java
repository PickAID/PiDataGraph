package org.pickaid.pidatagraph.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PiDataGraphGeneratedFacadeTest {
    @TempDir
    Path tempDir;
    private PiDataGraphProcessorCompiler compiler;

    @BeforeEach
    void setUpCompiler() {
        compiler = new PiDataGraphProcessorCompiler(tempDir);
    }

    @Test
    void generatedGlueProvidesADataSetBuilderForDatagenAndFacadeCode() throws Exception {
        Path generated = tempDir.resolve("generated-set-builder");
        CompilationResult result = compiler.compileGenerating(PiDataGraphProcessorCompiler.source("""
                import com.mojang.serialization.Codec;
                import java.math.BigDecimal;
                import net.minecraft.core.RegistryAccess;
                import net.minecraft.resources.ResourceKey;
                import net.minecraft.resources.ResourceLocation;
                import org.pickaid.pidatagraph.data.PiDataSet;
                import org.pickaid.pidatagraph.engine.PiEngineContext;
                import org.pickaid.pidatagraph.engine.PiEngineFrame;
                import org.pickaid.pidatagraph.engine.action.PiEngineAction;
                import org.pickaid.pidatagraph.engine.action.PiEngineActionType;
                import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;

                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action", folder = "skills/hit")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.builder()
                            .add(DamageAction.TYPE)
                            .build();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(double base, BigDecimal power, Target target) {
                }

                @PiGraphOutput(registry = "HIT_ACTIONS", name = "hit")
                record HitOutput(@PiOutput("damage.final") double damage, boolean accepted, Target lastTarget) {
                }

                record Target(String name) {
                }

                record HitResult(double damage, boolean accepted, Target target) {
                }

                final class DamageAction implements PiEngineAction {
                    static final PiEngineActionType<DamageAction> TYPE = PiEngineActionType.of(
                            new ResourceLocation("examplemod", "damage"),
                            actionCodec -> Codec.unit(new DamageAction()));

                    @Override
                    public PiEngineActionType<?> type() {
                        return TYPE;
                    }

                    @Override
                    public PiEngineContextContract contextContract() {
                        return PiEngineContextContract.builder()
                                .number(ExampleGraphs_PiDataGraph.HIT_BASE)
                                .number(ExampleGraphs_PiDataGraph.HIT_POWER)
                                .object(ExampleGraphs_PiDataGraph.HIT_TARGET)
                                .build();
                    }

                    @Override
                    public PiEngineFrame execute(PiEngineContext context) {
                        return PiEngineFrame.builder()
                                .number(ExampleGraphs_PiDataGraph.HIT_DAMAGE_FINAL, context.number(ExampleGraphs_PiDataGraph.HIT_BASE)
                                        + context.number(ExampleGraphs_PiDataGraph.HIT_POWER) * 2.0D)
                                .flag(ExampleGraphs_PiDataGraph.HIT_ACCEPTED, true)
                                .object(ExampleGraphs_PiDataGraph.HIT_LAST_TARGET, context.requireObject(ExampleGraphs_PiDataGraph.HIT_TARGET))
                                .build();
                    }
                }

                final class ExampleHitData {
                    static PiDataSet<PiEngineAction> defaults() {
                        return ExampleGraphs_PiDataGraph.HIT_ACTIONS_SET()
                                .entry("fire_hit", new DamageAction())
                                .build();
                    }
                }

                final class ExampleHitRuntime {
                    private static final ResourceLocation FIRE_HIT = ExampleGraphs_PiDataGraph.HIT_ACTIONS_ID("fire_hit");

                    static HitResult fireHit(Target target, double base, BigDecimal power) {
                        HitOutput output = ExampleGraphs_PiDataGraph.HIT.run(
                                ExampleHitData.defaults(),
                                FIRE_HIT,
                                new HitInput(base, power, target),
                                ExampleGraphs_PiDataGraph::HIT_OUTPUT);
                        return new HitResult(output.damage(), output.accepted(), output.lastTarget());
                    }
                }

                final class ExampleRegistryRuntime {
                    static PiDataSet<PiEngineAction> keyedDefaults() {
                        return ExampleGraphs_PiDataGraph.HIT_ACTIONS_SET()
                                .entry(ExampleGraphs_PiDataGraph.HIT_ACTIONS_ENTRY_KEY("fire_hit"), new DamageAction())
                                .build();
                    }

                    static HitResult fireHitFromRegistry(RegistryAccess access, Target target, double base, BigDecimal power) {
                        return ExampleGraphs_PiDataGraph.HIT.run(
                                access,
                                ExampleGraphs_PiDataGraph.HIT_ACTIONS_ENTRY_KEY("fire_hit"),
                                new HitInput(base, power, target),
                                frame -> new HitResult(
                                        frame.number(ExampleGraphs_PiDataGraph.HIT_DAMAGE_FINAL),
                                        frame.flag(ExampleGraphs_PiDataGraph.HIT_ACCEPTED),
                                        frame.value(ExampleGraphs_PiDataGraph.HIT_LAST_TARGET)));
                    }
                }

                final class ExampleGameplay {
                    static double castFireHit() {
                        return ExampleHitRuntime.fireHit(new Target("dummy"), 3.0D, new BigDecimal("2.0")).damage();
                    }
                }
                """), generated);

        assertTrue(result.success(), result.diagnostics());
        String generatedSource = Files.readString(generated.resolve("test/ExampleGraphs_PiDataGraph.java"));

        assertTrue(generatedSource.contains("PiEngineNumberKey HIT_BASE = PiEngineNumberKey.of(\"base\")"), generatedSource);
        assertTrue(generatedSource.contains("PiEngineContextKey<test.Target> HIT_TARGET = PiEngineContextKey.of(\"target\", test.Target.class)"), generatedSource);
        assertTrue(generatedSource.contains("PiEngineValueKey<Number> HIT_DAMAGE_FINAL = PiEngineValueKey.number(\"damage.final\")"), generatedSource);
        assertTrue(generatedSource.contains("PiEngineValueKey<Boolean> HIT_ACCEPTED = PiEngineValueKey.flag(\"accepted\")"), generatedSource);
        assertTrue(generatedSource.contains("PiEngineValueKey<test.Target> HIT_LAST_TARGET = PiEngineValueKey.object(\"lastTarget\", test.Target.class)"), generatedSource);
        assertTrue(generatedSource.contains("test.HitOutput HIT_OUTPUT(PiEngineFrame frame)"), generatedSource);
        assertTrue(generatedSource.contains("return new test.HitOutput("), generatedSource);
        assertTrue(generatedSource.contains("ResourceLocation HIT_ACTIONS_ID(String path)"), generatedSource);
        assertTrue(generatedSource.contains("return new ResourceLocation(\"examplemod\", path);"), generatedSource);
        assertTrue(generatedSource.contains("ResourceKey<PiEngineAction> HIT_ACTIONS_ENTRY_KEY(String path)"), generatedSource);
        assertTrue(generatedSource.contains("return ResourceKey.create(HIT_ACTIONS_KEY(), HIT_ACTIONS_ID(path));"), generatedSource);
        assertTrue(generatedSource.contains(".number(HIT_BASE)"), generatedSource);
        assertTrue(generatedSource.contains(".object(HIT_TARGET)"), generatedSource);
        assertTrue(generatedSource.contains("java.util.Objects.requireNonNull(input.power(), \"PiGraphInput number `power` must not be null\").doubleValue()"), generatedSource);
        assertTrue(generatedSource.contains("PiDataSet.Builder<PiEngineAction> HIT_ACTIONS_SET(String namespace)"), generatedSource);
        assertTrue(generatedSource.contains("PiDataSet.Builder<PiEngineAction> HIT_ACTIONS_SET()"), generatedSource);
        assertTrue(generatedSource.contains("return HIT_ACTIONS_SET(\"examplemod\");"), generatedSource);
        assertTrue(generatedSource.contains("return PiDataSet.builder(HIT_ACTIONS_DEFINITION(), namespace);"), generatedSource);
        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{result.classes().toUri().toURL()},
                PiDataGraphGeneratedFacadeTest.class.getClassLoader())) {
            Class<?> gameplay = Class.forName("test.ExampleGameplay", true, loader);
            Method castFireHit = gameplay.getDeclaredMethod("castFireHit");
            castFireHit.setAccessible(true);

            assertEquals(7.0D, castFireHit.invoke(null));
        }
    }
}
