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
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import org.eclipse.jdt.core.dom.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RefIgnore
public class InstrumentClosures extends VisitClosures {

  protected InstrumentClosures(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    super(projectInfo, compilationUnit, file);
  }

  public void addRefcounting(@Nonnull AnonymousClassDeclaration node, @Nonnull Collection<SymbolIndex.BindingID> closures) {
    final Optional<MethodDeclaration> freeMethodOpt = ASTUtil.findMethod(node, "_free");
    if (freeMethodOpt.isPresent()) {
      closures.stream().map(index.definitions::get).filter(x -> x != null).forEach(closureNode -> {
        if (closureNode instanceof SingleVariableDeclaration) {
          final SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) closureNode;
          final ITypeBinding type = getTypeBinding(singleVariableDeclaration);
          assert type != null;
          if (isRefCounted(node, type)) {
            final SimpleName name = ast.newSimpleName(singleVariableDeclaration.getName().getIdentifier());
            final Block freeMethodBody = RefUtil.get(freeMethodOpt).getBody();
            final boolean isNonNull = ASTUtil.hasAnnotation(singleVariableDeclaration.resolveBinding(), Nonnull.class);
            freeMethodBody.statements().add(0, isNonNull ? newFreeRef(name, type) : freeRefStatement(name, type));
            debug(name, "Adding freeRef for %s", name);
          }
        } else {
          warn(node, "Cannot handle " + closureNode.getClass().getSimpleName());
        }
      });
    }
    final Initializer initializer = ast.newInitializer();
    final Block initializerBlock = ast.newBlock();
    closures.stream().map(index.definitions::get).filter(x -> x != null).forEach(closureNode -> {
      if (closureNode instanceof SingleVariableDeclaration) {
        final SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) closureNode;
        final ITypeBinding type = resolveBinding(singleVariableDeclaration).getType();
        if (isRefCounted(node, type)) {
          final SimpleName name = ast.newSimpleName(singleVariableDeclaration.getName().getIdentifier());
          initializerBlock.statements().add(ast.newExpressionStatement(newAddRef(name, type)));
        }
      } else {
        warn(node, "Cannot handle " + closureNode.getClass().getSimpleName());
      }
    });
    initializer.setBody(initializerBlock);
    node.bodyDeclarations().add(initializer);
  }

  public void wrapInterface(@Nonnull Expression node, @Nonnull Collection<SymbolIndex.BindingID> closures) {
    final List<ASTNode> refClosures = closures.stream().filter(bindingID -> {
      final ASTNode definition = index.definitions.get(bindingID);
      if (definition == null) {
        warn(node, "Cannot find definition for %s", bindingID);
        return false;
      } else if (definition instanceof SingleVariableDeclaration) {
        final SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) definition;
        final ITypeBinding type = getTypeBinding(singleVariableDeclaration);
        assert type != null;
        if (isRefCounted(node, type)) {
          return true;
        } else {
          return false;
        }
      } else if (definition instanceof VariableDeclarationFragment) {
        final VariableDeclarationFragment singleVariableDeclaration = (VariableDeclarationFragment) definition;
        final ITypeBinding type = getTypeBinding(singleVariableDeclaration);
        assert type != null;
        if (isRefCounted(node, type)) {
          return true;
        } else {
          return false;
        }
      } else {
        warn(node, "Cannot handle " + definition.getClass().getSimpleName());
        return false;
      }
    }).map(index.definitions::get).collect(Collectors.toList());
    if (!refClosures.isEmpty()) {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      final Type castType = getType(node, false);
      methodInvocation.setExpression(ASTUtil.newQualifiedName(ast, RefUtil.class));
      methodInvocation.setName(ast.newSimpleName("wrapInterface"));
      replace(node, methodInvocation);
      final CastExpression castExpression = ast.newCastExpression();
      if (null != castType) {
        warn(node, "Unresolved binding");
        castExpression.setType(castType);
        castExpression.setExpression(node);
        methodInvocation.arguments().add(castExpression);
      } else {
        methodInvocation.arguments().add(node);
      }
      refClosures.stream().filter(x -> x != null).forEach(closureNode -> {
        if (closureNode instanceof SingleVariableDeclaration) {
          final SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) closureNode;
          final ITypeBinding type = getTypeBinding(singleVariableDeclaration);
          final SimpleName name = ast.newSimpleName(singleVariableDeclaration.getName().getIdentifier());
          assert type != null;
          methodInvocation.arguments().add(wrapAddRef(name, type));
        } else if (closureNode instanceof VariableDeclarationFragment) {
          final VariableDeclarationFragment singleVariableDeclaration = (VariableDeclarationFragment) closureNode;
          final ITypeBinding type = getTypeBinding(singleVariableDeclaration);
          final SimpleName name = ast.newSimpleName(singleVariableDeclaration.getName().getIdentifier());
          assert type != null;
          methodInvocation.arguments().add(wrapAddRef(name, type));
        }
      });
    }
  }

  @RefIgnore
  public static class ModifyAnonymousClassDeclaration extends InstrumentClosures {
    public ModifyAnonymousClassDeclaration(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(@Nonnull AnonymousClassDeclaration node) {
      final Collection<SymbolIndex.BindingID> closures = getClosures(node);
      if (closures.size() > 0) {
        final ITypeBinding typeBinding = resolveBinding(node);
        assert typeBinding != null;
        final SymbolIndex.BindingID bindingID = SymbolIndex.getBindingID(typeBinding);
        if (typeBinding.getSuperclass().getQualifiedName().equals("java.lang.Object") && typeBinding.getInterfaces().length > 0) {
          debug(node, String.format("Closures in anonymous interface %s at %s: %s",
              bindingID,
              getSpan(node),
              RefUtil.get(closures.stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b))));
          wrapInterface((Expression) node.getParent(), closures);
        } else if (isRefCounted(node, typeBinding)) {
          debug(node, String.format("Closures in anonymous RefCountable in %s at %s: %s",
              bindingID,
              getSpan(node),
              RefUtil.get(closures.stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b))));
          addRefcounting(node, closures);
        } else {
          debug(node, String.format("Closures in Non-RefCountable %s at %s: %s",
              bindingID,
              getSpan(node),
              RefUtil.get(closures.stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b))));
        }
      }
    }
  }

  @RefIgnore
  public static class ModifyLambdaExpression extends InstrumentClosures {
    public ModifyLambdaExpression(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
      super(projectInfo, compilationUnit, file);
    }

    @Override
    public void endVisit(@Nonnull LambdaExpression node) {
      final IMethodBinding methodBinding = resolveMethodBinding(node);
      if (null == methodBinding) return;
      final SymbolIndex.BindingID bindingID = SymbolIndex.getBindingID(methodBinding);
      final Collection<SymbolIndex.BindingID> closures = getClosures(node);
      if (closures.size() > 0) {
        debug(node, String.format("Closures in %s (body at %s)\n\t%s",
            bindingID,
            getSpan(node),
            RefUtil.get(closures.stream().map(x -> x.toString()).reduce((a, b) -> a + "\n\t" + b))));
        wrapInterface(node, closures);
      }
    }
  }

}
