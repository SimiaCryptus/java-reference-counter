package com.simiacryptus.devutil.ops;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.jetbrains.annotations.NotNull;

import java.io.File;

class RemoveGC extends FileAstVisitor {

  RemoveGC(CompilationUnit compilationUnit, File file) {
    super(compilationUnit, file);
  }

  @Override
  public void endVisit(@NotNull MethodInvocation node) {
    final Expression expression = node.getExpression();
    if (null == expression) return;
    final ITypeBinding typeBinding = expression.resolveTypeBinding();
    final String binaryName = typeBinding.getBinaryName();
    if (null != binaryName && binaryName.equals(System.class.getCanonicalName())) {
      node.getParent().delete();
    }
  }

}
