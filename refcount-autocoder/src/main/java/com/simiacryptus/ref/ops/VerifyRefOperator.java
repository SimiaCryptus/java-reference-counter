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
import com.simiacryptus.ref.lang.RefUtil;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VerifyRefOperator extends RefASTOperator {
  public VerifyRefOperator(ProjectInfo projectInfo, @NotNull CompilationUnit compilationUnit, @NotNull File file) {
    super(projectInfo, compilationUnit, file);
  }

  protected ReferenceState processStatement(Statement statement, SimpleName name, final ReferenceState state, List<Statement> finalizers, TerminalState requiredTermination) {
    if (statement instanceof IfStatement) {
      final IfStatement ifStatement = (IfStatement) statement;
      Expression expression = ifStatement.getExpression();
      String exprStr = expression.toString();
      boolean ifNull = exprStr.equals("null == " + name) || exprStr.equals(name + " == null");
      boolean ifNotNull = exprStr.equals("null != " + name) || exprStr.equals(name + " != null");
      ReferenceState expressionResult = processNode(expression, name, state, finalizers, requiredTermination);
      final ReferenceState thenResult;
      if (ifNull) {
        thenResult = state;
      } else {
        thenResult = processStatement(ifStatement.getThenStatement(), name, expressionResult, finalizers, requiredTermination);
      }
      final Statement elseStatement = ifStatement.getElseStatement();
      final ReferenceState elseResult;
      if (ifNotNull) {
        elseResult = state;
      } else if (null == elseStatement) {
        elseResult = expressionResult;
      } else {
        elseResult = processStatement(elseStatement, name, expressionResult, finalizers, requiredTermination);
      }
      if (ifNotNull) {
        return thenResult;
      } else if (ifNull) {
        return elseResult;
      } else if (elseResult.isTerminated()) {
        return thenResult;
      } else if (thenResult.isTerminated()) {
        return elseResult;
      } else {
        if (thenResult.isRefConsumed() != elseResult.isRefConsumed()) fatal(statement, "Mismatch ref consumed for %s", name);
        return thenResult;
      }
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
        if(!requiredTermination.validate(finalState))  {
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
        if(!requiredTermination.validate(finalState))  {
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
      ReferenceState loopResult;
      loopResult = processNode(forTest, name, testResult, finalizers, requiredTermination);
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
    } else if (statement instanceof Block) {
      ReferenceState blockState = state;
      for (Statement stmt : (List<Statement>) ((Block) statement).statements()) {
        blockState = processStatement(stmt, name, blockState, finalizers, requiredTermination);
      }
      return blockState;
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

  protected ReferenceState processNode(ASTNode node, SimpleName name, ReferenceState state, List<Statement> finalizers, TerminalState requiredTermination) {
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
      if(!requiredTermination.validate(state))  {
        fatal(node, "Reference for %s ends in state %s", name, state);
      }
    }
    return state;
  }

  protected ReferenceState processStatements(SimpleName name, ReferenceState state, TerminalState requiredTermination, Statement... statements) {
    for (Statement statement : statements) {
      state = processStatement(statement, name, state, new ArrayList<>(), requiredTermination);
    }
    return state;
  }

  protected ReferenceState processReference(Name name, ASTNode node, ReferenceState state) {
    final ASTNode parent = node.getParent();
    if (parent instanceof MethodInvocation) {
      MethodInvocation methodInvocation = (MethodInvocation) parent;
      if (ASTUtil.strEquals(node, methodInvocation.getExpression())) {
        assert node == methodInvocation.getExpression();
        if (methodInvocation.getName().toString().equals("freeRef")) return state.setRefConsumed(node);
        return state;
      } else {
        if (methodInvocation.getName().toString().equals("addRef")) return state;
        if (methodInvocation.getName().toString().equals("addRefs")) return state;
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
    } else if (parent instanceof SynchronizedStatement) {
      return state;
    } else if (parent instanceof Assignment) {
      Assignment assignment = (Assignment) parent;
      if (assignment.getRightHandSide() == node) {
        return state.setRefConsumed(node);
      } else if (assignment.getLeftHandSide() == node) {
        if (!state.isRefConsumed()) {
          fatal(node, "Overwriting non-freed reference %s", name);
        }
        if(assignment.getRightHandSide() instanceof NullLiteral) {
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
    } else if (parent instanceof VariableDeclarationFragment) {
      return processReference(name, parent, state);
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

  @NotNull
  protected List<Name> getMentions(ASTNode node, Name searchFor) {
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
      public boolean validate(ReferenceState state) {
        return state.isRefConsumed();
      }
    },
    Open {
      @Override
      public boolean validate(ReferenceState state) {
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

    public ReferenceState setRefConsumed(ASTNode refConsumedAt) {
      if (null != refConsumedAt && isRefConsumed()) {
        fatal(refConsumedAt, "Previously freed %s at %s", name, getLocation(this.refConsumedAt));
      }
      return new VerifyMethodVariables.ReferenceState(this.name, refConsumedAt, terminatedAt);
    }

    public boolean isTerminated() {
      return terminatedAt != null;
    }

    public ReferenceState setTerminated(ASTNode terminatedAt) {
      if (isTerminated()) {
        fatal(terminatedAt, "Previous termination for %s at %s", name, getLocation(this.terminatedAt));
      }
      return new VerifyMethodVariables.ReferenceState(this.name, refConsumedAt, terminatedAt);
    }

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
