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

package com.simiacryptus.ref.core.ops;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;

public abstract class LoggingASTVisitor extends ASTVisitor {
  protected static final Logger logger = LoggerFactory.getLogger(ASTOperator.class);
  @NotNull
  protected final CompilationUnit compilationUnit;
  protected final AST ast;
  @NotNull
  protected final File file;

  public LoggingASTVisitor(@Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    this.compilationUnit = compilationUnit;
    this.ast = compilationUnit.getAST();
    this.file = file;
  }

  public final void debug(@NotNull ASTNode node, String formatString, Object... args) {
    debug(1, node, formatString, args);
  }

  protected final void debug(int frames, @NotNull ASTNode node, String formatString, Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.debug(String.format(getLogPrefix(node, caller) + formatString, args));
  }

  @NotNull
  protected final String getLocation(@Nonnull ASTNode node) {
    return file.getName() + ":" + compilationUnit.getLineNumber(node.getStartPosition());
  }

  @SuppressWarnings("unused")
  protected final void info(@Nonnull ASTNode node, @Nonnull String formatString, Object... args) {
    info(1, node, formatString, args);
  }

  protected final void info(int frames, @Nonnull ASTNode node, @Nonnull String formatString, @NotNull Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.info(String.format(getLogPrefix(node, caller) + formatString, Arrays.stream(args).map(o -> o == null ? null : o.toString().trim()).toArray()));
  }

  @NotNull
  protected final String toString(@NotNull StackTraceElement caller) {
    return caller.getFileName() + ":" + caller.getLineNumber();
  }

  public final void warn(@NotNull ASTNode node, @NotNull String formatString, Object... args) {
    warn(1, node, formatString, args);
  }

  protected final void warn(int frames, @NotNull ASTNode node, @NotNull String formatString, Object... args) {
    warnRaw(frames + 1, node, String.format(formatString, args));
  }

  protected final void fatal(@NotNull ASTNode node, @NotNull String formatString, Object... args) {
    throw new RuntimeException("(" + getLocation(node) + ") " + String.format(formatString, args));
  }

  protected void warnRaw(int frames, @NotNull ASTNode node, String format) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.warn(getLogPrefix(node, caller) + format);
  }

  @NotNull
  private String getLogPrefix(@Nonnull ASTNode node, @Nonnull StackTraceElement caller) {
    return "(" + toString(caller) + ") (" + getLocation(node) + ") - ";
  }
}
