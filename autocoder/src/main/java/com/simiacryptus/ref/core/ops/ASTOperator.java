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

package com.simiacryptus.ref.core.ops;

import com.simiacryptus.ref.core.ASTUtil;
import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.core.SymbolIndex;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The type Ast operator.
 */
public abstract class ASTOperator extends ASTEditor {

  /**
   * Instantiates a new Ast operator.
   *
   * @param projectInfo     the project info
   * @param compilationUnit the compilation unit
   * @param file            the file
   */
  public ASTOperator(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    super(projectInfo, compilationUnit, file, true);
  }

  /**
   * Resolve binding type binding.
   *
   * @param typeDeclaration the type declaration
   * @return the type binding
   */
  protected final static ITypeBinding resolveBinding(@NotNull TypeDeclaration typeDeclaration) {
    return typeDeclaration.resolveBinding();
  }

  /**
   * All methods stream.
   *
   * @param targetClass the target class
   * @return the stream
   */
  @NotNull
  protected final Stream<IMethodBinding> allMethods(@NotNull ITypeBinding targetClass) {
    final ITypeBinding superclass = targetClass.getSuperclass();
    Stream<IMethodBinding> declaredMethods = Arrays.stream(targetClass.getDeclaredMethods());
    if (null != superclass && !superclass.getQualifiedName().equals(Object.class.getCanonicalName())) {
      declaredMethods = Stream.concat(declaredMethods, allMethods(superclass));
    }
    declaredMethods = Stream.concat(declaredMethods, Arrays.stream(targetClass.getInterfaces()).flatMap(this::allMethods));
    return declaredMethods.distinct();
  }

  /**
   * Delete.
   *
   * @param statement the statement
   */
  protected final void delete(@NotNull Statement statement) {
    debug(1, statement, "Deleting %s", statement);
    final ASTNode parent = statement.getParent();
    if (parent instanceof Block) {
      final Block block = (Block) parent;
      if (block.statements().size() == 1) {
        final ASTNode blockParent = block.getParent();
        if (blockParent instanceof LambdaExpression) {
          debug(statement, "Keeping block; removing single statement %s", statement);
        } else if (blockParent instanceof MethodDeclaration) {
          debug(statement, "Keeping block; removing single statement %s", statement);
        } else if (blockParent instanceof TryStatement) {
          final TryStatement tryStatement = (TryStatement) blockParent;
          final Block tryStatementFinally = tryStatement.getFinally();
          if (null != tryStatementFinally && block.equals(tryStatementFinally) && tryStatement.catchClauses().isEmpty()) {
            debug(statement, "Unwrapping try statement %s", tryStatement);
            replace(tryStatement, copyIfAttached(tryStatement.getBody()));
            return;
          }
        } else {
          delete(block);
          return;
        }
      }
    } else if (parent instanceof Initializer) {
      parent.delete();
      return;
    } else if (parent instanceof EnhancedForStatement) {
      final EnhancedForStatement forStatement = (EnhancedForStatement) parent;
      final List<Statement> evaluableStatements = getEvaluableStatements(forStatement.getExpression());
      if (evaluableStatements.size() == 0) {
        debug(statement, "Removing for %s", forStatement);
        delete(forStatement);
      } else if (evaluableStatements.size() == 1) {
        debug(statement, "Removing for %s; keeping %s", forStatement, evaluableStatements.get(0));
        replace(forStatement, evaluableStatements.get(0));
      } else {
        final Block block = ast.newBlock();
        block.statements().addAll(evaluableStatements);
        debug(statement, "Removing for %s; keeping %s", forStatement, block);
        replace(forStatement, block);
      }
      return;
    } else if (parent instanceof ForStatement) {
      final ForStatement forStatement = (ForStatement) parent;
      final List<Statement> evaluableStatements = getEvaluableStatements(forStatement.getExpression());
      if (evaluableStatements.size() == 0) {
        debug(statement, "Removing for %s", forStatement);
        delete(forStatement);
      } else if (evaluableStatements.size() == 1) {
        debug(statement, "Removing for %s; keeping %s", forStatement, evaluableStatements.get(0));
        replace(forStatement, evaluableStatements.get(0));
      } else {
        final Block block = ast.newBlock();
        block.statements().addAll(evaluableStatements);
        debug(statement, "Removing for %s; keeping %s", forStatement, block);
        replace(forStatement, block);
      }
      return;
    } else if (parent instanceof IfStatement) {
      final IfStatement ifStatement = (IfStatement) parent;
      final Statement elseStatement = ifStatement.getElseStatement();
      final Statement thenStatement = ifStatement.getThenStatement();
      if (thenStatement.equals(statement)) {
        final Expression condition = ifStatement.getExpression();
        if (null != elseStatement) {
          debug(statement, "Deleting if-then; inverting %s", ifStatement);
          ifStatement.setElseStatement(null);
          ifStatement.setThenStatement(copyIfAttached(elseStatement));
          final PrefixExpression prefixExpression = ast.newPrefixExpression();
          prefixExpression.setOperand(copyIfAttached(condition));
          prefixExpression.setOperator(PrefixExpression.Operator.NOT);
          ifStatement.setExpression(prefixExpression);
          return;
        } else {
          final List<Statement> evaluableStatements = getEvaluableStatements(condition);
          if (evaluableStatements.size() == 0) {
            debug(statement, "Removing if-then %s", ifStatement);
            delete(ifStatement);
          } else if (evaluableStatements.size() == 1) {
            debug(statement, "Removing if-then %s; keeping %s", ifStatement, evaluableStatements.get(0));
            replace(ifStatement, evaluableStatements.get(0));
          } else {
            final Block block = ast.newBlock();
            block.statements().addAll(evaluableStatements);
            debug(statement, "Removing if-then %s; keeping %s", ifStatement, block);
            replace(ifStatement, block);
          }
          return;
        }
      } else if (null != elseStatement && elseStatement.equals(statement)) {
        debug(statement, "Removing if-else %s", ifStatement);
        ifStatement.setElseStatement(null);
        return;
      } else {
        warn(statement, "Not sure how to remove %s from %s", statement, ifStatement);
      }
    }
    statement.delete();
  }

  /**
   * Enclosing methods list.
   *
   * @param node the node
   * @return the list
   */
  @NotNull
  protected final List<IMethodBinding> enclosingMethods(@Nullable ASTNode node) {
    final ArrayList<IMethodBinding> list = new ArrayList<>();
    if (null != node) {
      if (node instanceof MethodDeclaration) {
        list.addAll(enclosingMethods(node.getParent()));
        final MethodDeclaration methodDeclaration = (MethodDeclaration) node;
        final IMethodBinding methodBinding = resolveBinding(methodDeclaration);
        if (null != methodBinding) list.add(methodBinding);
      } else if (null != node.getParent()) {
        list.addAll(enclosingMethods(node.getParent()));
      }
    }
    return list;
  }

  /**
   * Exits list.
   *
   * @param block   the block
   * @param startAt the start at
   * @param endAt   the end at
   * @return the list
   */
  @NotNull
  protected final List<StatementOfInterest> exits(@NotNull Block block, int startAt, int endAt) {
    final ArrayList<StatementOfInterest> exits = exits_(block, startAt, endAt);
    debug(1, block, "Exits from %s to %s: \n\t%s", startAt, endAt, exits.stream().map(x ->
        String.format("%s at line %s", x.statement.toString().trim(), x.line)
    ).reduce((a, b) -> a + "\n\t" + b).orElse(""));
    return exits;
  }

  /**
   * Gets evaluable statements.
   *
   * @param expression the expression
   * @return the evaluable statements
   */
  @NotNull
  protected final List<Statement> getEvaluableStatements(Expression expression) {
    final ArrayList<Statement> statements = new ArrayList<>();
    if (ASTUtil.isEvaluable(expression)) {
      if (expression instanceof InfixExpression) {
        final InfixExpression infixExpression = (InfixExpression) expression;
        if (ASTUtil.isEvaluable(infixExpression.getLeftOperand())) {
          statements.addAll(getEvaluableStatements(infixExpression.getLeftOperand()));
        }
        if (ASTUtil.isEvaluable(infixExpression.getRightOperand())) {
          statements.addAll(getEvaluableStatements(infixExpression.getRightOperand()));
        }
      } else if (expression instanceof PrefixExpression) {
        final PrefixExpression prefixExpression = (PrefixExpression) expression;
        if (ASTUtil.isEvaluable(prefixExpression.getOperand())) {
          statements.addAll(getEvaluableStatements(prefixExpression.getOperand()));
        }
      } else if (expression instanceof PostfixExpression) {
        final PostfixExpression postfixExpression = (PostfixExpression) expression;
        if (ASTUtil.isEvaluable(postfixExpression.getOperand())) {
          statements.addAll(getEvaluableStatements(postfixExpression.getOperand()));
        }
      } else if (expression instanceof ParenthesizedExpression) {
        final ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression) expression;
        if (ASTUtil.isEvaluable(parenthesizedExpression.getExpression())) {
          statements.addAll(getEvaluableStatements(parenthesizedExpression.getExpression()));
        }
      } else if (expression instanceof CastExpression) {
        final CastExpression castExpression = (CastExpression) expression;
        if (ASTUtil.isEvaluable(castExpression.getExpression())) {
          statements.addAll(getEvaluableStatements(castExpression.getExpression()));
        }
      } else if (expression instanceof InstanceofExpression) {
        final InstanceofExpression castExpression = (InstanceofExpression) expression;
        if (ASTUtil.isEvaluable(castExpression.getLeftOperand())) {
          statements.addAll(getEvaluableStatements(castExpression.getLeftOperand()));
        }
      } else {
        statements.add(ast.newExpressionStatement(copyIfAttached(expression)));
      }
    }
    return statements;
  }

  /**
   * Gets line number.
   *
   * @param block the block
   * @param node  the node
   * @return the line number
   */
  protected final int getLineNumber(@NotNull Block block, ASTNode node) {
    return block.statements().indexOf(ASTUtil.getStatement(node));
  }

  /**
   * Gets symbol index.
   *
   * @param node the node
   * @return the symbol index
   */
  @NotNull
  protected final SymbolIndex getSymbolIndex(@NotNull ASTNode node) {
    final SymbolIndex lambdaIndex = new SymbolIndex();
    node.accept(new IndexSymbols(projectInfo, compilationUnit, file, lambdaIndex) {
      @Override
      public void endVisit(QualifiedName node) {
        Name root = node;
        while (root instanceof QualifiedName) {
          root = ((QualifiedName) root).getQualifier();
        }
        final IBinding binding = resolveBinding(root);
        if (null != binding) {
          indexReference(root, binding);
        }
      }
    }.setVerbose(true));
    return lambdaIndex;
  }

  /**
   * Gets type.
   *
   * @param node          the node
   * @param name          the name
   * @param isDeclaration the is declaration
   * @return the type
   */
  @Nullable
  @SuppressWarnings("unused")
  protected final Type getType(ASTNode node, @NotNull String name, boolean isDeclaration) {
    if (name.endsWith("[]")) {
      int rank = 0;
      while (name.endsWith("[]")) {
        name = name.substring(0, name.length() - 2);
        rank++;
      }
      final ArrayType arrayType = ast.newArrayType(getType(node, name, isDeclaration), rank);
      debug(node, "Converted type string %s to %s", name, arrayType);
      return arrayType;
    } else if (name.isEmpty()) {
      final WildcardType wildcardType = ast.newWildcardType();
      debug(node, "Cannot determine type of %s", node);
      return null;
    } else if (name.equals("?")) {
      final WildcardType wildcardType = ast.newWildcardType();
      debug(node, "Converted type string %s to %s", name, wildcardType);
      return wildcardType;
    } else if (name.contains("\\.")) {
      final SimpleType simpleType = ast.newSimpleType(ASTUtil.newQualifiedName(ast, name.split("\\.")));
      debug(node, "Converted type string %s to %s", name, simpleType);
      return simpleType;
    } else {
      final PrimitiveType.Code typeCode = PrimitiveType.toCode(name);
      if (null != typeCode) {
        final PrimitiveType primitiveType = ast.newPrimitiveType(typeCode);
        debug(node, "Converted type string %s to %s", name, primitiveType);
        return primitiveType;
      } else {
        final int typeArg = name.indexOf('<');
        if (typeArg < 0) {
          final SimpleType simpleType = ast.newSimpleType(ASTUtil.newQualifiedName(ast, name.split("\\.")));
          debug(node, "Converted type string %s to %s", name, simpleType);
          return simpleType;
        } else {
          if (!name.endsWith(">")) {
            warn(node, "Unclosed type args: %s", name);
            return null;
          }
          final String mainType = name.substring(0, typeArg);
          final String innerType = name.substring(typeArg + 1, name.length() - 1);
          ArrayList<Integer> delimiters = getTopTypeDelimiters(node, innerType);
          if (delimiters == null) return null;
          final ArrayList<Type> innerTypes = new ArrayList<>();
          for (int i = 0; i < delimiters.size(); i++) {
            final int to = delimiters.get(i);
            final int from = i == 0 ? 0 : (delimiters.get(i - 1) + 1);
            final String substring = innerType.substring(from, to);
            if (substring.isEmpty()) {
              innerTypes.add(ast.newWildcardType());
            } else {
              innerTypes.add(getTypeParameter(node, substring, isDeclaration));
            }
          }
          final ParameterizedType parameterizedType = ast.newParameterizedType(ast.newSimpleType(ASTUtil.newQualifiedName(ast, mainType.split("\\."))));
          parameterizedType.typeArguments().addAll(innerTypes);
          debug(node, "Converted type string %s to %s", name, parameterizedType);
          return parameterizedType;
        }
      }
    }
  }

  /**
   * Gets type.
   *
   * @param node          the node
   * @param isDeclaration the is declaration
   * @return the type
   */
  @Nullable
  protected final Type getType(@NotNull Expression node, boolean isDeclaration) {
    final ITypeBinding typeBinding = resolveTypeBinding(node);
    if (null == typeBinding) {
      warn(1, node, "Unresolved binding");
      return null;
    }
    final String qualifiedName = typeBinding.getQualifiedName();
    if (qualifiedName.isEmpty()) {
      warn(1, node, "No type string for %s", typeBinding);
      return null;
    }
    return getType(node, qualifiedName, isDeclaration);
  }

  /**
   * Has return value boolean.
   *
   * @param lambdaExpression the lambda expression
   * @return the boolean
   */
  protected final boolean hasReturnValue(@NotNull LambdaExpression lambdaExpression) {
    ASTNode parent = lambdaExpression.getParent();
    if (!(parent instanceof MethodInvocation)) {
      debug(lambdaExpression, "lambda %s has no return value", lambdaExpression);
      return false;
    }
    return hasReturnValue((MethodInvocation) parent, lambdaExpression);
  }

  /**
   * Has return value boolean.
   *
   * @param methodInvocation the method invocation
   * @param lambdaExpression the lambda expression
   * @return the boolean
   */
  @Nullable
  protected final boolean hasReturnValue(@NotNull MethodInvocation methodInvocation, LambdaExpression lambdaExpression) {
    final int argIndex = methodInvocation.arguments().indexOf(lambdaExpression);
    final ITypeBinding targetClass = resolveMethodBinding(methodInvocation).getParameterTypes()[argIndex];
    if (ASTUtil.derives(targetClass, Consumer.class)) {
      debug(methodInvocation, "lambda %s has no return value", lambdaExpression);
      return false;
    } else if (ASTUtil.derives(targetClass, Function.class)) {
      debug(methodInvocation, "lambda %s has return value", lambdaExpression);
      return true;
    } else if (ASTUtil.derives(targetClass, Predicate.class)) {
      debug(methodInvocation, "lambda %s has return value", lambdaExpression);
      return true;
    } else {
      final List<IMethodBinding> methods = allMethods(targetClass)
          .filter(x -> (x.getModifiers() & Modifier.STATIC) == 0)
          .filter(x -> (x.getModifiers() & Modifier.DEFAULT) == 0)
          .collect(Collectors.toList());
      if (methods.size() == 1) {
        final ITypeBinding returnType = methods.get(0).getReturnType();
        if (returnType.equals(PrimitiveType.VOID)) {
          debug(methodInvocation, "lambda %s has no return value", lambdaExpression);
          return false;
        } else {
          debug(methodInvocation, "Lambda interface %s returns a %s value", targetClass.getQualifiedName(), returnType.getQualifiedName());
          return true;
        }
      } else {
        warn(methodInvocation, "Cannot determine if %s returns a value; it has %s methods", targetClass.getQualifiedName(), methods.size());
        return false;
      }
    }
  }

  /**
   * Last mention statement of interest.
   *
   * @param block    the block
   * @param variable the variable
   * @param startAt  the start at
   * @return the statement of interest
   */
  @Nullable
  protected final StatementOfInterest lastMention(@NotNull Block block, @NotNull SimpleName variable, int startAt) {
    return lastMention(block, variable, startAt, block.statements().size(), 2);
  }

  /**
   * Last mention statement of interest.
   *
   * @param block    the block
   * @param variable the variable
   * @param startAt  the start at
   * @param endAt    the end at
   * @param frames   the frames
   * @return the statement of interest
   */
  @Nullable
  protected final StatementOfInterest lastMention(@NotNull Block block, @NotNull SimpleName variable, int startAt, int endAt, int frames) {
    IBinding binding = resolveBinding(variable);
    if (null == binding) {
      warn(frames, variable, "Unresolved binding");
      return null;
    } else {
      final List statements = block.statements();
      StatementOfInterest lastMention = null;
      for (int j = Math.max(startAt, 0); j < endAt; j++) {
        final Statement statement = (Statement) statements.get(j);
        if (!ASTUtil.findExpressions(statement, variable).isEmpty()) {
          lastMention = new StatementOfInterest(statement, j);
        }
      }
      if (null == lastMention) {
        debug(frames, variable, "No mention of %s", variable);
      } else {
        debug(frames, variable, "Last mention of %s: %s at line %s\"", variable, lastMention.statement.toString().trim(), lastMention.line);
      }
      return lastMention;
    }
  }

  /**
   * New local variable variable declaration statement.
   *
   * @param identifier the identifier
   * @param expression the expression
   * @param type       the type
   * @return the variable declaration statement
   */
  @NotNull
  protected final VariableDeclarationStatement newLocalVariable(@NotNull String identifier, @NotNull Expression expression, @NotNull Type type) {
    final VariableDeclarationFragment variableDeclarationFragment = ast.newVariableDeclarationFragment();
    variableDeclarationFragment.setName(ast.newSimpleName(identifier));
    variableDeclarationFragment.setInitializer(copyIfAttached(expression));
    final VariableDeclarationStatement variableDeclarationStatement = ast.newVariableDeclarationStatement(variableDeclarationFragment);
    variableDeclarationStatement.setType(type);
    return variableDeclarationStatement;
  }

  /**
   * New local variable variable declaration statement.
   *
   * @param identifier the identifier
   * @param expression the expression
   * @return the variable declaration statement
   */
  @Nullable
  protected final VariableDeclarationStatement newLocalVariable(@NotNull String identifier, @NotNull Expression expression) {
    final Type type = getType(expression, true);
    if (null == type) {
      warn(expression, "Cannot resolve type of %s", expression);
      return null;
    }
    return newLocalVariable(identifier, expression, type);
  }

  /**
   * Remove methods.
   *
   * @param node       the node
   * @param methodName the method name
   */
  protected final void removeMethods(@NotNull TypeDeclaration node, String methodName) {
    for (final Iterator iterator = node.bodyDeclarations().iterator(); iterator.hasNext(); ) {
      final ASTNode bodyDecl = (ASTNode) iterator.next();
      if (bodyDecl instanceof MethodDeclaration) {
        final SimpleName name = ((MethodDeclaration) bodyDecl).getName();
        if (name.toString().equals(methodName)) {
          iterator.remove();
          debug(bodyDecl, "Removing %s", bodyDecl);
        }
      }
    }
  }

  /**
   * Resolve r.
   *
   * @param <T>      the type parameter
   * @param <R>      the type parameter
   * @param node     the node
   * @param function the function
   * @return the r
   */
  @Nullable
  protected final <T extends ASTNode, R extends IBinding> R resolve(@Nullable T node, @NotNull Function<T, R> function) {
    if (null == node) return null;
    R binding = function.apply(node);
    if (null != binding) return binding;
    T reparsed = findReparsed1(node).orElse(null);
    if (null != reparsed) {
      binding = function.apply(reparsed);
      if (null != binding) return binding;
    }
    reparsed = findReparsed2(node).orElse(null);
    if (null != reparsed) {
      binding = function.apply(reparsed);
      if (null != binding) return binding;
    }
    return null;
  }

  /**
   * Find reparsed t.
   *
   * @param <T>  the type parameter
   * @param node the node
   * @return the t
   */
  @SuppressWarnings("unused")
  protected <T extends ASTNode> T findReparsed(@NotNull T node) {
    T reparsed = findReparsed1(node).orElse(null);
    if (null != reparsed) {
      return reparsed;
    }
    return findReparsed2(node).orElse(null);

  }

  /**
   * Resolve binding binding.
   *
   * @param node the node
   * @return the binding
   */
  @Nullable
  protected final IBinding resolveBinding(@NotNull Name node) {
    return resolve(node, Name::resolveBinding);
  }

  /**
   * Resolve binding method binding.
   *
   * @param node the node
   * @return the method binding
   */
  @Nullable
  protected final IMethodBinding resolveBinding(MethodDeclaration node) {
    return resolve(node, MethodDeclaration::resolveBinding);
  }

  /**
   * Resolve binding variable binding.
   *
   * @param node the node
   * @return the variable binding
   */
  @Nullable
  @SuppressWarnings("unused")
  protected final IVariableBinding resolveBinding(VariableDeclarationFragment node) {
    return resolve(node, VariableDeclaration::resolveBinding);
  }

  /**
   * Resolve binding variable binding.
   *
   * @param node the node
   * @return the variable binding
   */
  @Nullable
  protected final IVariableBinding resolveBinding(SingleVariableDeclaration node) {
    return resolve(node, VariableDeclaration::resolveBinding);
  }

  /**
   * Resolve binding type binding.
   *
   * @param node the node
   * @return the type binding
   */
  @Nullable
  protected final ITypeBinding resolveBinding(TypeParameter node) {
    return resolve(node, TypeParameter::resolveBinding);
  }

  /**
   * Resolve binding type binding.
   *
   * @param node the node
   * @return the type binding
   */
  @Nullable
  protected final ITypeBinding resolveBinding(Type node) {
    return resolve(node, Type::resolveBinding);
  }

  /**
   * Resolve binding type binding.
   *
   * @param node the node
   * @return the type binding
   */
  @Nullable
  protected final ITypeBinding resolveBinding(AnonymousClassDeclaration node) {
    return resolve(node, AnonymousClassDeclaration::resolveBinding);
  }

  /**
   * Resolve binding variable binding.
   *
   * @param node the node
   * @return the variable binding
   */
  @Nullable
  protected final IVariableBinding resolveBinding(@NotNull VariableDeclaration node) {
    return resolve(node, VariableDeclaration::resolveBinding);
  }

  /**
   * Resolve constructor binding method binding.
   *
   * @param node the node
   * @return the method binding
   */
  @Nullable
  protected final IMethodBinding resolveConstructorBinding(@NotNull ClassInstanceCreation node) {
    return resolve(node, ClassInstanceCreation::resolveConstructorBinding);
  }

  /**
   * Resolve constructor binding method binding.
   *
   * @param node the node
   * @return the method binding
   */
  @Nullable
  protected final IMethodBinding resolveConstructorBinding(@NotNull ConstructorInvocation node) {
    return resolve(node, ConstructorInvocation::resolveConstructorBinding);
  }

  /**
   * Resolve field binding variable binding.
   *
   * @param node the node
   * @return the variable binding
   */
  @Nullable
  protected final IVariableBinding resolveFieldBinding(FieldAccess node) {
    return resolve(node, FieldAccess::resolveFieldBinding);
  }

  /**
   * Resolve method binding method binding.
   *
   * @param node the node
   * @return the method binding
   */
  @Nullable
  protected final IMethodBinding resolveMethodBinding(LambdaExpression node) {
    return resolve(node, LambdaExpression::resolveMethodBinding);
  }

  /**
   * Resolve method binding method binding.
   *
   * @param node the node
   * @return the method binding
   */
  @Nullable
  protected final IMethodBinding resolveMethodBinding(MethodInvocation node) {
    return resolve(node, MethodInvocation::resolveMethodBinding);
  }

  /**
   * Resolve type binding type binding.
   *
   * @param node the node
   * @return the type binding
   */
  @Nullable
  protected final ITypeBinding resolveTypeBinding(@NotNull Expression node) {
    return resolve(node, Expression::resolveTypeBinding);
  }

  /**
   * To block block.
   *
   * @param lambdaExpression the lambda expression
   * @return the block
   */
  @NotNull
  protected final Block toBlock(@NotNull LambdaExpression lambdaExpression) {
    final ASTNode body = lambdaExpression.getBody();
    if (body instanceof Block) {
      return (Block) body;
    } else {
      final Block block = ast.newBlock();
      if (hasReturnValue(lambdaExpression)) {
        final ReturnStatement returnStatement = ast.newReturnStatement();
        returnStatement.setExpression(copyIfAttached((Expression) body));
        block.statements().add(returnStatement);
      } else {
        block.statements().add(ast.newExpressionStatement(copyIfAttached((Expression) body)));
      }
      debug(lambdaExpression, "Replace lambda %s with block %s", lambdaExpression, block);
      lambdaExpression.setBody(block);
      return block;
    }
  }

  @NotNull
  private ArrayList<StatementOfInterest> exits_(@NotNull Block block, int startAt, int endAt) {
    final List statements = block.statements();
    final ArrayList<StatementOfInterest> exits = new ArrayList<>();
    for (int j = Math.max(0, startAt); j <= endAt; j++) {
      final Statement statement = (Statement) statements.get(j);
      if (statement instanceof IfStatement) {
        final IfStatement ifStatement = (IfStatement) statement;
        exits.addAll(exits_(ast, ifStatement::getThenStatement, ifStatement::setThenStatement));
        exits.addAll(exits_(ast, ifStatement::getElseStatement, ifStatement::setElseStatement));
      } else if (statement instanceof WhileStatement) {
        final WhileStatement whileStatement = (WhileStatement) statement;
        exits.addAll(exits_(ast, whileStatement::getBody, whileStatement::setBody));
      } else if (statement instanceof DoStatement) {
        final DoStatement doStatement = (DoStatement) statement;
        exits.addAll(exits_(ast, doStatement::getBody, doStatement::setBody));
      } else if (statement instanceof ForStatement) {
        final ForStatement forStatement = (ForStatement) statement;
        exits.addAll(exits_(ast, forStatement::getBody, forStatement::setBody));
      } else if (statement instanceof EnhancedForStatement) {
        final EnhancedForStatement forStatement = (EnhancedForStatement) statement;
        exits.addAll(exits_(ast, forStatement::getBody, forStatement::setBody));
      } else if (statement instanceof TryStatement) {
        final TryStatement tryStatement = (TryStatement) statement;
        exits.addAll(exits_(ast, tryStatement::getBody, tryStatement::setBody));
      } else if (statement instanceof SynchronizedStatement) {
        final SynchronizedStatement synchronizedStatement = (SynchronizedStatement) statement;
        exits.addAll(exits_(ast, synchronizedStatement::getBody, synchronizedStatement::setBody));
      } else if (statement instanceof Block) {
        final Block synchronizedStatement = (Block) statement;
        exits.addAll(exits_(ast, () -> synchronizedStatement, x -> {
          throw new RuntimeException();
        }));
      } else if (ASTUtil.isExit(statement)) {
        exits.add(new StatementOfInterest(statement, j));
      }
    }
    return exits;
  }

  @NotNull
  private List<StatementOfInterest> exits_(@NotNull AST ast, @NotNull Supplier<Statement> supplier, @NotNull Consumer<Block> consumer) {
    Statement body = supplier.get();
    if (!(body instanceof Block) && ASTUtil.isExit(body)) {
      final Block newBlock = ast.newBlock();
      newBlock.statements().add(copyIfAttached(body));
      consumer.accept(newBlock);
      body = newBlock;
    }
    if (body instanceof Block) {
      final Block thenBlock = (Block) body;
      return exits_(thenBlock, 0, (thenBlock).statements().size() - 1);
    } else {
      return new ArrayList<>();
    }
  }

  private <T extends ASTNode> Optional<T> findReparsed1(@NotNull T node) {
    ASTMapping reparsed = getReparsed();
    if (null == reparsed) return Optional.empty();
    return find(node, reparsed);
  }

  private <T extends ASTNode> Optional<T> findReparsed2(@NotNull T node) {
    ASTMapping reparsed = getReparsed();
    if (null != reparsed) {
      final Optional<T> optional = find(node, reparsed);
      if (optional.isPresent()) return optional;
    }
    reparsed = update(true, false);
    final Optional<T> optional = find(node, reparsed);
    if (!optional.isPresent()) warn(node, "Could not find %s at %s in reparsed tree", node, node.getStartPosition());
    return optional;
  }

  private <T extends ASTNode> Optional<T> find(@NotNull T node, @Nullable ASTMapping reparsed) {
    T any;
    final Optional<T> optional;
    if (null == reparsed) {
      optional = Optional.empty();
    } else {
      any = (T) reparsed.matches.get(node);
      if (null == any) any = (T) reparsed.mismatches.get(node);
      if (null != any) {
        optional = Optional.of(any);
      } else {
        optional = Optional.empty();
      }
    }
    return optional;
  }

  @Nullable
  private ArrayList<Integer> getTopTypeDelimiters(ASTNode node, @NotNull String typeString) {
    int nesting = 0;
    ArrayList<Integer> delimiters = new ArrayList<>();
    for (int i = 0; i < typeString.length(); i++) {
      final char c = typeString.charAt(i);
      if (c == '<') nesting++;
      if (c == '>') nesting--;
      if (c == ',' && nesting == 0) delimiters.add(i);
    }
    if (nesting != 0) {
      warn(node, "Unbalanced type args: %s", typeString);
      return null;
    }
    delimiters.add(typeString.length());
    return delimiters;
  }

  @Nullable
  private Type getTypeParameter(ASTNode node, @NotNull String name, boolean isDeclaration) {
    final String extendsPrefix = "? extends ";
    final String superPrefix = "? super ";
    if (name.startsWith(extendsPrefix)) {
      final WildcardType wildcardType = ast.newWildcardType();
      wildcardType.setBound(getType(node, name.substring(extendsPrefix.length()), isDeclaration));
      wildcardType.setUpperBound(true);
      return wildcardType;
    } else if (name.startsWith(superPrefix)) {
      final WildcardType wildcardType = ast.newWildcardType();
      wildcardType.setBound(getType(node, name.substring(superPrefix.length()), isDeclaration));
      wildcardType.setUpperBound(false);
      return wildcardType;
    } else {
      return getType(node, name, isDeclaration);
    }
  }

  /**
   * The type Statement of interest.
   */
  public static class StatementOfInterest {
    /**
     * The Line.
     */
    public final int line;
    /**
     * The Statement.
     */
    @NotNull
    public final Statement statement;
    /**
     * The Block.
     */
    @NotNull
    public final Block block;

    /**
     * Instantiates a new Statement of interest.
     *
     * @param statement the statement
     * @param line      the line
     */
    public StatementOfInterest(@Nonnull Statement statement, int line) {
      this.statement = statement;
      this.block = (Block) this.statement.getParent();
      this.line = line;
    }

    /**
     * Is complex return boolean.
     *
     * @return the boolean
     */
    public boolean isComplexReturn() {
      if (!isReturn()) return false;
      return !(((ReturnStatement) statement).getExpression() instanceof SimpleName);
    }

    /**
     * Is return boolean.
     *
     * @return the boolean
     */
    public boolean isReturn() {
      return statement instanceof ReturnStatement;
    }

    /**
     * Is return value boolean.
     *
     * @return the boolean
     */
    @SuppressWarnings("unused")
    public boolean isReturnValue() {
      if (!isReturn()) return false;
      return (((ReturnStatement) statement).getExpression() != null);
    }

  }
}