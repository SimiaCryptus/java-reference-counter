package com.simiacryptus.devutil.ref;

import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;

class InlineRefs extends RefFileAstVisitor {

  InlineRefs(CompilationUnit compilationUnit, File file) {
    super(compilationUnit, file);
  }

  @Override
  public void endVisit(@NotNull Block node) {
    if (node.statements().size() == 1 && node.getParent() instanceof Block) {
      final Block parent = (Block) node.getParent();
      parent.statements().set(parent.statements().indexOf(node),
          ASTNode.copySubtree(node.getAST(), (ASTNode) node.statements().get(0)));
    }
  }

  @Override
  public void endVisit(@NotNull Assignment node) {
    Statement previousStatement = previousStatement(node);
    if (previousStatement != null) {
      if (previousStatement instanceof VariableDeclarationStatement) {
        final List fragments = ((VariableDeclarationStatement) previousStatement).fragments();
        if (1 == fragments.size()) {
          final VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(0);
          if (fragment.getName().toString().equals(node.getRightHandSide().toString())) {
            info(node, "Inlining %s", fragment.getName());
            node.setRightHandSide((Expression) ASTNode.copySubtree(node.getAST(), fragment.getInitializer()));
            info(previousStatement, "delete %s", previousStatement);
            previousStatement.delete();
          } else {
            warn(node, "previous variable %s is not used in %s", fragment.getName(), node.getRightHandSide());
          }
        } else {
          warn(node, "previous variable has multiple fragments");
        }
      } else {
        warn(node, "previous statement is %s", previousStatement.getClass().getSimpleName());
      }
    }
  }

  @Override
  public void endVisit(@NotNull ReturnStatement node) {
    if (node.getExpression() instanceof Name) {
      Statement previousStatement = previousStatement(node);
      if (previousStatement != null) {
        if (previousStatement instanceof VariableDeclarationStatement) {
          final List fragments = ((VariableDeclarationStatement) previousStatement).fragments();
          if (1 == fragments.size()) {
            final VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(0);
            if (fragment.getName().toString().equals(node.getExpression().toString())) {
              info(node, "Inlining %s", fragment.getName());
              node.setExpression((Expression) ASTNode.copySubtree(node.getAST(), fragment.getInitializer()));
              info(previousStatement, "delete %s", previousStatement);
              previousStatement.delete();
            }
          }
        } else {
          info(node, "Cannot inline - Previous statement is %s", previousStatement.getClass().getSimpleName());
        }
      } else {
        info(node, "Cannot inline - No previous statement");
      }
    }
  }

  @Nullable
  public Statement previousStatement(@Nonnull ASTNode node) {
    if (node instanceof Statement) {
      final ASTNode statementParent = node.getParent();
      if (statementParent instanceof Block) {
        final List statements = ((Block) statementParent).statements();
        final int statementNumber = statements.indexOf(node);
        if (statementNumber > 0) {
          return (Statement) statements.get(statementNumber - 1);
        } else {
          //info(String.format("No previous statement for %s at %s", node.getClass().getSimpleName(), location(node)));
          return null;
        }
      } else {
        info(node, "No previous statement for %s", node.getClass().getSimpleName());
        return null;
      }
    } else {
      final ASTNode parent = node.getParent();
      if (null == parent) {
        info(node, "No previous statement for %s", node.getClass().getSimpleName());
        return null;
      } else {
        return previousStatement(parent);
      }
    }
  }
}
