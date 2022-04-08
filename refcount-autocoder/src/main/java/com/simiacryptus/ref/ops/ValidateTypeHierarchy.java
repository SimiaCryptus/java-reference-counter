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
import org.eclipse.jdt.core.dom.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

/**
 * This class validates the type hierarchy.
 *
 * @docgenVersion 9
 */
@RefIgnore
public class ValidateTypeHierarchy extends RefASTOperator {

  public ValidateTypeHierarchy(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    super(projectInfo, compilationUnit, file);
  }

  /**
   * This method is called when the end of a type declaration is reached.
   *
   * @param node the type declaration that is ending
   * @docgenVersion 9
   */
  @Override
  public void endVisit(@Nonnull TypeDeclaration node) {
    final ITypeBinding typeBinding = node.resolveBinding();
    if (null == typeBinding) {
      warn(node, "Cannot resolve type of %s", node.getName());
      return;
    }
    if (!typeBinding.isTopLevel() && !Modifier.isStatic(typeBinding.getModifiers()) && isRefCounted(node, typeBinding)) {
      fatal(node, "Non-static inner class cannot be Refcounted", node.getName());
    }
  }

  /**
   * This method is called when the visitor encounters a variable declaration fragment.
   *
   * @param declaration the variable declaration fragment to visit
   * @docgenVersion 9
   */
  @Override
  public void endVisit(@Nonnull VariableDeclarationFragment declaration) {
    if (skip(declaration)) return;
    debug(declaration, "VariableDeclarationFragment: %s", declaration);
    final ITypeBinding declarationType = getTypeBinding(declaration);
    if (null == declarationType) {
      warn(declaration, "Cannot resolve type of %s", declaration);
      return;
    }
    if (skip(declaration)) return;
    debug(1, declaration, "addFreeRef: %s", declaration);
    if (isRefCounted(declaration, declarationType)) {
      ASTNode parent = declaration.getParent();
      if (parent instanceof FieldDeclaration) {
        if (!Modifier.isStatic(((FieldDeclaration) parent).getModifiers())) {
          final ASTNode fieldParent = parent.getParent();
          if (fieldParent instanceof TypeDeclaration) {
            validateField(declaration, ((TypeDeclaration) fieldParent).resolveBinding());
          } else if (fieldParent instanceof AnonymousClassDeclaration) {
            validateField(declaration, ((AnonymousClassDeclaration) fieldParent).resolveBinding());
          } else {
            fatal(declaration, "Cannot add freeRef for %s (FieldDeclaration) in %s : %s", declaration.getName(), fieldParent.getClass(), fieldParent.toString().trim());
          }
        }
      }
    } else {
      debug(declaration, "%s is not refcounted (%s)", declaration, declarationType);
    }
  }

  /**
   * Validates a field.
   *
   * @param declaration the field declaration
   * @param typeBinding the type binding, or null if none
   * @docgenVersion 9
   */
  private void validateField(@Nonnull VariableDeclarationFragment declaration, @Nullable ITypeBinding typeBinding) {
    if (null == typeBinding) {
      warn(declaration, "Unresolved binding");
    } else {
      if (!isRefCounted(declaration, typeBinding)) {
        fatal(declaration, "Unaccountable Reference: %s::%s", declaration.getName(), declaration.getName());
      }
    }
  }

}
