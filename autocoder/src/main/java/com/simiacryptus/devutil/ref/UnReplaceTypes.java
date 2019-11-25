package com.simiacryptus.devutil.ref;

import com.simiacryptus.devutil.AutoCoder;
import com.simiacryptus.lang.ref.wrappers.*;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class UnReplaceTypes extends RefFileAstVisitor {

  UnReplaceTypes(CompilationUnit compilationUnit, File file) {
    super(compilationUnit, file);
  }

  public void apply(Name node) {
    if (skip(node)) return;
    final IBinding binding = node.resolveBinding();
    if (binding instanceof ITypeBinding) {
      final String className = ((ITypeBinding) binding).getBinaryName();
      info(node, "Type found: %s", className);
      if (className.equals(RefStream.class.getCanonicalName())) {
        replace(node, AutoCoder.newQualifiedName(node.getAST(), Stream.class));
      }
      if (className.equals(RefArrays.class.getCanonicalName())) {
        replace(node, AutoCoder.newQualifiedName(node.getAST(), Arrays.class));
      }
      if (className.equals(RefArrayList.class.getCanonicalName())) {
        replace(node, AutoCoder.newQualifiedName(node.getAST(), ArrayList.class));
      }
      if (className.equals(RefList.class.getCanonicalName())) {
        replace(node, AutoCoder.newQualifiedName(node.getAST(), List.class));
      }
      if (className.equals(RefMap.class.getCanonicalName())) {
        replace(node, AutoCoder.newQualifiedName(node.getAST(), HashMap.class));
      }
      if (className.equals(RefSet.class.getCanonicalName())) {
        replace(node, AutoCoder.newQualifiedName(node.getAST(), HashSet.class));
      }
      if (className.equals(RefCollectors.class.getCanonicalName())) {
        replace(node, AutoCoder.newQualifiedName(node.getAST(), Collectors.class));
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
