package org.pickaid.pidatagraph.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PiDataGraphOutputProcessorTest {
    @TempDir
    Path tempDir;
    private PiDataGraphProcessorCompiler compiler;

    @BeforeEach
    void setUpCompiler() {
        compiler = new PiDataGraphProcessorCompiler(tempDir);
    }

    @Test
    void rejectsOutputKeysThatCollideWithGeneratedInputMembers() throws IOException {
        CompilationResult result = compiler.compile(PiDataGraphProcessorCompiler.source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                record Target(String name) {
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(Target target) {
                }

                @PiGraphOutput(registry = "HIT_ACTIONS", name = "hit")
                record HitOutput(Target target) {
                }
                """), List.of("-proc:only"));

        assertFalse(result.success(), "compilation unexpectedly passed");
        assertTrue(result.diagnostics().contains(
                "PiGraphOutput component key `target` generates duplicate member `HIT_TARGET`"), result.diagnostics());
    }

    @Test
    void generatedOutputMapperConvertsNumberFieldsToRecordTypes() throws Exception {
        CompilationResult result = compiler.compileGenerating(PiDataGraphProcessorCompiler.source("""
                import org.pickaid.pidatagraph.engine.PiEngineFrame;

                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "stat_action")
                    static final PiEngineActionRegistry STAT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphOutput(registry = "STAT_ACTIONS", name = "stat")
                record StatOutput(int level, Integer charges, Number rawScore) {
                }

                final class ExampleOutputRuntime {
                    static String map() {
                        PiEngineFrame frame = PiEngineFrame.builder()
                                .integer(ExampleGraphs_PiDataGraph.STAT_LEVEL, 3)
                                .integer(ExampleGraphs_PiDataGraph.STAT_CHARGES, 4)
                                .number(ExampleGraphs_PiDataGraph.STAT_RAW_SCORE, 12.5D)
                                .build();
                        StatOutput output = ExampleGraphs_PiDataGraph.STAT_OUTPUT(frame);
                        return output.level() + "/" + output.charges() + "/" + output.rawScore().doubleValue();
                    }
                }
                """), tempDir.resolve("generated-output-mapper"));

        assertTrue(result.success(), result.diagnostics());
        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{result.classes().toUri().toURL()},
                PiDataGraphOutputProcessorTest.class.getClassLoader())) {
            Class<?> runtime = Class.forName("test.ExampleOutputRuntime", true, loader);
            Method map = runtime.getDeclaredMethod("map");
            map.setAccessible(true);

            assertEquals("3/4/12.5", map.invoke(null));
        }
    }
}
