package com.simiacryptus.devutil.ref;

import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.regex.Pattern;

class InlineTempVars extends RefFileAstVisitor {

  InlineTempVars(RefAutoCoder refAutoCoder, CompilationUnit compilationUnit, File file) {
    super(refAutoCoder, compilationUnit, file);
  }

  @Override
  public void endVisit(VariableDeclarationStatement node) {
    if (node.fragments().size() > 1) return;
    final Object head = node.fragments().get(0);
    if (head instanceof VariableDeclarationFragment) {
      final VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) head;
      final SimpleName name = variableDeclarationFragment.getName();
      if (Pattern.matches("temp\\d{0,4}", name.toString())) {
        final Expression expression = variableDeclarationFragment.getInitializer();
        final ASTNode parent = node.getParent();
        info(node, "delete %s", node);
        delete(node);
        if (parent instanceof Block) {
          final Block block = (Block) parent;
          block.accept(new ASTVisitor() {
            @Override
            public void endVisit(SimpleName simpleName) {
              if (contains(node, simpleName)) return;
              if (name.toString().equals(simpleName.toString())) {
                replace(simpleName, ASTNode.copySubtree(simpleName.getAST(), expression));
              }
            }
          });
        } else {
          warn(node, "Statement not in block");
        }
      }
    }
  }

}
