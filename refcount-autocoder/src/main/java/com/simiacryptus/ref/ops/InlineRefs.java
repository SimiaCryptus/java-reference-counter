/*
 * Copyright (c) 2020 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.ref.ops;

import com.simiacryptus.ref.core.ASTUtil;
import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.lang.RefIgnore;
import org.eclipse.jdt.core.dom.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

@RefIgnore
public class InlineRefs extends RefASTOperator {

  protected InlineRefs(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    super(projectInfo, compilationUnit, file);
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
        debug(node, "No previous statement for %s", node.getClass().getSimpleName());
        return null;
      }
    } else {
      final ASTNode parent = node.getParent();
      if (null == parent) {
        debug(node, "No previous statement for %s", node.getClass().getSimpleName());
        return null;
      } else {
        return previousStatement(parent);
      }
    }
  }

  public static class ModifyBlock extends InlineRefs {

    public ModifyBlock(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(@Nonnull Block node) {
      if (node.statements().size() == 1 && node.getParent() instanceof Block) {
        final Block parent = (Block) node.getParent();
        parent.statements().set(parent.statements().indexOf(node),
            copyIfAttached((ASTNode) node.statements().get(0)));
      }
    }
  }

  public static class ModifyAssignment extends InlineRefs {

    public ModifyAssignment(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(@Nonnull Assignment node) {
      Statement previousStatement = previousStatement(node);
      if (previousStatement != null) {
        if (previousStatement instanceof VariableDeclarationStatement) {
          final List fragments = ((VariableDeclarationStatement) previousStatement).fragments();
          if (1 == fragments.size()) {
            final VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(0);
            final StatementOfInterest lastMention = lastMention(ASTUtil.getBlock(node), fragment.getName(), 0);
            if (null == lastMention) {
              debug(node, "No mentions of %s", fragment.getName());
              return;
            }
            if (!lastMention.statement.equals(previousStatement)) {
              debug(node, "Assignment is not last usage of %s", fragment.getName());
              return;
            }
            if (fragment.getName().toString().equals(node.getRightHandSide().toString())) {
              debug(node, "Inlining %s", fragment.getName());
              node.setRightHandSide(copyIfAttached(fragment.getInitializer()));
              debug(previousStatement, "delete %s", previousStatement);
              previousStatement.delete();
            } else {
              debug(node, "previous variable %s is not used in %s", fragment.getName(), node.getRightHandSide());
            }
          } else {
            warn(node, "previous variable has multiple fragments");
          }
        } else {
          warn(node, "previous statement is %s", previousStatement.getClass().getSimpleName());
        }
      }
    }
  }

  public static class ModifyReturnStatement extends InlineRefs {

    public ModifyReturnStatement(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(@Nonnull ReturnStatement node) {
      if (node.getExpression() instanceof Name) {
        Statement previousStatement = previousStatement(node);
        if (previousStatement != null) {
          if (previousStatement instanceof VariableDeclarationStatement) {
            final List fragments = ((VariableDeclarationStatement) previousStatement).fragments();
            if (1 == fragments.size()) {
              final VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(0);
              final StatementOfInterest lastMention = lastMention(ASTUtil.getBlock(node), fragment.getName(), 0);
              if (null == lastMention) {
                debug(node, "No mentions of %s", fragment.getName());
                return;
              }
              if (lastMention.statement.equals(previousStatement)) {
                debug(node, "Assignment is not last usage of %s", fragment.getName());
                return;
              }
              if (fragment.getName().toString().equals(node.getExpression().toString())) {
                final Expression initializer = fragment.getInitializer();
                debug(node, "Inlining %s initialized by %s", fragment.getName(), initializer.getClass().getSimpleName());
                if (initializer instanceof ArrayInitializer) {
                  final ArrayCreation arrayCreation = ast.newArrayCreation();
                  arrayCreation.setType(ast.newArrayType(getType(initializer, initializer.resolveTypeBinding().getElementType().getQualifiedName(), false)));
                  arrayCreation.setInitializer(copyIfAttached((ArrayInitializer) initializer));
                  node.setExpression(arrayCreation);
                } else {
                  node.setExpression(copyIfAttached(initializer));
                }
                debug(previousStatement, "delete %s", previousStatement);
                previousStatement.delete();
              }
            }
          } else {
            debug(node, "Cannot inline - Previous statement is %s", previousStatement.getClass().getSimpleName());
          }
        } else {
          debug(node, "Cannot inline - No previous statement");
        }
      }
    }
  }
}
