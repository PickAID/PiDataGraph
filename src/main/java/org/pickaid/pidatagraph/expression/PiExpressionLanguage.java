package org.pickaid.pidatagraph.expression;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import net.objecthunter.exp4j.operator.Operator;

public final class PiExpressionLanguage {
    private final List<FunctionSpec> functions;
    private final List<Operator> operators;

    private PiExpressionLanguage(List<FunctionSpec> functions, List<Operator> operators) {
        this.functions = List.copyOf(functions);
        this.operators = List.copyOf(operators);
    }

    public static PiExpressionLanguage standard() {
        return standardBuilder().build();
    }

    public static Builder standardBuilder() {
        return builder()
                .function("min", 2, (context, args) -> Math.min(args[0], args[1]))
                .function("max", 2, (context, args) -> Math.max(args[0], args[1]))
                .function("clamp", 3, (context, args) -> Math.max(args[1], Math.min(args[2], args[0])))
                .function("abs", 1, (context, args) -> Math.abs(args[0]))
                .function("floor", 1, (context, args) -> Math.floor(args[0]))
                .function("ceil", 1, (context, args) -> Math.ceil(args[0]))
                .function("round", 1, (context, args) -> Math.rint(args[0]))
                .function("sqrt", 1, (context, args) -> Math.sqrt(args[0]))
                .function("pow", 2, (context, args) -> Math.pow(args[0], args[1]))
                .function("sin", 1, (context, args) -> Math.sin(args[0]))
                .function("cos", 1, (context, args) -> Math.cos(args[0]))
                .function("tan", 1, (context, args) -> Math.tan(args[0]))
                .function("rand", 2, (context, args) -> context.random() * (args[1] - args[0]) + args[0])
                .operator("<", 100, false, (left, right) -> bool(left < right))
                .operator("<=", 100, false, (left, right) -> bool(left <= right))
                .operator(">", 100, false, (left, right) -> bool(left > right))
                .operator(">=", 100, false, (left, right) -> bool(left >= right))
                .operator("==", 100, false, (left, right) -> bool(left == right))
                .operator("!=", 100, false, (left, right) -> bool(left != right))
                .operator("&", 70, false, (left, right) -> bool(truthy(left) && truthy(right)))
                .operator("|", 50, false, (left, right) -> bool(truthy(left) || truthy(right)));
    }

    public static Builder builder() {
        return new Builder();
    }

    public PiCompiledExpression compile(String source, PiExpressionScope scope) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(scope, "scope");
        ThreadLocal<PiExpressionContext> activeContext = new ThreadLocal<>();
        try {
            ExpressionBuilder builder = new ExpressionBuilder(source)
                    .variables(scope.variables());
            for (FunctionSpec function : functions) {
                builder.function(function.toExp4j(activeContext));
            }
            for (Operator operator : operators) {
                builder.operator(operator);
            }
            return new PiCompiledExpression(source, scope, builder.build(), activeContext);
        } catch (RuntimeException error) {
            throw new PiExpressionCompileException(source, scope, error);
        }
    }

    private static boolean truthy(double value) {
        return value > 0.5;
    }

    private static double bool(boolean value) {
        return value ? 1 : 0;
    }

    public static final class Builder {
        private final List<FunctionSpec> functions = new ArrayList<>();
        private final List<Operator> operators = new ArrayList<>();
        private final Set<String> functionNames = new LinkedHashSet<>();
        private final Set<String> operatorSymbols = new LinkedHashSet<>();

        public Builder function(String name, int arguments, PiExpressionFunction function) {
            FunctionSpec spec = new FunctionSpec(name, arguments, function);
            if (!functionNames.add(spec.name())) {
                throw new IllegalArgumentException("duplicate expression function: " + spec.name());
            }
            functions.add(spec);
            return this;
        }

        public Builder operator(String symbol, int precedence, boolean leftAssociative, PiExpressionBinaryOperator operator) {
            String checkedSymbol = Objects.requireNonNull(symbol, "symbol");
            if (!operatorSymbols.add(checkedSymbol)) {
                throw new IllegalArgumentException("duplicate expression operator: " + checkedSymbol);
            }
            operators.add(new Operator(checkedSymbol, 2, leftAssociative, precedence) {
                @Override
                public double apply(double... values) {
                    return operator.apply(values[0], values[1]);
                }
            });
            return this;
        }

        public PiExpressionLanguage build() {
            return new PiExpressionLanguage(functions, operators);
        }
    }

    private record FunctionSpec(String name, int arguments, PiExpressionFunction function) {
        private FunctionSpec {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(function, "function");
            if (arguments < 0) {
                throw new IllegalArgumentException("function arguments must be >= 0");
            }
        }

        private Function toExp4j(ThreadLocal<PiExpressionContext> activeContext) {
            return new Function(name, arguments) {
                @Override
                public double apply(double... args) {
                    PiExpressionContext context = activeContext.get();
                    if (context == null) {
                        throw new IllegalStateException("expression context is not active");
                    }
                    return function.apply(context, args);
                }
            };
        }
    }
}
