package com.simiacryptus.devutil.ref;

import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;

class ModifyFieldSets extends RefFileAstVisitor {

  ModifyFieldSets(CompilationUnit compilationUnit, File file) {
    super(compilationUnit, file);
  }

  @Override
  public void endVisit(@NotNull Assignment node) {
    if (node.getLeftHandSide() instanceof FieldAccess) {
      final FieldAccess fieldAccess = (FieldAccess) node.getLeftHandSide();
      final ITypeBinding typeBinding = fieldAccess.resolveTypeBinding();
      if (null == typeBinding) {
        warn(node, "Unresolved binding");
        return;
      }
      if (!isRefCounted(node, typeBinding)) return;
      final ASTNode parent = node.getParent();
      if (parent instanceof ExpressionStatement) {
        final ASTNode parent2 = parent.getParent();
        if (parent2 instanceof Block) {
          final Block block = (Block) parent2;
          final int lineNumber = block.statements().indexOf(parent);
          final Expression rightHandSide = node.getRightHandSide();
          final AST ast = node.getAST();
          if (rightHandSide instanceof Name) {
            block.statements().add(lineNumber, freeRefStatement(fieldAccess, fieldAccess.resolveTypeBinding()));
            node.setRightHandSide(wrapAddRef(rightHandSide, typeBinding));
            info(node, "Simple field-set statement at line " + lineNumber);
          } else {
            final Block exchangeBlock = ast.newBlock();
            final String identifier = randomIdentifier(node);
            exchangeBlock.statements().add(newLocalVariable(identifier, rightHandSide, getType(ast, typeBinding.getName())));
            exchangeBlock.statements().add(freeRefStatement(fieldAccess, fieldAccess.resolveTypeBinding()));
            final Assignment assignment = ast.newAssignment();
            assignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, fieldAccess));
            assignment.setOperator(Assignment.Operator.ASSIGN);
            assignment.setRightHandSide(wrapAddRef(ast.newSimpleName(identifier), typeBinding));
            exchangeBlock.statements().add(ast.newExpressionStatement(assignment));
            block.statements().set(lineNumber, exchangeBlock);
            info(node, "Complex field-set statement at line " + lineNumber);
          }
        } else {
          warn(node, "Non-block field-set statement: %s (%s)", parent.getClass(), parent);
        }
      } else {
        warn(node, "Non-ExpressionStatement field-set statement: %s (%s)", parent.getClass(), parent);
      }
    }
    super.endVisit(node);
  }

}
