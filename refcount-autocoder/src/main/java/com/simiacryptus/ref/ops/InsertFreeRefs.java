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
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import org.eclipse.jdt.core.dom.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * This class is responsible for inserting free references.
 *
 * @docgenVersion 9
 */
@RefIgnore
public class InsertFreeRefs extends RefASTOperator {

  protected InsertFreeRefs(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    super(projectInfo, compilationUnit, file);
  }

  /**
   * Returns the line number of the first line in the given block of code.
   *
   * @param body       the block of code to examine
   * @param declaredAt the line number where the block of code was declared
   * @return the line number of the first line in the given block of code
   * @docgenVersion 9
   */
  private static int firstLine(@Nonnull Block body, int declaredAt) {
    final List<Statement> statements = body.statements();
    for (int i = Math.max(declaredAt, 0); i < statements.size(); i++) {
      final Statement statement = statements.get(i);
      if (statement instanceof ConstructorInvocation) {
        declaredAt = i;
        break;
      }
      if (statement instanceof SuperConstructorInvocation) {
        declaredAt = i;
        break;
      }
    }
    return declaredAt;
  }

  /**
   * Adds a free type reference for the given variable declaration and type binding.
   *
   * @param declaration the variable declaration to add a free type reference for
   * @param typeBinding the type binding to add a free type reference for
   * @docgenVersion 9
   */
  public void addFreeRef(@Nonnull VariableDeclaration declaration, @Nonnull ITypeBinding typeBinding) {
    if (skip(declaration)) return;
    debug(1, declaration, "addFreeRef: %s", declaration);
    if (isRefCounted(declaration, typeBinding)) {
      final SimpleName name = declaration.getName();
      ASTNode parent = declaration.getParent();
      boolean isNonNull = ASTUtil.hasAnnotation(declaration.resolveBinding(), Nonnull.class);
      if (parent instanceof MethodDeclaration) {
        final MethodDeclaration node = (MethodDeclaration) parent;
        final Block body = node.getBody();
        if (null != body) insertFreeRefs(typeBinding, name, body, -1, isNonNull);
      } else if (parent instanceof LambdaExpression) {
        final LambdaExpression lambdaExpression = (LambdaExpression) parent;
        final ASTNode body = lambdaExpression.getBody();
        if (body instanceof Block) {
          insertFreeRefs(typeBinding, name, (Block) body, -1, isNonNull);
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
          debug(lambdaExpression, "Replace lambda %s with block %s", lambdaExpression, block);
          insertFreeRefs(typeBinding, name, block, -1, isNonNull);
        }
      } else if (parent instanceof VariableDeclarationStatement) {
        final ASTNode parentParent = parent.getParent();
        if (parentParent instanceof Block) {
          final Block parentBlock = (Block) parentParent;
          insertFreeRefs(typeBinding, name, parentBlock, parentBlock.statements().indexOf(parent), isNonNull);
        } else {
          warn(declaration, "Cannot add freeRef for %s (VariableDeclarationStatement) in %s : %s", name, parentParent.getClass(), parentParent.toString().trim());
        }
      } else if (parent instanceof FieldDeclaration) {
        if (!Modifier.isStatic(((FieldDeclaration) parent).getModifiers())) {
          add_freeRef_entry(declaration, typeBinding, name, parent);
        }
      } else {
        warn(declaration, "Cannot add freeRef for %s in %s : %s",
            name,
            parent.getClass(), parent.toString().trim());
      }
    } else {
      debug(declaration, "%s is not refcounted (%s)", declaration, typeBinding);
    }
  }

  /**
   * Adds a free reference entry for the given declaration, type binding, and name.
   *
   * @param declaration the declaration to add
   * @param typeBinding the type binding to add
   * @param name        the name to add
   * @param parent      the parent AST node
   * @docgenVersion 9
   */
  public void add_freeRef_entry(@Nonnull VariableDeclaration declaration, @Nonnull ITypeBinding typeBinding, @Nonnull SimpleName name, @Nonnull ASTNode parent) {
    final ASTNode fieldParent = parent.getParent();
    if (fieldParent instanceof TypeDeclaration) {
      final TypeDeclaration typeDeclaration = (TypeDeclaration) fieldParent;
      final Optional<MethodDeclaration> freeMethodOpt = ASTUtil.findMethod(typeDeclaration, "_free");
      if (freeMethodOpt.isPresent()) {
        debug(declaration, "Adding freeRef for %s to (%s)", name, getLocation(name));
        final boolean isFinal = isFinal(declaration);
        final List<Statement> statements = RefUtil.get(freeMethodOpt).getBody().statements();
        if (!isFinal) {
          statements.add(0, ast.newExpressionStatement(setToNull(name)));
        }
        statements.add(0, isFinal && ASTUtil.hasAnnotation(declaration.resolveBinding(), Nonnull.class) ? newFreeRef(name, typeBinding) : freeRefStatement(name, typeBinding));
      } else {
        fatal(declaration, "Cannot add freeRef for %s::%s - no _free method", typeDeclaration.getName(), declaration.getName());
      }
    } else {
      fatal(declaration, "Cannot add freeRef for %s (FieldDeclaration) in %s : %s", name, fieldParent.getClass(), fieldParent.toString().trim());
    }
  }

  /**
   * Inserts a free reference of the given type binding into the given node in the given body at the given line.
   *
   * @param typeBinding the type binding of the free reference to insert
   * @param node        the node to insert the free reference into
   * @param body        the body containing the node
   * @param line        the line number of the node in the body
   * @param isNonNull   whether the free reference is non-null
   * @docgenVersion 9
   */
  public void insertFreeRef(@Nonnull ITypeBinding typeBinding, @Nonnull SimpleName node, @Nonnull Block body, int line, boolean isNonNull) {
    final Statement statement = isNonNull ? newFreeRef(node, typeBinding) : freeRefStatement(node, typeBinding);
    if (line < 0) {
      body.statements().add(0, statement);
    } else {
      body.statements().add(line + 1, statement);
    }
    debug(1, body, "Added freeRef for value %s (%s) at line %s", node, typeBinding.getQualifiedName(), line);
  }

  /**
   * Inserts references to {@code typeBinding} into {@code body} at the location {@code declaredAt}.
   * If {@code isNonNull} is true, the references will be annotated with {@code @Nonnull}.
   *
   * @param typeBinding the type to insert references to
   * @param node        the node to insert references for
   * @param body        the body to insert references into
   * @param declaredAt  the location to insert references at
   * @param isNonNull   whether or not the references should be annotated with {@code @Nonnull}
   * @docgenVersion 9
   */
  public void insertFreeRefs(@Nonnull ITypeBinding typeBinding, @Nonnull SimpleName node, @Nonnull Block body, int declaredAt, boolean isNonNull) {
    debug(1, node, "Insert freeRef for %s", node);
    declaredAt = firstLine(body, declaredAt);
    final StatementOfInterest lastMention = lastMention(body, node, declaredAt);
    if (null == lastMention) {
      debug(node, "No mentions in body. Adding freeRef");
      insertFreeRef(typeBinding, node, body, declaredAt, isNonNull);
    } else {
      final List<StatementOfInterest> inScopeExits = exits(body, declaredAt, lastMention.line);
      if (lastMention.isComplexReturn()) {
        insertFreeRef_ComplexReturn(typeBinding, node, lastMention.block, lastMention.line, isNonNull);
      } else if (lastMention.isReturn()) {
        debug(node, "Last usage returns reference to %s", node);
      } else {
        //final boolean canFlowPast = lastMention.line == mainBodySize - 1 && inScopeExits.stream().anyMatch(x -> x.isReturnValue());
        if (!canFlowPast(body, lastMention.line)) {
          debug(node, "Last reference to %s at %s past all exits", node, lastMention.line);
        } else {
          debug(node, "Adding freeRef for %s after last reference at %s", node, lastMention.line);
          insertFreeRef(typeBinding, node, lastMention.block, lastMention.line, isNonNull);
        }
      }
      for (StatementOfInterest exitPoint : inScopeExits) {
        if (lastMention.block == exitPoint.block) continue;
        if (exitPoint.isReturn()) {
          final ReturnStatement returnStatement = (ReturnStatement) exitPoint.statement;
          final Expression expression = returnStatement.getExpression();
          if (null != expression) {
            if (expression.toString().equals(node.toString())) {
              debug(node, "Last usage returns reference");
            } else if (null != ASTUtil.findExpressions(returnStatement, (ASTNode) node).stream().findAny().orElse(null)) {
              debug(node, "Last usage uses reference");
              insertFreeRef_ComplexReturn(typeBinding, node, exitPoint.block, exitPoint.line, isNonNull);
            } else {
              debug(node, "Exit %s unrelated to %s at line %s", returnStatement, node, exitPoint.line);
              insertFreeRef(typeBinding, node, exitPoint.block, exitPoint.line - 1, isNonNull);
            }
          } else {
            debug(node, "Empty return");
            insertFreeRef(typeBinding, node, exitPoint.block, exitPoint.line - 1, isNonNull);
          }
        } else {
          final ThrowStatement throwStatement = (ThrowStatement) exitPoint.statement;
          final Expression expression = throwStatement.getExpression();
          if (null != expression) {
            if (expression.toString().equals(node.toString())) {
              debug(node, "Last usage throws reference");
            } else if (null != ASTUtil.findExpressions(throwStatement, (ASTNode) node).stream().findAny().orElse(null)) {
              debug(node, "Last usage (throws) uses reference");
              insertFreeRef_ComplexThrow(typeBinding, node, exitPoint.block, exitPoint.line, isNonNull);
            } else {
              debug(node, "Throw %s unrelated to %s at line %s", throwStatement, node, exitPoint.line);
              insertFreeRef(typeBinding, node, exitPoint.block, exitPoint.line - 1, isNonNull);
            }
          } else {
            debug(node, "Empty return");
            insertFreeRef(typeBinding, node, exitPoint.block, exitPoint.line - 1, isNonNull);
          }
        }
      }
    }
  }

  /**
   * Returns true if the given variable declaration is final.
   *
   * @param declaration the variable declaration to check
   * @return true if the declaration is final, false otherwise
   * @docgenVersion 9
   */
  protected boolean isFinal(@Nonnull VariableDeclaration declaration) {
    if (declaration instanceof SingleVariableDeclaration) {
      return Modifier.isFinal(((SingleVariableDeclaration) declaration).getModifiers());
    } else {
      final ASTNode declarationParent = declaration.getParent();
      if (declarationParent instanceof VariableDeclarationExpression) {
        return Modifier.isFinal(((VariableDeclarationExpression) declarationParent).getModifiers());
      } else if (declarationParent instanceof VariableDeclarationStatement) {
        return Modifier.isFinal(((VariableDeclarationStatement) declarationParent).getModifiers());
      } else if (declarationParent instanceof FieldDeclaration) {
        return Modifier.isFinal(((FieldDeclaration) declarationParent).getModifiers());
      } else {
        throw new RuntimeException(declarationParent.getClass().getName());
      }
    }
  }

  /**
   * This method is used to free the result of an expression.
   *
   * @param node        The expression node.
   * @param typeBinding The type binding of the expression.
   * @docgenVersion 9
   */
  protected void freeExpressionResult(@Nonnull Expression node, @Nonnull ITypeBinding typeBinding) {
    if (isRefCounted(node, typeBinding)) {
      debug(node, "Ref-returning method: %s", node);
      final ASTNode parent = node.getParent();
      if (parent instanceof MethodInvocation) {
        final MethodInvocation methodInvocation = (MethodInvocation) parent;
        final IMethodBinding methodBinding = resolveMethodBinding(methodInvocation);
        if (null == methodBinding) {
          warn(methodInvocation, "Unresolved Binding: %s", methodInvocation);
          return;
        }
        if (0 <= methodInvocation.arguments().indexOf(node)) {
          if (ASTUtil.hasAnnotation(methodBinding.getDeclaringClass(), RefAware.class)) {
            debug(node, "Ref is consumed as parameter to method: %s", node);
            return;
          } else {
            debug(node, "Ref is consumed by non-RefAware method: %s", node);
          }
        }
        final Expression expression = methodInvocation.getExpression();
        if (null != expression && null != ASTUtil.findExpressions(expression, (ASTNode) node).stream().findAny().orElse(null)) {
          if (!methodConsumesSelfRefs(methodBinding)) {
            debug(node, "Adding freeref for Ref-returning method: %s", node);
            freeRefs(node, typeBinding, false);
          } else {
            debug(node, "Chained method consumes ref for Ref-returning method: %s", node);
          }
          return;
        }
        debug(node, "Result consumed by method");
      } else if (parent instanceof FieldAccess) {
        final FieldAccess methodInvocation = (FieldAccess) parent;
        final IVariableBinding fieldBinding = resolveFieldBinding(methodInvocation);
        if (null == fieldBinding) {
          warn(methodInvocation, "Unresolved Binding: %s", methodInvocation);
        }
        if (null != ASTUtil.findExpressions(methodInvocation.getExpression(), (ASTNode) node).stream().findAny().orElse(null)) {
          freeRefs(node, typeBinding, false);
        } else {
          debug(node, "Result consumed by method");
        }
      } else if (parent instanceof ExpressionStatement) {
        final ASTNode statementParent = parent.getParent();
        if (statementParent instanceof Block) {
          final Block block = (Block) statementParent;
          final int lineNumber = block.statements().indexOf(parent);
          block.statements().set(lineNumber, freeRefUtilStatement(node, typeBinding));
          debug(node, "Wrapped method call with freeref at line " + lineNumber);
        } else {
          warn(node, "Non-block method call statement: %s (%s)", parent.getClass(), parent);
        }
      } else if (parent instanceof InfixExpression) {
        freeRefs(node, typeBinding, false);
        //addFreeRef(parent, node, typeBinding);
        debug(node, "Reftype wrapped as local variable: %s (%s)", parent.getClass(), parent);
      } else if (parent instanceof ReturnStatement) {
        debug(node, "Reftype returned from method: %s", parent);
      } else if (parent instanceof VariableDeclarationFragment) {
        debug(node, "Reftype assigned as local variable: %s", parent);
      } else if (null == parent) {
        warn(node, "Null parent: %s", node);
      } else {
        warn(node, "Non-ExpressionStatement method: %s (%s)", parent.getClass(), parent);
      }
    } else {
      warn(node, "Ignored method returning %s: %s", typeBinding.getQualifiedName(), node);
    }
  }

  /**
   * Returns true if the given line can have flow past it, false otherwise.
   *
   * @param body the body of the method
   * @param line the line to check
   * @return true if the given line can have flow past it, false otherwise
   * @docgenVersion 9
   */
  protected boolean canFlowPast(@Nonnull Block body, int line) {
    final List statements = body.statements();
    if (statements.size() - 1 > line) return true;
    return !isTerminal((Statement) statements.get(line));
  }

  /**
   * This method frees the references for the given node and type binding. If the node is non-null,
   * then the references are freed. Otherwise, they are not freed.
   *
   * @param node        the node to free references for
   * @param typeBinding the type binding to free references for
   * @param isNonNull   whether or not the node is non-null
   * @docgenVersion 9
   */
  protected void freeRefs(@Nonnull Expression node, @Nonnull ITypeBinding typeBinding, boolean isNonNull) {
    final LambdaExpression lambda = ASTUtil.getLambda(node);
    if (null != lambda) {
      toBlock(lambda);
    }

    Statement statement = ASTUtil.getStatement(node);
    if (null == statement) {
      warn(node, "No containing statement for %s", node);
      return;
    }

    final ASTNode statementParent = statement.getParent();
    if (!(statementParent instanceof Block)) {
      Block block = ast.newBlock();
      replace(statement, block);
      block.statements().add(statement);
      debug(node, "Wrapped %s in new block", node);
      freeRefs(node, typeBinding, isNonNull);
      return;
    }
    Block block = (Block) statementParent;
    if (statement instanceof ReturnStatement) {
      final Expression expression = ((ReturnStatement) statement).getExpression();
      final String identifier = getTempIdentifier(node);
      final ITypeBinding returnTypeBinding = resolveTypeBinding(expression);
      if (null == returnTypeBinding) {
        warn(expression, "Unresolved binding for %s", expression);
        return;
      }
      final Type type = getType(expression, returnTypeBinding, true);
      assert type != null;
      final VariableDeclarationStatement localVariable = newLocalVariable(identifier, expression, type);
      replace(expression, ast.newSimpleName(identifier));
      final List statements = block.statements();
      final int lineNumber = statements.indexOf(statement);
      statements.add(lineNumber, localVariable);
      debug(node, "Wrapped %s in new variable %s", node, identifier);
      freeRefs(ASTUtil.findExpressions(((VariableDeclarationFragment) localVariable.fragments().get(0)).getInitializer(), node).stream().findAny().orElse(null), typeBinding, isNonNull);
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
    final SimpleName name = ast.newSimpleName(identifier);
    statements.add(lineNumber + 1, isNonNull ? newFreeRef(name, typeBinding) : freeRefStatement(name, typeBinding));
    statements.add(lineNumber, newLocalVariable(identifier, node, type));
    debug(node, "Wrapped method call with freeRef for %s at line %s", name, lineNumber);
  }

  /**
   * Returns the type of the given node.
   *
   * @param node          the node to get the type of
   * @param typeBinding   the type binding of the node
   * @param isDeclaration whether the node is a declaration
   * @return the type of the node, or null if the type could not be determined
   * @docgenVersion 9
   */
  @Nullable
  protected Type getType(@Nonnull Expression node, @Nonnull ITypeBinding typeBinding, boolean isDeclaration) {
    final Type type = getType(node, typeBinding.getQualifiedName(), isDeclaration);
    if (null == type) {
      final ITypeBinding superclass = typeBinding.getSuperclass();
      if (null != superclass) {
        return getType(node, superclass, isDeclaration);
      }
    }
    return type;
  }

  /**
   * Returns true if the given statement has a break; false otherwise.
   *
   * @param statement the statement to check
   * @return true if the statement has a break; false otherwise
   * @docgenVersion 9
   */
  protected boolean hasBreak(Statement statement) {
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

  /**
   * Returns true if the given node represents a lambda expression with a return value, false otherwise.
   *
   * @param node          the node to check
   * @param lambdaParent  the parent node of the lambda expression
   * @param methodBinding the binding for the lambda expression, or null if none
   * @return true if the given node represents a lambda expression with a return value, false otherwise
   * @docgenVersion 9
   */
  protected boolean hasReturnValue(LambdaExpression node, ASTNode lambdaParent, @Nullable IMethodBinding methodBinding) {
    if (null != methodBinding) {
      if (methodBinding.getReturnType().toString().equals("void")) {
        return false;
      } else {
        return true;
      }
    } else {
      if (!(lambdaParent instanceof MethodInvocation)) {
        debug(lambdaParent, "lambda %s has no return value", lambdaParent);
        return false;
      }
      return this.hasReturnValue((MethodInvocation) lambdaParent, node);
    }
  }

  /**
   * Inserts a free reference of the given type binding into the given block at the given line.
   *
   * @param typeBinding the type binding of the free reference to insert
   * @param node        the node corresponding to the free reference to insert
   * @param block       the block into which to insert the free reference
   * @param line        the line at which to insert the free reference
   * @param isNonNull   whether the free reference is known to be non-null
   * @docgenVersion 9
   */
  protected void insertFreeRef_ComplexReturn(@Nonnull ITypeBinding typeBinding, @Nonnull SimpleName node, @Nonnull Block block, int line, boolean isNonNull) {
    debug(1, node, "Adding freeRef for %s at %s", node, line);
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
      tryStatement.getFinally().statements().add(isNonNull ? newFreeRef(node, typeBinding) : freeRefStatement(node, typeBinding));
      debug(node, "Added freeRef in finally for %s", node);
    } else {
      statements.add(line, isNonNull ? newFreeRef(copyIfAttached(node), typeBinding) : freeRefStatement(copyIfAttached(node), typeBinding));
      statements.add(line, newLocalVariable);
      returnStatement.setExpression(ast.newSimpleName(identifier));
      debug(node, "Added freeRef for return value %s using %s at %s", node, newLocalVariable, line);
    }
  }

  /**
   * @param typeBinding
   * @param node
   * @param block
   * @param line
   * @param isNonNull
   * @SuppressWarnings("unused")
   * @docgenVersion 9
   */
  @SuppressWarnings("unused")
  protected void insertFreeRef_ComplexThrow(@Nonnull ITypeBinding typeBinding, @Nonnull SimpleName node, @Nonnull Block block, int line, boolean isNonNull) {
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
      tryStatement.getFinally().statements().add(freeRefStatement(node, typeBinding));
      debug(block, "Added freeRef in finally for %s", node);
    } else {
      statements.add(line, freeRefStatement(copyIfAttached(node), typeBinding));
      statements.add(line, newLocalVariable);
      throwStatement.setExpression(ast.newSimpleName(identifier));
      debug(block, "Added freeRef for throw value %s", node);
    }
  }

  /**
   * Returns true if the given statement is a terminal statement.
   *
   * @param statement the statement to check
   * @return true if the statement is a terminal statement
   * @docgenVersion 9
   */
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

  /**
   * Sets the given name to null.
   *
   * @param name the name to set to null
   * @return the assignment setting the name to null
   * @docgenVersion 9
   */
  @Nonnull
  private Assignment setToNull(@Nonnull SimpleName name) {
    final Assignment assignment = ast.newAssignment();
    assignment.setLeftHandSide(copyIfAttached(name));
    assignment.setOperator(Assignment.Operator.ASSIGN);
    assignment.setRightHandSide(ast.newNullLiteral());
    return assignment;
  }

  /**
   * This class is responsible for modifying a variable declaration fragment.
   *
   * @docgenVersion 9
   */
  @RefIgnore
  public static class ModifyVariableDeclarationFragment extends InsertFreeRefs {
    public ModifyVariableDeclarationFragment(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
      super(projectInfo, compilationUnit, file);
    }

    /**
     * This method is called when the visitor encounters a variable declaration fragment.
     *
     * @param declaration the variable declaration fragment to visit
     * @docgenVersion 9
     */
    @Override
    public void endVisit(@Nonnull VariableDeclarationFragment declaration) {
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
  }

  /**
   * This class demonstrates how to modify a single
   * variable declaration in Java.
   *
   * @docgenVersion 9
   */
  @RefIgnore
  public static class ModifySingleVariableDeclaration extends InsertFreeRefs {
    public ModifySingleVariableDeclaration(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
      super(projectInfo, compilationUnit, file);
    }

    /**
     * This method is called when the end of a SingleVariableDeclaration is reached in the Java code.
     *
     * @param declaration the SingleVariableDeclaration that is ending
     * @docgenVersion 9
     */
    @Override
    public void endVisit(@Nonnull SingleVariableDeclaration declaration) {
      if (skip(declaration)) return;
      debug(declaration, "SingleVariableDeclaration: %s", declaration);
      final IVariableBinding variableBinding = resolveBinding(declaration);
      if (null == variableBinding) {
        warn(declaration, "Cannot add freeRef for field access %s (binding not resolved)", declaration.getName());
      } else {
        addFreeRef(declaration, getTypeBinding(declaration));
      }
    }
  }

  /**
   * This class is used to modify an instance of a class.
   *
   * @docgenVersion 9
   */
  @RefIgnore
  public static class ModifyClassInstanceCreation extends InsertFreeRefs {
    public ModifyClassInstanceCreation(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
      super(projectInfo, compilationUnit, file);
    }

    /**
     * This method is called when the visitor encounters a class instance creation expression.
     *
     * @param node the class instance creation expression to visit
     * @docgenVersion 9
     */
    @Override
    public void endVisit(@Nonnull ClassInstanceCreation node) {
      debug(node, "Processing constructor: %s", node);
      if (skip(node)) {
        debug(node, "Skip");
        return;
      }
      final IMethodBinding methodBinding = resolveConstructorBinding(node);
      if (null == methodBinding) {
        warn(node, "Unresolved constructor binding on %s", node);
        return;
      }
      freeExpressionResult(node, methodBinding.getDeclaringClass());
    }
  }

  /**
   * This class is responsible for modifying a method invocation.
   *
   * @docgenVersion 9
   */
  @RefIgnore
  public static class ModifyMethodInvocation extends InsertFreeRefs {
    public ModifyMethodInvocation(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
      super(projectInfo, compilationUnit, file);
    }

    /**
     * This method is called when the visitor encounters a method invocation node.
     *
     * @param node the method invocation node that was visited
     * @docgenVersion 9
     */
    @Override
    public void endVisit(@Nonnull MethodInvocation node) {
      if (skip(node)) return;
      //astRewrite.track(node);
      if (Arrays.asList("addRef", "addRef").contains(node.getName().toString())) return;
      final IMethodBinding methodBinding = resolveMethodBinding(node);
      if (null == methodBinding) {
        warn(node, "Unresolved binding on %s", node);
        return;
      }
      freeExpressionResult(node, methodBinding.getReturnType());
    }
  }

}
