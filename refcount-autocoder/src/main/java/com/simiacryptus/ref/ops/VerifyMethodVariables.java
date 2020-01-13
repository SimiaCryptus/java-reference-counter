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
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.*;

@RefIgnore
public class VerifyMethodVariables extends VerifyRefOperator {

  public VerifyMethodVariables(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
    super(projectInfo, compilationUnit, file);
  }

  @Override
  public void endVisit(MethodDeclaration node) {
    final IMethodBinding methodBinding = node.resolveBinding();
    if (null == methodBinding) {
      warn(node, "Unresolved binding");
      return;
    }
    final List<SingleVariableDeclaration> parameters = node.parameters();
    for (int i = 0; i < methodBinding.getParameterTypes().length; i++) {
      IAnnotationBinding[] annotations = methodBinding.getParameterAnnotations(i);
      ITypeBinding type = methodBinding.getParameterTypes()[i];
      if (ASTUtil.findAnnotation(RefAware.class, annotations).isPresent() || isRefCounted(node, type)) {
        Block body = node.getBody();
        SimpleName name = parameters.get(i).getName();
        if (null == body) continue;
        TerminalState requiredTermination = TerminalState.Freed;
        ReferenceState finalState = processStatement(body, name, new ReferenceState(name, null, null), new ArrayList<>(), requiredTermination);
        if (!requiredTermination.validate(finalState)) {
          fatal(body, "Reference for %s ends in state %s", name, finalState);
        }
      }
    }
  }

  @Override
  public void endVisit(VariableDeclarationStatement node) {
    MethodDeclaration methodDeclaration = getMethodDeclaration(node);
    if (null == methodDeclaration) {
      debug(node, "No method");
      return;
    }
    Block block = ASTUtil.getBlock(node);
    List<Statement> statements = block.statements();
    int definedAt = statements.indexOf(node);
    for (VariableDeclarationFragment fragment : (List<VariableDeclarationFragment>) node.fragments()) {
      ArrayList<Statement> finalizers = new ArrayList<>();
      IVariableBinding binding = fragment.resolveBinding();
      if (null == binding) {
        warn(node, "Unresolved type");
        return;
      }
      if (isRefCounted(node, binding.getType()) || ASTUtil.hasAnnotation(binding, RefAware.class)) {
        SimpleName name = fragment.getName();
        ReferenceState state = new ReferenceState(name, null, null);
        TerminalState requiredTermination = TerminalState.Freed;
        for (int i = definedAt; i < statements.size(); i++) {
          state = processStatement(statements.get(i), name, state, finalizers, requiredTermination);
        }
        if (!requiredTermination.validate(state)) {
          fatal(block, "Reference for %s ends in state %s", name, state);
        }
      }
    }
    super.endVisit(node);
  }

}
