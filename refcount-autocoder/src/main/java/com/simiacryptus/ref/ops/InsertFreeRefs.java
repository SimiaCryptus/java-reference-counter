/*
 * Copyright (c) 2019 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.ref.ops;

import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RefIgnore
public class InsertFreeRefs extends RefFileAstVisitor {

  public InsertFreeRefs(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
    super(projectInfo, compilationUnit, file);
  }

  public void addFreeRef(@Nonnull VariableDeclaration declaration, @Nonnull ITypeBinding typeBinding) {
    if (skip(declaration)) return;
    if (null == typeBinding) {
      warn(declaration, "Unresolved binding");
      return;
    }
    info(1, declaration, "addFreeRef: %s", declaration);
    if (isRefCounted(declaration, typeBinding)) {
      final SimpleName name = declaration.getName();
      ASTNode parent = declaration.getParent();
      if (parent instanceof MethodDeclaration) {
        final MethodDeclaration node = (MethodDeclaration) parent;
        final Block body = node.getBody();
        if (null != body) insertFreeRefs(typeBinding, name, body, -1);
      } else if (parent instanceof LambdaExpression) {
        final LambdaExpression lambdaExpression = (LambdaExpression) parent;
        final ASTNode body = lambdaExpression.getBody();
        if (body instanceof Block) {
          insertFreeRefs(typeBinding, name, (Block) body, -1);
        } else {
          final Block block = ast.newBlock();
          lambdaExpression.setBody(block);
          final IMethodBinding methodBinding = resolveMethodBinding(lambdaExpression);
          if (hasReturnValue(lambdaExpression, lambdaExpression.getParent(), methodBinding)) {
            final ReturnStatement returnStatement = ast.newReturnStatement();
            returnStatement.setExpression(copyIfAttached((Expression) body));
            block.statements().add(returnStatement);
          } else {
            block.statements().add(ast.newExpressionStatement(copyIfAttached((Expression) body)));
          }
          info(lambdaExpression, "Replace lambda %s with block %s", lambdaExpression, block);
          insertFreeRefs(typeBinding, name, block, -1);
        }
      } else if (parent instanceof VariableDeclarationStatement) {
        final ASTNode parentParent = parent.getParent();
        if (parentParent instanceof Block) {
          final Block parentBlock = (Block) parentParent;
          insertFreeRefs(typeBinding, name, parentBlock, parentBlock.statements().indexOf(parent));
        } else {
          warn(declaration, "Cannot add freeRef for %s (VariableDeclarationStatement) in %s : %s", name, parentParent.getClass(), parentParent.toString().trim());
        }
      } else if (parent instanceof FieldDeclaration) {
        add_freeRef_entry(declaration, typeBinding, name, parent);
      } else {
        warn(declaration, "Cannot add freeRef for %s in %s : %s",
            name,
            parent.getClass(), parent.toString().trim());
      }
    } else {
      info(declaration, "%s is not refcounted (%s)", declaration, typeBinding);
    }
  }

  public void add_freeRef_entry(@Nonnull VariableDeclaration declaration, @Nonnull ITypeBinding typeBinding, SimpleName name, ASTNode parent) {
    final ASTNode fieldParent = parent.getParent();
    if (fieldParent instanceof TypeDeclaration) {
      final TypeDeclaration typeDeclaration = (TypeDeclaration) fieldParent;
      final Optional<MethodDeclaration> freeMethodOpt = findMethod(typeDeclaration, "_free");
      if (freeMethodOpt.isPresent()) {
        final IfStatement ifStatement = ast.newIfStatement();
        final InfixExpression nullTest = ast.newInfixExpression();
        nullTest.setLeftOperand(ast.newNullLiteral());
        nullTest.setOperator(InfixExpression.Operator.NOT_EQUALS);
        nullTest.setRightOperand(copyIfAttached(name));
        ifStatement.setExpression(nullTest);
        ifStatement.setThenStatement(ast.newExpressionStatement(newFreeRef(name, typeBinding)));
        info(declaration, "Adding freeRef for %s::%s to %s", typeDeclaration.getName(), declaration.getName(), "(" + getLocation(name) + ")");
        freeMethodOpt.get().getBody().statements().add(0, ifStatement);
      } else {
        warn(declaration, "Cannot add freeRef for %s::%s - no _free method", typeDeclaration.getName(), declaration.getName());
      }
    } else {
      warn(declaration, "Cannot add freeRef for %s (FieldDeclaration) in %s : %s", name, fieldParent.getClass(), fieldParent.toString().trim());
    }
  }

  private void apply(@NotNull Expression node, ITypeBinding typeBinding) {
    if (isRefCounted(node, typeBinding)) {
      info(node, "Ref-returning method: %s", node);
      final ASTNode parent = node.getParent();
      if (parent instanceof MethodInvocation) {
        final MethodInvocation methodInvocation = (MethodInvocation) parent;
        final IMethodBinding methodBinding = resolveMethodBinding(methodInvocation);
        if (null == methodBinding) {
          warn(methodInvocation, "Unresolved Binding: %s", methodInvocation);
          return;
        }
        if (0 <= methodInvocation.arguments().indexOf(node)) {
          if (hasAnnotation(methodBinding.getDeclaringClass(), RefAware.class)) {
            info(node, "Ref is consumed as parameter to method: %s", node);
            return;
          } else {
            info(node, "Ref is consumed by non-RefAware method: %s", node);
          }
        }
        final Expression expression = methodInvocation.getExpression();
        if (null != expression && null != findExpressions(expression, (ASTNode) node).stream().findAny().orElse(null)) {
          if (!methodConsumesSelfRefs(methodBinding)) {
            info(node, "Adding freeref for Ref-returning method: %s", node);
            freeRefs(node, typeBinding);
          } else {
            info(node, "Chained method consumes ref for Ref-returning method: %s", node);
          }
          return;
        }
        if (!consumesRefs(methodBinding, null == expression ? null : resolveTypeBinding(expression))) {
          freeRefs(node, typeBinding);
          info(node, "Adding freeref for Ref-returning method: %s", node);
          return;
        }
        info(node, "Result consumed by method");
      } else if (parent instanceof FieldAccess) {
        final FieldAccess methodInvocation = (FieldAccess) parent;
        final IVariableBinding fieldBinding = resolveFieldBinding(methodInvocation);
        if (null == fieldBinding) {
          warn(methodInvocation, "Unresolved Binding: %s", methodInvocation);
        }
        if (null != findExpressions(methodInvocation.getExpression(), (ASTNode) node).stream().findAny().orElse(null)) {
          freeRefs(node, typeBinding);
        } else {
          info(node, "Result consumed by method");
        }
      } else if (parent instanceof ExpressionStatement) {
        final ASTNode statementParent = parent.getParent();
        if (statementParent instanceof Block) {
          final Block block = (Block) statementParent;
          final int lineNumber = block.statements().indexOf(parent);
          block.statements().set(lineNumber, freeRefUtilStatement(node, typeBinding));
          info(node, "Wrapped method call with freeref at line " + lineNumber);
        } else {
          warn(node, "Non-block method call statement: %s (%s)", parent.getClass(), parent);
        }
      } else if (parent instanceof InfixExpression) {
        freeRefs(node, typeBinding);
        //addFreeRef(parent, node, typeBinding);
        info(node, "Reftype wrapped as local variable: %s (%s)", parent.getClass(), parent);
      } else if (parent instanceof ReturnStatement) {
        info(node, "Reftype returned from method: %s", parent);
      } else if (parent instanceof VariableDeclarationFragment) {
        info(node, "Reftype assigned as local variable: %s", parent);
      } else if (null == parent) {
        warn(node, "Null parent: %s", node);
      } else {
        warn(node, "Non-ExpressionStatement method: %s (%s)", parent.getClass(), parent);
      }
    } else {
      warn(node, "Ignored method returning %s: %s", typeBinding.getQualifiedName(), node);
    }
  }

  protected boolean canFlowPast(Block body, int line) {
    final List statements = body.statements();
    if (statements.size() - 1 > line) return true;
    return !isTerminal((Statement) statements.get(line));
  }

  @Override
  public void endVisit(@NotNull VariableDeclarationFragment declaration) {
    if (skip(declaration)) return;
    debug(declaration, "VariableDeclarationFragment: %s", declaration);
    final ASTNode parent = declaration.getParent();
    final ITypeBinding declarationType = getTypeBinding(declaration);
    if (null == declarationType) {
      warn(declaration, "Cannot resolve type of %s", declaration);
      return;
    }
    if (parent instanceof VariableDeclarationStatement) {
      final ITypeBinding typeBinding = resolveBinding(((VariableDeclarationStatement) parent).getType());
      if (null == typeBinding) {
        warn(declaration, "Cannot resolve type of %s", parent);
        return;
      }
      addFreeRef(declaration, declarationType);
    } else if (parent instanceof VariableDeclarationExpression) {
      final Type type = ((VariableDeclarationExpression) parent).getType();
      final ITypeBinding typeBinding = resolveBinding(type);
      if (null == typeBinding) {
        warn(declaration, "Cannot resolve type of %s", parent);
        return;
      }
      addFreeRef(declaration, declarationType);
    } else if (parent instanceof FieldDeclaration) {
      final ITypeBinding typeBinding = resolveBinding(((FieldDeclaration) parent).getType());
      if (null == typeBinding) {
        warn(declaration, "Cannot resolve type of %s", parent);
        return;
      }
      addFreeRef(declaration, declarationType);
    } else if (parent instanceof LambdaExpression) {
      final LambdaExpression lambdaExpression = (LambdaExpression) parent;
      final IMethodBinding methodBinding = resolveMethodBinding(lambdaExpression);
      if (methodBinding == null) {
        warn(declaration, "Cannot resolve method of %s", parent);
        return;
      }
      addFreeRef(declaration, declarationType);
    } else {
      warn(declaration, "Cannot handle %s", parent);
    }
  }

  @Override
  public void endVisit(@NotNull SingleVariableDeclaration declaration) {
    if (skip(declaration)) return;
    info(declaration, "SingleVariableDeclaration: %s", declaration);
    final IVariableBinding variableBinding = resolveBinding(declaration);
    if (null == variableBinding) {
      warn(declaration, "Cannot add freeRef for field access %s (binding not resolved)", declaration.getName());
    } else {
      addFreeRef(declaration, getTypeBinding(declaration));
    }
  }

  @Override
  public void endVisit(ClassInstanceCreation node) {
    info(node, "Processing constructor: %s", node);
    if (skip(node)) {
      info(node, "Skip");
      return;
    }
    final IMethodBinding methodBinding = resolveConstructorBinding(node);
    if (null == methodBinding) {
      warn(node, "Unresolved constructor binding on %s", node);
      return;
    }
    apply(node, methodBinding.getDeclaringClass());
  }

  @Override
  public void endVisit(@NotNull MethodInvocation node) {
    if (skip(node)) return;
    //astRewrite.track(node);
    if (Arrays.asList("addRef", "addRefs").contains(node.getName().toString())) return;
    final IMethodBinding methodBinding = resolveMethodBinding(node);
    if (null == methodBinding) {
      warn(node, "Unresolved binding on %s", node);
      return;
    }
    apply(node, methodBinding.getReturnType());
  }

  private void freeRefs(@NotNull Expression node, ITypeBinding typeBinding) {
    final LambdaExpression lambda = getLambda(node);
    if (null != lambda) {
      toBlock(lambda);
    }

    Statement statement = getStatement(node);
    if (null == statement) {
      warn(node, "No containing statement for %s", node);
      return;
    }

    final ASTNode statementParent = statement.getParent();
    if (!(statementParent instanceof Block)) {
      Block block = ast.newBlock();
      replace(statement, block);
      block.statements().add(statement);
      info(node, "Wrapped %s in new block", node);
      freeRefs(node, typeBinding);
      return;
    }
    Block block = (Block) statementParent;
    if (statement instanceof ReturnStatement) {
      final Expression expression = ((ReturnStatement) statement).getExpression();
      final String identifier = getTempIdentifier(node);
      final ITypeBinding returnTypeBinding = resolveTypeBinding(expression);
      final Type type = getType(expression, returnTypeBinding, true);
      final VariableDeclarationStatement localVariable = newLocalVariable(identifier, expression, type);
      replace(expression, ast.newSimpleName(identifier));
      final List statements = block.statements();
      final int lineNumber = statements.indexOf(statement);
      statements.add(lineNumber, localVariable);
      info(node, "Wrapped %s in new variable %s", node, identifier);
      freeRefs(findExpressions(((VariableDeclarationFragment) localVariable.fragments().get(0)).getInitializer(), node).stream().findAny().orElse(null), typeBinding);
      return;
    }
    final Type type = getType(node, typeBinding, true);
    if (type == null) {
      warn(node, "Cannot declare type %s", typeBinding);
      return;
    }
    final String identifier = getTempIdentifier(node);
    replace(node, ast.newSimpleName(identifier));
    assert node.getParent() == null;
    final List statements = block.statements();
    final int lineNumber = statements.indexOf(statement);
    statements.add(lineNumber + 1, ast.newExpressionStatement(newFreeRef(ast.newSimpleName(identifier), typeBinding)));
    statements.add(lineNumber, newLocalVariable(identifier, node, type));
    info(node, "Wrapped method call with freeref at line %s", lineNumber);
  }

  private Type getType(@NotNull Expression node, ITypeBinding typeBinding, boolean isDeclaration) {
    final Type type = getType(node, typeBinding.getQualifiedName(), isDeclaration);
    if (null == type) {
      final ITypeBinding superclass = typeBinding.getSuperclass();
      if (null != superclass) {
        return getType(node, superclass, isDeclaration);
      }
    }
    return type;
  }

  private boolean hasBreak(Statement statement) {
    if (statement instanceof BreakStatement) {
      return true;
    } else if (statement instanceof IfStatement) {
      final IfStatement ifStatement = (IfStatement) statement;
      final Statement thenStatement = ifStatement.getThenStatement();
      if (thenStatement != null && hasBreak(thenStatement)) return true;
      final Statement elseStatement = ifStatement.getElseStatement();
      if (elseStatement != null && hasBreak(elseStatement)) return true;
      return false;
    } else if (statement instanceof TryStatement) {
      final TryStatement tryStatement = (TryStatement) statement;
      final Block statementFinally = tryStatement.getFinally();
      if (null != statementFinally && hasBreak(statementFinally)) return true;
      final Block body = tryStatement.getBody();
      if (null != body && hasBreak(body)) return true;
      for (CatchClause catchStatement : (List<CatchClause>) tryStatement.catchClauses()) {
        if (hasBreak(catchStatement.getBody())) return true;
      }
      return false;
    } else if (statement instanceof Block) {
      final Block block = (Block) statement;
      return ((List<Statement>) block.statements()).stream().anyMatch(x -> hasBreak(x));
    } else if (statement instanceof SynchronizedStatement) {
      return hasBreak(((SynchronizedStatement) statement).getBody());
    } else {
      return false;
    }
  }

  private boolean hasReturnValue(LambdaExpression node, ASTNode lambdaParent, IMethodBinding methodBinding) {
    if (null != methodBinding) {
      if (methodBinding.getReturnType().toString().equals("void")) {
        return false;
      } else {
        return true;
      }
    } else {
      if (!(lambdaParent instanceof MethodInvocation)) {
        info(lambdaParent, "lambda %s has no return value", lambdaParent);
        return false;
      }
      return this.hasReturnValue((MethodInvocation) lambdaParent, node);
    }
  }

  public void insertFreeRef(@NotNull ITypeBinding typeBinding, @NotNull SimpleName node, Block body, int line) {
    final ExpressionStatement statement = ast.newExpressionStatement(newFreeRef(node, typeBinding));
    if (line < 0) {
      body.statements().add(0, statement);
    } else {
      body.statements().add(line + 1, statement);
    }
    info(1, body, "Added freeRef for value %s (%s) at line %s", node, typeBinding.getQualifiedName(), line);
  }

  private void insertFreeRef_ComplexReturn(@NotNull ITypeBinding typeBinding, @NotNull SimpleName node, Block block, int line) {
    info(1, node, "Adding freeRef for %s at %s", node, line);
    ReturnStatement returnStatement = (ReturnStatement) block.statements().get(line);
    final String identifier = getTempIdentifier(node);
    final List statements = block.statements();
    final Expression expression = returnStatement.getExpression();
    final VariableDeclarationStatement newLocalVariable = newLocalVariable(identifier, expression);
    if (null == newLocalVariable) {
      warn(node, "Cannot define local variable for %s", expression);
      final TryStatement tryStatement = ast.newTryStatement();
      replace(returnStatement, tryStatement);
      tryStatement.getBody().statements().add(returnStatement);
      tryStatement.setFinally(ast.newBlock());
      tryStatement.getFinally().statements().add(ast.newExpressionStatement(newFreeRef(node, typeBinding)));
    } else {
      statements.add(line, ast.newExpressionStatement(newFreeRef(copyIfAttached(node), typeBinding)));
      statements.add(line, newLocalVariable);
      returnStatement.setExpression(ast.newSimpleName(identifier));
      info(node, "Added freeRef for return value %s using %s at %s", node, newLocalVariable, line);
    }
  }

  private void insertFreeRef_ComplexThrow(@NotNull ITypeBinding typeBinding, @NotNull SimpleName node, Block block, int line) {
    ThrowStatement throwStatement = (ThrowStatement) block.statements().get(line);
    final String identifier = getTempIdentifier(block);
    final List statements = block.statements();
    final Expression expression = throwStatement.getExpression();
    final VariableDeclarationStatement newLocalVariable = newLocalVariable(identifier, expression);
    if (null == newLocalVariable) {
      warn(block, "Cannot define local variable for %s", expression);
      final TryStatement tryStatement = ast.newTryStatement();
      replace(throwStatement, tryStatement);
      tryStatement.getBody().statements().add(throwStatement);
      tryStatement.setFinally(ast.newBlock());
      tryStatement.getFinally().statements().add(ast.newExpressionStatement(newFreeRef(node, typeBinding)));
    } else {
      statements.add(line, ast.newExpressionStatement(newFreeRef(copyIfAttached(node), typeBinding)));
      statements.add(line, newLocalVariable);
      throwStatement.setExpression(ast.newSimpleName(identifier));
      info(block, "Added freeRef for throw value %s", node);
    }
  }

  public void insertFreeRefs(@NotNull ITypeBinding typeBinding, @NotNull SimpleName node, @NotNull Block body, int declaredAt) {
    info(1, node, "Insert freeRef for %s", node);
    if (null == body) {
      warn(node, "No body for %s", node);
      return;
    }
    final StatementOfInterest lastMention = lastMention(body, node, declaredAt);
    if (null == lastMention) {
      info(node, "No mentions in body. Adding freeRef");
      insertFreeRef(typeBinding, node, body, -1);
    } else {
      final int mainBodySize = body.statements().size();
      //final List<StatementOfInterest> allExits = exits(body, node, 0, mainBodySize);
      //final List<StatementOfInterest> postExits = exits(body, node, lastMention.line + 1, mainBodySize);
      final List<StatementOfInterest> inScopeExits = exits(body, declaredAt, lastMention.line);
      //final boolean hasReturns = allExits.stream().filter(x -> x.isReturn()).findAny().isPresent();
      if (lastMention.isComplexReturn()) {
        insertFreeRef_ComplexReturn(typeBinding, node, lastMention.block, lastMention.line);
      } else if (lastMention.isReturn()) {
        info(node, "Last usage returns reference to %s", node);
      } else {
        //final boolean canFlowPast = lastMention.line == mainBodySize - 1 && inScopeExits.stream().anyMatch(x -> x.isReturnValue());
        if (!canFlowPast(body, lastMention.line)) {
          info(node, "Last reference to %s at %s past all exits", node, lastMention.line);
        } else {
          info(node, "Adding freeRef for %s after last reference at %s", node, lastMention.line);
          insertFreeRef(typeBinding, node, lastMention.block, lastMention.line);
        }
      }
      for (StatementOfInterest exitPoint : inScopeExits) {
        if (lastMention.block == exitPoint.block) continue;
        if (exitPoint.isReturn()) {
          final ReturnStatement returnStatement = (ReturnStatement) exitPoint.statement;
          final Expression expression = returnStatement.getExpression();
          if (null != expression) {
            if (expression.toString().equals(node.toString())) {
              info(node, "Last usage returns reference");
            } else if (null != findExpressions(returnStatement, (ASTNode) node).stream().findAny().orElse(null)) {
              info(node, "Last usage uses reference");
              insertFreeRef_ComplexReturn(typeBinding, node, exitPoint.block, exitPoint.line);
            } else {
              info(node, "Exit %s unrelated to %s at line %s", returnStatement, node, exitPoint.line);
              insertFreeRef(typeBinding, node, exitPoint.block, exitPoint.line - 1);
            }
          } else {
            info(node, "Empty return");
            insertFreeRef(typeBinding, node, exitPoint.block, exitPoint.line - 1);
          }
        } else {
          final ThrowStatement throwStatement = (ThrowStatement) exitPoint.statement;
          final Expression expression = throwStatement.getExpression();
          if (null != expression) {
            if (expression.toString().equals(node.toString())) {
              info(node, "Last usage throws reference");
            } else if (null != findExpressions(throwStatement, (ASTNode) node).stream().findAny().orElse(null)) {
              info(node, "Last usage (throws) uses reference");
              insertFreeRef_ComplexThrow(typeBinding, node, exitPoint.block, exitPoint.line);
            } else {
              info(node, "Throw %s unrelated to %s at line %s", throwStatement, node, exitPoint.line);
              insertFreeRef(typeBinding, node, exitPoint.block, exitPoint.line - 1);
            }
          } else {
            info(node, "Empty return");
            insertFreeRef(typeBinding, node, exitPoint.block, exitPoint.line - 1);
          }
        }
      }
    }
  }

  protected boolean isTerminal(Statement statement) {
    if (statement instanceof ReturnStatement) return true;
    else if (statement instanceof ThrowStatement) return true;
    else if (statement instanceof IfStatement) {
      final IfStatement ifStatement = (IfStatement) statement;
      final Statement thenStatement = ifStatement.getThenStatement();
      if (thenStatement != null) {
        final Statement elseStatement = ifStatement.getElseStatement();
        if (elseStatement != null) {
          if (isTerminal(thenStatement) && isTerminal(elseStatement)) {
            return true;
          }
        }
      }
      return false;
    } else if (statement instanceof TryStatement) {
      final TryStatement tryStatement = (TryStatement) statement;
      final Block statementFinally = tryStatement.getFinally();
      if (null != statementFinally && isTerminal(statementFinally)) return true;
      final Block body = tryStatement.getBody();
      if (null != body && !isTerminal(body)) return false;
      for (CatchClause catchStatement : (List<CatchClause>) tryStatement.catchClauses()) {
        if (!isTerminal(catchStatement.getBody())) return false;
      }
      return true;
    } else if (statement instanceof Block) {
      final Block block = (Block) statement;
      final List statements = block.statements();
      if (statements.size() == 0) return false;
      return isTerminal((Statement) statements.get(statements.size() - 1));
    } else if (statement instanceof SynchronizedStatement) {
      return isTerminal(((SynchronizedStatement) statement).getBody());
    }
    if (statement instanceof WhileStatement) {
      final WhileStatement whileStatement = (WhileStatement) statement;
      if (!whileStatement.getExpression().toString().equals("true")) return false;
      return !hasBreak(whileStatement.getBody());
    } else {
      return false;
    }
  }

}
