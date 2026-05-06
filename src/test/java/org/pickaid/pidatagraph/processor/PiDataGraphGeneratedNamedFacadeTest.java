package org.pickaid.pidatagraph.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PiDataGraphGeneratedNamedFacadeTest {
    @TempDir
    Path tempDir;
    private PiDataGraphProcessorCompiler compiler;

    @BeforeEach
    void setUpCompiler() {
        compiler = new PiDataGraphProcessorCompiler(tempDir);
    }

    @Test
    void generatedNamedFacadeHidesInternalGlueForRuntimeCalls() throws Exception {
        Path generated = tempDir.resolve("generated-named-facade");
        CompilationResult result = compiler.compileGenerating(PiDataGraphProcessorCompiler.source("""
                import com.mojang.serialization.Codec;
                import java.nio.file.Files;
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

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit", facade = "HitGraph")
                record HitInput(double base, double power, Target target) {
                }

                @PiGraphOutput(registry = "HIT_ACTIONS", name = "hit")
                record HitOutput(@PiOutput("damage.final") double damage, boolean accepted, Target lastTarget) {
                }

                record Target(String name) {
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
                                .number(HitGraph.Context.BASE)
                                .number(HitGraph.Context.POWER)
                                .object(HitGraph.Context.TARGET)
                                .build();
                    }

                    @Override
                    public PiEngineFrame execute(PiEngineContext context) {
                        return PiEngineFrame.builder()
                                .number(HitGraph.Output.DAMAGE_FINAL, context.number(HitGraph.Context.BASE)
                                        + context.number(HitGraph.Context.POWER) * 2.0D)
                                .flag(HitGraph.Output.ACCEPTED, true)
                                .object(HitGraph.Output.LAST_TARGET, context.requireObject(HitGraph.Context.TARGET))
                                .build();
                    }
                }

                final class ExampleHitData {
                    static PiDataSet<PiEngineAction> defaults() {
                        return HitGraph.dataSet()
                                .entry("fire_hit", new DamageAction())
                                .build();
                    }
                }

                final class ExampleGameplay {
                    static double castFireHit() {
                        HitOutput output = HitGraph.run(
                                ExampleHitData.defaults(),
                                "fire_hit",
                                new HitInput(3.0D, 2.0D, new Target("dummy")));
                        return output.damage();
                    }
                }
                """), generated);

        assertTrue(result.success(), result.diagnostics());
        String facadeSource = Files.readString(generated.resolve("test/HitGraph.java"));
        String modEventsSource = Files.readString(generated.resolve("test/ExampleGraphs_PiDataGraphModEvents.java"));
        String forgeEventsSource = Files.readString(generated.resolve("test/ExampleGraphs_PiDataGraphForgeEvents.java"));

        assertTrue(facadeSource.contains("public final class HitGraph"), facadeSource);
        assertTrue(facadeSource.contains("public static final class Context"), facadeSource);
        assertTrue(facadeSource.contains("public static final PiEngineNumberKey BASE = ExampleGraphs_PiDataGraph.HIT_BASE;"), facadeSource);
        assertTrue(facadeSource.contains("public static final PiEngineValueKey<Number> DAMAGE_FINAL = ExampleGraphs_PiDataGraph.HIT_DAMAGE_FINAL;"), facadeSource);
        assertTrue(facadeSource.contains("static PiDataBuildContext validationContext()"), facadeSource);
        assertTrue(facadeSource.contains("return ExampleGraphs_PiDataGraph.HIT.validationContext();"), facadeSource);
        assertTrue(facadeSource.contains("static test.HitOutput run(PiDataSet<PiEngineAction> actions, String path, test.HitInput input)"), facadeSource);
        assertTrue(facadeSource.contains("return run(actions, id(path), input);"), facadeSource);
        assertTrue(facadeSource.contains("return ExampleGraphs_PiDataGraph.HIT.run(actions, key, input, ExampleGraphs_PiDataGraph::HIT_OUTPUT);"), facadeSource);
        assertTrue(modEventsSource.contains("@Mod.EventBusSubscriber(modid = \"examplemod\", bus = Mod.EventBusSubscriber.Bus.MOD)"), modEventsSource);
        assertTrue(modEventsSource.contains("ExampleGraphs_PiDataGraph.registerDatapackRegistries(event);"), modEventsSource);
        assertTrue(forgeEventsSource.contains("@Mod.EventBusSubscriber(modid = \"examplemod\", bus = Mod.EventBusSubscriber.Bus.FORGE)"), forgeEventsSource);
        assertTrue(forgeEventsSource.contains("ExampleGraphs_PiDataGraph.addReloadListeners(event);"), forgeEventsSource);
        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{result.classes().toUri().toURL()},
                PiDataGraphGeneratedNamedFacadeTest.class.getClassLoader())) {
            Class<?> gameplay = Class.forName("test.ExampleGameplay", true, loader);
            Method castFireHit = gameplay.getDeclaredMethod("castFireHit");
            castFireHit.setAccessible(true);
            try {
                assertEquals(7.0D, castFireHit.invoke(null));
            } catch (InvocationTargetException error) {
                Throwable cause = error.getCause();
                if (cause instanceof Exception exception) {
                    throw exception;
                }
                if (cause instanceof Error fatal) {
                    throw fatal;
                }
                throw new AssertionError(cause);
            }
        }
    }
}
