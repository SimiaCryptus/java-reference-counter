/*
 * Copyright (c) 2019 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.ref.core;

import com.simiacryptus.ref.core.ops.FileAstVisitor;
import com.simiacryptus.ref.core.ops.IndexSymbols;
import com.simiacryptus.ref.lang.RefIgnore;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
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

@RefIgnore
public abstract class AutoCoder {
  protected static final Logger logger = LoggerFactory.getLogger(AutoCoder.class);
  @NotNull
  protected final ProjectInfo projectInfo;

  public AutoCoder(ProjectInfo projectInfo) {
    try {
      this.projectInfo = projectInfo;
    } catch (@NotNull Exception e) {
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

  public static String format(@NotNull String finalSrc) {
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
  public static DefaultCodeFormatterOptions formattingSettings() {
    final DefaultCodeFormatterOptions javaConventionsSettings = DefaultCodeFormatterOptions.getJavaConventionsSettings();
    javaConventionsSettings.align_with_spaces = true;
    javaConventionsSettings.tab_char = DefaultCodeFormatterOptions.SPACE;
    javaConventionsSettings.indentation_size = 2;
    return javaConventionsSettings;
  }

  public static String read(File file) {
    String prevSrc;
    try {
      prevSrc = FileUtils.readFileToString(file, "UTF-8");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return prevSrc;
  }

  @NotNull
  protected IndexSymbols.SymbolIndex getSymbolIndex() {
    final IndexSymbols.SymbolIndex index = new IndexSymbols.SymbolIndex();
    scan((projectInfo, cu, file) -> new IndexSymbols(projectInfo, cu, file, index));
    return index;
  }

  @Nonnull
  public abstract void rewrite();

  protected int rewrite(@NotNull VisitorFactory visitorFactory) {
    return projectInfo.parse().entrySet().stream().mapToInt(entry -> {
      File file = entry.getKey();
      CompilationUnit compilationUnit = entry.getValue();
      logger.debug(String.format("Scanning %s", file));
      final FileAstVisitor astVisitor = visitorFactory.apply(projectInfo, compilationUnit, file);
      compilationUnit.accept(astVisitor);
      try {
        if (astVisitor.writeFinal(true)) {
          logger.info(String.format("Changed: %s with %s", file, astVisitor.getClass().getSimpleName()));
          return 1;
        } else {
          logger.info("Not Touched: " + file);
          return 0;
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }).sum();
  }

  protected void scan(@NotNull VisitorFactory visitor) {
    projectInfo.parse().entrySet().stream().forEach(entry -> {
      File file = entry.getKey();
      CompilationUnit compilationUnit = entry.getValue();
      logger.debug(String.format("Scanning %s", file));
      final FileAstVisitor fileAstVisitor = visitor.apply(projectInfo, compilationUnit, file);
      compilationUnit.accept(fileAstVisitor);
      if (fileAstVisitor.revert()) {
        logger.warn("File modified in scan: " + file);
      }
    });
  }

  public interface VisitorFactory {
    FileAstVisitor apply(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file);
  }

}
