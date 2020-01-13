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
import com.simiacryptus.ref.core.SymbolIndex;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@RefIgnore
public class VerifyFields extends VerifyClassMembers {

  public VerifyFields(ProjectInfo projectInfo, @NotNull CompilationUnit compilationUnit, @NotNull File file) {
    super(projectInfo, compilationUnit, file);
  }

  @NotNull
  public List<SimpleName> fields(List<ASTNode> bodyDeclarations) {
    return bodyDeclarations.stream().filter(x -> x instanceof FieldDeclaration).map(x -> (FieldDeclaration) x)
        .filter(x -> !Modifier.isStatic(x.getModifiers()))
        .flatMap(x -> ((List<VariableDeclarationFragment>) x.fragments()).stream()
            .filter(variableDeclarationFragment -> {
              IVariableBinding resolveBinding = variableDeclarationFragment.resolveBinding();
              if (null == resolveBinding) {
                warn(variableDeclarationFragment, "Unresolved binding");
                return false;
              }
              return isRefCounted(variableDeclarationFragment, resolveBinding.getType()) || ASTUtil.hasAnnotation(resolveBinding, RefAware.class);
            })
            .map(VariableDeclaration::getName))
        .collect(Collectors.toList());
  }

  @Override
  public void endVisit(TypeDeclaration node) {
    final List<SimpleName> fields = fields(node.bodyDeclarations());
    if (fields.size() > 0) {
      final ITypeBinding typeBinding = resolveBinding(node);
      final SymbolIndex.BindingID bindingID = SymbolIndex.getBindingID(typeBinding);
      if (isRefCounted(node, typeBinding)) {
        debug(node, String.format("Closures in anonymous RefCountable in %s at %s: %s",
            bindingID,
            getSpan(node),
            RefUtil.get(fields.stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b))));
        verifyClassDeclarations(node.bodyDeclarations());
      } else {
        fatal(node, "Non-refcounted type defined refcounted fields %s",
            RefUtil.get(fields.stream().map(x -> x.getFullyQualifiedName()).reduce((a, b) -> a + ", " + b)));
      }
    }
  }

  @Override
  public void endVisit(@NotNull AnonymousClassDeclaration node) {
    final List<SimpleName> fields = fields(node.bodyDeclarations());
    if (fields.size() > 0) {
      final ITypeBinding typeBinding = resolveBinding(node);
      final SymbolIndex.BindingID bindingID = SymbolIndex.getBindingID(typeBinding);
      if (isRefCounted(node, typeBinding)) {
        debug(node, String.format("Closures in anonymous RefCountable in %s at %s: %s",
            bindingID,
            getSpan(node),
            RefUtil.get(fields.stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b))));
        verifyClassDeclarations(node.bodyDeclarations());
      } else {
        if (typeBinding.getSuperclass().getQualifiedName().equals("java.lang.Object")) {
          if (typeBinding.getInterfaces().length > 0) {
            debug(node, String.format("Closures in anonymous interface %s at %s: %s",
                bindingID,
                getSpan(node),
                RefUtil.get(fields.stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b))));
            verifyClassDeclarations(node.bodyDeclarations());
          } else {
            warn(node, String.format("Closures in Non-RefCountable %s at %s: %s",
                bindingID,
                getSpan(node),
                RefUtil.get(fields.stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b))));
            verifyClassDeclarations(node.bodyDeclarations());
          }
        } else {
          debug(node, String.format("Closures in Non-RefCountable %s at %s: %s",
              bindingID,
              getSpan(node),
              RefUtil.get(fields.stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b))));
          verifyClassDeclarations(node.bodyDeclarations());
        }
      }
    }
  }

  protected void verifyClassDeclarations(List<ASTNode> bodyDeclarations) {
    for (MethodDeclaration methodDeclaration : VerifyClassMembers.methods(bodyDeclarations)) {
      if(methodDeclaration.isConstructor()) continue;
      Block body = methodDeclaration.getBody();
      if(null==body) continue;
      verifyClassMethod(methodDeclaration.getName().toString(), body.statements(), fields(bodyDeclarations));
    }
  }

}
