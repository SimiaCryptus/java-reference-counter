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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Stream;

public abstract class AutoCoder {
  protected static final Logger logger = LoggerFactory.getLogger(AutoCoderMojo.class);
  protected final ProjectInfo projectInfo;
  private boolean parallel = Boolean.parseBoolean(System.getProperty("parallel", Boolean.toString(false)));

  protected AutoCoder(ProjectInfo projectInfo) {
    this.projectInfo = projectInfo;
  }

  protected ProjectInfo getProjectInfo() {
    return projectInfo;
  }

  @SuppressWarnings("unused")
  @Nonnull
  protected SymbolIndex getSymbolIndex() {
    final SymbolIndex index = new SymbolIndex();
    scan((projectInfo, cu, file) -> new IndexSymbols(projectInfo, cu, file, index));
    return index;
  }

  public boolean isParallel() {
    return parallel;
  }

  @Nonnull
  @SuppressWarnings("unused")
  public AutoCoder setParallel(boolean parallel) {
    this.parallel = parallel;
    return this;
  }

  public static String read(@Nonnull File file) {
    try {
      return FileUtils.readFileToString(file, "UTF-8");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public abstract void rewrite();

  protected int rewrite(@Nonnull VisitorFactory visitorFactory) {
    return rewrite(visitorFactory, isParallel(), false);
  }

  protected int rewrite(@Nonnull VisitorFactory visitorFactory, boolean parallel, boolean failAtEnd) {
    Stream<Map.Entry<File, CompilationUnit>> stream = getProjectInfo().parse().entrySet().stream();
    if (parallel) stream = stream.parallel();
    final ArrayList<CollectableException> errors = new ArrayList<>();
    final int sum = stream.mapToInt(entry -> {
      File file = entry.getKey();
      CompilationUnit compilationUnit = entry.getValue();
      logger.debug(String.format("Scanning %s", file));
      final ASTEditor astVisitor = visitorFactory.apply(getProjectInfo(), compilationUnit, file);
      try {
        compilationUnit.accept(astVisitor);
        if (astVisitor.writeFinal(true)) {
          logger.info(String.format("Changed by %s: %s", astVisitor.getClass().getName(), file));
          return 1;
        } else {
          logger.info(String.format("Not Touched by %s: %s", astVisitor.getClass().getName(), file));
          return 0;
        }
      } catch (CollectableException e) {
        if (!failAtEnd) {
          throw e;
        } else {
          errors.add(e);
          return 0;
        }
      } catch (Throwable e) {
        if (!failAtEnd) {
          throw new RuntimeException(String.format("Error processing %s with %s", file, astVisitor.getClass().getName()), e);
        } else {
          final String msg = String.format("Error processing %s with %s - %s", file, astVisitor.getClass().getName(), e.getMessage());
          logger.warn(msg, e);
          errors.add(new CollectableException(msg));
          return 0;
        }
      }
    }).sum();
    if (!errors.isEmpty()) {
      throw CollectableException.combine(errors);
    }
    return sum;
  }

  protected void scan(@Nonnull VisitorFactory visitor) {
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

  public interface VisitorFactory {
    @Nonnull
    ASTEditor apply(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file);
  }
}
