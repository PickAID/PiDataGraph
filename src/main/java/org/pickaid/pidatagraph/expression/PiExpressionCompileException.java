package org.pickaid.pidatagraph.expression;

public class PiExpressionCompileException extends IllegalArgumentException {
    private final String source;
    private final PiExpressionScope scope;

    public PiExpressionCompileException(String source, PiExpressionScope scope, Throwable cause) {
        super("invalid expression `" + source + "` for variables " + scope.variables(), cause);
        this.source = source;
        this.scope = scope;
    }

    public String source() {
        return source;
    }

    public PiExpressionScope scope() {
        return scope;
    }
}
