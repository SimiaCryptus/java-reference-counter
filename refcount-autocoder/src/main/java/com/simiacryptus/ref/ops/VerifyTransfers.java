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
import com.simiacryptus.ref.core.Tuple2;
import com.simiacryptus.ref.lang.RefIgnore;
import org.eclipse.jdt.core.dom.*;

import java.io.File;

@RefIgnore
public class VerifyTransfers extends RefASTOperator {

  public VerifyTransfers(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
    super(projectInfo, compilationUnit, file);
  }

  @Override
  public void endVisit(Assignment node) {
    final ITypeBinding rightType = node.getRightHandSide().resolveTypeBinding();
    if (rightType == null) {
      warn(node, "Unresolved right");
      return;
    }
    if (isRefCounted(node, rightType)) {
      final ITypeBinding leftType = node.getLeftHandSide().resolveTypeBinding();
      if (leftType == null) {
        warn(node, "Unresolved right");
        return;
      }
      if (!isRefCounted(node, leftType)) {
        fatal(node, "Assignment of %s loses RefCount typing to %s", rightType.getName(), leftType.getName());
      }
    }
  }

  @Override
  public void endVisit(ReturnStatement node) {
    final Expression expression = node.getExpression();
    if (null == expression) return;
    final ITypeBinding expressionType = expression.resolveTypeBinding();
    if (expressionType == null) {
      warn(node, "Unresolved expression");
      return;
    }
    if (isRefCounted(node, expressionType)) {
      final Tuple2<ASTNode, IMethodBinding> method = ASTUtil.getMethod(node);
      if (method == null) {
        fatal(node, "No containing method");
        return;
      }
      if (method._2 == null) {
        warn(node, "Unresolved method");
        return;
      }
      final ITypeBinding returnType = method._2.getReturnType();
      if (!isRefCounted(node, returnType)) {
        fatal(node, "Return of %s loses RefCount typing to %s", expressionType.getName(), returnType.getName());
      }
    }
  }

}
