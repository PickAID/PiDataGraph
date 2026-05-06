package org.pickaid.pidatagraph.processor;

import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Component;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Output;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.OutputComponent;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.OutputKind;

final class PiDataGraphFacadeKeyWriter {
    private PiDataGraphFacadeKeyWriter() {
    }

    static void write(StringBuilder source, String glueName, PiDataGraphProcessorModel.Input input, Output output) {
        source.append("    public static final class Context {\n");
        source.append("        private Context() {\n");
        source.append("        }\n\n");
        for (Component component : input.components()) {
            String type = component.number()
                    ? "PiEngineNumberKey"
                    : "PiEngineContextKey<" + component.type() + ">";
            source.append("        public static final ").append(type).append(" ")
                    .append(constant(component.key())).append(" = ").append(glueName).append(".")
                    .append(inputKey(input, component)).append(";\n");
        }
        source.append("    }\n\n");
        if (output == null) {
            return;
        }
        source.append("    public static final class Output {\n");
        source.append("        private Output() {\n");
        source.append("        }\n\n");
        for (OutputComponent component : output.components()) {
            source.append("        public static final ").append(outputType(component)).append(" ")
                    .append(constant(component.key())).append(" = ").append(glueName).append(".")
                    .append(outputKey(output, component)).append(";\n");
        }
        source.append("    }\n\n");
    }

    private static String outputType(OutputComponent component) {
        if (component.kind() == OutputKind.FLAG) {
            return "PiEngineValueKey<Boolean>";
        }
        if (component.kind() == OutputKind.NUMBER) {
            return "PiEngineValueKey<Number>";
        }
        return "PiEngineValueKey<" + component.type() + ">";
    }

    private static String inputKey(PiDataGraphProcessorModel.Input input, Component component) {
        return constant(input.name()) + "_" + constant(component.key());
    }

    private static String outputKey(Output output, OutputComponent component) {
        return constant(output.name()) + "_" + constant(component.key());
    }

    private static String constant(String name) {
        return PiDataGraphGeneratedNames.inputPrefix(name);
    }
}
