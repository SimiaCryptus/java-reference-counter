package com.simiacryptus.devutil;

import com.simiacryptus.devutil.ops.IndexSymbols;
import org.apache.commons.io.FileUtils;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiFunction;

public abstract class AutoCoder extends ASTVisitor {
  protected static final Logger logger = LoggerFactory.getLogger(AutoCoder.class);
  @NotNull
  protected final SimpleMavenProject project;

  public AutoCoder(@NotNull String pathname) {
    try {
      this.project = SimpleMavenProject.load(new File(pathname).getCanonicalPath());
    } catch (@NotNull IOException | PlexusContainerException | DependencyResolutionException | ProjectBuildingException | ComponentLookupException e) {
      throw new RuntimeException(e);
    }

  }

  @NotNull
  public static Field getField(@NotNull Class<?> nodeClass, String name) {
    final Field[] fields = nodeClass.getDeclaredFields();
    final Optional<Field> parent = Arrays.stream(fields).filter(x -> x.getName().equals(name)).findFirst();
    if (!parent.isPresent()) {
      final Class<?> superclass = nodeClass.getSuperclass();
      if (superclass != null) {
        return getField(superclass, name);
      } else {
        throw new AssertionError(String.format("Cannot find field %s", name));
      }
    }
    final Field field = parent.get();
    field.setAccessible(true);
    return field;
  }

  @NotNull
  public static <T> T setField(@NotNull T astNode, String name, Object value) {
    try {
      getField(astNode.getClass(), name).set(astNode, value);
      return astNode;
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  protected String format(@NotNull String finalSrc) {
    final Document document = new Document();
    document.set(finalSrc);
    try {
      new DefaultCodeFormatter(formattingSettings())
          .format(
              CodeFormatter.K_COMPILATION_UNIT,
              finalSrc,
              0,
              finalSrc.length(),
              0,
              "\n")
          .apply(document);
    } catch (BadLocationException e) {
      throw new RuntimeException();
    }
    return document.get();
  }

  @NotNull
  protected DefaultCodeFormatterOptions formattingSettings() {
    final DefaultCodeFormatterOptions javaConventionsSettings = DefaultCodeFormatterOptions.getJavaConventionsSettings();
    javaConventionsSettings.align_with_spaces = true;
    javaConventionsSettings.tab_char = DefaultCodeFormatterOptions.SPACE;
    javaConventionsSettings.indentation_size = 2;
    return javaConventionsSettings;
  }

  @NotNull
  protected IndexSymbols.SymbolIndex getSymbolIndex() {
    final IndexSymbols.SymbolIndex index = new IndexSymbols.SymbolIndex();
    scan((cu, file) -> new IndexSymbols(cu, file, index));
    return index;
  }

  @Nonnull
  public abstract void rewrite();

  protected int rewrite(@NotNull BiFunction<CompilationUnit, File, ASTVisitor> visitor) {
    return project.parse().entrySet().stream().mapToInt(entry -> {
      File file = entry.getKey();
      CompilationUnit compilationUnit = entry.getValue();
      logger.debug(String.format("Scanning %s", file));
      final String prevSrc = compilationUnit.toString();
      final ASTVisitor astVisitor = visitor.apply(compilationUnit, file);
      compilationUnit.accept(astVisitor);
      final String finalSrc = compilationUnit.toString();
      if (!prevSrc.equals(finalSrc)) {
        logger.info(String.format("Changed: %s with %s", file, astVisitor.getClass().getSimpleName()));
        try {
          FileUtils.write(file, format(finalSrc), "UTF-8");
          return 1;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else {
        logger.debug("Not Touched: " + file);
        return 0;
      }
    }).sum();
  }

  protected void scan(@NotNull BiFunction<CompilationUnit, File, ASTVisitor> visitor) {
    project.parse().entrySet().stream().forEach(entry -> {
      File file = entry.getKey();
      CompilationUnit compilationUnit = entry.getValue();
      logger.debug(String.format("Scanning %s", file));
      compilationUnit.accept(visitor.apply(compilationUnit, file));
    });
  }

}
