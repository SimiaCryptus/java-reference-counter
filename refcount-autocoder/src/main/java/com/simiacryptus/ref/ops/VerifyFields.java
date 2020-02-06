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
import com.simiacryptus.ref.core.CollectableException;
import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.core.SymbolIndex;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import org.eclipse.jdt.core.dom.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RefIgnore
public class VerifyFields extends VerifyClassMembers {

  public VerifyFields(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    super(projectInfo, compilationUnit, file);
  }

  @Nonnull
  public List<SimpleName> fields(@Nonnull List<ASTNode> bodyDeclarations) {
    return bodyDeclarations.stream().filter(x -> x instanceof FieldDeclaration).map(x -> (FieldDeclaration) x)
        .filter(x -> !Modifier.isStatic(x.getModifiers()))
        .flatMap(x -> ((List<VariableDeclarationFragment>) x.fragments()).stream()
            .filter(variableDeclarationFragment -> {
              IVariableBinding resolveBinding = variableDeclarationFragment.resolveBinding();
              if (null == resolveBinding) {
                warn(variableDeclarationFragment, "Unresolved binding");
                return false;
              }
              if(ASTUtil.hasAnnotation(resolveBinding, RefIgnore.class)) return false;
              return isRefCounted(variableDeclarationFragment, resolveBinding.getType()) || ASTUtil.hasAnnotation(resolveBinding, RefAware.class);
            })
            .map(VariableDeclaration::getName))
        .collect(Collectors.toList());
  }

  @Override
  public void endVisit(@Nonnull TypeDeclaration node) {
    final List<SimpleName> fields = fields(node.bodyDeclarations());
    final ITypeBinding typeBinding = resolveBinding(node);
    final SymbolIndex.BindingID bindingID = SymbolIndex.getBindingID(typeBinding);
    if (isRefCounted(node, typeBinding)) {
      debug(node, String.format("Closures in anonymous RefCountable in %s at %s: %s",
          bindingID,
          getSpan(node),
          fields.stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b).orElse("")));
      if(!typeBinding.isInterface()) assertHasFree(node, node.bodyDeclarations());
      verifyClassDeclarations(node.bodyDeclarations());
    } else if(!fields.isEmpty()) {
      fatal(node, "Non-refcounted type defined refcounted fields %s",
          fields.stream().map(x -> x.getFullyQualifiedName()).reduce((a, b) -> a + ", " + b).orElse(""));
    }
  }

  @Override
  public void endVisit(@Nonnull AnonymousClassDeclaration node) {
    final ITypeBinding typeBinding = resolveBinding(node);
    assert typeBinding != null;
    final List<SimpleName> fields = fields(node.bodyDeclarations());
    final SymbolIndex.BindingID bindingID = SymbolIndex.getBindingID(typeBinding);
    if (isRefCounted(node, typeBinding)) {
      debug(node, String.format("Closures in anonymous RefCountable in %s at %s: %s",
          bindingID,
          getSpan(node),
          fields.stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b).orElse("")));
      verifyClassDeclarations(node.bodyDeclarations());
      assertHasFree(node, node.bodyDeclarations());
    } else {
      if (typeBinding.getSuperclass().getQualifiedName().equals("java.lang.Object")) {
        if (typeBinding.getInterfaces().length > 0) {
          debug(node, String.format("Closures in anonymous interface %s at %s: %s",
              bindingID,
              getSpan(node),
              fields.stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b).orElse("")));
          verifyClassDeclarations(node.bodyDeclarations());
        } else {
          warn(node, String.format("Closures in Non-RefCountable %s at %s: %s",
              bindingID,
              getSpan(node),
              fields.stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b).orElse("")));
          verifyClassDeclarations(node.bodyDeclarations());
        }
      } else {
        debug(node, String.format("Closures in Non-RefCountable %s at %s: %s",
            bindingID,
            getSpan(node),
            fields.stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b).orElse("")));
        verifyClassDeclarations(node.bodyDeclarations());
      }
    }
  }

  protected void assertHasFree(ASTNode node, @Nonnull List<ASTNode> bodyDeclarations) {
    Optional<String> freeMethod = bodyDeclarations.stream()
        .filter(x -> x instanceof MethodDeclaration)
        .map(x -> (MethodDeclaration) x)
        .map(x -> x.getName().toString())
        .filter(x -> x.equals("_free"))
        .findAny();
    if (!freeMethod.isPresent()) {
      fatal(node, "No _free defined");
    }
  }

  protected void verifyClassDeclarations(@Nonnull List<ASTNode> bodyDeclarations) {
    ArrayList<CollectableException> exceptions = new ArrayList<>();
    List<SimpleName> fields = fields(bodyDeclarations);
    for (MethodDeclaration methodDeclaration : VerifyClassMembers.methods(bodyDeclarations)) {
      try {
        if (methodDeclaration.isConstructor()) continue;
        Block body = methodDeclaration.getBody();
        if (null == body) continue;
        verifyClassMethod(methodDeclaration.getName().toString(), body.statements(), fields);
      } catch (CollectableException e) {
        exceptions.add(e);
      }
    }
    if (!exceptions.isEmpty()) {
      throw CollectableException.combine(exceptions);
    }
  }

}
