package org.pickaid.pidatagraph.graphcontext;

import java.util.Objects;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.expression.PiExpressionScope;

/**
 * Converts one typed Java input object into a graph context.
 */
public interface PiGraphContextBinder<T> {
    PiGraphContextSchema schema();

    PiGraphContext bind(T input);

    default PiDataBuildContext validationContext() {
        PiGraphContextSchema schema = Objects.requireNonNull(schema(), "binder returned null schema");
        return PiDataBuildContext.builder()
                .expressionScope(PiExpressionScope.builder()
                        .variables(schema.numbers())
                        .build())
                .objects(schema.objects())
                .build();
    }
}
