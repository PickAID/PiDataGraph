package org.pickaid.pidatagraph.engine.action;

import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.data.PiDataVerificationException;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.engine.context.PiEngineKeyNames;
import org.pickaid.pidatagraph.expression.PiBooleanExpression;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;
import org.pickaid.pidatagraph.expression.PiIntExpression;

public final class PiEngineActions {
    public static final PiEngineActionType<PiSequenceAction> SEQUENCE = PiEngineActionType.of(
            id("sequence"),
            PiSequenceAction::codec
    );
    public static final PiEngineActionType<PiNoopAction> NOOP = PiEngineActionType.of(
            id("noop"),
            ignored -> PiNoopAction.CODEC
    );
    public static final PiEngineActionType<PiFailAction> FAIL = PiEngineActionType.of(
            id("fail"),
            ignored -> PiFailAction.CODEC
    );
    public static final PiEngineActionType<PiIfAction> IF = PiEngineActionType.of(
            id("if"),
            PiIfAction::codec
    );
    public static final PiEngineActionType<PiRepeatAction> REPEAT = PiEngineActionType.of(
            id("repeat"),
            PiRepeatAction::codec
    );
    public static final PiEngineActionType<PiWithNumberAction> WITH_NUMBER = PiEngineActionType.of(
            id("with_number"),
            PiWithNumberAction::codec
    );
    public static final PiEngineActionType<PiWithContextAction> WITH_CONTEXT = PiEngineActionType.of(
            id("with_context"),
            PiWithContextAction::codec
    );
    public static final PiEngineActionType<PiGuardAction> GUARD = PiEngineActionType.of(
            id("guard"),
            PiGuardAction::codec
    );
    public static final PiEngineActionType<PiForEachObjectAction> FOR_EACH_OBJECT = PiEngineActionType.of(
            id("for_each_object"),
            PiForEachObjectAction::codec
    );
    public static final PiEngineActionType<PiEmitNumberAction> EMIT_NUMBER = PiEngineActionType.of(
            id("emit_number"),
            PiEmitNumberAction::codec
    );
    public static final PiEngineActionType<PiEmitRandomNumberAction> EMIT_RANDOM_NUMBER = PiEngineActionType.of(
            id("emit_random_number"),
            ignored -> PiEmitRandomNumberAction.CODEC
    );
    public static final PiEngineActionType<PiEmitFlagAction> EMIT_FLAG = PiEngineActionType.of(
            id("emit_flag"),
            PiEmitFlagAction::codec
    );
    public static final PiEngineActionType<PiEmitObjectAction> EMIT_OBJECT = PiEngineActionType.of(
            id("emit_object"),
            PiEmitObjectAction::codec
    );
    public static final PiEngineActionType<PiSelectObjectAction> SELECT_OBJECT = PiEngineActionType.of(
            id("select_object"),
            ignored -> PiSelectObjectAction.CODEC
    );

    private PiEngineActions() {
    }

    public static List<PiEngineActionType<? extends PiEngineAction>> core() {
        return List.of(
                SEQUENCE, NOOP, FAIL, IF, REPEAT, WITH_NUMBER, WITH_CONTEXT, GUARD, FOR_EACH_OBJECT,
                EMIT_NUMBER, EMIT_RANDOM_NUMBER, EMIT_FLAG, EMIT_OBJECT, SELECT_OBJECT);
    }

    public static void verify(String path, PiDataBuildContext context, PiDoubleExpression expression) {
        try {
            expression.compile(context.expressionLanguage(), context.expressionScope());
        } catch (RuntimeException error) {
            throw new PiDataVerificationException(path, error);
        }
    }

    public static void verify(String path, PiDataBuildContext context, PiIntExpression expression) {
        try {
            expression.compile(context.expressionLanguage(), context.expressionScope());
        } catch (RuntimeException error) {
            throw new PiDataVerificationException(path, error);
        }
    }

    public static void verify(String path, PiDataBuildContext context, PiBooleanExpression expression) {
        try {
            expression.compile(context.expressionLanguage(), context.expressionScope());
        } catch (RuntimeException error) {
            throw new PiDataVerificationException(path, error);
        }
    }

    public static void verifyNumber(String path, PiDataBuildContext context, String key) {
        if (!context.hasVariable(key)) {
            throw new PiDataVerificationException(path, "missing engine context number `" + key + "`");
        }
    }

    public static void verifyObject(String path, PiDataBuildContext context, String key, Class<?> type) {
        if (!context.hasObject(key)) {
            throw new PiDataVerificationException(path,
                    "missing engine context object `" + key + "` of type " + type.getName());
        }
        if (!context.hasObject(key, type)) {
            throw new PiDataVerificationException(path,
                    "engine context object `" + key + "` must be " + type.getName()
                            + ", but validation context provides " + context.objectType(key).orElseThrow().getName());
        }
    }

    public static PiEngineContextContract contextContract(String owner, PiEngineAction action) {
        String checkedOwner = Objects.requireNonNull(owner, "owner");
        PiEngineAction checkedAction = Objects.requireNonNull(action, checkedOwner + " action");
        PiEngineActionType<?> type = Objects.requireNonNull(checkedAction.type(), checkedOwner + " action returned null type");
        return Objects.requireNonNull(
                checkedAction.contextContract(),
                checkedOwner + " action " + type.id() + " returned null context contract"
        );
    }

    public static String checkVariableName(String variable) {
        return PiEngineKeyNames.variable(variable);
    }

    public static String checkFrameValueName(String name) {
        return PiEngineKeyNames.frameValue(name);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("pidatagraph", path);
    }
}
