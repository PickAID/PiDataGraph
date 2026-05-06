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
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Registry;

final class PiDataGraphEventSubscriberGenerator {
    private final ProcessingEnvironment environment;
    private final Set<String> generated;

    PiDataGraphEventSubscriberGenerator(ProcessingEnvironment environment, Set<String> generated) {
        this.environment = environment;
        this.generated = generated;
    }

    void generate(TypeElement module, String modid, String glueName, List<Registry> registries, List<Input> inputs) {
        String packageName = environment.getElementUtils().getPackageOf(module).getQualifiedName().toString();
        if (!registries.isEmpty()) {
            write(module, packageName, glueName + "ModEvents", modid, "MOD", glueName + ".registerDatapackRegistries(event);",
                    "net.minecraftforge.registries.DataPackRegistryEvent.NewRegistry");
        }
        if (!inputs.isEmpty()) {
            write(module, packageName, glueName + "ForgeEvents", modid, "FORGE", glueName + ".addReloadListeners(event);",
                    "net.minecraftforge.event.AddReloadListenerEvent");
        }
    }

    private void write(
            TypeElement module,
            String packageName,
            String simpleName,
            String modid,
            String bus,
            String delegate,
            String eventType
    ) {
        String qualifiedName = packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
        if (!generated.add(qualifiedName)) {
            return;
        }
        try {
            JavaFileObject file = environment.getFiler().createSourceFile(qualifiedName, module);
            try (Writer writer = file.openWriter()) {
                writer.write(source(packageName, simpleName, modid, bus, delegate, eventType));
            }
        } catch (IOException error) {
            environment.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "failed to generate PiDataGraph event subscriber `" + qualifiedName + "`",
                    module);
        }
    }

    private String source(String packageName, String simpleName, String modid, String bus, String delegate, String eventType) {
        StringBuilder source = new StringBuilder();
        if (!packageName.isEmpty()) {
            source.append("package ").append(packageName).append(";\n\n");
        }
        source.append("""
                import net.minecraftforge.eventbus.api.SubscribeEvent;
                import net.minecraftforge.fml.common.Mod;

                """);
        source.append("@Mod.EventBusSubscriber(modid = \"").append(escape(modid))
                .append("\", bus = Mod.EventBusSubscriber.Bus.").append(bus).append(")\n");
        source.append("public final class ").append(simpleName).append(" {\n");
        source.append("    private ").append(simpleName).append("() {\n");
        source.append("    }\n\n");
        source.append("    @SubscribeEvent\n");
        source.append("    public static void handle(").append(eventType).append(" event) {\n");
        source.append("        ").append(delegate).append("\n");
        source.append("    }\n");
        source.append("}\n");
        return source.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
