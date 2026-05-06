package org.pickaid.pidatagraph.processor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import org.pickaid.pidatagraph.annotation.PiDataGraphModule;
import org.pickaid.pidatagraph.annotation.PiDataPackRegistry;
import org.pickaid.pidatagraph.annotation.PiGraphInput;
import org.pickaid.pidatagraph.annotation.PiGraphOutput;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Component;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Input;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Output;
import org.pickaid.pidatagraph.processor.PiDataGraphProcessorModel.Registry;

@SupportedAnnotationTypes({
        "org.pickaid.pidatagraph.annotation.PiDataGraphModule",
        "org.pickaid.pidatagraph.annotation.PiDataPackRegistry",
        "org.pickaid.pidatagraph.annotation.PiGraphInput",
        "org.pickaid.pidatagraph.annotation.PiGraphOutput",
        "org.pickaid.pidatagraph.annotation.PiNumber",
        "org.pickaid.pidatagraph.annotation.PiObject",
        "org.pickaid.pidatagraph.annotation.PiOutput"
})
public final class PiDataGraphProcessor extends AbstractProcessor {
    private static final String ACTION_REGISTRY = "org.pickaid.pidatagraph.engine.action.PiEngineActionRegistry";

    private final Set<String> generated = new LinkedHashSet<>();
    private PiDataGraphProcessorTypes types;
    private PiDataGraphComponentSupport components;
    private boolean failed;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        failed = false;
        new PiDataGraphModuleSupport(processingEnv, this::error).validateGeneratedModuleNames(roundEnv);
        List<Registry> registries = validateRegistries(roundEnv);
        List<Input> inputs = validateInputs(roundEnv, registries);
        List<Output> outputs = new PiDataGraphOutputSupport(processingEnv, types(), this::error).validate(roundEnv, registries, inputs);
        if (!failed) {
            new PiDataGraphGlueGenerator(processingEnv, generated).generateModules(roundEnv, registries, inputs, outputs);
        }
        return true;
    }

    private List<Registry> validateRegistries(RoundEnvironment roundEnv) {
        List<Registry> registries = new ArrayList<>();
        Map<Element, String> moduleModids = new LinkedHashMap<>();
        for (Element module : roundEnv.getElementsAnnotatedWith(PiDataGraphModule.class)) {
            PiDataGraphModule annotation = module.getAnnotation(PiDataGraphModule.class);
            if (module.getModifiers().contains(Modifier.PRIVATE)) {
                error(module, "@PiDataGraphModule class must not be private");
            }
            if (types().hasPrivateEnclosingType(module)) {
                error(module, "@PiDataGraphModule enclosing types must not be private");
            }
            String modid = annotation.modid().trim();
            moduleModids.put(module, modid);
            if (modid.isEmpty()) {
                error(module, "PiDataGraph module modid must not be blank");
            } else if (!PiDataGraphProcessorNames.isValidNamespace(modid)) {
                error(module, "PiDataGraph module modid must be a valid ResourceLocation namespace");
            }
            validateDuplicateRegistryPaths(module);
        }

        TypeMirror registryType = types().type(ACTION_REGISTRY);
        Map<String, Element> registryKeys = new LinkedHashMap<>();
        for (Element field : roundEnv.getElementsAnnotatedWith(PiDataPackRegistry.class)) {
            PiDataPackRegistry annotation = field.getAnnotation(PiDataPackRegistry.class);
            String path = annotation.path().trim();
            String folder = annotation.folder().trim();
            boolean validRegistry = true;
            if (path.isEmpty()) {
                error(field, "PiDataPackRegistry path must not be blank");
                validRegistry = false;
            } else if (!PiDataGraphProcessorNames.isValidPath(path)) {
                error(field, "PiDataPackRegistry path must be a valid ResourceLocation path");
                validRegistry = false;
            }
            if (!folder.isEmpty() && !PiDataGraphProcessorNames.isValidFolder(folder)) {
                error(field, "PiDataPackRegistry folder must be a relative data folder path");
                validRegistry = false;
            } else if (folder.isEmpty() && !path.isEmpty() && !PiDataGraphProcessorNames.isValidFolder(path)) {
                error(field, "PiDataPackRegistry path must be a relative data folder path when folder is blank");
                validRegistry = false;
            }
            Set<Modifier> modifiers = field.getModifiers();
            boolean validShape = field.getKind() == ElementKind.FIELD
                    && modifiers.contains(Modifier.STATIC)
                    && modifiers.contains(Modifier.FINAL)
                    && processingEnv.getTypeUtils().isSameType(field.asType(), registryType);
            if (!validShape) {
                error(field, "PiDataPackRegistry field must be static final PiEngineActionRegistry");
            }
            if (modifiers.contains(Modifier.PRIVATE)) {
                error(field, "PiDataPackRegistry field must not be private");
            }
            Element module = field.getEnclosingElement();
            boolean validModule = module.getAnnotation(PiDataGraphModule.class) != null;
            if (!validModule) {
                error(field, "PiDataPackRegistry field must be declared inside a @PiDataGraphModule class");
            }
            String modid = moduleModids.getOrDefault(module, "");
            if (validRegistry && validModule && PiDataGraphProcessorNames.isValidNamespace(modid)) {
                String registryKey = modid + ":" + path;
                Element previous = registryKeys.putIfAbsent(registryKey, field);
                if (previous != null) {
                    error(field, "duplicate PiDataGraph datapack registry key `" + registryKey + "`");
                    validRegistry = false;
                }
            }
            if (!validRegistry || !validShape || !validModule || modifiers.contains(Modifier.PRIVATE)) {
                continue;
            }
            registries.add(new Registry(
                    module.toString(),
                    module.getSimpleName().toString(),
                    processingEnv.getElementUtils().getPackageOf(module).getQualifiedName().toString(),
                    modid,
                    field.getSimpleName().toString(),
                    path,
                    folder.isEmpty() ? path : folder,
                    annotation.sync().name()));
        }
        return registries;
    }

    private void validateDuplicateRegistryPaths(Element module) {
        Map<String, Element> paths = new LinkedHashMap<>();
        for (Element enclosed : module.getEnclosedElements()) {
            PiDataPackRegistry annotation = enclosed.getAnnotation(PiDataPackRegistry.class);
            if (annotation == null) {
                continue;
            }
            String path = annotation.path().trim();
            if (path.isEmpty()) {
                continue;
            }
            Element previous = paths.putIfAbsent(path, enclosed);
            if (previous != null) {
                error(enclosed, "duplicate PiDataPackRegistry path `" + path + "`");
            }
        }
    }

    private List<Input> validateInputs(RoundEnvironment roundEnv, List<Registry> registries) {
        List<Input> inputs = new ArrayList<>();
        Map<String, Element> inputNames = new LinkedHashMap<>();
        Map<String, Element> inputGeneratedPrefixes = new LinkedHashMap<>();
        Map<String, Element> inputRegistries = new LinkedHashMap<>();
        PiDataGraphFacadeSupport facadeSupport = new PiDataGraphFacadeSupport(this::error);
        for (Element input : roundEnv.getElementsAnnotatedWith(PiGraphInput.class)) {
            PiGraphInput annotation = input.getAnnotation(PiGraphInput.class);
            String name = annotation.name().trim();
            if (name.isEmpty()) {
                error(input, "PiGraphInput name must not be blank");
            } else if (!SourceVersion.isIdentifier(name) || SourceVersion.isKeyword(name)) {
                error(input, "PiGraphInput name must be a Java identifier");
            }
            String facadeName = annotation.facade().trim();
            String registry = annotation.registry().trim();
            Registry registryModel = resolveRegistry(input, registry, registries);
            if (registryModel == null) {
                if (!registry.isEmpty()) {
                    error(input, "unknown PiGraphInput registry `" + registry + "`");
                }
            } else if (!name.isEmpty()) {
                String scopedName = registryModel.moduleQualifiedName() + "#" + name;
                Element previous = inputNames.putIfAbsent(scopedName, input);
                if (previous != null) {
                    error(input, "duplicate PiGraphInput name `" + name + "`");
                }
                String generatedPrefix = PiDataGraphGeneratedNames.inputPrefix(name);
                String scopedGeneratedPrefix = registryModel.moduleQualifiedName() + "#" + generatedPrefix;
                previous = inputGeneratedPrefixes.putIfAbsent(scopedGeneratedPrefix, input);
                if (previous != null) {
                    error(input, "PiGraphInput name `" + name
                            + "` generates duplicate member prefix `" + generatedPrefix + "`");
                }
                String registryReference = registryModel.moduleQualifiedName() + "." + registryModel.fieldName();
                previous = inputRegistries.putIfAbsent(registryReference, input);
                if (previous != null) {
                    error(input, "multiple PiGraphInput records target `" + registryModel.moduleSimpleName()
                            + "." + registryModel.fieldName()
                            + "`; declare a separate @PiDataPackRegistry field for a different input contract");
                }
                facadeSupport.validate(input, registryModel, facadeName);
            }
            if (input.getKind() != ElementKind.RECORD) {
                error(input, "@PiGraphInput can only be applied to records");
                continue;
            }
            if (input.getModifiers().contains(Modifier.PRIVATE)) {
                error(input, "@PiGraphInput record must not be private");
                continue;
            }
            if (types().hasPrivateEnclosingType(input)) {
                error(input, "@PiGraphInput record enclosing types must not be private");
                continue;
            }
            if (registryModel != null && types().isDifferentPackage(input, registryModel)
                    && !input.getModifiers().contains(Modifier.PUBLIC)) {
                error(input, "@PiGraphInput record must be public when it is used from another package");
                continue;
            }
            if (registryModel != null && types().isDifferentPackage(input, registryModel)
                    && !types().isPubliclyAccessible(input)) {
                error(input, "@PiGraphInput record and enclosing types must be public when used from another package");
                continue;
            }
            List<Component> components = components().validate((TypeElement) input, registryModel);
            if (registryModel != null) {
                inputs.add(new Input((TypeElement) input, registryModel, name, facadeName, components));
            }
        }
        return inputs;
    }

    private Registry resolveRegistry(Element input, String reference, List<Registry> registries) {
        if (reference.isEmpty()) {
            error(input, "PiGraphInput registry must not be blank");
            return null;
        }
        List<Registry> matches = registries.stream()
                .filter(registry -> reference.equals(registry.fieldName())
                        || reference.equals(registry.moduleSimpleName() + "." + registry.fieldName())
                        || reference.equals(registry.moduleQualifiedName() + "." + registry.fieldName()))
                .toList();
        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        if (!reference.contains(".")) {
            error(input, "ambiguous PiGraphInput registry `" + reference + "`; use "
                    + PiDataGraphProcessorNames.registrySuggestions(matches));
            return null;
        }
        error(input, "ambiguous PiGraphInput registry `" + reference + "`; use a fully qualified module name");
        return null;
    }

    private PiDataGraphProcessorTypes types() {
        if (types == null) {
            types = new PiDataGraphProcessorTypes(processingEnv);
        }
        return types;
    }

    private PiDataGraphComponentSupport components() {
        if (components == null) {
            components = new PiDataGraphComponentSupport(processingEnv, types(), this::error);
        }
        return components;
    }

    private void error(Element element, String message) {
        failed = true;
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
