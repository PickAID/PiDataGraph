package org.pickaid.pidatagraph.engine;

import java.util.Objects;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.expression.PiExpressionScope;

/**
 * Converts one domain input object into an engine context.
 */
public interface PiEngineContextBinder<T> {
    PiEngineContextContract contract();

    PiEngineContext bind(T input);

    default PiDataBuildContext validationContext() {
        PiEngineContextContract contract = Objects.requireNonNull(contract(), "binder returned null contract");
        return PiDataBuildContext.builder()
                .expressionScope(PiExpressionScope.builder()
                        .variables(contract.numbers())
                        .build())
                .objects(contract.objects())
                .build();
    }
}
