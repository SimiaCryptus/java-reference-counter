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

/**
 * This class is a visitor for logging purposes that extends the ASTVisitor class.
 * It contains a logger and is used to log exceptions.
 * It also has a compilationUnit, an AST, and a file.
 * The ArrayList is used to store exceptions that are collected.
 * The failAtEnd boolean is used to determine whether or not to fail at the end.
 *
 * @docgenVersion 9
 */
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

  /**
   * Returns true if the process should fail at the end.
   *
   * @return true if the process should fail at the end
   * @docgenVersion 9
   */
  public boolean isFailAtEnd() {
    return failAtEnd;
  }

  /**
   * Sets whether or not the process should fail at the end.
   *
   * @param failAtEnd whether or not the process should fail at the end
   * @docgenVersion 9
   */
  public void setFailAtEnd(boolean failAtEnd) {
    this.failAtEnd = failAtEnd;
  }

  /**
   * Prints a debug message for the given node.
   *
   * @param node         the node to print a debug message for
   * @param formatString the format string for the debug message
   * @param args         the arguments for the format string
   * @docgenVersion 9
   */
  public final void debug(@Nonnull ASTNode node, String formatString, Object... args) {
    debug(1, node, formatString, args);
  }

  /**
   * Prints a warning message.
   *
   * @param node         the node to use for location information
   * @param formatString the message format string
   * @param args         the message arguments
   * @docgenVersion 9
   */
  public final void warn(@Nonnull ASTNode node, @Nonnull String formatString, Object... args) {
    warn(1, node, formatString, args);
  }

  /**
   * Logs a debug message.
   *
   * @param frames       the number of frames to skip
   * @param node         the node to use for location information
   * @param formatString the format string
   * @param args         the format string arguments
   * @docgenVersion 9
   */
  public final void debug(int frames, @Nonnull ASTNode node, String formatString, Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.debug(String.format(getLogPrefix(node, caller) + formatString, args));
  }

  /**
   * @SuppressWarnings("unused") public final void info(@Nonnull ASTNode node, @Nonnull String formatString, Object... args) {
   * info(1, node, formatString, args);
   * }
   * @docgenVersion 9
   */
  @SuppressWarnings("unused")
  public final void info(@Nonnull ASTNode node, @Nonnull String formatString, Object... args) {
    info(1, node, formatString, args);
  }

  /**
   * Prints an informational message to the console.
   *
   * @param frames       the number of stack frames to print
   * @param node         the node to print
   * @param formatString the format string
   * @param args         the format string arguments
   * @docgenVersion 9
   */
  public final void info(int frames, @Nonnull ASTNode node, @Nonnull String formatString, @Nonnull Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.info(String.format(getLogPrefix(node, caller) + formatString, Arrays.stream(args).map(o -> o == null ? null : o.toString().trim()).toArray()));
  }

  /**
   * Prints a warning message.
   *
   * @param frames       the number of frames to print
   * @param node         the node to print
   * @param formatString the format string
   * @param args         the format string arguments
   * @docgenVersion 9
   */
  public final void warn(int frames, @Nonnull ASTNode node, @Nonnull String formatString, Object... args) {
    warnRaw(frames + 1, node, String.format(formatString, args));
  }

  /**
   * Prints a fatal error message for the given node.
   *
   * @param node         the node to print the error message for
   * @param formatString the error message format string
   * @param args         the error message arguments
   * @docgenVersion 9
   */
  public final void fatal(@Nonnull ASTNode node, @Nonnull String formatString, Object... args) {
    CollectableException collectableException = new CollectableException("(" + getLocation(node) + ") " + String.format(formatString, args));
    if (isFailAtEnd()) {
      warn(1, node, formatString, args);
      exceptions.add(collectableException);
    } else {
      throw collectableException;
    }
  }

  /**
   * This method ends the visit of a compilation unit and throws any queued exceptions.
   *
   * @param node the compilation unit being visited
   * @docgenVersion 9
   */
  @Override
  public void endVisit(CompilationUnit node) {
    throwQueuedExceptions();
  }

  /**
   * Logs a warning message with the given format string and arguments.
   *
   * @param frames the number of frames to skip
   * @param node   the node to use for location information, or null
   * @param format the format string
   * @param args   the arguments to format
   * @docgenVersion 9
   */
  public void warnRaw(int frames, @Nonnull ASTNode node, String format) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.warn(getLogPrefix(node, caller) + format);
  }

  /**
   * Throws any exceptions that have been queued.
   *
   * @throws CollectableException if there are any queued exceptions
   * @docgenVersion 9
   */
  protected final void throwQueuedExceptions() {
    if (!exceptions.isEmpty()) throw CollectableException.combine(exceptions);
  }

  /**
   * Returns the location of the given node.
   *
   * @param node the node to get the location of
   * @return the location of the given node
   * @docgenVersion 9
   */
  @Nonnull
  protected final String getLocation(@Nonnull ASTNode node) {
    return file.getName() + ":" + compilationUnit.getLineNumber(node.getStartPosition());
  }

  /**
   * Returns a string representation of the stack trace element, including
   * the file name and line number.
   *
   * @param caller the stack trace element to convert to a string
   * @return a string representation of the stack trace element
   * @docgenVersion 9
   */
  @Nonnull
  protected final String toString(@Nonnull StackTraceElement caller) {
    return caller.getFileName() + ":" + caller.getLineNumber();
  }

  /**
   * Returns a log prefix for the given node and caller.
   *
   * @param node   the node to get the log prefix for
   * @param caller the caller to get the log prefix for
   * @return the log prefix for the given node and caller
   * @docgenVersion 9
   */
  @Nonnull
  private String getLogPrefix(@Nonnull ASTNode node, @Nonnull StackTraceElement caller) {
    return "(" + toString(caller) + ") (" + getLocation(node) + ") - ";
  }
}
