package org.pickaid.pidatagraph.processor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PiDataGraphProcessorValidationTest {
    @TempDir
    Path tempDir;
    private PiDataGraphProcessorCompiler compiler;

    @BeforeEach
    void setUpCompiler() {
        compiler = new PiDataGraphProcessorCompiler(tempDir);
    }

    @Test
    void rejectsEmptyModuleModid() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = " ")
                final class ExampleGraphs {
                }
                """), "PiDataGraph module modid must not be blank");
    }

    @Test
    void rejectsInvalidModuleModid() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "Example Mod")
                final class ExampleGraphs {
                }
                """), "PiDataGraph module modid must be a valid ResourceLocation namespace");
    }

    @Test
    void rejectsEmptyRegistryPath() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = " ")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }
                """), "PiDataPackRegistry path must not be blank");
    }

    @Test
    void rejectsInvalidRegistryPathAndFolder() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "Hit Action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }
                """), "PiDataPackRegistry path must be a valid ResourceLocation path");

        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action", folder = "/skills")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }
                """), "PiDataPackRegistry folder must be a relative data folder path");
    }

    @Test
    void rejectsRegistryFieldOutsideDataGraphModule() throws IOException {
        assertFails(source("""
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }
                """), "PiDataPackRegistry field must be declared inside a @PiDataGraphModule class");
    }

    @Test
    void rejectsPrivateModuleOrInputClasses() throws IOException {
        assertFails(source("""
                final class ExampleMod {
                    @PiDataGraphModule(modid = "examplemod")
                    private static final class Graphs {
                        @PiDataPackRegistry(path = "hit_action")
                        static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                    }
                }

                @PiGraphInput(registry = "Graphs.HIT_ACTIONS", name = "hit")
                record HitInput(double base) {
                }
                """), "@PiDataGraphModule class must not be private");

        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                final class ExampleSkills {
                    @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                    private record HitInput(double base) {
                    }
                }
                """), "@PiGraphInput record must not be private");
    }

    @Test
    void rejectsPrivateObjectComponentTypes() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                final class ExampleSkills {
                    private static final class Target {
                    }

                    @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                    record HitInput(Target target) {
                    }
                }
                """), "PiGraphInput object component `target` type must not be private");
    }

    @Test
    void rejectsPackagePrivateCrossPackageInputRecords() throws IOException {
        assertFails(List.of(
                sourceFile("test.ExampleGraphs", source("""
                        @PiDataGraphModule(modid = "examplemod")
                        public final class ExampleGraphs {
                            @PiDataPackRegistry(path = "hit_action")
                            public static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                        }
                        """)),
                sourceFile("other.HitInput", """
                        package other;

                        import org.pickaid.pidatagraph.annotation.PiGraphInput;

                        @PiGraphInput(registry = "ExampleGraphs.HIT_ACTIONS", name = "hit")
                        record HitInput(double base) {
                        }
                        """)
        ), "@PiGraphInput record must be public when it is used from another package");
    }

    @Test
    void rejectsPackagePrivateCrossPackageObjectComponentTypes() throws IOException {
        assertFails(List.of(
                sourceFile("test.ExampleGraphs", source("""
                        @PiDataGraphModule(modid = "examplemod")
                        public final class ExampleGraphs {
                            @PiDataPackRegistry(path = "hit_action")
                            public static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                        }
                        """)),
                sourceFile("other.Target", """
                        package other;

                        final class Target {
                        }
                        """),
                sourceFile("other.HitInput", """
                        package other;

                        import org.pickaid.pidatagraph.annotation.PiGraphInput;

                        @PiGraphInput(registry = "ExampleGraphs.HIT_ACTIONS", name = "hit")
                        public record HitInput(Target target) {
                        }
                        """)
        ), "PiGraphInput object component `target` type and enclosing types must be public when it is used from another package");
    }

    @Test
    void rejectsDuplicateRegistryPathInOneModule() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();

                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry OTHER_ACTIONS = PiEngineActionRegistry.standard();
                }
                """), "duplicate PiDataPackRegistry path `hit_action`");
    }

    @Test
    void rejectsDuplicateRegistryKeysAcrossModules() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiDataGraphModule(modid = "examplemod")
                final class OtherGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry OTHER_HIT_ACTIONS = PiEngineActionRegistry.standard();
                }
                """), "duplicate PiDataGraph datapack registry key `examplemod:hit_action`");
    }

    @Test
    void rejectsUnknownInputRegistry() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "MISSING", name = "hit")
                record HitInput(double base) {
                }
                """), "unknown PiGraphInput registry `MISSING`");
    }

    @Test
    void rejectsAmbiguousSimpleRegistryReferenceWhenModulesShareAFieldName() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiDataGraphModule(modid = "othermod")
                final class OtherGraphs {
                    @PiDataPackRegistry(path = "other_hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(double base) {
                }
                """), "ambiguous PiGraphInput registry `HIT_ACTIONS`; use `ExampleGraphs.HIT_ACTIONS` or `OtherGraphs.HIT_ACTIONS`");
    }

    @Test
    void ambiguousRegistrySuggestionsUseQualifiedNamesWhenNestedModulesShareASimpleName() throws IOException {
        assertFails(source("""
                final class ExampleMod {
                    @PiDataGraphModule(modid = "examplemod")
                    static final class Graphs {
                        @PiDataPackRegistry(path = "hit_action")
                        static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                    }
                }

                final class OtherMod {
                    @PiDataGraphModule(modid = "othermod")
                    static final class Graphs {
                        @PiDataPackRegistry(path = "other_hit_action")
                        static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                    }
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(double base) {
                }
                """), "ambiguous PiGraphInput registry `HIT_ACTIONS`; use `test.ExampleMod.Graphs.HIT_ACTIONS` or `test.OtherMod.Graphs.HIT_ACTIONS`");
    }

    @Test
    void rejectsInputNamesThatCannotGenerateStableJavaMembers() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "1hit")
                record HitInput(double base) {
                }
                """), "PiGraphInput name must be a Java identifier");
    }

    @Test
    void rejectsDuplicateInputNamesInOneGeneratedModule() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(double base) {
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record OtherHitInput(double base) {
                }
                """), "duplicate PiGraphInput name `hit`");
    }

    @Test
    void rejectsMultipleInputContractsForOneDatapackRegistry() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(double base) {
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "projectile")
                record ProjectileInput(double speed) {
                }
                """), "multiple PiGraphInput records target `ExampleGraphs.HIT_ACTIONS`; declare a separate @PiDataPackRegistry field for a different input contract");
    }

    @Test
    void rejectsInputNamesThatGenerateTheSameMembers() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "fireHit")
                record FireHitInput(double base) {
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "fire_hit")
                record OtherFireHitInput(double base) {
                }
                """), "PiGraphInput name `fire_hit` generates duplicate member prefix `FIRE_HIT`");
    }

    @Test
    void rejectsNonRecordInput() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                final class HitInput {
                }
                """), "@PiGraphInput can only be applied to records");
    }

    @Test
    void rejectsDuplicateContextKeys() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(double base, @PiNumber("base") double power) {
                }
                """), "duplicate PiDataGraph context key `base`");
    }

    @Test
    void rejectsInvalidNumberAndObjectComponents() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(@PiNumber("target") String target) {
                }
                """), "@PiNumber component `target` must be numeric");

        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(@PiObject("count") int count) {
                }
                """), "@PiObject component `count` must not be primitive");
    }

    @Test
    void rejectsPrimitiveBooleanInputWithoutExplicitObjectWrapper() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(boolean critical) {
                }
                """), "primitive PiGraphInput component `critical` must be numeric or explicitly mapped");
    }

    @Test
    void rejectsAmbiguousComponentClassification() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(@PiNumber("value") @PiObject("value") double value) {
                }
                """), "record component `value` cannot use both @PiNumber and @PiObject");
    }

    @Test
    void rejectsInvalidContextKey() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(@PiNumber("bad-key") double value) {
                }
                """), "invalid PiDataGraph context key `bad-key`");
    }

    @Test
    void rejectsRegistryFieldWithWrongShape() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }
                """), "PiDataPackRegistry field must be static final PiEngineActionRegistry");
    }

    @Test
    void reportsGeneratedClassNameCollisionAsAuthoringError() throws IOException {
        assertFails(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                final class ExampleGraphs_PiDataGraph {
                }
                """), "failed to generate PiDataGraph glue `test.ExampleGraphs_PiDataGraph`");
    }

    @Test
    void rejectsModulesThatGenerateTheSameGlueClassName() throws IOException {
        assertFails(source("""
                final class Example {
                    @PiDataGraphModule(modid = "examplemod")
                    static final class Graphs {
                        @PiDataPackRegistry(path = "hit_action")
                        static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                    }
                }

                @PiDataGraphModule(modid = "othermod")
                final class Example_Graphs {
                    @PiDataPackRegistry(path = "other_hit_action")
                    static final PiEngineActionRegistry OTHER_HIT_ACTIONS = PiEngineActionRegistry.standard();
                }
                """), "duplicate PiDataGraph generated helper class `test.Example_Graphs_PiDataGraph`");
    }

    private void assertFails(String source, String expectedDiagnostic) throws IOException {
        CompilationResult result = compiler.compile(source);

        assertFalse(result.success(), "compilation unexpectedly passed");
        assertTrue(result.diagnostics().contains(expectedDiagnostic), result.diagnostics());
    }

    private void assertFails(List<SourceFile> sources, String expectedDiagnostic) throws IOException {
        CompilationResult result = compiler.compile(sources, List.of("-proc:only"));

        assertFalse(result.success(), "compilation unexpectedly passed");
        assertTrue(result.diagnostics().contains(expectedDiagnostic), result.diagnostics());
    }

    private static String source(String body) {
        return PiDataGraphProcessorCompiler.source(body);
    }

    private static SourceFile sourceFile(String className, String source) {
        return PiDataGraphProcessorCompiler.sourceFile(className, source);
    }
}
