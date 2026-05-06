package org.pickaid.pidatagraph.processor;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

final class PiDataGraphProcessorCompiler {
    private final Path tempDir;

    PiDataGraphProcessorCompiler(Path tempDir) {
        this.tempDir = tempDir;
    }

    CompilationResult compile(String source) throws IOException {
        return compile(source, List.of("-proc:only"));
    }

    CompilationResult compileGenerating(String source, Path generated) throws IOException {
        Files.createDirectories(generated);
        return compile(source, List.of("-s", generated.toString()));
    }

    CompilationResult compileGenerating(List<SourceFile> sources, Path generated) throws IOException {
        Files.createDirectories(generated);
        return compile(sources, List.of("-s", generated.toString()));
    }

    CompilationResult compile(String source, List<String> extraOptions) throws IOException {
        return compile(List.of(sourceFile("test.Example", source)), extraOptions);
    }

    CompilationResult compile(List<SourceFile> sources, List<String> extraOptions) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Path classes = tempDir.resolve("classes");
        Files.createDirectories(classes);

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                diagnostics,
                Locale.ROOT,
                StandardCharsets.UTF_8)) {
            java.util.ArrayList<String> options = new java.util.ArrayList<>();
            options.addAll(List.of("-classpath", System.getProperty("java.class.path"), "-d", classes.toString()));
            options.addAll(extraOptions);
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, sources);
            task.setProcessors(List.of(new PiDataGraphProcessor()));
            boolean success = Boolean.TRUE.equals(task.call());
            return new CompilationResult(success, diagnostics.getDiagnostics().stream()
                    .map(PiDataGraphProcessorCompiler::message)
                    .collect(Collectors.joining("\n")), classes);
        }
    }

    static String source(String body) {
        return """
                package test;

                import org.pickaid.pidatagraph.annotation.PiDataGraphModule;
                import org.pickaid.pidatagraph.annotation.PiDataPackRegistry;
                import org.pickaid.pidatagraph.annotation.PiGraphInput;
                import org.pickaid.pidatagraph.annotation.PiGraphOutput;
                import org.pickaid.pidatagraph.annotation.PiNumber;
                import org.pickaid.pidatagraph.annotation.PiObject;
                import org.pickaid.pidatagraph.annotation.PiOutput;
                import org.pickaid.pidatagraph.engine.action.PiEngineActionRegistry;

                """ + body;
    }

    static SourceFile sourceFile(String className, String source) {
        return new SourceFile(className, source);
    }

    private static String message(Diagnostic<?> diagnostic) {
        return diagnostic.getKind() + ": " + diagnostic.getMessage(Locale.ROOT);
    }
}

record CompilationResult(boolean success, String diagnostics, Path classes) {
}

final class SourceFile extends SimpleJavaFileObject {
    private final String source;

    SourceFile(String className, String source) {
        super(URI.create("string:///" + className.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE);
        this.source = source;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return source;
    }
}
