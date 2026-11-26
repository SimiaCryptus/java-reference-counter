package com.simiacryptus.ref.bytecode;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Compiles Java source snippets in memory (with full debug info and parameter metadata) into raw {@code .class} bytes.
 */
final class InMemoryJavaCompiler {
  private InMemoryJavaCompiler() {
  }

  static Map<String, byte[]> compile(Map<String, String> sourcesByBinaryName) {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new IllegalStateException("No system JavaCompiler available");
    }
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    StandardJavaFileManager stdManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, null);
    Map<String, ByteArrayOutputStream> outputs = new LinkedHashMap<>();

    JavaFileManager fileManager = new ForwardingJavaFileManager<StandardJavaFileManager>(stdManager) {
      @Override
      public JavaFileObject getJavaFileForOutput(
          Location location,
          String className,
          JavaFileObject.Kind kind,
          FileObject sibling
      ) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        outputs.put(className, baos);
        return new SimpleJavaFileObject(
            URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind
        ) {
          @Override
          public OutputStream openOutputStream() {
            return baos;
          }
        };
      }
    };

    List<JavaFileObject> units = new ArrayList<>();
    for (Map.Entry<String, String> entry : sourcesByBinaryName.entrySet()) {
      String binaryName = entry.getKey();
      String code = entry.getValue();
      units.add(new SimpleJavaFileObject(
          URI.create("string:///" + binaryName.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension),
          JavaFileObject.Kind.SOURCE
      ) {
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
          return code;
        }
      });
    }

    String classpath = System.getProperty("java.class.path", "");
    List<String> options = Arrays.asList("-g", "-parameters", "-classpath", classpath);
    Boolean ok = compiler.getTask(null, fileManager, diagnostics, options, null, units).call();
    if (!Boolean.TRUE.equals(ok)) {
      StringBuilder sb = new StringBuilder("Compilation failed:\n");
      for (javax.tools.Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
        sb.append("  ").append(d).append('\n');
      }
      throw new AssertionError(sb.toString());
    }

    Map<String, byte[]> compiled = new LinkedHashMap<>();
    for (Map.Entry<String, ByteArrayOutputStream> entry : outputs.entrySet()) {
      compiled.put(entry.getKey(), entry.getValue().toByteArray());
    }
    return compiled;
  }

  static Map<String, byte[]> compileSingle(String binaryName, String source) {
    return compile(Collections.singletonMap(binaryName, source));
  }

  static void writeClassesToDir(Map<String, byte[]> classes, Path targetDir) throws IOException {
    for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
      Path classFile = targetDir.resolve(entry.getKey().replace('.', '/') + ".class");
      Files.createDirectories(classFile.getParent());
      Files.write(classFile, entry.getValue());
    }
  }

  static File writeClassesToJar(Map<String, byte[]> classes, Path jarPath) throws IOException {
    Files.createDirectories(jarPath.getParent());
    try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
      for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
        JarEntry je = new JarEntry(entry.getKey().replace('.', '/') + ".class");
        jos.putNextEntry(je);
        jos.write(entry.getValue());
        jos.closeEntry();
      }
    }
    return jarPath.toFile();
  }
}
