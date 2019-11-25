package com.simiacryptus.devutil.ref;

import com.simiacryptus.devutil.AutoCoder;
import com.simiacryptus.devutil.ops.IndexSymbols;
import com.simiacryptus.lang.ref.RefUtil;
import org.eclipse.jdt.core.dom.*;
import scala.Tuple2;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class InstrumentClosures extends RefFileAstVisitor {

  private final IndexSymbols.SymbolIndex index;

  InstrumentClosures(CompilationUnit compilationUnit, File file, IndexSymbols.SymbolIndex index) {
    super(compilationUnit, file);
    this.index = index;
  }

  public void addRefcounting(@Nonnull AnonymousClassDeclaration declaration, Map<IndexSymbols.BindingId, List<IndexSymbols.Span>> closures) {
    final AST ast = declaration.getAST();
    final Optional<MethodDeclaration> freeMethodOpt = AutoCoder.findMethod(declaration, "_free");
    if (freeMethodOpt.isPresent()) {
      closures.keySet().stream().map(index.definitionNodes::get).filter(x -> x != null).forEach(closureNode -> {
        if (closureNode instanceof SingleVariableDeclaration) {
          final SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) closureNode;
          final ITypeBinding type = singleVariableDeclaration.resolveBinding().getType();
          if (isRefCounted(type)) {
            final SimpleName name = (SimpleName) ASTNode.copySubtree(ast, singleVariableDeclaration.getName());
            freeMethodOpt.get().getBody().statements().add(0, ast.newExpressionStatement(newFreeRef(name, type)));
          }
        } else {
          warn(declaration, "Cannot handle " + closureNode.getClass().getSimpleName());
        }
      });
    }
    final Initializer initializer = ast.newInitializer();
    final Block initializerBlock = ast.newBlock();
    closures.keySet().stream().map(index.definitionNodes::get).filter(x -> x != null).forEach(closureNode -> {
      if (closureNode instanceof SingleVariableDeclaration) {
        final SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) closureNode;
        final ITypeBinding type = singleVariableDeclaration.resolveBinding().getType();
        if (isRefCounted(type)) {
          final SimpleName name = (SimpleName) ASTNode.copySubtree(ast, singleVariableDeclaration.getName());
          initializerBlock.statements().add(ast.newExpressionStatement(newAddRef(name, type)));
        }
      } else {
        warn(declaration, "Cannot handle " + closureNode.getClass().getSimpleName());
      }
    });
    initializer.setBody(initializerBlock);
    declaration.bodyDeclarations().add(initializer);
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
      } else if (isRefCounted(typeBinding)) {
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
    methodInvocation.setExpression(AutoCoder.newQualifiedName(ast, RefUtil.class));
    methodInvocation.setName(ast.newSimpleName("wrapInterface"));
    methodInvocation.arguments().add(ASTNode.copySubtree(ast, node));
    closures.keySet().stream().map(index.definitionNodes::get).filter(x -> x != null).forEach(closureNode -> {
      if (closureNode instanceof SingleVariableDeclaration) {
        final MethodInvocation addRefInvoke = ast.newMethodInvocation();
        final SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) closureNode;
        final ITypeBinding type = singleVariableDeclaration.resolveBinding().getType();
        if (isRefCounted(type)) {
          addRefInvoke.setExpression((Name) ASTNode.copySubtree(ast, singleVariableDeclaration.getName()));
          addRefInvoke.setName(ast.newSimpleName("addRef"));
          methodInvocation.arguments().add(addRefInvoke);
        }
      } else {
        warn(node, "Cannot handle " + closureNode.getClass().getSimpleName());
      }
    });
    if (methodInvocation.arguments().size() > 1) {
      replace(node, methodInvocation);
    }
  }

}
