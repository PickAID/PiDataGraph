package org.pickaid.pidatagraph.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.Codec;
import java.lang.reflect.Constructor;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.data.PiDataDefinition;
import org.pickaid.pidatagraph.data.PiDataSet;
import org.pickaid.pidatagraph.data.PiDataVerificationException;
import org.pickaid.pidatagraph.engine.action.PiEngineAction;
import org.pickaid.pidatagraph.engine.action.PiEngineActionType;
import org.pickaid.pidatagraph.engine.action.PiSequenceAction;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;

class PiEngineRunnerTest {
    @Test
    void binderValidationContextComesFromItsContract() {
        PiDataBuildContext context = HitBinder.INSTANCE.validationContext();

        assertTrue(context.hasVariable("base"));
        assertTrue(context.hasVariable("power"));
        assertTrue(context.hasObject("target", Target.class));
    }

    @Test
    void runnerExposesValidationContextForDatagenAndReloadChecks() {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(HitBinder.INSTANCE);

        PiDataBuildContext context = runner.validationContext();

        assertTrue(context.hasVariable(HitKeys.BASE));
        assertTrue(context.hasVariable(HitKeys.POWER));
        assertTrue(context.hasObject(HitKeys.TARGET));
    }

    @Test
    void binderValidationContextRejectsNullContract() {
        NullPointerException error = assertThrows(NullPointerException.class, () ->
                NullContractBinder.INSTANCE.validationContext());

        assertEquals("binder returned null contract", error.getMessage());
    }

    @Test
    void runnerChecksActionContractBeforeExecuting() {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(HitBinder.INSTANCE);

        PiEngineContractViolation error = assertThrows(PiEngineContractViolation.class, () ->
                runner.run(new ExtraObjectAction(), new HitInput(3, 2, new Target("dummy"))));

        assertTrue(error.getMessage().contains("engine context contract failed for action example:extra_object"));
        assertTrue(error.getMessage().contains("missing object `source` of type " + Target.class.getName()));
        assertTrue(error.getMessage().contains("available numbers: [base, power]"));
    }

    @Test
    void runnerChecksBinderContractBeforeExecuting() {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(BrokenBinder.INSTANCE);

        PiEngineContractViolation error = assertThrows(PiEngineContractViolation.class, () ->
                runner.run(new BaseOnlyAction(), new HitInput(3, 2, new Target("dummy"))));

        assertTrue(error.getMessage().contains("engine context contract failed for binder "));
        assertTrue(error.getMessage().contains("missing number `power`"));
        assertTrue(error.getMessage().contains("available numbers: [base]"));
    }

    @Test
    void runnerRejectsNullContextFromBinder() {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(NullBinder.INSTANCE);

        NullPointerException error = assertThrows(NullPointerException.class, () ->
                runner.run(new BaseOnlyAction(), new HitInput(3, 2, new Target("dummy"))));

        assertEquals("binder returned null context", error.getMessage());
    }

    @Test
    void runnerRejectsNullFrameFromAction() {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(HitBinder.INSTANCE);

        NullPointerException error = assertThrows(NullPointerException.class, () ->
                runner.run(new NullFrameAction(), new HitInput(3, 2, new Target("dummy"))));

        assertEquals("action example:null_frame returned null frame", error.getMessage());
    }

    @Test
    void runnerRejectsNullContractFromBinder() {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(NullContractBinder.INSTANCE);

        NullPointerException error = assertThrows(NullPointerException.class, () ->
                runner.run(new BaseOnlyAction(), new HitInput(3, 2, new Target("dummy"))));

        assertEquals("binder returned null contract", error.getMessage());
    }

    @Test
    void runnerRejectsNullTypeFromAction() {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(HitBinder.INSTANCE);

        NullPointerException error = assertThrows(NullPointerException.class, () ->
                runner.run(new NullTypeAction(), new HitInput(3, 2, new Target("dummy"))));

        assertEquals("action returned null type", error.getMessage());
    }

    @Test
    void runnerRejectsNullContractFromAction() {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(HitBinder.INSTANCE);

        NullPointerException error = assertThrows(NullPointerException.class, () ->
                runner.run(new NullContractAction(), new HitInput(3, 2, new Target("dummy"))));

        assertEquals("action example:null_contract returned null context contract", error.getMessage());
    }

    @Test
    void runnerReportsNullContractFromNestedActionWithParentSlot() {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(HitBinder.INSTANCE);

        NullPointerException error = assertThrows(NullPointerException.class, () ->
                runner.run(new PiSequenceAction(List.of(new NullContractAction())), new HitInput(3, 2, new Target("dummy"))));

        assertEquals("sequence child[0] action example:null_contract returned null context contract", error.getMessage());
    }

    @Test
    void contextExecutionRejectsNullTypeFromChildAction() {
        NullPointerException error = assertThrows(NullPointerException.class, () ->
                PiEngineContext.builder().build().execute(new NullTypeAction()));

        assertEquals("action returned null type", error.getMessage());
    }

    @Test
    void contextExecutionRejectsNullFrameFromChildAction() {
        NullPointerException error = assertThrows(NullPointerException.class, () ->
                PiEngineContext.builder().build().execute(new NullFrameAction()));

        assertEquals("action example:null_frame returned null frame", error.getMessage());
    }

    @Test
    void runnerExecutesAnActionFromADataSet() {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(HitBinder.INSTANCE);

        PiEngineFrame frame = runner.run(actions(), id("fire_hit"), new HitInput(3, 2, new Target("dummy")));

        assertEquals(7.0, frame.number("damage"));
    }

    @Test
    void runnerExecutesAnActionFromADataSetResourceKey() throws Exception {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(HitBinder.INSTANCE);

        PiEngineFrame frame = runner.run(actions(), resourceKey("fire_hit"), new HitInput(3, 2, new Target("dummy")));

        assertEquals(7.0, frame.number("damage"));
    }

    @Test
    void runnerCanMapFramesIntoDomainResults() {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(HitBinder.INSTANCE);

        HitResult result = runner.run(
                actions(),
                id("fire_hit"),
                new HitInput(3, 2, new Target("dummy")),
                frame -> new HitResult(frame.number("damage")));

        assertEquals(7.0, result.damage());
    }

    @Test
    void runnerCanMapFramesFromADataSetResourceKey() throws Exception {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(HitBinder.INSTANCE);

        HitResult result = runner.run(
                actions(),
                resourceKey("fire_hit"),
                new HitInput(3, 2, new Target("dummy")),
                frame -> new HitResult(frame.number("damage")));

        assertEquals(7.0, result.damage());
    }

    @Test
    void runnerReportsMissingActionIdFromADataSet() {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(HitBinder.INSTANCE);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                runner.run(actions(), id("missing"), new HitInput(3, 2, new Target("dummy"))));

        assertEquals("missing engine action example:missing in data set pidatagraph:test_actions", error.getMessage());
    }

    @Test
    void verifyAllReportsTheEntryThatDoesNotMatchTheBinderContract() {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(MissingPowerBinder.INSTANCE);

        PiDataVerificationException error = assertThrows(PiDataVerificationException.class, () ->
                runner.verifyAll(actions()));

        assertEquals("example:fire_hit/context.numbers.power", error.path());
        assertEquals("missing engine context number `power`", error.getMessage());
    }

    @Test
    void verifyAllRejectsNullTypeFromAction() {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(HitBinder.INSTANCE);

        NullPointerException error = assertThrows(NullPointerException.class, () ->
                runner.verifyAll(actionSet("broken", new NullTypeAction())));

        assertEquals("action returned null type", error.getMessage());
    }

    @Test
    void verifyAllRejectsNullContractFromAction() {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(HitBinder.INSTANCE);

        NullPointerException error = assertThrows(NullPointerException.class, () ->
                runner.verifyAll(actionSet("broken", new NullContractAction())));

        assertEquals("action example:null_contract returned null context contract", error.getMessage());
    }

    @Test
    void verifyAllWrapsUnexpectedActionVerificationFailuresWithEntryId() {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(HitBinder.INSTANCE);

        PiDataVerificationException error = assertThrows(PiDataVerificationException.class, () ->
                runner.verifyAll(actionSet("broken", new ThrowingVerifyAction())));

        assertEquals("example:broken", error.path());
        assertEquals(NullPointerException.class.getName(), error.getMessage());
    }

    @Test
    void verifyAllChecksActionContractEvenWhenActionOverridesVerify() {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(BrokenBinder.INSTANCE);

        PiDataVerificationException error = assertThrows(PiDataVerificationException.class, () ->
                runner.verifyAll(actionSet("broken", new VerifyOverrideContractAction())));

        assertEquals("example:broken/context.objects.target", error.path());
        assertEquals("missing engine context object `target` of type " + Target.class.getName(), error.getMessage());
    }

    @Test
    void defaultActionVerificationReportsNullContractWithPath() {
        NullPointerException error = assertThrows(NullPointerException.class, () ->
                new NullContractAction().verify(PiDataBuildContext.builder().build(), "spell.root"));

        assertEquals("engine action returned null context contract at spell.root", error.getMessage());
    }

    @Test
    void unkeyedRunnerCannotCreateAReloadVerifier() {
        PiEngineRunner<HitInput> runner = PiEngineRunner.actionRegistry(HitBinder.INSTANCE);

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                runner.reloadVerifier(RegistryAccess.EMPTY));

        assertEquals("this engine runner was created without a datapack registry key", error.getMessage());
    }

    private static PiDataSet<PiEngineAction> actions() {
        return actionSet("fire_hit", new TargetDamageAction());
    }

    private static PiDataSet<PiEngineAction> actionSet(String path, PiEngineAction value) {
        PiDataDefinition<PiEngineAction> definition = PiDataDefinition
                .<PiEngineAction>builder(new ResourceLocation("pidatagraph", "test_actions"), "test_actions", Codec.unit(new TargetDamageAction()))
                .verify("context", (context, action) -> action.verify(context, "context"))
                .build();
        return PiDataSet.builder(definition, "example")
                .entry(path, value)
                .build();
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("example", path);
    }

    private static ResourceKey<?> resourceKey(String path) throws Exception {
        Constructor<ResourceKey> constructor = ResourceKey.class.getDeclaredConstructor(ResourceLocation.class, ResourceLocation.class);
        constructor.setAccessible(true);
        return constructor.newInstance(id("registry"), id(path));
    }

    private enum HitBinder implements PiEngineContextBinder<HitInput> {
        INSTANCE;

        @Override
        public PiEngineContextContract contract() {
            return PiEngineContextContract.builder()
                    .number("base")
                    .number("power")
                    .object("target", Target.class)
                    .build();
        }

        @Override
        public PiEngineContext bind(HitInput input) {
            PiEngineContext.Builder builder = PiEngineContext.builder()
                    .number("base", input.base())
                    .number("power", input.power());
            if (input.target() != null) {
                builder.object("target", input.target());
            }
            return builder.build();
        }
    }

    private enum MissingPowerBinder implements PiEngineContextBinder<HitInput> {
        INSTANCE;

        @Override
        public PiEngineContextContract contract() {
            return PiEngineContextContract.builder()
                    .number("base")
                    .object("target", Target.class)
                    .build();
        }

        @Override
        public PiEngineContext bind(HitInput input) {
            return PiEngineContext.builder()
                    .number("base", input.base())
                    .object("target", input.target())
                    .build();
        }
    }

    private enum BrokenBinder implements PiEngineContextBinder<HitInput> {
        INSTANCE;

        @Override
        public PiEngineContextContract contract() {
            return PiEngineContextContract.builder()
                    .number("base")
                    .number("power")
                    .build();
        }

        @Override
        public PiEngineContext bind(HitInput input) {
            return PiEngineContext.builder()
                    .number("base", input.base())
                    .build();
        }
    }

    private enum NullBinder implements PiEngineContextBinder<HitInput> {
        INSTANCE;

        @Override
        public PiEngineContextContract contract() {
            return PiEngineContextContract.empty();
        }

        @Override
        public PiEngineContext bind(HitInput input) {
            return null;
        }
    }

    private enum NullContractBinder implements PiEngineContextBinder<HitInput> {
        INSTANCE;

        @Override
        public PiEngineContextContract contract() {
            return null;
        }

        @Override
        public PiEngineContext bind(HitInput input) {
            return PiEngineContext.builder().build();
        }
    }

    private record HitInput(double base, double power, Target target) {
    }

    private record Target(String id) {
    }

    private record HitResult(double damage) {
    }

    private static final class HitKeys {
        private static final org.pickaid.pidatagraph.engine.context.PiEngineNumberKey BASE =
                org.pickaid.pidatagraph.engine.context.PiEngineNumberKey.of("base");
        private static final org.pickaid.pidatagraph.engine.context.PiEngineNumberKey POWER =
                org.pickaid.pidatagraph.engine.context.PiEngineNumberKey.of("power");
        private static final org.pickaid.pidatagraph.engine.context.PiEngineContextKey<Target> TARGET =
                org.pickaid.pidatagraph.engine.context.PiEngineContextKey.of("target", Target.class);
    }

    private record BaseOnlyAction() implements PiEngineAction {
        private static final PiEngineActionType<BaseOnlyAction> TYPE = PiEngineActionType.of(
                id("base_only"),
                ignored -> Codec.unit(new BaseOnlyAction())
        );

        @Override
        public PiEngineActionType<?> type() {
            return TYPE;
        }

        @Override
        public PiEngineFrame execute(PiEngineContext context) {
            return PiEngineFrame.builder().number("observed", context.number("base")).build();
        }

        @Override
        public PiEngineContextContract contextContract() {
            return PiEngineContextContract.builder().number("base").build();
        }
    }

    private record NullFrameAction() implements PiEngineAction {
        private static final PiEngineActionType<NullFrameAction> TYPE = PiEngineActionType.of(
                id("null_frame"),
                ignored -> Codec.unit(new NullFrameAction())
        );

        @Override
        public PiEngineActionType<?> type() {
            return TYPE;
        }

        @Override
        public PiEngineFrame execute(PiEngineContext context) {
            return null;
        }
    }

    private record NullTypeAction() implements PiEngineAction {
        @Override
        public PiEngineActionType<?> type() {
            return null;
        }

        @Override
        public PiEngineFrame execute(PiEngineContext context) {
            return PiEngineFrame.empty();
        }
    }

    private record NullContractAction() implements PiEngineAction {
        private static final PiEngineActionType<NullContractAction> TYPE = PiEngineActionType.of(
                id("null_contract"),
                ignored -> Codec.unit(new NullContractAction())
        );

        @Override
        public PiEngineActionType<?> type() {
            return TYPE;
        }

        @Override
        public PiEngineFrame execute(PiEngineContext context) {
            return PiEngineFrame.empty();
        }

        @Override
        public PiEngineContextContract contextContract() {
            return null;
        }
    }

    private record ExtraObjectAction() implements PiEngineAction {
        private static final PiEngineActionType<ExtraObjectAction> TYPE = PiEngineActionType.of(
                id("extra_object"),
                ignored -> Codec.unit(new ExtraObjectAction())
        );

        @Override
        public PiEngineActionType<?> type() {
            return TYPE;
        }

        @Override
        public PiEngineFrame execute(PiEngineContext context) {
            return PiEngineFrame.empty();
        }

        @Override
        public PiEngineContextContract contextContract() {
            return PiEngineContextContract.builder()
                    .object("source", Target.class)
                    .build();
        }
    }

    private record ThrowingVerifyAction() implements PiEngineAction {
        private static final PiEngineActionType<ThrowingVerifyAction> TYPE = PiEngineActionType.of(
                id("throwing_verify"),
                ignored -> Codec.unit(new ThrowingVerifyAction())
        );

        @Override
        public PiEngineActionType<?> type() {
            return TYPE;
        }

        @Override
        public PiEngineFrame execute(PiEngineContext context) {
            return PiEngineFrame.empty();
        }

        @Override
        public void verify(PiDataBuildContext context, String path) {
            throw new NullPointerException();
        }
    }

    private record VerifyOverrideContractAction() implements PiEngineAction {
        private static final PiEngineActionType<VerifyOverrideContractAction> TYPE = PiEngineActionType.of(
                id("verify_override_contract"),
                ignored -> Codec.unit(new VerifyOverrideContractAction())
        );

        @Override
        public PiEngineActionType<?> type() {
            return TYPE;
        }

        @Override
        public PiEngineFrame execute(PiEngineContext context) {
            return PiEngineFrame.empty();
        }

        @Override
        public PiEngineContextContract contextContract() {
            return PiEngineContextContract.builder().object("target", Target.class).build();
        }

        @Override
        public void verify(PiDataBuildContext context, String path) {
        }
    }

    private record TargetDamageAction() implements PiEngineAction {
        private static final PiEngineActionType<TargetDamageAction> TYPE = PiEngineActionType.of(
                id("target_damage"),
                ignored -> Codec.unit(new TargetDamageAction())
        );

        @Override
        public PiEngineActionType<?> type() {
            return TYPE;
        }

        @Override
        public PiEngineFrame execute(PiEngineContext context) {
            context.object("target", Target.class).orElseThrow();
            return PiEngineFrame.builder()
                    .number("damage", context.number("base") + context.number("power") * 2)
                    .build();
        }

        @Override
        public PiEngineContextContract contextContract() {
            return PiEngineContextContract.builder()
                    .number("base")
                    .number("power")
                    .object("target", Target.class)
                    .build();
        }
    }
}
