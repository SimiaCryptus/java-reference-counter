package com.simiacryptus.devutil.ops;

import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

public class InsertGCs extends FileAstVisitor {

  public InsertGCs(CompilationUnit cu, File file) {
    super(cu, file);
  }

  @Override
  public void endVisit(MethodDeclaration node) {
    final AST ast = node.getAST();
    final Block body = node.getBody();
    if (!node.isConstructor()) {
      body.statements().add(0, newGCCall(ast));
    }
    if (null != body && body.statements().size() > 0) {
      final ArrayList<ReturnStatement> returnStatements = new ArrayList<>();
      body.accept(new ASTVisitor() {
        @Override
        public void endVisit(ReturnStatement node) {
          final Block block = ast.newBlock();
          block.statements().add(newGCCall(ast));
          block.statements().add(ASTNode.copySubtree(ast, node));
          replace(node, block);
          returnStatements.add(node);
        }
      });
      if (returnStatements.isEmpty()) {
        body.statements().add(newGCCall(ast));
      }
    }
  }

  @NotNull
  public ASTNode newGCCall(AST ast) {
    final MethodInvocation methodInvocation = ast.newMethodInvocation();
    methodInvocation.setExpression(ast.newSimpleName("System"));
    methodInvocation.setName(ast.newSimpleName("gc"));
    return ast.newExpressionStatement(methodInvocation);
  }
}