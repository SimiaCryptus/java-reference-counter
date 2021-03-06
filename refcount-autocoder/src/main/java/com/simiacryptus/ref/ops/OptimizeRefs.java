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
import java.util.ArrayList;

@RefIgnore
public class OptimizeRefs extends RefASTOperator {

  public OptimizeRefs(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    super(projectInfo, compilationUnit, file);
  }

  @Override
  public void endVisit(@Nonnull MethodInvocation freeRefNode) {
    if (freeRefNode.getName().toString().equals("freeRef")) {
      final IMethodBinding methodBinding = resolveMethodBinding(freeRefNode);
      if (null == methodBinding) {
        warn(freeRefNode, "Unresolved binding");
        return;
      }
      if ((methodBinding.getModifiers() & Modifier.STATIC) != 0) {
        debug(freeRefNode, "Non-instance freeRef");
        return;
      }
      if (!(freeRefNode.getExpression() instanceof SimpleName)) {
        warn(freeRefNode, "Complex freeRef");
        return;
      }
      final SimpleName variable = (SimpleName) freeRefNode.getExpression();
      Block parentBlock = ASTUtil.getBlock(freeRefNode);
      if (null == parentBlock) {
        warn(freeRefNode, "Could not find containing block");
        return;
      }
      final int freeRefLineNumber = getLineNumber(parentBlock, freeRefNode);
      if (freeRefLineNumber < 0) {
        warn(freeRefNode, "Could not find statement in block");
        return;
      }
      MethodInvocation addRefNode = findAddRef(parentBlock, freeRefLineNumber, variable);
      if (addRefNode == null) {
        debug(freeRefNode, "Could not find preceeding addRef");
        return;
      }
      final int addRefLineNumber = getLineNumber(parentBlock, addRefNode);
      if (addRefLineNumber < 0) {
        warn(freeRefNode, "Could not find statement in block");
        return;
      }
      final StatementOfInterest lastMention = lastMention(parentBlock, variable, 0, freeRefLineNumber, 1);
      assert lastMention != null;
      if (lastMention.line > addRefLineNumber) {
        debug(freeRefNode, "Value used at line %s, after prior addRef at line %s", lastMention.line, addRefLineNumber);
        return;
      }
      debug(freeRefNode, "Consolidating freeRef of %s at line %s with addRef at line %s", variable, freeRefLineNumber, addRefLineNumber);
      replace(addRefNode, copyIfAttached(variable));
      parentBlock.statements().remove(freeRefLineNumber);
    }
  }

  @Nullable
  private MethodInvocation findAddRef(@Nonnull Block block, int line, @Nonnull final Expression subject) {
    final ArrayList<MethodInvocation> addRefInvocations = new ArrayList<>();
    for (int i = line - 1; i >= 0; i--) {
      final Statement statement = (Statement) block.statements().get(i);
      statement.accept(new ASTVisitor() {
        @Override
        public void endVisit(@Nonnull MethodInvocation addRefNode) {
          if (addRefNode.getName().toString().equals("addRef")) {
            if (addRefNode.getExpression().toString().equals(subject.toString())) {
              addRefInvocations.add(addRefNode);
            }
          }
        }

        @Override
        public boolean visit(Block node) {
          return false;
        }

        @Override
        public boolean visit(AnonymousClassDeclaration node) {
          return false;
        }
      });
      if (!addRefInvocations.isEmpty()) {
        debug(statement, "Found %s preceeding addRef(s)", addRefInvocations.size());
        return addRefInvocations.get(0);
      }
    }
    return null;
  }

}
