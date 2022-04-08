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
import com.simiacryptus.ref.core.CollectableException;
import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.core.SymbolIndex;
import com.simiacryptus.ref.lang.RefIgnore;
import org.eclipse.jdt.core.dom.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The VerifyClassMembers class is used to verify the class members.
 *
 * @param index The index of the class members.
 * @docgenVersion 9
 */
public class VerifyClassMembers extends VerifyRefOperator {
  @Nonnull
  protected SymbolIndex index;

  public VerifyClassMembers(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    super(projectInfo, compilationUnit, file);
    index = getSymbolIndex(compilationUnit);
  }

  /**
   * Returns a list of all the methods in the given list of body declarations.
   *
   * @param bodyDeclarations the list of body declarations to search through
   * @return a list of all the methods in the given list of body declarations
   * @docgenVersion 9
   */
  @Nonnull
  public static List<MethodDeclaration> methods(@Nonnull List<ASTNode> bodyDeclarations) {
    return bodyDeclarations.stream()
        .filter(x -> x instanceof MethodDeclaration)
        .map(x -> (MethodDeclaration) x)
        .filter(m -> {
          IMethodBinding methodBinding = m.resolveBinding();
          if (methodBinding == null) {
            logger.warn("Unresolved binding: " + m.getName());
            return false;
          }
          return !ASTUtil.hasAnnotation(methodBinding, RefIgnore.class);
        })
        .collect(Collectors.toList());
  }

  /**
   * Verifies that the given class method has the given body and contains
   * only the given names.
   *
   * @param methodName the name of the method to verify
   * @param body       the expected body of the method
   * @param names      the expected names in the method
   * @docgenVersion 9
   */
  protected void verifyClassMethod(@Nonnull String methodName, @Nonnull List<Statement> body, @Nonnull Collection<SimpleName> names) {
    ArrayList<CollectableException> exceptions = new ArrayList<>();
    for (SimpleName closureName : names) {
      try {
        if (null != closureName && body.size() > 0) {
          ReferenceState state = new ReferenceState(closureName, null, null);
          TerminalState requiredTermination;
          if (methodName.equals("_free")) {
            requiredTermination = TerminalState.Freed;
          } else {
            requiredTermination = TerminalState.Open;
          }
          for (int i = 0; i < body.size(); i++) {
            state = processStatement(body.get(i), closureName, state, new ArrayList<>(), requiredTermination);
          }
          Statement lastStatement = body.get(body.size() - 1);
          if (!requiredTermination.validate(state)) {
            fatal(lastStatement, "Reference for %s ends in state %s", methodName, state);
          }
        }
      } catch (CollectableException e) {
        exceptions.add(e);
      }
    }
    if (!exceptions.isEmpty()) {
      throw CollectableException.combine(exceptions);
    }
  }

  /**
   * @param methodName          the name of the method to verify
   * @param expression          the expression to verify
   * @param requiredTermination the required termination state
   * @param names               the collection of simple names
   * @docgenVersion 9
   */
  protected void verifyLambda(String methodName, @Nonnull Expression expression, @Nonnull TerminalState requiredTermination, @Nonnull Collection<SimpleName> names) {
    ArrayList<CollectableException> exceptions = new ArrayList<>();
    for (SimpleName closureName : names) {
      try {
        if (null != closureName) {
          ReferenceState state = new ReferenceState(closureName, null, null);
          state = processNode(expression, closureName, state, new ArrayList<>(), requiredTermination);
          if (state.isRefConsumed()) {
            fatal(expression, "%s freed by method %s", closureName, methodName);
          }
        }
      } catch (CollectableException e) {
        exceptions.add(e);
      }
    }
    if (!exceptions.isEmpty()) {
      throw CollectableException.combine(exceptions);
    }
  }

}
