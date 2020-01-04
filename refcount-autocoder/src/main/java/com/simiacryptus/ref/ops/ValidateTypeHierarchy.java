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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Optional;

@RefIgnore
public class ValidateTypeHierarchy extends RefASTOperator {

  public ValidateTypeHierarchy(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
    super(projectInfo, compilationUnit, file);
  }

  @Override
  public void endVisit(TypeDeclaration node) {
    final ITypeBinding typeBinding = node.resolveBinding();
    if (null == typeBinding) {
      warn(node, "Cannot resolve type of %s", node.getName());
      return;
    }
    if(!typeBinding.isTopLevel() && !Modifier.isStatic(typeBinding.getModifiers()) && isRefCounted(node, typeBinding)) {
      fatal(node, "Non-static inner class cannot be Refcounted", node.getName());
    }
  }

  @Override
  public void endVisit(@NotNull VariableDeclarationFragment declaration) {
    if (skip(declaration)) return;
    debug(declaration, "VariableDeclarationFragment: %s", declaration);
    final ITypeBinding declarationType = getTypeBinding(declaration);
    if (null == declarationType) {
      warn(declaration, "Cannot resolve type of %s", declaration);
      return;
    }
    if (skip(declaration)) return;
    if (null == declarationType) {
      warn(declaration, "Unresolved binding");
      return;
    }
    debug(1, declaration, "addFreeRef: %s", declaration);
    if (isRefCounted(declaration, declarationType)) {
      ASTNode parent = declaration.getParent();
      if (parent instanceof FieldDeclaration) {
        if(!Modifier.isStatic(((FieldDeclaration)parent).getModifiers())) {
          final ASTNode fieldParent = parent.getParent();
          if (fieldParent instanceof TypeDeclaration) {
            final TypeDeclaration typeDeclaration = (TypeDeclaration) fieldParent;
            final ITypeBinding typeBinding = typeDeclaration.resolveBinding();
            if(null == typeBinding) {
              warn(typeDeclaration, "Unresolved binding");
              return;
            }
            final Optional<MethodDeclaration> freeMethodOpt = ASTUtil.findMethod(typeDeclaration, "_free");
            if (!isRefCounted(typeDeclaration, typeBinding)) {
              fatal(declaration, "Unaccountable Reference: %s::%s", typeDeclaration.getName(), declaration.getName());
            }
          } else {
            fatal(declaration, "Cannot add freeRef for %s (FieldDeclaration) in %s : %s", declaration.getName(), fieldParent.getClass(), fieldParent.toString().trim());
          }
        }
      }
    } else {
      debug(declaration, "%s is not refcounted (%s)", declaration, declarationType);
    }
  }

}
