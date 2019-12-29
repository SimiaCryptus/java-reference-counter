/*
 * Copyright (c) 2019 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import com.simiacryptus.ref.core.ops.FileAstVisitor;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@RefIgnore
public class RemoveRefMethods extends RefFileAstVisitor {

  public RemoveRefMethods(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
    super(projectInfo, compilationUnit, file);
  }

  @Override
  public void endVisit(@NotNull TypeDeclaration node) {
    final ITypeBinding typeBinding = FileAstVisitor.resolveBinding(node);
    if (derives(typeBinding, ReferenceCounting.class)) {
      removeMethods(node, "addRef");
      removeMethods(node, "freeRef");
      //removeMethods(node, "_free");
      removeMethods(node, "addRefs");
      removeMethods(node, "freeRefs");
    }
  }

}
