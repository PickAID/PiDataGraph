package org.pickaid.pidatagraph.engine.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.PiEngineFrame;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;

public final class PiForEachObjectAction implements PiEngineAction {
    private final String list;
    private final String item;
    private final String index;
    private final PiEngineAction child;

    public PiForEachObjectAction(String list, String item, String index, PiEngineAction child) {
        this.list = PiEngineActions.checkVariableName(Objects.requireNonNull(list, "list"));
        this.item = PiEngineActions.checkVariableName(Objects.requireNonNull(item, "item"));
        this.index = index == null || index.isBlank() ? "" : PiEngineActions.checkVariableName(index);
        this.child = Objects.requireNonNull(child, "child");
    }

    static Codec<PiForEachObjectAction> codec(Codec<PiEngineAction> actionCodec) {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("list").forGetter(PiForEachObjectAction::list),
                Codec.STRING.fieldOf("item").forGetter(PiForEachObjectAction::item),
                Codec.STRING.optionalFieldOf("index", "").forGetter(PiForEachObjectAction::index),
                actionCodec.fieldOf("child").forGetter(PiForEachObjectAction::child)
        ).apply(instance, PiForEachObjectAction::new));
    }

    public String list() {
        return list;
    }

    public String item() {
        return item;
    }

    public String index() {
        return index;
    }

    public PiEngineAction child() {
        return child;
    }

    @Override
    public PiEngineActionType<?> type() {
        return PiEngineActions.FOR_EACH_OBJECT;
    }

    @Override
    public PiEngineFrame execute(PiEngineContext context) {
        Object raw = context.object(list).orElseThrow(() -> new IllegalArgumentException(
                "missing engine context object list: " + list
        ));
        if (!(raw instanceof Iterable<?> iterable)) {
            throw new ClassCastException("engine context object `" + list + "` is " + raw.getClass().getName() + ", not java.lang.Iterable");
        }
        PiEngineFrame frame = PiEngineFrame.empty();
        int i = 0;
        for (Object value : iterable) {
            PiEngineContext.Builder builder = context.derive().object(item, value);
            if (!index.isEmpty()) {
                builder.number(index, i);
            }
            frame = frame.merge(builder.build().execute(child));
            i++;
        }
        return frame;
    }

    @Override
    public PiEngineContextContract contextContract() {
        PiEngineContextContract childContract = PiEngineActions.contextContract("for_each_object child", child).withoutObject(item);
        if (!index.isEmpty()) {
            childContract = childContract.withoutNumber(index);
        }
        return PiEngineContextContract.builder()
                .merge(childContract)
                .object(list, Iterable.class)
                .build();
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        PiEngineActions.verifyObject(path + ".list", context, list, Iterable.class);
        PiDataBuildContext childContext = context.withObject(item, Object.class);
        if (!index.isEmpty()) {
            childContext = childContext.withVariable(index);
        }
        child.verify(childContext, path + ".child");
    }
}
