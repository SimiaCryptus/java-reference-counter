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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.jetbrains.annotations.NotNull;

import java.io.File;

@RefIgnore
public class InsertAnnotations extends RefASTOperator {

  public InsertAnnotations(ProjectInfo projectInfo, @NotNull CompilationUnit compilationUnit, @NotNull File file) {
    super(projectInfo, compilationUnit, file);
  }

  @Override
  public void endVisit(@NotNull TypeDeclaration node) {
    if (!ASTUtil.hasAnnotation(node.resolveBinding(), RefAware.class)) {
      final MarkerAnnotation annotation = ast.newMarkerAnnotation();
      annotation.setTypeName(ASTUtil.newQualifiedName(ast, RefAware.class));
      node.modifiers().add(annotation);
      debug(node, "Added @RefAware to %s", node.getName());
    }
    super.endVisit(node);
  }
}
