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

import com.simiacryptus.ref.core.ASTUtil;
import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.core.SymbolIndex;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import org.eclipse.jdt.core.dom.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RefIgnore
public class InstrumentClosures extends RefASTOperator {

  protected final SymbolIndex index;

  protected InstrumentClosures(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file, SymbolIndex index) {
    super(projectInfo, compilationUnit, file);
    this.index = index;
  }

  public void addRefcounting(@Nonnull AnonymousClassDeclaration node, Map<SymbolIndex.BindingID, List<SymbolIndex.Span>> closures) {
    final Optional<MethodDeclaration> freeMethodOpt = ASTUtil.findMethod(node, "_free");
    if (freeMethodOpt.isPresent()) {
      closures.keySet().stream().map(index.definitionNodes::get).filter(x -> x != null).forEach(closureNode -> {
        if (closureNode instanceof SingleVariableDeclaration) {
          final SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) closureNode;
          final ITypeBinding type = getTypeBinding(singleVariableDeclaration);
          if (isRefCounted(node, type)) {
            final SimpleName name = ast.newSimpleName(singleVariableDeclaration.getName().getIdentifier());
            freeMethodOpt.get().getBody().statements().add(0, ASTUtil.hasAnnotation(singleVariableDeclaration.resolveBinding(), Nonnull.class) ? newFreeRef(name, type) : freeRefStatement(name, type));
          }
        } else {
          warn(node, "Cannot handle " + closureNode.getClass().getSimpleName());
        }
      });
    }
    final Initializer initializer = ast.newInitializer();
    final Block initializerBlock = ast.newBlock();
    closures.keySet().stream().map(index.definitionNodes::get).filter(x -> x != null).forEach(closureNode -> {
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

  public void wrapInterface(Expression node, Map<SymbolIndex.BindingID, List<SymbolIndex.Span>> closures) {
    final List<ASTNode> refClosures = closures.entrySet().stream().filter(entry -> {
      final ASTNode definition = index.definitionNodes.get(entry.getKey());
      if (definition == null) {
        warn(node, "Cannot find definition for %s", entry.getKey());
        return false;
      } else if (definition instanceof SingleVariableDeclaration) {
        final SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) definition;
        final ITypeBinding type = getTypeBinding(singleVariableDeclaration);
        if (isRefCounted(node, type)) {
          return true;
        } else {
          return false;
        }
      } else if (definition instanceof VariableDeclarationFragment) {
        final VariableDeclarationFragment singleVariableDeclaration = (VariableDeclarationFragment) definition;
        final ITypeBinding type = getTypeBinding(singleVariableDeclaration);
        if (isRefCounted(node, type)) {
          return true;
        } else {
          return false;
        }
      } else {
        warn(node, "Cannot handle " + definition.getClass().getSimpleName());
        return false;
      }
    }).map(Map.Entry::getKey).map(index.definitionNodes::get).collect(Collectors.toList());
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
          methodInvocation.arguments().add(wrapAddRef(name, type));
        } else if (closureNode instanceof VariableDeclarationFragment) {
          final VariableDeclarationFragment singleVariableDeclaration = (VariableDeclarationFragment) closureNode;
          final ITypeBinding type = getTypeBinding(singleVariableDeclaration);
          final SimpleName name = ast.newSimpleName(singleVariableDeclaration.getName().getIdentifier());
          methodInvocation.arguments().add(wrapAddRef(name, type));
        }
      });
    }
  }

  protected Map<SymbolIndex.BindingID, List<SymbolIndex.Span>> getClosures(ASTNode node, SymbolIndex.Span lambdaLocation, SymbolIndex lambdaIndex) {
    return lambdaIndex.references.entrySet().stream().flatMap(e -> {
      final SymbolIndex.BindingID bindingID = e.getKey();
      final SymbolIndex.ContextLocation definition = index.definitionLocations.get(bindingID);
      if (bindingID.type.equals("Type")) {
      } else if (definition == null) {
        debug(node, "Unresolved ref %s in %s", bindingID, lambdaLocation);
      } else if (!lambdaLocation.contains(definition.location)) {
        debug(node, String.format("Closure %s in %s defined by %s", bindingID, e.getValue().stream().map(x -> x.location.toString()).reduce((a, b) -> a + ", " + b).get(), definition.location));
        return e.getValue().stream().map(x -> new Tuple2<>(bindingID, x.location));
      }
      return Stream.empty();
    }).filter(x -> x != null).collect(Collectors.groupingBy(x -> x._1, Collectors.mapping(x -> x._2, Collectors.toList())));
  }

  public static class ModifyAnonymousClassDeclaration extends InstrumentClosures {
    public ModifyAnonymousClassDeclaration(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file, SymbolIndex index) {
      super(projectInfo, compilationUnit, file, index);
    }

    @Override
    public void endVisit(AnonymousClassDeclaration node) {
      final SymbolIndex.Span lambdaLocation = getSpan(node);
      final Map<SymbolIndex.BindingID, List<SymbolIndex.Span>> closures = getClosures(node, lambdaLocation, getSymbolIndex(node));
      if (closures.size() > 0) {
        final ITypeBinding typeBinding = resolveBinding(node);
        final SymbolIndex.BindingID bindingID = index.getBindingID(typeBinding);
        if (typeBinding.getSuperclass().getQualifiedName().equals("java.lang.Object") && typeBinding.getInterfaces().length > 0) {
          info(node, String.format("Closures in anonymous interface %s at %s: %s",
              bindingID,
              lambdaLocation,
              closures.keySet().stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b).get()));
          wrapInterface((Expression) node.getParent(), closures);
        } else if (isRefCounted(node, typeBinding)) {
          info(node, String.format("Closures in anonymous RefCountable in %s at %s: %s",
              bindingID,
              lambdaLocation,
              closures.keySet().stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b).get()));
          addRefcounting(node, closures);
        } else {
          warn(node, String.format("Closures in Non-RefCountable %s at %s: %s",
              bindingID,
              lambdaLocation,
              closures.keySet().stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b).get()));
        }
      }
    }
  }

  public static class ModifyLambdaExpression extends InstrumentClosures {
    public ModifyLambdaExpression(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file, SymbolIndex index) {
      super(projectInfo, compilationUnit, file, index);
    }

    @Override
    public void endVisit(LambdaExpression node) {
      final IMethodBinding methodBinding = resolveMethodBinding(node);
      if (null == methodBinding) return;
      final SymbolIndex.BindingID bindingID = index.getBindingID(methodBinding).setType("Lambda");
      final SymbolIndex.Span lambdaLocation = getSpan(node);
      final SymbolIndex symbolIndex = getSymbolIndex(node);
      final Map<SymbolIndex.BindingID, List<SymbolIndex.Span>> closures = getClosures(node, lambdaLocation, symbolIndex);
      if (closures.size() > 0) {
        info(node, String.format("Closures in %s at %s\n\t%s",
            bindingID,
            lambdaLocation,
            closures.keySet().stream().map(x -> x.toString()).reduce((a, b) -> a + "\n\t" + b).get()));
        wrapInterface(node, closures);
      }
    }
  }

  protected static class Tuple2<A, B> implements Serializable {
    public final A _1;
    public final B _2;

    public Tuple2() {
      this(null, null);
    }

    public Tuple2(final A a, final B b) {
      _1 = a;
      _2 = b;
    }

    public A getFirst() {
      return _1;
    }

    public B getSecond() {
      return _2;
    }
  }
}
