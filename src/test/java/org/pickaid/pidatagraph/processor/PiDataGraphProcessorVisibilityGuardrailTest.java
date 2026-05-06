package org.pickaid.pidatagraph.processor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PiDataGraphProcessorVisibilityGuardrailTest {
    @TempDir
    Path tempDir;
    private PiDataGraphProcessorCompiler compiler;

    @BeforeEach
    void setUpCompiler() {
        compiler = new PiDataGraphProcessorCompiler(tempDir);
    }

    @Test
    void rejectsCrossPackageInputRecordsInsidePackagePrivateEnclosingTypes() throws IOException {
        assertFails(List.of(
                sourceFile("test.ExampleGraphs", PiDataGraphProcessorCompiler.source("""
                        @PiDataGraphModule(modid = "examplemod")
                        public final class ExampleGraphs {
                            @PiDataPackRegistry(path = "hit_action")
                            public static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                        }
                        """)),
                sourceFile("other.Holder", """
                        package other;

                        import org.pickaid.pidatagraph.annotation.PiGraphInput;

                        final class Holder {
                            @PiGraphInput(registry = "ExampleGraphs.HIT_ACTIONS", name = "hit")
                            public record HitInput(double base) {
                            }
                        }
                        """)
        ), "@PiGraphInput record and enclosing types must be public when used from another package");
    }

    @Test
    void rejectsCrossPackageObjectComponentsInsidePackagePrivateEnclosingTypes() throws IOException {
        assertFails(List.of(
                sourceFile("test.ExampleGraphs", PiDataGraphProcessorCompiler.source("""
                        @PiDataGraphModule(modid = "examplemod")
                        public final class ExampleGraphs {
                            @PiDataPackRegistry(path = "hit_action")
                            public static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                        }
                        """)),
                sourceFile("other.PackageHolder", """
                        package other;

                        final class PackageHolder {
                            public static final class Target {
                            }
                        }
                        """),
                sourceFile("other.HitInput", """
                        package other;

                        import org.pickaid.pidatagraph.annotation.PiGraphInput;

                        @PiGraphInput(registry = "ExampleGraphs.HIT_ACTIONS", name = "hit")
                        public record HitInput(PackageHolder.Target target) {
                        }
                        """)
        ), "PiGraphInput object component `target` type and enclosing types must be public when it is used from another package");
    }

    private void assertFails(List<SourceFile> sources, String expectedDiagnostic) throws IOException {
        CompilationResult result = compiler.compile(sources, List.of("-proc:only"));

        assertFalse(result.success(), "compilation unexpectedly passed");
        assertTrue(result.diagnostics().contains(expectedDiagnostic), result.diagnostics());
    }

    private static SourceFile sourceFile(String className, String source) {
        return PiDataGraphProcessorCompiler.sourceFile(className, source);
    }
}
