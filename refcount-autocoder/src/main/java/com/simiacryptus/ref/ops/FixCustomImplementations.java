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
import com.simiacryptus.ref.wrappers.RefIterator;
import com.simiacryptus.ref.wrappers.RefIteratorBase;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;

@RefIgnore
public class FixCustomImplementations extends RefASTOperator {

  public FixCustomImplementations(ProjectInfo projectInfo, @NotNull CompilationUnit compilationUnit, @NotNull File file) {
    super(projectInfo, compilationUnit, file);
  }

  @Override
  public void endVisit(@NotNull AnonymousClassDeclaration node) {
    final ITypeBinding typeBinding = node.resolveBinding();
    if (null == typeBinding) {
      warn(node, "Unresolved binding");
      return;
    }
    if (replace(node, typeBinding, RefIterator.class, RefIteratorBase.class)) return;
  }

  protected boolean replace(@NotNull AnonymousClassDeclaration node, @NotNull ITypeBinding typeBinding, @NotNull Class<?> match, @NotNull Class<?> replace) {
    if (typeBinding.getSuperclass().getBinaryName().equals(match.getName())) {
      debug(node, "RefIterator anonymous class");
      final ClassInstanceCreation parent = (ClassInstanceCreation) node.getParent();
      final SimpleType simpleType = ast.newSimpleType(ASTUtil.newQualifiedName(ast, replace));
      final Type parentType = parent.getType();
      if (parentType instanceof ParameterizedType) {
        final ParameterizedType type = ast.newParameterizedType(simpleType);
        ((ParameterizedType) parentType).typeArguments().forEach(astNode -> type.typeArguments().add(ASTNode.copySubtree(ast, (ASTNode) astNode)));
        parent.setType(type);
      } else {
        parent.setType(simpleType);
      }
      return true;
    } else {
      return false;
    }
  }

}
