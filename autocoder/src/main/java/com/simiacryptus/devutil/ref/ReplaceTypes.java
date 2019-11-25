package com.simiacryptus.devutil.ref;

import com.simiacryptus.devutil.AutoCoder;
import com.simiacryptus.lang.ref.wrappers.*;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ReplaceTypes extends RefFileAstVisitor {

  ReplaceTypes(CompilationUnit compilationUnit, File file) {
    super(compilationUnit, file);
  }

  public void apply(Name node) {
    if (skip(node)) return;
    final IBinding binding = node.resolveBinding();
    if (binding instanceof ITypeBinding) {
      final String className = ((ITypeBinding) binding).getBinaryName();
      if (className.equals(Stream.class.getCanonicalName())) {
        replace(node, AutoCoder.newQualifiedName(node.getAST(), RefStream.class));
      }
      if (className.equals(Arrays.class.getCanonicalName())) {
        replace(node, AutoCoder.newQualifiedName(node.getAST(), RefArrays.class));
      }
      if (className.equals(ArrayList.class.getCanonicalName())) {
        replace(node, AutoCoder.newQualifiedName(node.getAST(), RefList.class));
      }
      if (className.equals(HashMap.class.getCanonicalName())) {
        replace(node, AutoCoder.newQualifiedName(node.getAST(), RefMap.class));
      }
      if (className.equals(HashSet.class.getCanonicalName())) {
        replace(node, AutoCoder.newQualifiedName(node.getAST(), RefSet.class));
      }
      if (className.equals(Collectors.class.getCanonicalName())) {
        replace(node, AutoCoder.newQualifiedName(node.getAST(), RefCollectors.class));
      }
    }
  }

  @Override
  public void endVisit(SimpleName node) {
    if (node.getParent() instanceof QualifiedName) return;
    apply(node);
  }

  @Override
  public void endVisit(QualifiedName node) {
    apply(node);
  }

  public boolean skip(@NotNull ASTNode node) {
    return AutoCoder.enclosingMethods(node).stream().filter(enclosingMethod -> {
      final String methodName = enclosingMethod.getName();
      return methodName.equals("addRefs") || methodName.equals("freeRefs");
    }).findFirst().isPresent();
  }

}
