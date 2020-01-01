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

/**
 * The type Logging ast visitor.
 */
public abstract class LoggingASTVisitor extends ASTVisitor {
  /**
   * The constant logger.
   */
  protected static final Logger logger = LoggerFactory.getLogger(ASTOperator.class);
  /**
   * The Compilation unit.
   */
  @NotNull
  protected final CompilationUnit compilationUnit;
  /**
   * The Ast.
   */
  protected final AST ast;
  /**
   * The File.
   */
  @NotNull
  protected final File file;

  /**
   * Instantiates a new Logging ast visitor.
   *
   * @param compilationUnit the compilation unit
   * @param file            the file
   */
  public LoggingASTVisitor(@Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    this.compilationUnit = compilationUnit;
    this.ast = compilationUnit.getAST();
    this.file = file;
  }

  /**
   * Debug.
   *
   * @param node         the node
   * @param formatString the format string
   * @param args         the args
   */
  protected final void debug(@NotNull ASTNode node, String formatString, Object... args) {
    debug(1, node, formatString, args);
  }

  /**
   * Debug.
   *
   * @param frames       the frames
   * @param node         the node
   * @param formatString the format string
   * @param args         the args
   */
  protected final void debug(int frames, @NotNull ASTNode node, String formatString, Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.debug(String.format(getLogPrefix(node, caller) + formatString, args));
  }

  /**
   * Gets location.
   *
   * @param node the node
   * @return the location
   */
  @NotNull
  protected final String getLocation(@Nonnull ASTNode node) {
    return file.getName() + ":" + compilationUnit.getLineNumber(node.getStartPosition());
  }

  /**
   * Info.
   *
   * @param node         the node
   * @param formatString the format string
   * @param args         the args
   */
  @SuppressWarnings("unused")
  protected final void info(@Nonnull ASTNode node, @Nonnull String formatString, Object... args) {
    info(1, node, formatString, args);
  }

  /**
   * Info.
   *
   * @param frames       the frames
   * @param node         the node
   * @param formatString the format string
   * @param args         the args
   */
  protected final void info(int frames, @Nonnull ASTNode node, @Nonnull String formatString, @NotNull Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.info(String.format(getLogPrefix(node, caller) + formatString, Arrays.stream(args).map(o -> o == null ? null : o.toString().trim()).toArray()));
  }

  /**
   * To string string.
   *
   * @param caller the caller
   * @return the string
   */
  @NotNull
  protected final String toString(@NotNull StackTraceElement caller) {
    return caller.getFileName() + ":" + caller.getLineNumber();
  }

  /**
   * Warn.
   *
   * @param node         the node
   * @param formatString the format string
   * @param args         the args
   */
  protected final void warn(@NotNull ASTNode node, @NotNull String formatString, Object... args) {
    warn(1, node, formatString, args);
  }

  /**
   * Warn.
   *
   * @param frames       the frames
   * @param node         the node
   * @param formatString the format string
   * @param args         the args
   */
  protected final void warn(int frames, @NotNull ASTNode node, @NotNull String formatString, Object... args) {
    warnRaw(frames + 1, node, String.format(formatString, args));
  }

  /**
   * Warn raw.
   *
   * @param frames the frames
   * @param node   the node
   * @param format the format
   */
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