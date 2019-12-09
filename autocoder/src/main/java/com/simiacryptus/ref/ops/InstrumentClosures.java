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

import com.simiacryptus.ref.core.ops.IndexSymbols;
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
public class InstrumentClosures extends RefFileAstVisitor {

  private final IndexSymbols.SymbolIndex index;

  public InstrumentClosures(CompilationUnit compilationUnit, File file, IndexSymbols.SymbolIndex index) {
    super(compilationUnit, file);
    this.index = index;
  }

  public void addRefcounting(@Nonnull AnonymousClassDeclaration node, Map<IndexSymbols.BindingId, List<IndexSymbols.Span>> closures) {
    final AST ast = node.getAST();
    final Optional<MethodDeclaration> freeMethodOpt = findMethod(node, "_free");
    if (freeMethodOpt.isPresent()) {
      closures.keySet().stream().map(index.definitionNodes::get).filter(x -> x != null).forEach(closureNode -> {
        if (closureNode instanceof SingleVariableDeclaration) {
          final SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) closureNode;
          final ITypeBinding type = getTypeBinding(singleVariableDeclaration);
          if (isRefCounted(node, type)) {
            final SimpleName name = (SimpleName) ASTNode.copySubtree(ast, singleVariableDeclaration.getName());
            freeMethodOpt.get().getBody().statements().add(0, ast.newExpressionStatement(newFreeRef(name, type)));
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
        final ITypeBinding type = singleVariableDeclaration.resolveBinding().getType();
        if (isRefCounted(node, type)) {
          final SimpleName name = (SimpleName) ASTNode.copySubtree(ast, singleVariableDeclaration.getName());
          initializerBlock.statements().add(ast.newExpressionStatement(newAddRef(name, type)));
        }
      } else {
        warn(node, "Cannot handle " + closureNode.getClass().getSimpleName());
      }
    });
    initializer.setBody(initializerBlock);
    node.bodyDeclarations().add(initializer);
  }

  @Override
  public void endVisit(AnonymousClassDeclaration node) {
    final IndexSymbols.Span lambdaLocation = getSpan(node);
    final Map<IndexSymbols.BindingId, List<IndexSymbols.Span>> closures = getClosures(node, lambdaLocation, getSymbolIndex(node));
    if (closures.size() > 0) {
      final ITypeBinding typeBinding = node.resolveBinding();
      final IndexSymbols.BindingId bindingId = index.describe(typeBinding);
      if (typeBinding.getSuperclass().getQualifiedName().equals("java.lang.Object") && typeBinding.getInterfaces().length > 0) {
        info(node, String.format("Closures in anonymous interface %s at %s: %s",
            bindingId,
            lambdaLocation,
            closures.keySet().stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b).get()));
        wrapInterface((Expression) node.getParent(), closures);
      } else if (isRefCounted(node, typeBinding)) {
        info(node, String.format("Closures in anonymous RefCountable in %s at %s: %s",
            bindingId,
            lambdaLocation,
            closures.keySet().stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b).get()));
        addRefcounting(node, closures);
      } else {
        warn(node, String.format("Closures in Non-RefCountable %s at %s: %s",
            bindingId,
            lambdaLocation,
            closures.keySet().stream().map(x -> x.toString()).reduce((a, b) -> a + ", " + b).get()));
      }
    }
  }

  @Override
  public void endVisit(LambdaExpression node) {
    final IMethodBinding methodBinding = node.resolveMethodBinding();
    if (null == methodBinding) return;
    final IndexSymbols.BindingId bindingId = index.describe(methodBinding).setType("Lambda");
    final IndexSymbols.Span lambdaLocation = getSpan(node);
    final IndexSymbols.SymbolIndex symbolIndex = getSymbolIndex(node);
    final Map<IndexSymbols.BindingId, List<IndexSymbols.Span>> closures = getClosures(node, lambdaLocation, symbolIndex);
    if (closures.size() > 0) {
      info(node, String.format("Closures in %s at %s\n\t%s",
          bindingId,
          lambdaLocation,
          closures.keySet().stream().map(x -> x.toString()).reduce((a, b) -> a + "\n\t" + b).get()));
      wrapInterface(node, closures);
    }
  }

  private Map<IndexSymbols.BindingId, List<IndexSymbols.Span>> getClosures(ASTNode node, IndexSymbols.Span lambdaLocation, IndexSymbols.SymbolIndex lambdaIndex) {
    return lambdaIndex.references.entrySet().stream().flatMap(e -> {
      final IndexSymbols.BindingId bindingId = e.getKey();
      final IndexSymbols.ContextLocation definition = index.definitionLocations.get(bindingId);
      if (bindingId.type.equals("Type")) {
      } else if (definition == null) {
        debug(node, "Unresolved ref %s in %s", bindingId, lambdaLocation);
      } else if (!lambdaLocation.contains(definition.location)) {
        debug(node, String.format("Closure %s in %s defined by %s", bindingId, e.getValue().stream().map(x -> x.location.toString()).reduce((a, b) -> a + ", " + b).get(), definition.location));
        return e.getValue().stream().map(x -> new Tuple2<>(bindingId, x.location));
      }
      return Stream.empty();
    }).filter(x -> x != null).collect(Collectors.groupingBy(x -> x._1, Collectors.mapping(x -> x._2, Collectors.toList())));
  }

  public void wrapInterface(Expression node, Map<IndexSymbols.BindingId, List<IndexSymbols.Span>> closures) {
    AST ast = node.getAST();
    final MethodInvocation methodInvocation = ast.newMethodInvocation();
    methodInvocation.setExpression(newQualifiedName(ast, RefUtil.class));
    methodInvocation.setName(ast.newSimpleName("wrapInterface"));
    final CastExpression castExpression = ast.newCastExpression();
    final Type castType = getType(node, false);
    if (null != castType) {
      warn(node, "Unresolved binding");
      castExpression.setType(castType);
      castExpression.setExpression((Expression) ASTNode.copySubtree(ast, node));
      methodInvocation.arguments().add(castExpression);
    } else {
      methodInvocation.arguments().add(ASTNode.copySubtree(ast, node));
    }
    closures.keySet().stream().map(index.definitionNodes::get).filter(x -> x != null).forEach(closureNode -> {
      if (closureNode instanceof SingleVariableDeclaration) {
        final SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) closureNode;
        final ITypeBinding type = getTypeBinding(singleVariableDeclaration);
        if (isRefCounted(node, type)) {
          methodInvocation.arguments().add(wrapAddRef((Expression) ASTNode.copySubtree(ast, singleVariableDeclaration.getName()), type));
        }
      } else if (closureNode instanceof VariableDeclarationFragment) {
        final VariableDeclarationFragment singleVariableDeclaration = (VariableDeclarationFragment) closureNode;
        final ITypeBinding type = getTypeBinding(singleVariableDeclaration);
        if (isRefCounted(node, type)) {
          methodInvocation.arguments().add(wrapAddRef((Expression) ASTNode.copySubtree(ast, singleVariableDeclaration.getName()), type));
        }
      } else {
        warn(node, "Cannot handle " + closureNode.getClass().getSimpleName());
      }
    });
    if (methodInvocation.arguments().size() > 1) {
      replace(node, methodInvocation);
    }
  }


  private static class Tuple2<A, B> implements Serializable {
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
