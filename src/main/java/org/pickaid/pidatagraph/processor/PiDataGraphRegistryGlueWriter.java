package org.pickaid.pidatagraph.processor;

import java.util.List;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Registry;

final class PiDataGraphRegistryGlueWriter {
    private PiDataGraphRegistryGlueWriter() {
    }

    static void write(StringBuilder source, List<Registry> registries, String modid, String moduleName) {
        for (Registry registry : registries) {
            source.append("    public static ResourceKey<Registry<PiEngineAction>> ")
                    .append(registry.fieldName()).append("_KEY() {\n");
            source.append("        return ResourceKey.createRegistryKey(new ResourceLocation(\"")
                    .append(escape(modid)).append("\", \"").append(escape(registry.path())).append("\"));\n");
            source.append("    }\n\n");
            source.append("    public static ResourceLocation ").append(registry.fieldName()).append("_ID(String path) {\n");
            source.append("        return new ResourceLocation(\"").append(escape(modid)).append("\", path);\n");
            source.append("    }\n\n");
            source.append("    public static ResourceKey<PiEngineAction> ")
                    .append(registry.fieldName()).append("_ENTRY_KEY(String path) {\n");
            source.append("        return ResourceKey.create(").append(registry.fieldName())
                    .append("_KEY(), ").append(registry.fieldName()).append("_ID(path));\n");
            source.append("    }\n\n");
            source.append("    public static PiDataDefinition<PiEngineAction> ")
                    .append(registry.fieldName()).append("_DEFINITION() {\n");
            source.append("        return PiEngineActionData.definition(new ResourceLocation(\"")
                    .append(escape(modid)).append("\", \"").append(escape(registry.path())).append("\"), \"")
                    .append(escape(registry.folder())).append("\", ").append(moduleName).append(".")
                    .append(registry.fieldName()).append(");\n");
            source.append("    }\n\n");
            source.append("    public static PiDataSet.Builder<PiEngineAction> ")
                    .append(registry.fieldName()).append("_SET() {\n");
            source.append("        return ").append(registry.fieldName()).append("_SET(\"").append(escape(modid)).append("\");\n");
            source.append("    }\n\n");
            source.append("    public static PiDataSet.Builder<PiEngineAction> ")
                    .append(registry.fieldName()).append("_SET(String namespace) {\n");
            source.append("        return PiDataSet.builder(").append(registry.fieldName())
                    .append("_DEFINITION(), namespace);\n");
            source.append("    }\n\n");
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
