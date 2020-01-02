/*
 * Copyright (c) 2020 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.ref.core;

import com.simiacryptus.ref.core.ops.ASTEditor;
import com.simiacryptus.ref.core.ops.IndexSymbols;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The type Auto coder.
 */
public abstract class AutoCoder {
  /**
   * The constant logger.
   */
  protected static final Logger logger = LoggerFactory.getLogger(AutoCoderMojo.class);
  private final ProjectInfo projectInfo;
  private boolean parallel = Boolean.parseBoolean(System.getProperty("parallel", Boolean.toString(false)));

  /**
   * Instantiates a new Auto coder.
   *
   * @param projectInfo the project info
   */
  protected AutoCoder(ProjectInfo projectInfo) {
    this.projectInfo = projectInfo;
  }

  /**
   * Gets project info.
   *
   * @return the project info
   */
  protected ProjectInfo getProjectInfo() {
    return projectInfo;
  }

  /**
   * Gets symbol index.
   *
   * @return the symbol index
   */
  @SuppressWarnings("unused")
  @NotNull
  protected SymbolIndex getSymbolIndex() {
    final SymbolIndex index = new SymbolIndex();
    scan((projectInfo, cu, file) -> new IndexSymbols(projectInfo, cu, file, index));
    return index;
  }

  /**
   * Is parallel boolean.
   *
   * @return the boolean
   */
  public boolean isParallel() {
    return parallel;
  }

  /**
   * Sets parallel.
   *
   * @param parallel the parallel
   * @return the parallel
   */
  @NotNull
  @SuppressWarnings("unused")
  public AutoCoder setParallel(boolean parallel) {
    this.parallel = parallel;
    return this;
  }

  /**
   * Read string.
   *
   * @param file the file
   * @return the string
   */
  public static String read(@NotNull File file) {
    try {
      return FileUtils.readFileToString(file, "UTF-8");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Rewrite.
   */
  @Nonnull
  public abstract void rewrite();

  /**
   * Rewrite int.
   *
   * @param visitorFactory the visitor factory
   * @return the int
   */
  protected int rewrite(@NotNull VisitorFactory visitorFactory) {
    return rewrite(visitorFactory, isParallel());
  }

  /**
   * Rewrite int.
   *
   * @param visitorFactory the visitor factory
   * @param parallel       the parallel
   * @return the int
   */
  protected int rewrite(@NotNull VisitorFactory visitorFactory, boolean parallel) {
    Stream<Map.Entry<File, CompilationUnit>> stream = getProjectInfo().parse().entrySet().stream();
    if (parallel) stream = stream.parallel();
    return stream.mapToInt(entry -> {
      File file = entry.getKey();
      CompilationUnit compilationUnit = entry.getValue();
      logger.debug(String.format("Scanning %s", file));
      final ASTEditor astVisitor = visitorFactory.apply(getProjectInfo(), compilationUnit, file);
      compilationUnit.accept(astVisitor);
      if (astVisitor.writeFinal(true)) {
        logger.info(String.format("Changed by %s: %s", astVisitor.getClass().getName(), file));
        return 1;
      } else {
        logger.info(String.format("Not Touched by %s: %s", astVisitor.getClass().getName(), file));
        return 0;
      }
    }).sum();
  }

  /**
   * Scan.
   *
   * @param visitor the visitor
   */
  protected void scan(@NotNull VisitorFactory visitor) {
    getProjectInfo().parse().entrySet().stream().forEach(entry -> {
      File file = entry.getKey();
      CompilationUnit compilationUnit = entry.getValue();
      logger.debug(String.format("Scanning %s", file));
      final ASTEditor autoCoderOperator = visitor.apply(getProjectInfo(), compilationUnit, file);
      compilationUnit.accept(autoCoderOperator);
      if (autoCoderOperator.revert()) {
        logger.warn("File modified in scan: " + file);
      }
    });
  }

  /**
   * The interface Visitor factory.
   */
  public interface VisitorFactory {
    /**
     * Apply ast editor.
     *
     * @param projectInfo     the project info
     * @param compilationUnit the compilation unit
     * @param file            the file
     * @return the ast editor
     */
    @NotNull ASTEditor apply(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file);
  }
}
