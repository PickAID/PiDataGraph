package org.pickaid.pidatagraph.processor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PiDataGraphProcessorGuardrailTest {
    @TempDir
    Path tempDir;
    private PiDataGraphProcessorCompiler compiler;

    @BeforeEach
    void setUpCompiler() {
        compiler = new PiDataGraphProcessorCompiler(tempDir);
    }

    @Test
    void rejectsPrivateDatapackRegistryFieldsBeforeGeneratedGlueFailsAccessChecks() throws IOException {
        assertFails(PiDataGraphProcessorCompiler.source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    private static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(double base) {
                }
                """), "PiDataPackRegistry field must not be private");
    }

    @Test
    void rejectsModulesInsidePrivateEnclosingTypes() throws IOException {
        assertFails(PiDataGraphProcessorCompiler.source("""
                final class ExampleMod {
                    private static final class PrivateHolder {
                        @PiDataGraphModule(modid = "examplemod")
                        static final class Graphs {
                            @PiDataPackRegistry(path = "hit_action")
                            static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                        }
                    }
                }
                """), "@PiDataGraphModule enclosing types must not be private");
    }

    @Test
    void rejectsInputRecordsInsidePrivateEnclosingTypes() throws IOException {
        assertFails(PiDataGraphProcessorCompiler.source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                final class ExampleSkills {
                    private static final class PrivateHolder {
                        @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                        record HitInput(double base) {
                        }
                    }
                }
                """), "@PiGraphInput record enclosing types must not be private");
    }

    @Test
    void rejectsObjectComponentsWhoseRuntimeTypeIsInsidePrivateEnclosingTypes() throws IOException {
        assertFails(PiDataGraphProcessorCompiler.source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                final class ExampleSkills {
                    private static final class PrivateHolder {
                        static final class Target {
                        }
                    }

                    @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                    record HitInput(PrivateHolder.Target target) {
                    }
                }
                """), "PiGraphInput object component `target` type enclosing types must not be private");
    }

    @Test
    void rejectsInputComponentsThatGenerateDuplicateKeyConstants() throws IOException {
        assertFails(PiDataGraphProcessorCompiler.source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(double baseDamage, @PiNumber("base_damage") double explicitBaseDamage) {
                }
                """), "PiGraphInput component key `base_damage` generates duplicate member `HIT_BASE_DAMAGE`");
    }

    @Test
    void rejectsInputComponentKeyConstantsThatCollideWithGeneratedRunnerMembers() throws IOException {
        assertFails(PiDataGraphProcessorCompiler.source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(@PiNumber("binder") double binder) {
                }
                """), "PiGraphInput component key `binder` generates duplicate member `HIT_BINDER`");
    }

    @Test
    void rejectsRegistryPathsThatWouldGenerateUnsafeDefaultFolders() throws IOException {
        assertFails(PiDataGraphProcessorCompiler.source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "/hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }
                """), "PiDataPackRegistry path must be a relative data folder path when folder is blank");
    }

    private void assertFails(String source, String expectedDiagnostic) throws IOException {
        CompilationResult result = compiler.compile(source, List.of("-proc:only"));

        assertFalse(result.success(), "compilation unexpectedly passed");
        assertTrue(result.diagnostics().contains(expectedDiagnostic), result.diagnostics());
    }

}
