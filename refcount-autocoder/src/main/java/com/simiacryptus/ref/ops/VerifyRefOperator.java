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
import com.simiacryptus.ref.core.SymbolIndex;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import org.eclipse.jdt.core.dom.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VerifyRefOperator extends RefASTOperator {
  public VerifyRefOperator(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    super(projectInfo, compilationUnit, file);
  }

  public static boolean isWriting(@Nonnull ASTNode node) {
    ASTNode parent = node.getParent();
    if (parent instanceof Assignment) {
      if (((Assignment) parent).getLeftHandSide() == node) {
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  protected ReferenceState processStatement(Statement statement, @Nonnull SimpleName name, @Nonnull final ReferenceState state, @Nonnull List<Statement> finalizers, @Nonnull TerminalState requiredTermination) {
    if (statement instanceof IfStatement) {
      final IfStatement ifStatement = (IfStatement) statement;
      Expression expression = ifStatement.getExpression();
      String exprStr = expression.toString();
      boolean ifNull = exprStr.equals("null == " + name) || exprStr.equals(name + " == null");
      boolean ifNonnull = exprStr.equals("null != " + name) || exprStr.equals(name + " != null");
      ReferenceState expressionResult = processNode(expression, name, state, finalizers, requiredTermination);
      final ReferenceState thenResult;
      if (ifNull) {
//        thenResult = state;
        thenResult = processStatement(ifStatement.getThenStatement(), name, expressionResult, finalizers, TerminalState.Any);
      } else {
        thenResult = processStatement(ifStatement.getThenStatement(), name, expressionResult, finalizers, requiredTermination);
      }
      final Statement elseStatement = ifStatement.getElseStatement();
      final ReferenceState elseResult;
      if (null == elseStatement) {
        elseResult = expressionResult;
      } else if (ifNonnull) {
//        elseResult = state;
        elseResult = processStatement(elseStatement, name, expressionResult, finalizers, TerminalState.Any);
      } else {
        elseResult = processStatement(elseStatement, name, expressionResult, finalizers, requiredTermination);
      }
      if (elseResult.isTerminated()) {
        return thenResult;
      } else if (thenResult.isTerminated()) {
        return elseResult;
      } else if (ifNonnull) {
        return thenResult;
      } else if (ifNull) {
        return elseResult;
      } else {
        if (thenResult.isRefConsumed() != elseResult.isRefConsumed()) fatal(statement, "Mismatch ref consumed for %s", name);
        return thenResult;
      }
    } else if (statement instanceof SwitchStatement) {
      final SwitchStatement switchStatement = (SwitchStatement) statement;
      Expression expression = switchStatement.getExpression();
      ReferenceState expressionResult = processNode(expression, name, state, finalizers, requiredTermination);
      List<Statement> statements = switchStatement.statements();
      ArrayList<ReferenceState> referenceStates = new ArrayList<>();
      int fromIndex = 0;
      for (int i = 0; i < statements.size(); i++) {
        Statement statement1 = statements.get(i);
        if (statement1 instanceof BreakStatement || statement1 instanceof ReturnStatement || statement1 instanceof ThrowStatement) {
          List<Statement> block = statements.subList(fromIndex, i + 1);
          ReferenceState blockState = expressionResult;
          for (Statement stmt : block) {
            blockState = processStatement(stmt, name, blockState, finalizers, requiredTermination);
          }
          referenceStates.add(blockState);
          fromIndex = i + 1;
        }
      }
      {
        List<Statement> block = statements.subList(fromIndex, statements.size());
        if (!block.isEmpty()) {
          ReferenceState blockState = expressionResult;
          for (Statement stmt : block) {
            blockState = processStatement(stmt, name, blockState, finalizers, requiredTermination);
          }
          referenceStates.add(blockState);
        }
      }
      List<Boolean> refConsumed = referenceStates.stream().filter(x -> !x.isTerminated()).map(x -> x.isRefConsumed()).distinct().collect(Collectors.toList());
      if (refConsumed.size() == 0) {
        return referenceStates.get(0);
      }
      if (refConsumed.size() > 1) {
        fatal(statement, "Inconsistent reference consumer for %s", name);
      }
      return referenceStates.stream().filter(x -> !x.isTerminated()).findAny().get();
    } else if (statement instanceof TryStatement) {
      final TryStatement tryStatement = (TryStatement) statement;
      final Statement finallyStatement = tryStatement.getFinally();
      final Block tryBody = tryStatement.getBody();
      if (null == finallyStatement) {
        return processStatement(tryBody, name, state, finalizers, requiredTermination);
      } else {
        final ReferenceState tryResult = processStatement(tryBody, name, state, ASTUtil.copyPrepend(finalizers, finallyStatement), requiredTermination);
        if (tryResult.isTerminated()) {
          return tryResult;
        } else {
          return processStatement(finallyStatement, name, tryResult, finalizers, requiredTermination);
        }
      }
    } else if (statement instanceof WhileStatement) {
      WhileStatement whileStatement = (WhileStatement) statement;
      Statement whileBody = whileStatement.getBody();
      Expression expression = whileStatement.getExpression();
      ReferenceState testResult = processNode(expression, name, state, finalizers, requiredTermination);
      if (!state.isRefConsumed() && testResult.isRefConsumed()) {
        fatal(expression, "Possible repeated free of %s at %s", name, getLocation(testResult.refConsumedAt));
      }
      ReferenceState loopResult = processStatement(whileBody, name, testResult, finalizers, requiredTermination);
      if (!state.isRefConsumed() && loopResult.isRefConsumed()) {
        fatal(whileBody, "Possible repeated free of %s at %s", name, getLocation(loopResult.refConsumedAt));
      }
      if (ASTUtil.isLoopTerminal(whileStatement)) {
        ReferenceState whileResult = loopResult.setTerminated(whileStatement);
        ReferenceState finalState = processStatements(name, whileResult, requiredTermination, finalizers.toArray(new Statement[]{}));
        if (!requiredTermination.validate(finalState)) {
          fatal(whileStatement, "Reference for %s ends in state %s", name, state);
        }
        return finalState;
      }
      return loopResult;
    } else if (statement instanceof DoStatement) {
      DoStatement doStatement = (DoStatement) statement;
      Statement whileBody = doStatement.getBody();
      Expression expression = doStatement.getExpression();
      ReferenceState loopResult = processStatement(whileBody, name, state, finalizers, requiredTermination);
      if (!state.isRefConsumed() && loopResult.isRefConsumed()) {
        fatal(whileBody, "Possible repeated free of %s at %s", name, getLocation(loopResult.refConsumedAt));
      }
      ReferenceState testResult = processNode(expression, name, loopResult, finalizers, requiredTermination);
      if (!state.isRefConsumed() && testResult.isRefConsumed()) {
        fatal(expression, "Possible repeated free of %s at %s", name, getLocation(testResult.refConsumedAt));
      }
      if (ASTUtil.isLoopTerminal(doStatement)) {
        ReferenceState whileResult = testResult.setTerminated(doStatement);
        ReferenceState finalState = processStatements(name, whileResult, requiredTermination, finalizers.toArray(new Statement[]{}));
        if (!requiredTermination.validate(finalState)) {
          fatal(doStatement, "Reference for %s ends in state %s", name, state);
        }
        return finalState;
      }
      return testResult;
    } else if (statement instanceof ForStatement) {
      ForStatement forStatement = (ForStatement) statement;
      Statement whileBody = forStatement.getBody();
      Expression forTest = forStatement.getExpression();
      List<Expression> initializers = forStatement.initializers();
      List<Expression> updaters = forStatement.updaters();
      ReferenceState testResult = state;
      for (Expression initializer : initializers) {
        testResult = processNode(initializer, name, testResult, finalizers, requiredTermination);
      }
      ReferenceState loopResult = processNode(forTest, name, testResult, finalizers, requiredTermination);
      for (Expression updater : updaters) {
        loopResult = processNode(updater, name, loopResult, finalizers, requiredTermination);
      }
      loopResult = processStatement(whileBody, name, loopResult, finalizers, requiredTermination);
      if (!testResult.isRefConsumed() && loopResult.isRefConsumed()) {
        fatal(whileBody, "Possible repeated free of %s at %s", name, getLocation(loopResult.refConsumedAt));
      }
      return loopResult;
    } else if (statement instanceof EnhancedForStatement) {
      EnhancedForStatement forStatement = (EnhancedForStatement) statement;
      ReferenceState loopResult = processNode(forStatement.getExpression(), name, state, finalizers, requiredTermination);
      Statement whileBody = forStatement.getBody();
      loopResult = processStatement(whileBody, name, loopResult, finalizers, requiredTermination);
      if (!state.isRefConsumed() && loopResult.isRefConsumed()) {
        fatal(whileBody, "Possible repeated free of %s at %s", name, getLocation(loopResult.refConsumedAt));
      }
      return loopResult;
    } else if (statement instanceof SynchronizedStatement) {
      SynchronizedStatement synchronizedStatement = (SynchronizedStatement) statement;
      return processStatement(synchronizedStatement.getBody(), name,
          processNode(synchronizedStatement.getExpression(), name,
              state,
              finalizers, requiredTermination),
          finalizers, requiredTermination);
    } else if (statement instanceof Block) {
      return processBlock(name, state, finalizers, requiredTermination, (Block) statement);
    } else if (statement instanceof ExpressionStatement) {
      Expression expression = ((ExpressionStatement) statement).getExpression();
      if (expression instanceof Assignment) {
        Assignment assignment = (Assignment) expression;
        ReferenceState rightResult = processNode(assignment.getRightHandSide(), name, state, finalizers, requiredTermination);
        return processNode(assignment.getLeftHandSide(), name, rightResult, finalizers, requiredTermination);
      }
    }
    return processNode(statement, name, state, finalizers, requiredTermination);
  }

  private ReferenceState processBlock(@Nonnull SimpleName name, @Nonnull ReferenceState state, @Nonnull List<Statement> finalizers, @Nonnull TerminalState requiredTermination, Block block) {
    ReferenceState blockState = state;
    List<Statement> statements = block.statements();
    for (Statement stmt : statements) {
      blockState = processStatement(stmt, name, blockState, finalizers, requiredTermination);
    }
    return blockState;
  }

  protected ReferenceState processNode(@Nullable ASTNode node, @Nonnull SimpleName name, ReferenceState state, @Nonnull List<Statement> finalizers, @Nonnull TerminalState requiredTermination) {
    if (null == node) return state;
    List<Name> mentions = getMentions(node, name).stream()
        .filter(mention -> !ASTUtil.withinLambda(node, mention))
        .filter(mention -> !ASTUtil.withinAnonymousClass(node, mention))
        .collect(Collectors.toList());
    for (Name mention : mentions) {
      state = processReference(name, mention, state);
    }
    List<ReturnStatement> returnStatements = ASTUtil.findExpressions(node, ReturnStatement.class).stream()
        .filter(returnStatement -> !ASTUtil.withinLambda(node, returnStatement))
        .filter(returnStatement -> !ASTUtil.withinAnonymousClass(node, returnStatement))
        .filter(returnStatement -> !ASTUtil.withinSubMethod(node, returnStatement))
        .collect(Collectors.toList());
    List<ThrowStatement> throwStatements = ASTUtil.findExpressions(node, ThrowStatement.class).stream()
        .filter(throwStatement -> !ASTUtil.withinLambda(node, throwStatement))
        .filter(throwStatement -> !ASTUtil.withinAnonymousClass(node, throwStatement))
        .filter(throwStatement -> !ASTUtil.withinSubMethod(node, throwStatement))
        .collect(Collectors.toList());
    if (returnStatements.size() > 0 || throwStatements.size() > 0) {
      if (returnStatements.size() + throwStatements.size() > 1) fatal(node, "Multiple exit points: %s", RefUtil.get(Stream.concat(
          returnStatements.stream(), throwStatements.stream()
      ).map(x -> getLocation(x)).reduce((a, b) -> a + ", " + b)));
      state = state.setTerminated(returnStatements.isEmpty() ? throwStatements.get(0) : returnStatements.get(0));
      state = processStatements(name, state, requiredTermination, finalizers.toArray(new Statement[]{}));
      if (!requiredTermination.validate(state)) {
        fatal(node, "Reference for %s (%s) ends in state %s", name, getLocation(name), state);
      }
    }
    return state;
  }

  protected ReferenceState processStatements(@Nonnull SimpleName name, ReferenceState state, @Nonnull TerminalState requiredTermination, @Nonnull Statement... statements) {
    for (Statement statement : statements) {
      state = processStatement(statement, name, state, new ArrayList<>(), requiredTermination);
    }
    return state;
  }

  @Nonnull
  protected ReferenceState processReference(Name name, @Nonnull ASTNode node, @Nonnull ReferenceState state) {
    if (!isWriting(node) && state.isRefConsumed() && getStatement(state.refConsumedAt) != getStatement(node)) {
      fatal(node, "Reference to %s already consumed at %s", name, getLocation(state.refConsumedAt));
    }
    final ASTNode parent = node.getParent();
    if (parent instanceof MethodInvocation) {
      MethodInvocation methodInvocation = (MethodInvocation) parent;
      String methodName = methodInvocation.getName().toString();
      Expression expression = methodInvocation.getExpression();
      if (null != expression && node.toString().equals(expression.toString())) {
        if (methodName.equals("freeRef")) return state.setRefConsumed(node);
        return state;
      } else {
        //if (methodName.equals("addRef")) return state;
        //if (methodName.equals("addRef")) return state;
        //if (methodName.equals("freeRef")) return state.setRefConsumed(node);
        //if (methodName.equals("freeRefs")) return state.setRefConsumed(node);
        if (methodName.equals("equals")) return state;
        int argIndex = methodInvocation.arguments().indexOf(node);
        IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
        if (null == methodBinding) {
          warn(node, "Unresolved binding");
        } else {
          if (argIndex >= methodBinding.getParameterTypes().length) argIndex = methodBinding.getParameterTypes().length - 1;
          ITypeBinding parameterType = methodBinding.getParameterTypes()[argIndex];
          IAnnotationBinding[] parameterAnnotations = methodBinding.getParameterAnnotations(argIndex);
          boolean isRefCounted = isRefCounted(node, parameterType);
          boolean hasRefAware = ASTUtil.findAnnotation(RefAware.class, parameterAnnotations).isPresent();
          boolean hasRefIgnore = ASTUtil.findAnnotation(RefIgnore.class, parameterAnnotations).isPresent();
          if (hasRefIgnore) return state;
          if (!isRefCounted && !hasRefAware) {
            fatal(node, "Reference passed as blind parameter %s of %s", argIndex, methodBinding.getName());
          }
        }
        return state.setRefConsumed(node);
      }
    } else if (parent instanceof InfixExpression) {
      return state;
    } else if (parent instanceof InstanceofExpression) {
      return state;
    } else if (parent instanceof QualifiedName) {
      return state;
    } else if (parent instanceof ConstructorInvocation) {
      return state.setRefConsumed(node);
    } else if (parent instanceof SuperMethodInvocation) {
      return state.setRefConsumed(node);
    } else if (parent instanceof SuperConstructorInvocation) {
      return state.setRefConsumed(node);
    } else if (parent instanceof ReturnStatement) {
      return state.setRefConsumed(node);
    } else if (parent instanceof SynchronizedStatement) {
      return state;
    } else if (parent instanceof ClassInstanceCreation) {
      return state.setRefConsumed(node);
    } else if (parent instanceof ArrayAccess) {
      return state;
    } else if (parent instanceof Assignment) {
      Assignment assignment = (Assignment) parent;
      if (assignment.getRightHandSide() == node) {
        return state.setRefConsumed(node);
      } else if (assignment.getLeftHandSide() == node) {
        if (node instanceof SimpleName) {
          IBinding binding = ((SimpleName) node).resolveBinding();
          if (binding instanceof IVariableBinding) {
            IVariableBinding variableBinding = (IVariableBinding) binding;
            if (Modifier.isFinal((variableBinding).getModifiers())) {
              return state;
            }
          }
        }
        if (!state.isRefConsumed()) {
          fatal(node, "Overwriting non-freed reference %s", name);
        }
        if (assignment.getRightHandSide() instanceof NullLiteral) {
          return state;
        }
        return state.setRefConsumed(null);
      } else {
        fatal(node, "Node not found in assignment");
        return state;
      }
    } else if (parent instanceof ParenthesizedExpression) {
      return processReference(name, parent, state);
    } else if (parent instanceof CastExpression) {
      return processReference(name, parent, state);
    } else if (parent instanceof ConditionalExpression) {
      return processReference(name, parent, state);
    } else if (parent instanceof ArrayInitializer) {
      return processReference(name, parent, state);
    } else if (parent instanceof ArrayCreation) {
      return processReference(name, parent, state);
    } else if (parent instanceof VariableDeclarationFragment) {
      VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) parent;
      IVariableBinding variableBinding = variableDeclarationFragment.resolveBinding();
      if (null == variableBinding) {
        warn(node, "Unresolved binding");
      } else {
        ITypeBinding parameterType = variableBinding.getType();
        if (!isRefCounted(node, parameterType) && !ASTUtil.hasAnnotation(variableBinding, RefAware.class)) {
          fatal(node, "Reference passed as blind initializer for %s", variableBinding.getName());
        }
      }
      return state.setRefConsumed(node);
    } else if (parent instanceof FieldAccess) {
      return state;
    } else if (parent instanceof VariableDeclarationStatement) {
      return state.setRefConsumed(node);
    } else if (parent instanceof ExpressionMethodReference) {
      return state;
    } else if (parent instanceof LambdaExpression) {
      return state;
    } else if (parent instanceof EnhancedForStatement) {
      ITypeBinding typeBinding = ((Name) node).resolveTypeBinding();
      if (null == typeBinding || !typeBinding.isArray()) {
        fatal(node, "Refcounted collection %s used in EnhancedForStatement", name);
      }
      return state;
    } else if (parent == null) {
      fatal(node, "Null parent for %s", name);
      return state;
    } else {
      fatal(node, "Unhandled parent for %s: %s", name, parent.getClass().getSimpleName());
      return state;
    }
  }

  @Nonnull
  protected List<Name> getMentions(@Nullable ASTNode node, @Nonnull Name searchFor) {
    final IBinding nameBinding = searchFor.resolveBinding();
    if (nameBinding == null) {
      warn(searchFor, "Unresolved binding");
      return new ArrayList<>();
    } else if (node == null) {
      return new ArrayList<>();
    } else {
      return ASTUtil.findExpressions(node, searchFor)
          .stream().filter(mention -> {
            final IBinding mentionBinding = mention.resolveBinding();
            if (mentionBinding == null) {
              warn(mention, "Unresolved binding");
              return false;
            }
            return SymbolIndex.equals(nameBinding, mentionBinding);
          }).collect(Collectors.toList());
    }
  }

  public enum TerminalState {
    Freed {
      @Override
      public boolean validate(@Nonnull ReferenceState state) {
        return state.isRefConsumed();
      }
    },
    Open {
      @Override
      public boolean validate(@Nonnull ReferenceState state) {
        return !state.isRefConsumed();
      }
    },
    Any {
      @Override
      public boolean validate(ReferenceState state) {
        return true;
      }
    };

    public abstract boolean validate(ReferenceState state);
  }

  protected class ReferenceState {

    public final ASTNode refConsumedAt;
    public final ASTNode terminatedAt;
    public final SimpleName name;

    ReferenceState(SimpleName name, ASTNode refConsumedAt, ASTNode terminatedAt) {
      this.name = name;
      this.refConsumedAt = refConsumedAt;
      this.terminatedAt = terminatedAt;
    }

    public boolean isRefConsumed() {
      return refConsumedAt != null;
    }

    @Nonnull
    public ReferenceState setRefConsumed(@Nullable ASTNode refConsumedAt) {
      if (null != refConsumedAt && isRefConsumed()) {
        fatal(refConsumedAt, "Previously freed %s at %s", name, getLocation(this.refConsumedAt));
      }
      return new VerifyMethodVariables.ReferenceState(this.name, refConsumedAt, terminatedAt);
    }

    public boolean isTerminated() {
      return terminatedAt != null;
    }

    @Nonnull
    public ReferenceState setTerminated(@Nonnull ASTNode terminatedAt) {
      if (isTerminated()) {
        fatal(terminatedAt, "Previous termination for %s at %s", name, getLocation(this.terminatedAt));
      }
      return new VerifyMethodVariables.ReferenceState(this.name, refConsumedAt, terminatedAt);
    }

    @Nonnull
    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("ReferenceState{");
      sb.append("name=").append(name);
      if (null != refConsumedAt) sb.append(", refConsumedAt=").append(getLocation(refConsumedAt));
      if (null != terminatedAt) sb.append(", terminatedAt=").append(getLocation(terminatedAt));
      sb.append('}');
      return sb.toString();
    }
  }
}
