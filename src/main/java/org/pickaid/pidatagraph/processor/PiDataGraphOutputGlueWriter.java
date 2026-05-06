package org.pickaid.pidatagraph.processor;

import java.util.List;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Output;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.OutputComponent;

final class PiDataGraphOutputGlueWriter {
    private PiDataGraphOutputGlueWriter() {
    }

    static void write(StringBuilder source, List<Output> outputs) {
        for (Output output : outputs) {
            writeKeys(source, output);
            writeMapper(source, output);
        }
    }

    private static void writeKeys(StringBuilder source, Output output) {
        for (OutputComponent component : output.components()) {
            String name = keyName(output, component);
            switch (component.kind()) {
                case NUMBER -> source.append("    public static final PiEngineValueKey<Number> ").append(name)
                        .append(" = PiEngineValueKey.number(\"").append(escape(component.key())).append("\");\n");
                case FLAG -> source.append("    public static final PiEngineValueKey<Boolean> ").append(name)
                        .append(" = PiEngineValueKey.flag(\"").append(escape(component.key())).append("\");\n");
                case OBJECT -> source.append("    public static final PiEngineValueKey<").append(component.type()).append("> ")
                        .append(name).append(" = PiEngineValueKey.object(\"").append(escape(component.key()))
                        .append("\", ").append(component.type()).append(".class);\n");
            }
        }
        source.append("\n");
    }

    private static void writeMapper(StringBuilder source, Output output) {
        String prefix = constant(output.name());
        source.append("    public static ").append(output.element().getQualifiedName())
                .append(" ").append(prefix).append("_OUTPUT(PiEngineFrame frame) {\n");
        source.append("        java.util.Objects.requireNonNull(frame, \"frame\");\n");
        source.append("        return new ").append(output.element().getQualifiedName()).append("(");
        if (!output.components().isEmpty()) {
            source.append("\n");
            for (int index = 0; index < output.components().size(); index++) {
                OutputComponent component = output.components().get(index);
                source.append("                ").append(frameValue(keyName(output, component), component));
                source.append(index + 1 == output.components().size() ? ");\n" : ",\n");
            }
        } else {
            source.append(");\n");
        }
        source.append("    }\n\n");
    }

    private static String frameValue(String keyName, OutputComponent component) {
        return switch (component.kind()) {
            case NUMBER -> numberValue(keyName, component.type());
            case FLAG -> "frame.flag(" + keyName + ")";
            case OBJECT -> "frame.value(" + keyName + ")";
        };
    }

    private static String numberValue(String keyName, String type) {
        return switch (type) {
            case "byte", "java.lang.Byte" -> "(byte) frame.value(" + keyName + ").intValue()";
            case "short", "java.lang.Short" -> "(short) frame.value(" + keyName + ").intValue()";
            case "int", "java.lang.Integer" -> "frame.value(" + keyName + ").intValue()";
            case "long", "java.lang.Long" -> "frame.value(" + keyName + ").longValue()";
            case "float", "java.lang.Float" -> "frame.value(" + keyName + ").floatValue()";
            case "double", "java.lang.Double" -> "frame.value(" + keyName + ").doubleValue()";
            case "java.lang.Number" -> "frame.value(" + keyName + ")";
            default -> "(" + type + ") frame.value(" + keyName + ")";
        };
    }

    private static String keyName(Output output, OutputComponent component) {
        return constant(output.name()) + "_" + constant(component.key());
    }

    private static String constant(String name) {
        return PiDataGraphGeneratedNames.inputPrefix(name);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
