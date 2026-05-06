package org.pickaid.pidatagraph.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.pickaid.pidatagraph.annotation.PiDataGraphModule;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Component;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Input;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Output;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Registry;

final class PiDataGraphGlueGenerator {
    private final ProcessingEnvironment environment;
    private final Set<String> generated;

    PiDataGraphGlueGenerator(ProcessingEnvironment environment, Set<String> generated) {
        this.environment = environment;
        this.generated = generated;
    }

    void generateModules(
            RoundEnvironment roundEnv,
            List<Registry> registries,
            List<Input> inputs,
            List<Output> outputs
    ) {
        for (Element moduleElement : roundEnv.getElementsAnnotatedWith(PiDataGraphModule.class)) {
            if (!(moduleElement instanceof TypeElement module)) {
                continue;
            }
            String packageName = environment.getElementUtils().getPackageOf(module).getQualifiedName().toString();
            String generatedName = generatedName(module, packageName);
            String qualifiedName = packageName.isEmpty() ? generatedName : packageName + "." + generatedName;
            if (!generated.add(qualifiedName)) {
                continue;
            }
            PiDataGraphModule annotation = module.getAnnotation(PiDataGraphModule.class);
            String moduleQualifiedName = module.getQualifiedName().toString();
            List<Registry> moduleRegistries = registries.stream()
                    .filter(registry -> registry.moduleQualifiedName().equals(moduleQualifiedName))
                    .toList();
            Set<String> registryReferences = new LinkedHashSet<>();
            for (Registry registry : moduleRegistries) {
                registryReferences.add(registry.moduleQualifiedName() + "." + registry.fieldName());
            }
            List<Input> moduleInputs = inputs.stream()
                    .filter(input -> registryReferences.contains(input.registry().moduleQualifiedName()
                            + "." + input.registry().fieldName()))
                    .toList();
            List<Output> moduleOutputs = outputs.stream()
                    .filter(output -> registryReferences.contains(output.registry().moduleQualifiedName()
                            + "." + output.registry().fieldName()))
                    .toList();
            writeModule(module, annotation.modid().trim(), moduleRegistries, moduleInputs, moduleOutputs);
            new PiDataGraphEventSubscriberGenerator(environment, generated)
                    .generate(module, annotation.modid().trim(), generatedName, moduleRegistries, moduleInputs);
            new PiDataGraphFacadeGenerator(environment, generated)
                    .generate(module, annotation.modid().trim(), generatedName, moduleInputs, moduleOutputs);
        }
    }

    private void writeModule(
            TypeElement module,
            String modid,
            List<Registry> registries,
            List<Input> inputs,
            List<Output> outputs
    ) {
        String packageName = environment.getElementUtils().getPackageOf(module).getQualifiedName().toString();
        String generatedName = generatedName(module, packageName);
        String qualifiedName = packageName.isEmpty() ? generatedName : packageName + "." + generatedName;
        try {
            JavaFileObject file = environment.getFiler().createSourceFile(qualifiedName, module);
            try (Writer writer = file.openWriter()) {
                writer.write(source(packageName, generatedName, module.getQualifiedName().toString(), modid, registries, inputs, outputs));
            }
        } catch (IOException error) {
            environment.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "failed to generate PiDataGraph glue `" + qualifiedName + "`; check for an existing class with that name",
                    module);
        }
    }

    private String generatedName(TypeElement module, String packageName) {
        return PiDataGraphGeneratedNames.moduleName(module, packageName);
    }

    private String source(
            String packageName,
            String generatedName,
            String moduleName,
            String modid,
            List<Registry> registries,
            List<Input> inputs,
            List<Output> outputs
    ) {
        StringBuilder source = new StringBuilder();
        if (!packageName.isEmpty()) {
            source.append("package ").append(packageName).append(";\n\n");
        }
        source.append("""
                import net.minecraft.core.Registry;
                import net.minecraft.resources.ResourceKey;
                import net.minecraft.resources.ResourceLocation;
                import net.minecraftforge.event.AddReloadListenerEvent;
                import net.minecraftforge.registries.DataPackRegistryEvent;
                import org.pickaid.pidatagraph.data.PiDataDefinition;
                import org.pickaid.pidatagraph.data.PiDataPackRegistries;
                import org.pickaid.pidatagraph.data.PiDataPackSync;
                import org.pickaid.pidatagraph.data.PiDataSet;
                import org.pickaid.pidatagraph.engine.PiEngineContext;
                import org.pickaid.pidatagraph.engine.PiEngineContextBinder;
                import org.pickaid.pidatagraph.engine.PiEngineFrame;
                import org.pickaid.pidatagraph.engine.PiEngineRunner;
                import org.pickaid.pidatagraph.engine.action.PiEngineAction;
                import org.pickaid.pidatagraph.engine.action.PiEngineActionData;
                import org.pickaid.pidatagraph.engine.context.PiEngineContextKey;
                import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
                import org.pickaid.pidatagraph.engine.context.PiEngineNumberKey;
                import org.pickaid.pidatagraph.engine.context.PiEngineValueKey;

                """);
        source.append("""
                /**
                 * Generated PiDataGraph glue. Prefer a generated @PiGraphInput facade for project code.
                 * Direct usage is a low-level fallback when no facade is declared.
                 */
                """);
        source.append("public final class ").append(generatedName).append(" {\n");
        source.append("    private ").append(generatedName).append("() {\n");
        source.append("    }\n\n");
        PiDataGraphRegistryGlueWriter.write(source, registries, modid, moduleName);
        for (Input input : inputs) {
            writeInputKeys(source, input);
        }
        PiDataGraphOutputGlueWriter.write(source, outputs);
        for (Input input : inputs) {
            String prefix = constant(input.name());
            source.append("    public static final PiEngineContextBinder<").append(input.typeName()).append("> ")
                    .append(prefix).append("_BINDER = new ").append(binderName(input)).append("();\n");
            source.append("    public static final PiEngineRunner<").append(input.typeName()).append("> ")
                    .append(prefix).append(" = PiEngineRunner.actionRegistry(").append(generatedName).append("::")
                    .append(input.registry().fieldName()).append("_KEY, ").append(prefix).append("_BINDER);\n\n");
        }
        source.append("    public static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {\n");
        for (Registry registry : registries) {
            source.append("        PiDataPackRegistries.action(event, ").append(registry.fieldName())
                    .append("_KEY(), ").append(moduleName).append(".").append(registry.fieldName())
                    .append(", PiDataPackSync.").append(registry.sync()).append(");\n");
        }
        source.append("    }\n\n");
        source.append("    public static void addReloadListeners(AddReloadListenerEvent event) {\n");
        for (Input input : inputs) {
            source.append("        event.addListener(").append(constant(input.name()))
                    .append(".reloadVerifier(event.getRegistryAccess()));\n");
        }
        source.append("    }\n\n");
        for (Input input : inputs) {
            writeBinder(source, input);
        }
        source.append("}\n");
        return source.toString();
    }

    private void writeInputKeys(StringBuilder source, Input input) {
        for (Component component : input.components()) {
            String name = keyConstant(input, component);
            if (component.number()) {
                source.append("    public static final PiEngineNumberKey ").append(name)
                        .append(" = PiEngineNumberKey.of(\"").append(escape(component.key())).append("\");\n");
            } else {
                source.append("    public static final PiEngineContextKey<").append(component.type()).append("> ")
                        .append(name).append(" = PiEngineContextKey.of(\"").append(escape(component.key()))
                        .append("\", ").append(component.type()).append(".class);\n");
            }
        }
        source.append("\n");
    }

    private void writeBinder(StringBuilder source, Input input) {
        source.append("    private static final class ").append(binderName(input))
                .append(" implements PiEngineContextBinder<").append(input.typeName()).append("> {\n");
        source.append("        @Override\n");
        source.append("        public PiEngineContextContract contract() {\n");
        source.append("            return PiEngineContextContract.builder()\n");
        for (Component component : input.components()) {
            if (component.number()) {
                source.append("                    .number(").append(keyConstant(input, component)).append(")\n");
            } else {
                source.append("                    .object(").append(keyConstant(input, component)).append(")\n");
            }
        }
        source.append("                    .build();\n");
        source.append("        }\n\n");
        source.append("        @Override\n");
        source.append("        public PiEngineContext bind(").append(input.typeName()).append(" input) {\n");
        source.append("            java.util.Objects.requireNonNull(input, \"PiGraphInput `")
                .append(escape(input.name())).append("` must not be null\");\n");
        source.append("            return PiEngineContext.builder()\n");
        for (Component component : input.components()) {
            if (component.number()) {
                source.append("                    .number(").append(keyConstant(input, component)).append(", ")
                        .append(numberInputExpression(component)).append(")\n");
            } else {
                source.append("                    .object(").append(keyConstant(input, component)).append(", java.util.Objects.requireNonNull(input.")
                        .append(component.accessor()).append("(), \"PiGraphInput object `")
                        .append(escape(component.key())).append("` must not be null\"))\n");
            }
        }
        source.append("                    .build();\n");
        source.append("        }\n");
        source.append("    }\n\n");
    }

    private static String numberInputExpression(Component component) {
        String accessor = "input." + component.accessor() + "()";
        if (!component.referenceNumber()) {
            return accessor;
        }
        return "java.util.Objects.requireNonNull(" + accessor + ", \"PiGraphInput number `"
                + escape(component.key()) + "` must not be null\").doubleValue()";
    }

    private static String binderName(Input input) {
        return PiDataGraphGeneratedNames.binderName(input.name());
    }

    private static String constant(String name) {
        return PiDataGraphGeneratedNames.inputPrefix(name);
    }

    private static String keyConstant(Input input, Component component) {
        return constant(input.name()) + "_" + constant(component.key());
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
