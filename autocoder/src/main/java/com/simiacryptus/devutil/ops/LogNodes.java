package com.simiacryptus.devutil.ops;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class LogNodes extends FileAstVisitor {

  public LogNodes(CompilationUnit compilationUnit, File file) {
    super(compilationUnit, file);
  }

  @Override
  public void preVisit(@NotNull ASTNode node) {
    info(node, "Previsit: %s at (%s:%s)", node.getClass(), file.getName(), compilationUnit.getLineNumber(node.getStartPosition()));
  }
}
