package org.pickaid.pidatagraph.engine;

import java.util.Objects;
import org.pickaid.pidatagraph.data.PiDataDefinition;
import org.pickaid.pidatagraph.data.PiDataEntry;
import org.pickaid.pidatagraph.expression.PiExpressionScope;

public final class PiEngineContentType<S, R> {
    private final PiDataDefinition<S> definition;
    private final PiExpressionScope scope;
    private final PiEngineContentCompiler<S, R> compiler;

    private PiEngineContentType(
            PiDataDefinition<S> definition,
            PiExpressionScope scope,
            PiEngineContentCompiler<S, R> compiler
    ) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.compiler = Objects.requireNonNull(compiler, "compiler");
    }

    public static <S, R> Builder<S, R> builder(PiDataDefinition<S> definition, PiEngineContentCompiler<S, R> compiler) {
        return new Builder<>(definition, compiler);
    }

    public PiDataDefinition<S> definition() {
        return definition;
    }

    public PiExpressionScope scope() {
        return scope;
    }

    R compile(PiDataEntry<S> entry, PiEngineBuildContext context) {
        return compiler.compile(entry, context);
    }

    public static final class Builder<S, R> {
        private final PiDataDefinition<S> definition;
        private final PiEngineContentCompiler<S, R> compiler;
        private PiExpressionScope scope = PiExpressionScope.empty();

        private Builder(PiDataDefinition<S> definition, PiEngineContentCompiler<S, R> compiler) {
            this.definition = Objects.requireNonNull(definition, "definition");
            this.compiler = Objects.requireNonNull(compiler, "compiler");
        }

        public Builder<S, R> scope(PiExpressionScope scope) {
            this.scope = Objects.requireNonNull(scope, "scope");
            return this;
        }

        public PiEngineContentType<S, R> build() {
            return new PiEngineContentType<>(definition, scope, compiler);
        }
    }
}
