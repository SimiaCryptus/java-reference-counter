package com.simiacryptus.devutil.ops;

import com.simiacryptus.devutil.AutoCoder;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Random;

public class FileAstVisitor extends ASTVisitor {
  protected static final Logger logger = LoggerFactory.getLogger(FileAstVisitor.class);

  protected final CompilationUnit compilationUnit;
  protected final File file;

  public FileAstVisitor(CompilationUnit compilationUnit, File file) {
    this.compilationUnit = compilationUnit;
    this.file = file;
  }

  @NotNull
  public String getLocation(ASTNode node) {
    final int lineNumber = compilationUnit.getLineNumber(node.getStartPosition());
    return file.getName() + ":" + lineNumber;
  }

  @NotNull
  public IndexSymbols.Span getSpan(ASTNode node) {
    final int startPosition = node.getStartPosition();
    final int length = node.getLength();

    return new IndexSymbols.Span(
        file,
        compilationUnit.getLineNumber(startPosition),
        compilationUnit.getColumnNumber(startPosition),
        compilationUnit.getLineNumber(startPosition + length),
        compilationUnit.getColumnNumber(startPosition + length)
    );
  }

  @NotNull
  public IndexSymbols.SymbolIndex getSymbolIndex(ASTNode node) {
    final IndexSymbols.SymbolIndex lambdaIndex = new IndexSymbols.SymbolIndex();
    node.accept(new IndexSymbols(compilationUnit, file, lambdaIndex) {
      @Override
      public void endVisit(QualifiedName node) {
        Name root = node;
        while (root instanceof QualifiedName) {
          root = ((QualifiedName) root).getQualifier();
        }
        final IBinding binding = root.resolveBinding();
        if (null != binding) {
          indexReference(root, binding);
        }
      }
    }.setVerbose(false));
    return lambdaIndex;
  }

  @NotNull
  private static final Random random = new Random();

  public String randomIdentifier(ASTNode node) {
    final String id = "temp" + Long.toString(Math.abs(random.nextLong())).substring(0, 4);
    info(node, "Creating %s", id);
    return id;
  }

  public void debug(ASTNode node, String formatString, Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2];
    logger.debug(String.format(getFormatString(node, formatString, caller), args));
  }

  public void info(ASTNode node, String formatString, Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2];
    logger.info(String.format(getFormatString(node, formatString, caller), args));
  }

  public void warn(ASTNode node, String formatString, Object... args) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    final StackTraceElement caller = stackTrace[2];
    logger.warn(String.format(getFormatString(node, formatString, caller), args));
  }

  public String getFormatString(ASTNode node, String formatString, StackTraceElement caller) {
    return String.format("(%s) (%s) - %s", toString(caller), getLocation(node), formatString);
  }

  @NotNull
  public static String toString(StackTraceElement caller) {
    return caller.getFileName() + ":" + caller.getLineNumber();
  }

}
