package org.pickaid.pidatagraph.engine.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.PiEngineFrame;
import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.engine.context.PiEngineNumberKey;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;

public final class PiWithContextAction implements PiEngineAction {
    private static final Codec<Map<String, PiDoubleExpression>> NUMBERS_CODEC =
            Codec.unboundedMap(Codec.STRING, PiDoubleExpression.CODEC);
    private static final Codec<Map<String, String>> OBJECTS_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING);

    private final Map<String, PiDoubleExpression> numbers;
    private final Map<String, String> objects;
    private final PiEngineAction child;

    public PiWithContextAction(Map<String, PiDoubleExpression> numbers, Map<String, String> objects, PiEngineAction child) {
        this.numbers = checkNumbers(numbers);
        this.objects = checkObjects(objects);
        this.child = Objects.requireNonNull(child, "child");
    }

    public static Builder builder(PiEngineAction child) {
        return new Builder(child);
    }

    static Codec<PiWithContextAction> codec(Codec<PiEngineAction> actionCodec) {
        return RecordCodecBuilder.create(instance -> instance.group(
                NUMBERS_CODEC.optionalFieldOf("numbers", Map.of()).forGetter(PiWithContextAction::numbers),
                OBJECTS_CODEC.optionalFieldOf("objects", Map.of()).forGetter(PiWithContextAction::objects),
                actionCodec.fieldOf("child").forGetter(PiWithContextAction::child)
        ).apply(instance, PiWithContextAction::new));
    }

    public Map<String, PiDoubleExpression> numbers() {
        return numbers;
    }

    public Map<String, String> objects() {
        return objects;
    }

    public PiEngineAction child() {
        return child;
    }

    @Override
    public PiEngineActionType<?> type() {
        return PiEngineActions.WITH_CONTEXT;
    }

    @Override
    public PiEngineFrame execute(PiEngineContext context) {
        PiEngineContext.Builder builder = context.derive();
        for (Map.Entry<String, PiDoubleExpression> entry : numbers.entrySet()) {
            builder.number(entry.getKey(), context.evaluate(entry.getValue()));
        }
        for (Map.Entry<String, String> entry : objects.entrySet()) {
            Object value = context.requireObject(entry.getValue(), Object.class);
            builder.object(entry.getKey(), value);
        }
        return builder.build().execute(child);
    }

    @Override
    public PiEngineContextContract contextContract() {
        PiEngineContextContract childContract = PiEngineActions.contextContract("with_context child", child);
        PiEngineContextContract.Builder builder = PiEngineContextContract.builder()
                .numbers(childContract.withoutNumbers(numbers.keySet()).numbers());

        for (Map.Entry<String, Class<?>> entry : childContract.objects().entrySet()) {
            String source = objects.get(entry.getKey());
            if (source == null) {
                builder.object(entry.getKey(), entry.getValue());
            } else {
                builder.object(source, entry.getValue());
            }
        }
        for (String source : objects.values()) {
            builder.object(source, Object.class);
        }
        return builder.build();
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        PiDataBuildContext childContext = context;
        for (Map.Entry<String, PiDoubleExpression> entry : numbers.entrySet()) {
            PiEngineActions.verify(path + ".numbers." + entry.getKey(), context, entry.getValue());
            childContext = childContext.withVariable(entry.getKey());
        }
        PiEngineContextContract childContract = PiEngineActions.contextContract("with_context child", child);
        for (Map.Entry<String, String> entry : objects.entrySet()) {
            Class<?> requiredType = childContract.objectType(entry.getKey()).orElse(Object.class);
            PiEngineActions.verifyObject(path + ".objects." + entry.getKey(), context, entry.getValue(), requiredType);
            childContext = childContext.withObject(entry.getKey(), requiredType);
        }
        child.verify(childContext, path + ".child");
    }

    private static Map<String, PiDoubleExpression> checkNumbers(Map<String, PiDoubleExpression> values) {
        LinkedHashMap<String, PiDoubleExpression> checked = new LinkedHashMap<>();
        Objects.requireNonNull(values, "numbers").forEach((key, value) ->
                checked.put(PiEngineActions.checkVariableName(key), Objects.requireNonNull(value, "number value"))
        );
        return Map.copyOf(checked);
    }

    private static Map<String, String> checkObjects(Map<String, String> values) {
        LinkedHashMap<String, String> checked = new LinkedHashMap<>();
        Objects.requireNonNull(values, "objects").forEach((key, value) ->
                checked.put(
                        PiEngineActions.checkVariableName(key),
                        PiEngineActions.checkVariableName(Objects.requireNonNull(value, "object source"))
                )
        );
        return Map.copyOf(checked);
    }

    public static final class Builder {
        private final PiEngineAction child;
        private final LinkedHashMap<String, PiDoubleExpression> numbers = new LinkedHashMap<>();
        private final LinkedHashMap<String, String> objects = new LinkedHashMap<>();

        private Builder(PiEngineAction child) {
            this.child = Objects.requireNonNull(child, "child");
        }

        public Builder number(String name, PiDoubleExpression value) {
            numbers.put(PiEngineActions.checkVariableName(Objects.requireNonNull(name, "name")),
                    Objects.requireNonNull(value, "value"));
            return this;
        }

        public Builder number(PiEngineNumberKey name, PiDoubleExpression value) {
            return number(Objects.requireNonNull(name, "name").name(), value);
        }

        public Builder object(String name, String source) {
            objects.put(
                    PiEngineActions.checkVariableName(Objects.requireNonNull(name, "name")),
                    PiEngineActions.checkVariableName(Objects.requireNonNull(source, "source")));
            return this;
        }

        public Builder object(PiEngineContextKey<?> name, PiEngineContextKey<?> source) {
            return object(
                    Objects.requireNonNull(name, "name").name(),
                    Objects.requireNonNull(source, "source").name());
        }

        public PiWithContextAction build() {
            return new PiWithContextAction(numbers, objects, child);
        }
    }
}
