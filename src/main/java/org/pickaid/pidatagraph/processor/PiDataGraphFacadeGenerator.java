package org.pickaid.pidatagraph.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Input;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Output;

final class PiDataGraphFacadeGenerator {
    private final ProcessingEnvironment environment;
    private final Set<String> generated;

    PiDataGraphFacadeGenerator(ProcessingEnvironment environment, Set<String> generated) {
        this.environment = environment;
        this.generated = generated;
    }

    void generate(TypeElement module, String modid, String glueName, List<Input> inputs, List<Output> outputs) {
        String packageName = environment.getElementUtils().getPackageOf(module).getQualifiedName().toString();
        for (Input input : inputs) {
            if (input.facadeName().isEmpty()) {
                continue;
            }
            writeFacade(packageName, module, modid, glueName, input, outputFor(input, outputs));
        }
    }

    private void writeFacade(String packageName, TypeElement module, String modid, String glueName, Input input, Output output) {
        String qualifiedName = packageName.isEmpty() ? input.facadeName() : packageName + "." + input.facadeName();
        if (!generated.add(qualifiedName)) {
            return;
        }
        try {
            JavaFileObject file = environment.getFiler().createSourceFile(qualifiedName, module, input.element());
            try (Writer writer = file.openWriter()) {
                writer.write(source(packageName, modid, glueName, input, output));
            }
        } catch (IOException error) {
            environment.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "failed to generate PiDataGraph facade `" + qualifiedName + "`; check for an existing class with that name",
                    input.element());
        }
    }

    private String source(String packageName, String modid, String glueName, Input input, Output output) {
        StringBuilder source = new StringBuilder();
        if (!packageName.isEmpty()) {
            source.append("package ").append(packageName).append(";\n\n");
        }
        source.append("""
                import net.minecraft.core.RegistryAccess;
                import net.minecraft.resources.ResourceKey;
                import net.minecraft.resources.ResourceLocation;
                import net.minecraftforge.event.AddReloadListenerEvent;
                import net.minecraftforge.registries.DataPackRegistryEvent;
                import org.pickaid.pidatagraph.data.PiDataBuildContext;
                import org.pickaid.pidatagraph.data.PiDataPackRegistries;
                import org.pickaid.pidatagraph.data.PiDataPackSync;
                import org.pickaid.pidatagraph.data.PiDataSet;
                import org.pickaid.pidatagraph.engine.PiEngineFrame;
                import org.pickaid.pidatagraph.engine.action.PiEngineAction;
                import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;
                import org.pickaid.pidatagraph.engine.context.PiEngineNumberKey;
                import org.pickaid.pidatagraph.engine.context.PiEngineValueKey;

                """);
        source.append("""
                /**
                 * Generated PiDataGraph facade. Project code can depend on this class instead of the internal glue class.
                 */
                """);
        source.append("public final class ").append(input.facadeName()).append(" {\n");
        source.append("    private ").append(input.facadeName()).append("() {\n");
        source.append("    }\n\n");
        source.append("    public static ResourceLocation id(String path) {\n");
        source.append("        return ").append(glueName).append(".").append(input.registry().fieldName()).append("_ID(path);\n");
        source.append("    }\n\n");
        source.append("    public static ResourceKey<PiEngineAction> key(String path) {\n");
        source.append("        return ").append(glueName).append(".").append(input.registry().fieldName()).append("_ENTRY_KEY(path);\n");
        source.append("    }\n\n");
        PiDataGraphFacadeKeyWriter.write(source, glueName, input, output);
        writeRegistration(source, modid, glueName, input);
        writeDataSet(source, glueName, input);
        writeRunMethods(source, glueName, input, output);
        source.append("}\n");
        return source.toString();
    }

    private void writeRegistration(StringBuilder source, String modid, String glueName, Input input) {
        source.append("    public static void registerDatapackRegistry(DataPackRegistryEvent.NewRegistry event) {\n");
        source.append("        PiDataPackRegistries.action(event, ").append(glueName).append(".")
                .append(input.registry().fieldName()).append("_KEY(), ")
                .append(input.registry().moduleQualifiedName()).append(".").append(input.registry().fieldName())
                .append(", PiDataPackSync.").append(input.registry().sync()).append(");\n");
        source.append("    }\n\n");
        source.append("    public static void addReloadVerifier(AddReloadListenerEvent event) {\n");
        source.append("        event.addListener(").append(glueName).append(".").append(prefix(input)).append(".reloadVerifier(event.getRegistryAccess()));\n");
        source.append("    }\n\n");
    }

    private void writeDataSet(StringBuilder source, String glueName, Input input) {
        source.append("    public static PiDataSet.Builder<PiEngineAction> dataSet() {\n");
        source.append("        return ").append(glueName).append(".").append(input.registry().fieldName()).append("_SET();\n");
        source.append("    }\n\n");
        source.append("    public static PiDataSet.Builder<PiEngineAction> dataSet(String namespace) {\n");
        source.append("        return ").append(glueName).append(".").append(input.registry().fieldName()).append("_SET(namespace);\n");
        source.append("    }\n\n");
        source.append("    public static PiDataBuildContext validationContext() {\n");
        source.append("        return ").append(glueName).append(".").append(prefix(input)).append(".validationContext();\n");
        source.append("    }\n\n");
    }

    private void writeRunMethods(StringBuilder source, String glueName, Input input, Output output) {
        String result = output == null ? "PiEngineFrame" : output.element().getQualifiedName().toString();
        String inputType = input.typeName();
        source.append("    public static ").append(result).append(" run(RegistryAccess access, String path, ").append(inputType).append(" input) {\n");
        source.append("        return run(access, id(path), input);\n");
        source.append("    }\n\n");
        source.append("    public static ").append(result).append(" run(RegistryAccess access, ResourceLocation id, ").append(inputType).append(" input) {\n");
        source.append("        return ").append(runCall(glueName, input, output, "access", "id", "input")).append(";\n");
        source.append("    }\n\n");
        source.append("    public static ").append(result).append(" run(RegistryAccess access, ResourceKey<?> key, ").append(inputType).append(" input) {\n");
        source.append("        return ").append(runCall(glueName, input, output, "access", "key", "input")).append(";\n");
        source.append("    }\n\n");
        source.append("    public static ").append(result).append(" run(PiDataSet<PiEngineAction> actions, String path, ").append(inputType).append(" input) {\n");
        source.append("        return run(actions, id(path), input);\n");
        source.append("    }\n\n");
        source.append("    public static ").append(result).append(" run(PiDataSet<PiEngineAction> actions, ResourceLocation id, ").append(inputType).append(" input) {\n");
        source.append("        return ").append(runCall(glueName, input, output, "actions", "id", "input")).append(";\n");
        source.append("    }\n\n");
        source.append("    public static ").append(result).append(" run(PiDataSet<PiEngineAction> actions, ResourceKey<?> key, ").append(inputType).append(" input) {\n");
        source.append("        return ").append(runCall(glueName, input, output, "actions", "key", "input")).append(";\n");
        source.append("    }\n");
    }

    private String runCall(String glueName, Input input, Output output, String source, String id, String value) {
        String prefix = prefix(input);
        if (output == null) {
            return glueName + "." + prefix + ".run(" + source + ", " + id + ", " + value + ")";
        }
        return glueName + "." + prefix + ".run(" + source + ", " + id + ", " + value + ", "
                + glueName + "::" + prefix + "_OUTPUT)";
    }

    private Output outputFor(Input input, List<Output> outputs) {
        for (Output output : outputs) {
            if (output.registry().equals(input.registry()) && output.name().equals(input.name())) {
                return output;
            }
        }
        return null;
    }

    private String prefix(Input input) {
        return PiDataGraphGeneratedNames.inputPrefix(input.name());
    }
}
