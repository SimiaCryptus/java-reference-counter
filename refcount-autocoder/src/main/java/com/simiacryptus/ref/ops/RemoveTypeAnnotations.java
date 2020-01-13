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

import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.lang.RefIgnore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@RefIgnore
public class RemoveTypeAnnotations extends RefASTOperator {

  List<String> toRemove;

  public RemoveTypeAnnotations(ProjectInfo projectInfo, @NotNull CompilationUnit compilationUnit, @NotNull File file, String... annotationsToRemove) {
    super(projectInfo, compilationUnit, file);
    toRemove = Arrays.asList(annotationsToRemove);
  }

  @Override
  public void endVisit(@NotNull TypeDeclaration node) {
    final Iterator iterator = node.modifiers().iterator();
    while (iterator.hasNext()) {
      final Object next = iterator.next();
      if (next instanceof MarkerAnnotation) {
        String fullyQualifiedName = ((MarkerAnnotation) next).getTypeName().getFullyQualifiedName();
        if (toRemove.stream().anyMatch(x->x.endsWith(fullyQualifiedName))) {
          debug(node, "Removed @RefAware from %s", node.getName());
          iterator.remove();
        }
      }
    }
  }
}
