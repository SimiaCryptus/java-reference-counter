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

import com.simiacryptus.ref.core.CollectableException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class LoggingASTVisitor extends ASTVisitor {
  protected static final Logger logger = LoggerFactory.getLogger(ASTOperator.class);
  @Nonnull
  protected final CompilationUnit compilationUnit;
  protected final AST ast;
  @Nonnull
  protected final File file;
  private final ArrayList<CollectableException> exceptions = new ArrayList<>();
  private boolean failAtEnd = false;

  public LoggingASTVisitor(@Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    this.compilationUnit = compilationUnit;
    this.ast = compilationUnit.getAST();
    this.file = file;
  }

  public boolean isFailAtEnd() {
    return failAtEnd;
  }

  public void setFailAtEnd(boolean failAtEnd) {
    this.failAtEnd = failAtEnd;
  }

  public final void debug(@Nonnull ASTNode node, String formatString, Object... args) {
    debug(1, node, formatString, args);
  }

  public final void warn(@Nonnull ASTNode node, @Nonnull String formatString, Object... args) {
    warn(1, node, formatString, args);
  }

  public final void debug(int frames, @Nonnull ASTNode node, String formatString, Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.debug(String.format(getLogPrefix(node, caller) + formatString, args));
  }

  @SuppressWarnings("unused")
  public final void info(@Nonnull ASTNode node, @Nonnull String formatString, Object... args) {
    info(1, node, formatString, args);
  }

  public final void info(int frames, @Nonnull ASTNode node, @Nonnull String formatString, @Nonnull Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.info(String.format(getLogPrefix(node, caller) + formatString, Arrays.stream(args).map(o -> o == null ? null : o.toString().trim()).toArray()));
  }

  public final void warn(int frames, @Nonnull ASTNode node, @Nonnull String formatString, Object... args) {
    warnRaw(frames + 1, node, String.format(formatString, args));
  }

  public final void fatal(@Nonnull ASTNode node, @Nonnull String formatString, Object... args) {
    CollectableException collectableException = new CollectableException("(" + getLocation(node) + ") " + String.format(formatString, args));
    if (isFailAtEnd()) {
      warn(1, node, formatString, args);
      exceptions.add(collectableException);
    } else {
      throw collectableException;
    }
  }

  @Override
  public void endVisit(CompilationUnit node) {
    throwQueuedExceptions();
  }

  public void warnRaw(int frames, @Nonnull ASTNode node, String format) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.warn(getLogPrefix(node, caller) + format);
  }

  protected final void throwQueuedExceptions() {
    if (!exceptions.isEmpty()) throw CollectableException.combine(exceptions);
  }

  @Nonnull
  protected final String getLocation(@Nonnull ASTNode node) {
    return file.getName() + ":" + compilationUnit.getLineNumber(node.getStartPosition());
  }

  @Nonnull
  protected final String toString(@Nonnull StackTraceElement caller) {
    return caller.getFileName() + ":" + caller.getLineNumber();
  }

  @Nonnull
  private String getLogPrefix(@Nonnull ASTNode node, @Nonnull StackTraceElement caller) {
    return "(" + toString(caller) + ") (" + getLocation(node) + ") - ";
  }
}
