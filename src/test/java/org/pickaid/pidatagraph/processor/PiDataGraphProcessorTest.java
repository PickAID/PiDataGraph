package org.pickaid.pidatagraph.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pickaid.pidatagraph.engine.action.PiEngineAction;

class PiDataGraphProcessorTest {
    @TempDir
    Path tempDir;
    private PiDataGraphProcessorCompiler compiler;

    @BeforeEach
    void setUpCompiler() {
        compiler = new PiDataGraphProcessorCompiler(tempDir);
    }

    @Test
    void jarRegistersTheAnnotationProcessorService() throws IOException {
        String service = new String(Objects.requireNonNull(
                PiDataGraphProcessorTest.class.getClassLoader()
                        .getResourceAsStream("META-INF/services/javax.annotation.processing.Processor"),
                "processor service file"
        ).readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(service.contains(PiDataGraphProcessor.class.getName()), service);
    }

    @Test
    void acceptsOneModuleRegistryAndInputRecord() throws IOException {
        CompilationResult result = compile(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(double base, double power, String target) {
                }
                """));

        assertTrue(result.success(), result.diagnostics());
    }

    @Test
    void generatesExplicitApiGlueForRegistryAndInputRecord() throws IOException {
        Path generated = tempDir.resolve("generated");
        CompilationResult result = compileGenerating(source("""
                import org.pickaid.pidatagraph.engine.PiEngineContextBinder;
                import org.pickaid.pidatagraph.engine.PiEngineRunner;

                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(double base, double power, String target) {
                }

                final class UseGenerated {
                    static PiEngineRunner<HitInput> runner() {
                        return ExampleGraphs_PiDataGraph.HIT;
                    }

                    static PiEngineContextBinder<HitInput> binder() {
                        return ExampleGraphs_PiDataGraph.HIT_BINDER;
                    }
                }
                """), generated);

        assertTrue(result.success(), result.diagnostics());
        String generatedSource = Files.readString(generated.resolve("test/ExampleGraphs_PiDataGraph.java"));

        assertTrue(generatedSource.contains("Generated PiDataGraph glue. Prefer a generated @PiGraphInput facade for project code."), generatedSource);
        assertTrue(generatedSource.contains("public final class ExampleGraphs_PiDataGraph"), generatedSource);
        assertTrue(generatedSource.contains("ResourceKey<Registry<PiEngineAction>> HIT_ACTIONS_KEY()"), generatedSource);
        assertTrue(generatedSource.contains("PiDataDefinition<PiEngineAction> HIT_ACTIONS_DEFINITION()"), generatedSource);
        assertTrue(generatedSource.contains("PiEngineContextBinder<test.HitInput> HIT_BINDER"), generatedSource);
        assertTrue(generatedSource.contains("PiEngineRunner<test.HitInput> HIT"), generatedSource);
        assertTrue(generatedSource.contains("PiEngineNumberKey HIT_BASE = PiEngineNumberKey.of(\"base\")"), generatedSource);
        assertTrue(generatedSource.contains(".number(HIT_BASE, input.base())"), generatedSource);
        assertTrue(generatedSource.contains(".number(HIT_POWER, input.power())"), generatedSource);
        assertTrue(generatedSource.contains(".object(HIT_TARGET, java.util.Objects.requireNonNull(input.target(), \"PiGraphInput object `target` must not be null\"))"), generatedSource);
        assertTrue(generatedSource.contains("PiDataPackRegistries.action(event, HIT_ACTIONS_KEY(), test.ExampleGraphs.HIT_ACTIONS, PiDataPackSync.SERVER_ONLY);"), generatedSource);
    }

    @Test
    void generatedRunnerSupportsADomainFacadeShape() throws IOException {
        CompilationResult result = compileGenerating(source("""
                import net.minecraft.resources.ResourceLocation;
                import org.pickaid.pidatagraph.engine.action.PiEngineAction;
                import org.pickaid.pidatagraph.engine.context.PiEngineNumberKey;

                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(double base, double power, String target) {
                }

                record HitResult(double damage) {
                }

                final class ExampleSkills {
                    private static final ResourceLocation FIRE_HIT = new ResourceLocation("examplemod", "fire_hit");
                    private static final PiEngineNumberKey DAMAGE = PiEngineNumberKey.of("damage");

                    static HitResult fireHit(PiEngineAction action, HitInput input) {
                        return ExampleGraphs_PiDataGraph.HIT.run(
                                action,
                                input,
                                frame -> new HitResult(frame.numberOr(DAMAGE, 0.0D)));
                    }
                }
                """), tempDir.resolve("generated-facade"));

        assertTrue(result.success(), result.diagnostics());
    }

    @Test
    void generatedRegistrationHelpersCanBeUsedAsForgeListenerMethods() throws IOException {
        CompilationResult result = compileGenerating(source("""
                import net.minecraftforge.eventbus.api.IEventBus;

                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(double base, String target) {
                }

                final class ExampleMod {
                    static void register(IEventBus modBus, IEventBus forgeBus) {
                        modBus.addListener(ExampleGraphs_PiDataGraph::registerDatapackRegistries);
                        forgeBus.addListener(ExampleGraphs_PiDataGraph::addReloadListeners);
                    }
                }
                """), tempDir.resolve("generated-listener-methods"));

        assertTrue(result.success(), result.diagnostics());
    }

    @Test
    void generatedGlueCanUseNestedInputRecords() throws IOException {
        Path generated = tempDir.resolve("generated-nested-input");
        CompilationResult result = compileGenerating(source("""
                import org.pickaid.pidatagraph.engine.PiEngineRunner;

                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                final class ExampleSkills {
                    @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                    record HitInput(double base, String target) {
                    }

                    static PiEngineRunner<HitInput> runner() {
                        return ExampleGraphs_PiDataGraph.HIT;
                    }
                }
                """), generated);

        assertTrue(result.success(), result.diagnostics());
        String generatedSource = Files.readString(generated.resolve("test/ExampleGraphs_PiDataGraph.java"));

        assertTrue(generatedSource.contains("PiEngineRunner<test.ExampleSkills.HitInput> HIT"), generatedSource);
    }

    @Test
    void generatedGlueCanUseGenericAndArrayObjectComponents() throws IOException {
        Path generated = tempDir.resolve("generated-object-types");
        CompilationResult result = compileGenerating(source("""
                import java.util.List;

                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(List<String> targets, String[] tags) {
                }
                """), generated);

        assertTrue(result.success(), result.diagnostics());
        String generatedSource = Files.readString(generated.resolve("test/ExampleGraphs_PiDataGraph.java"));

        assertTrue(generatedSource.contains("PiEngineContextKey<java.util.List> HIT_TARGETS = PiEngineContextKey.of(\"targets\", java.util.List.class)"), generatedSource);
        assertTrue(generatedSource.contains("PiEngineContextKey<java.lang.String[]> HIT_TAGS = PiEngineContextKey.of(\"tags\", java.lang.String[].class)"), generatedSource);
        assertTrue(generatedSource.contains(".object(HIT_TARGETS)"), generatedSource);
        assertTrue(generatedSource.contains(".object(HIT_TAGS)"), generatedSource);
    }

    @Test
    void generatedGlueCanUseBoxedBooleanObjectComponents() throws IOException {
        Path generated = tempDir.resolve("generated-boolean-object");
        CompilationResult result = compileGenerating(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(@PiObject("critical") Boolean critical) {
                }
                """), generated);

        assertTrue(result.success(), result.diagnostics());
        String generatedSource = Files.readString(generated.resolve("test/ExampleGraphs_PiDataGraph.java"));

        assertTrue(generatedSource.contains("PiEngineContextKey<java.lang.Boolean> HIT_CRITICAL = PiEngineContextKey.of(\"critical\", java.lang.Boolean.class)"), generatedSource);
        assertTrue(generatedSource.contains(".object(HIT_CRITICAL)"), generatedSource);
        assertTrue(generatedSource.contains(".object(HIT_CRITICAL, java.util.Objects.requireNonNull(input.critical(), \"PiGraphInput object `critical` must not be null\"))"), generatedSource);
    }

    @Test
    void generatedGlueCanUseNestedModuleClasses() throws IOException {
        Path generated = tempDir.resolve("generated-nested-module");
        CompilationResult result = compileGenerating(source("""
                final class ExampleMod {
                    @PiDataGraphModule(modid = "examplemod")
                    static final class Graphs {
                        @PiDataPackRegistry(path = "hit_action")
                        static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                    }
                }

                @PiGraphInput(registry = "Graphs.HIT_ACTIONS", name = "hit")
                record HitInput(double base, String target) {
                }
                """), generated);

        assertTrue(result.success(), result.diagnostics());
        String generatedSource = Files.readString(generated.resolve("test/ExampleMod_Graphs_PiDataGraph.java"));

        assertTrue(generatedSource.contains("PiEngineActionData.definition(new ResourceLocation(\"examplemod\", \"hit_action\"), \"hit_action\", test.ExampleMod.Graphs.HIT_ACTIONS);"), generatedSource);
        assertTrue(generatedSource.contains("PiDataPackRegistries.action(event, HIT_ACTIONS_KEY(), test.ExampleMod.Graphs.HIT_ACTIONS, PiDataPackSync.SERVER_ONLY);"), generatedSource);
    }

    @Test
    void generatedGlueCanUsePublicInputRecordsFromAnotherPackage() throws IOException {
        Path generated = tempDir.resolve("generated-cross-package-input");
        CompilationResult result = compileGenerating(List.of(
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
                        public record HitInput(double base, String target) {
                        }
                        """)
        ), generated);

        assertTrue(result.success(), result.diagnostics());
        String generatedSource = Files.readString(generated.resolve("test/ExampleGraphs_PiDataGraph.java"));

        assertTrue(generatedSource.contains("PiEngineRunner<other.HitInput> HIT"), generatedSource);
    }

    @Test
    void generatesMultipleRegistriesWithFolderAndSyncMode() throws IOException {
        Path generated = tempDir.resolve("generated-multiple-registries");
        CompilationResult result = compileGenerating(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(
                            path = "hit_action",
                            folder = "skills/hit",
                            sync = org.pickaid.pidatagraph.data.PiDataPackSync.SYNC_TO_CLIENT)
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();

                    @PiDataPackRegistry(path = "projectile_action")
                    static final PiEngineActionRegistry PROJECTILE_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(double base, String target) {
                }

                @PiGraphInput(registry = "PROJECTILE_ACTIONS", name = "projectile")
                record ProjectileInput(double speed, String projectile) {
                }
                """), generated);

        assertTrue(result.success(), result.diagnostics());
        String generatedSource = Files.readString(generated.resolve("test/ExampleGraphs_PiDataGraph.java"));

        assertTrue(generatedSource.contains("PiEngineActionData.definition(new ResourceLocation(\"examplemod\", \"hit_action\"), \"skills/hit\", test.ExampleGraphs.HIT_ACTIONS);"), generatedSource);
        assertTrue(generatedSource.contains("PiDataPackRegistries.action(event, HIT_ACTIONS_KEY(), test.ExampleGraphs.HIT_ACTIONS, PiDataPackSync.SYNC_TO_CLIENT);"), generatedSource);
        assertTrue(generatedSource.contains("PiEngineRunner<test.HitInput> HIT"), generatedSource);
        assertTrue(generatedSource.contains("PiEngineRunner<test.ProjectileInput> PROJECTILE"), generatedSource);
    }

    @Test
    void acceptsQualifiedRegistryReferenceWhenModulesShareAFieldName() throws IOException {
        Path generated = tempDir.resolve("generated-qualified-registry");
        CompilationResult result = compileGenerating(source("""
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

                @PiGraphInput(registry = "ExampleGraphs.HIT_ACTIONS", name = "hit")
                record HitInput(double base) {
                }

                @PiGraphInput(registry = "OtherGraphs.HIT_ACTIONS", name = "otherHit")
                record OtherHitInput(double base) {
                }
                """), generated);

        assertTrue(result.success(), result.diagnostics());
        String exampleSource = Files.readString(generated.resolve("test/ExampleGraphs_PiDataGraph.java"));
        String otherSource = Files.readString(generated.resolve("test/OtherGraphs_PiDataGraph.java"));

        assertTrue(exampleSource.contains("PiEngineRunner<test.HitInput> HIT"), exampleSource);
        assertFalse(exampleSource.contains("OtherHitInput"), exampleSource);
        assertTrue(otherSource.contains("PiEngineRunner<test.OtherHitInput> OTHER_HIT"), otherSource);
        assertFalse(otherSource.contains("PiEngineRunner<test.HitInput> HIT"), otherSource);
    }

    @Test
    void generatedFacadeCanExecuteAnActionAndReturnTypedResult() throws Exception {
        CompilationResult result = compileGenerating(source("""
                import com.mojang.serialization.Codec;
                import net.minecraft.resources.ResourceLocation;
                import org.pickaid.pidatagraph.engine.PiEngineContext;
                import org.pickaid.pidatagraph.engine.PiEngineFrame;
                import org.pickaid.pidatagraph.engine.action.PiEngineAction;
                import org.pickaid.pidatagraph.engine.action.PiEngineActionRegistry;
                import org.pickaid.pidatagraph.engine.action.PiEngineActionType;
                import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
                import org.pickaid.pidatagraph.engine.context.PiEngineNumberKey;

                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.builder()
                            .add(DamageAction.TYPE)
                            .build();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(double base, double power, String target) {
                }

                record HitResult(double damage) {
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
                                .number("base")
                                .number("power")
                                .object("target", String.class)
                                .build();
                    }

                    @Override
                    public PiEngineFrame execute(PiEngineContext context) {
                        return PiEngineFrame.builder()
                                .number("damage", context.number("base") + context.number("power") * 2.0D)
                                .build();
                    }
                }

                final class ExampleSkills {
                    private static final PiEngineNumberKey DAMAGE = PiEngineNumberKey.of("damage");

                    static HitResult fireHit(PiEngineAction action) {
                        return ExampleGraphs_PiDataGraph.HIT.run(
                                action,
                                new HitInput(3.0D, 2.0D, "dummy"),
                                frame -> new HitResult(frame.numberOr(DAMAGE, 0.0D)));
                    }
                }
                """), tempDir.resolve("generated-runtime"));

        assertTrue(result.success(), result.diagnostics());
        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{result.classes().toUri().toURL()},
                PiDataGraphProcessorTest.class.getClassLoader())) {
            Class<?> actionType = Class.forName("test.DamageAction", true, loader);
            Constructor<?> constructor = actionType.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object action = constructor.newInstance();

            Class<?> skills = Class.forName("test.ExampleSkills", true, loader);
            Method fireHit = skills.getDeclaredMethod("fireHit", PiEngineAction.class);
            fireHit.setAccessible(true);
            Object hitResult = fireHit.invoke(null, action);

            Method damage = hitResult.getClass().getDeclaredMethod("damage");
            damage.setAccessible(true);
            assertEquals(7.0D, (Double) damage.invoke(hitResult));
        }
    }

    @Test
    void generatedBinderReportsTheNullObjectKey() throws Exception {
        CompilationResult result = compileGenerating(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(double base, String target) {
                }

                final class ExampleSkills {
                    static String bindNullTargetMessage() {
                        try {
                            ExampleGraphs_PiDataGraph.HIT_BINDER.bind(new HitInput(3.0D, null));
                            return "missing failure";
                        } catch (NullPointerException error) {
                            return error.getMessage();
                        }
                    }
                }
                """), tempDir.resolve("generated-null-object"));

        assertTrue(result.success(), result.diagnostics());
        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{result.classes().toUri().toURL()},
                PiDataGraphProcessorTest.class.getClassLoader())) {
            Class<?> skills = Class.forName("test.ExampleSkills", true, loader);
            Method message = skills.getDeclaredMethod("bindNullTargetMessage");
            message.setAccessible(true);

            assertEquals("PiGraphInput object `target` must not be null", message.invoke(null));
        }
    }

    @Test
    void generatedBinderReportsTheNullInputName() throws Exception {
        CompilationResult result = compileGenerating(source("""
                @PiDataGraphModule(modid = "examplemod")
                final class ExampleGraphs {
                    @PiDataPackRegistry(path = "hit_action")
                    static final PiEngineActionRegistry HIT_ACTIONS = PiEngineActionRegistry.standard();
                }

                @PiGraphInput(registry = "HIT_ACTIONS", name = "hit")
                record HitInput(double base, String target) {
                }

                final class ExampleSkills {
                    static String bindNullInputMessage() {
                        try {
                            ExampleGraphs_PiDataGraph.HIT_BINDER.bind(null);
                            return "missing failure";
                        } catch (NullPointerException error) {
                            return error.getMessage();
                        }
                    }
                }
                """), tempDir.resolve("generated-null-input"));

        assertTrue(result.success(), result.diagnostics());
        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{result.classes().toUri().toURL()},
                PiDataGraphProcessorTest.class.getClassLoader())) {
            Class<?> skills = Class.forName("test.ExampleSkills", true, loader);
            Method message = skills.getDeclaredMethod("bindNullInputMessage");
            message.setAccessible(true);

            assertEquals("PiGraphInput `hit` must not be null", message.invoke(null));
        }
    }

    private CompilationResult compile(String source) throws IOException {
        return compiler.compile(source);
    }

    private CompilationResult compileGenerating(String source, Path generated) throws IOException {
        return compiler.compileGenerating(source, generated);
    }

    private CompilationResult compileGenerating(List<SourceFile> sources, Path generated) throws IOException {
        return compiler.compileGenerating(sources, generated);
    }

    private static String source(String body) {
        return PiDataGraphProcessorCompiler.source(body);
    }

    private static SourceFile sourceFile(String className, String source) {
        return PiDataGraphProcessorCompiler.sourceFile(className, source);
    }
}
