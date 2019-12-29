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
  protected final CompilationUnit compilationUnit;
  protected final AST ast;
  protected final File file;

  public LoggingASTVisitor(@Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    this.compilationUnit = compilationUnit;
    this.ast = compilationUnit.getAST();
    this.file = file;
  }

  protected final void debug(ASTNode node, String formatString, Object... args) {
    debug(1, node, formatString, args);
  }

  protected final void debug(int frames, ASTNode node, String formatString, Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.debug(String.format(getLogPrefix(node, caller) + formatString, args));
  }

  @NotNull
  protected final String getLocation(@Nonnull ASTNode node) {
    return file.getName() + ":" + compilationUnit.getLineNumber(node.getStartPosition());
  }

  protected final void info(@Nonnull ASTNode node, @Nonnull String formatString, Object... args) {
    info(1, node, formatString, args);
  }

  protected final void info(int frames, @Nonnull ASTNode node, @Nonnull String formatString, Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.info(String.format(getLogPrefix(node, caller) + formatString, Arrays.stream(args).map(o -> o == null ? null : o.toString().trim()).toArray()));
  }

  @NotNull
  protected final String toString(StackTraceElement caller) {
    return caller.getFileName() + ":" + caller.getLineNumber();
  }

  protected final void warn(ASTNode node, String formatString, Object... args) {
    warn(1, node, formatString, args);
  }

  protected final void warn(int frames, ASTNode node, String formatString, Object... args) {
    warnRaw(frames + 1, node, String.format(formatString, args));
  }

  protected void warnRaw(int frames, ASTNode node, String format) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2 + frames];
    logger.warn(getLogPrefix(node, caller) + format);
  }

  @NotNull
  private String getLogPrefix(@Nonnull ASTNode node, @Nonnull StackTraceElement caller) {
    return "(" + toString(caller) + ") (" + getLocation(node) + ") - ";
  }
}
