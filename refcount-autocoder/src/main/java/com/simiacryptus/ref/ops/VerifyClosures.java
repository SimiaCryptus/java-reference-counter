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
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RefIgnore
public class VerifyClosures extends VerifyClassMembers {

  public VerifyClosures(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    super(projectInfo, compilationUnit, file);
  }

  @Override
  public void endVisit(@Nonnull AnonymousClassDeclaration node) {
    final List<SimpleName> closures = getClosures(node);
    if (closures.size() > 0) {
      final ITypeBinding typeBinding = resolveBinding(node);
      assert typeBinding != null;
      final SymbolIndex.BindingID bindingID = SymbolIndex.getBindingID(typeBinding);
      if (isRefCounted(node, typeBinding)) {
        debug(node, String.format("Closures in anonymous RefCountable in %s at %s: %s",
            bindingID,
            getSpan(node),
            RefUtil.get(closures.stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b))));
        verifyClassDeclarations(closures, node.bodyDeclarations());
      } else {
        if (typeBinding.getSuperclass().getQualifiedName().equals("java.lang.Object")) {
          if (typeBinding.getInterfaces().length > 0) {
            debug(node, String.format("Closures in anonymous interface %s at %s: %s",
                bindingID,
                getSpan(node),
                RefUtil.get(closures.stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b))));
            verifyClassDeclarations(closures, node.bodyDeclarations());
          } else {
            warn(node, String.format("Closures in Non-RefCountable %s at %s: %s",
                bindingID,
                getSpan(node),
                RefUtil.get(closures.stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b))));
            verifyClassDeclarations(closures, node.bodyDeclarations());
          }
        } else {
          debug(node, String.format("Closures in Non-RefCountable %s at %s: %s",
              bindingID,
              getSpan(node),
              RefUtil.get(closures.stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b))));
          verifyClassDeclarations(closures, node.bodyDeclarations());
        }
      }
    }
  }

  @Override
  public void endVisit(@Nonnull LambdaExpression node) {
    final IMethodBinding methodBinding = resolveMethodBinding(node);
    if (null == methodBinding) return;
    final SymbolIndex.BindingID bindingID = SymbolIndex.getBindingID(methodBinding);
    final List<SimpleName> closures = getClosures(node);
    if (closures.size() > 0) {
      debug(node, String.format("Closures in %s (body at %s)\n\t%s",
          bindingID,
          getSpan(node),
          RefUtil.get(closures.stream().map(x -> x.toString()).reduce((a, b) -> a + "\n\t" + b))));
      ASTNode body = node.getBody();
      if (body instanceof Block) {
        verifyClassMethod(SymbolIndex.getBindingID(node.resolveMethodBinding()).path, ((Block) body).statements(), closures);
      } else if (body instanceof Expression) {
        verifyLambda(SymbolIndex.getBindingID(node.resolveMethodBinding()).path, (Expression) body, TerminalState.Open, closures);
      } else {
        fatal(body, "Unhandled body type: %s", body.getClass().getSimpleName());
      }
    }
  }

  protected void verifyClassDeclarations(@Nonnull Collection<SimpleName> closures, @Nonnull List<ASTNode> bodyDeclarations) {
    ArrayList<CollectableException> exceptions = new ArrayList<>();
    for (MethodDeclaration methodDeclaration : VerifyClassMembers.methods(bodyDeclarations)) {
      try {
        verifyClassMethod(methodDeclaration.getName().toString(), methodDeclaration.getBody().statements(), closures);
      } catch (CollectableException e) {
        exceptions.add(e);
      }
    }
    if (!exceptions.isEmpty()) {
      throw CollectableException.combine(exceptions);
    }
  }

  @Nullable
  protected SimpleName getName(SymbolIndex.BindingID bindingID) {
    ASTNode definition = index.definitions.get(bindingID);
    if (definition instanceof VariableDeclarationFragment) {
      return ((VariableDeclarationFragment) definition).getName();
    } else if (definition instanceof SingleVariableDeclaration) {
      return ((SingleVariableDeclaration) definition).getName();
    } else if (definition instanceof FieldDeclaration) {
      List fragments = ((FieldDeclaration) definition).fragments();
      if (fragments.size() > 1) fatal(definition, "Multi-definition not supported: %s", bindingID);
      return ((VariableDeclarationFragment) fragments.get(0)).getName();
    } else {
      fatal(definition, "Unhandled type %s for %s", definition.getClass().getSimpleName(), bindingID);
      return null;
    }
  }

  protected List<SimpleName> getClosures(@Nonnull ASTNode node) {
    return VisitClosures.getClosures(this, index, node).stream().filter(bindingID -> {
      ASTNode definition = index.definitions.get(bindingID);
      if (definition instanceof VariableDeclarationFragment) {
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) definition;
        IVariableBinding variableBinding = fragment.resolveBinding();
        return isRefCounted(fragment, variableBinding.getType()) || ASTUtil.hasAnnotation(variableBinding, RefAware.class);
      } else if (definition instanceof SingleVariableDeclaration) {
        SingleVariableDeclaration variableDeclaration = (SingleVariableDeclaration) definition;
        IVariableBinding variableBinding = variableDeclaration.resolveBinding();
        return isRefCounted(variableDeclaration, variableBinding.getType()) || ASTUtil.hasAnnotation(variableBinding, RefAware.class);
      } else if (definition instanceof FieldDeclaration) {
        FieldDeclaration fieldDeclaration = (FieldDeclaration) definition;
        List fragments = fieldDeclaration.fragments();
        if (fragments.size() > 1) fatal(definition, "Multi-definition not supported: %s", bindingID);
        VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(0);
        IVariableBinding variableBinding = fragment.resolveBinding();
        return isRefCounted(fragment, variableBinding.getType()) || ASTUtil.hasAnnotation(variableBinding, RefAware.class);
      } else if (definition instanceof MethodDeclaration) {
        return false;
      } else {
        fatal(definition, "Unhandled type %s for %s", definition.getClass().getSimpleName(), bindingID);
        return false;
      }
    }).map(this::getName).collect(Collectors.toList());
  }

}
