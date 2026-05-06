package org.pickaid.pidatagraph.engine.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.PiEngineContext;
import org.pickaid.pidatagraph.engine.PiEngineFrame;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;
import org.pickaid.pidatagraph.engine.context.PiEngineValueKey;
import org.pickaid.pidatagraph.expression.PiIntExpression;

public final class PiSelectObjectAction implements PiEngineAction {
    static final Codec<PiSelectObjectAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(PiSelectObjectAction::name),
            Codec.STRING.fieldOf("list").forGetter(PiSelectObjectAction::list),
            PiIntExpression.CODEC.fieldOf("index").forGetter(PiSelectObjectAction::index)
    ).apply(instance, PiSelectObjectAction::new));

    private final String name;
    private final String list;
    private final PiIntExpression index;

    public PiSelectObjectAction(String name, String list, PiIntExpression index) {
        this.name = PiEngineActions.checkFrameValueName(Objects.requireNonNull(name, "name"));
        this.list = PiEngineActions.checkVariableName(Objects.requireNonNull(list, "list"));
        this.index = Objects.requireNonNull(index, "index");
    }

    public <T> PiSelectObjectAction(PiEngineValueKey<T> name, PiEngineContextKey<? extends Iterable> list, PiIntExpression index) {
        this(Objects.requireNonNull(name, "name").name(), Objects.requireNonNull(list, "list").name(), index);
    }

    public String name() {
        return name;
    }

    public String list() {
        return list;
    }

    public PiIntExpression index() {
        return index;
    }

    @Override
    public PiEngineActionType<?> type() {
        return PiEngineActions.SELECT_OBJECT;
    }

    @Override
    public PiEngineFrame execute(PiEngineContext context) {
        Iterable<?> values = context.requireObject(list, Iterable.class);
        int selected = context.evaluate(index);
        Object value = select(values, selected);
        return PiEngineFrame.builder().object(name, value).build();
    }

    @Override
    public PiEngineContextContract contextContract() {
        return PiEngineContextContract.builder().object(list, Iterable.class).build();
    }

    @Override
    public void verify(PiDataBuildContext context, String path) {
        PiEngineActions.verifyObject(path + ".list", context, list, Iterable.class);
        PiEngineActions.verify(path + ".index", context, index);
    }

    private Object select(Iterable<?> values, int index) {
        if (index < 0) {
            throw outOfRange(index);
        }
        if (values instanceof List<?> listValues) {
            if (index >= listValues.size()) {
                throw outOfRange(index);
            }
            return Objects.requireNonNull(listValues.get(index), "engine object list `" + list + "` contains null at index " + index);
        }
        int current = 0;
        for (Object value : values) {
            if (current == index) {
                return Objects.requireNonNull(value, "engine object list `" + list + "` contains null at index " + index);
            }
            current++;
        }
        throw outOfRange(index);
    }

    private IndexOutOfBoundsException outOfRange(int index) {
        return new IndexOutOfBoundsException("engine object list `" + list + "` has no element at index " + index);
    }
}
