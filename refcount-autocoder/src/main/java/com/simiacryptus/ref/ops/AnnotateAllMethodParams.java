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

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * This class is responsible for annotating all method parameters.
 *
 * @docgenVersion 9
 */
@RefIgnore
public class AnnotateAllMethodParams extends RefASTOperator {

  public AnnotateAllMethodParams(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    super(projectInfo, compilationUnit, file);
  }

  /**
   * This method is called when the visitor encounters a MethodDeclaration node.
   *
   * @param node the MethodDeclaration node
   * @docgenVersion 9
   */
  @Override
  public void endVisit(@Nonnull MethodDeclaration node) {
    for (SingleVariableDeclaration param : (List<SingleVariableDeclaration>) node.parameters()) {
      final IVariableBinding variableBinding = param.resolveBinding();
      if (null == variableBinding) {
        warn(param, "Unresolved");
        continue;
      }
      if (ASTUtil.isPrimitive(variableBinding.getType())) {
        info(param, "Primitive: %s", param);
        remove(param, variableBinding);
        continue;
      }
      if (isRefCounted(node, variableBinding.getType())) {
        info(param, "ReferenceCounted by type: %s", param);
        remove(param, variableBinding);
        continue;
      }
      if (ASTUtil.hasAnnotation(variableBinding, RefAware.class)) {
        info(param, "Already @RefAware: %s", param);
        continue;
      }
      info(param, "Adding @RefAware to %s", param);
      param.modifiers().add(ASTUtil.newMarkerAnnotation(ast, RefAware.class));
    }
  }

  /**
   * @return {@code false}
   * @override
   * @protected
   * @docgenVersion 9
   */
  @Override
  protected boolean skip(ASTNode node, IBinding binding) {
    return false;
  }

  /**
   * Remove the given parameter from the list of parameters.
   *
   * @param param           the parameter to remove
   * @param variableBinding the variable binding for the parameter
   * @docgenVersion 9
   */
  private void remove(@Nonnull SingleVariableDeclaration param, IVariableBinding variableBinding) {
    if (ASTUtil.hasAnnotation(variableBinding, RefAware.class)) {
      warn(param, "Needless @RefAware: %s", param);
      for (Iterator iterator = param.modifiers().iterator(); iterator.hasNext(); ) {
        if (iterator.next().toString().contains("RefAware")) {
          info(param, "Removed @RefAware: %s", param);
          iterator.remove();
        }
      }
    }
  }

}
