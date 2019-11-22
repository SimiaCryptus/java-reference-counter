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

package com.simiacryptus.devutil;

import com.simiacryptus.devutil.ops.FileAstVisitor;
import com.simiacryptus.devutil.ops.IndexSymbols;
import com.simiacryptus.devutil.ops.LogNodes;
import com.simiacryptus.lang.ref.ReferenceCounting;
import com.simiacryptus.lang.ref.ReferenceCountingBase;
import com.simiacryptus.lang.ref.wrappers.*;
import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import scala.Tuple2;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RefAutoCoder extends AutoCoder {

  private boolean verbose = true;
  private boolean addRefcounting = true;

  public RefAutoCoder(@NotNull String pathname) {
    super(pathname);
  }

  @Override
  @Nonnull
  public void rewrite() {
    if (isVerbose()) rewrite((cu, file) -> new LogNodes(cu, file));
    rewrite((cu, file) -> new RemoveRefs(cu, file));
    rewrite((cu, file) -> new UnReplaceTypes(cu, file));
    while (rewrite((cu, file) -> new InlineRefs(cu, file)) > 0) {
      logger.info("Re-running InlineRefs");
    }
    if (isAddRefcounting()) {
      rewrite((cu, file) -> new ReplaceTypes(cu, file));
      rewrite((cu, file) -> new InsertMethods(cu, file));
      rewrite((cu, file) -> new InsertAddRefs(cu, file));
      rewrite((cu, file) -> new ModifyFieldSets(cu, file));
      rewrite((cu, file) -> new InsertFreeRefs(cu, file));
      final IndexSymbols.SymbolIndex index = getSymbolIndex();
      rewrite((cu, file) -> new InstrumentClosures(cu, file, index));
      // Paranoid GC instrumentation
      // Wrap critical APIs in Ref-Aware support classes
      //    collections
      //    streams
      // Optimize adjacent addRef / freeRef
    }
  }

  public boolean isVerbose() {
    return verbose;
  }

  @NotNull
  public RefAutoCoder setVerbose(boolean verbose) {
    this.verbose = verbose;
    return this;
  }

  public boolean isAddRefcounting() {
    return addRefcounting;
  }

  @NotNull
  public RefAutoCoder setAddRefcounting(boolean addRefcounting) {
    this.addRefcounting = addRefcounting;
    return this;
  }

  public static boolean isRefCounted(@NotNull ITypeBinding resolveTypeBinding) {
    final ITypeBinding type;
    if (resolveTypeBinding.isArray()) {
      type = resolveTypeBinding.getElementType();
    } else {
      type = resolveTypeBinding;
    }
    return derives(type, ReferenceCountingBase.class);
  }

  @NotNull
  public static MethodInvocation newFreeRef(@NotNull ASTNode name, @NotNull ITypeBinding typeBinding) {
    AST ast = name.getAST();
    if (typeBinding.isArray()) {
      final String qualifiedName = typeBinding.getElementType().getQualifiedName();
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("freeRefs"));
      methodInvocation.setExpression(newQualifiedName(ast, qualifiedName.split("\\.")));
      methodInvocation.arguments().add(ASTNode.copySubtree(ast, name));
      return methodInvocation;
    } else {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("freeRef"));
      methodInvocation.setExpression((Expression) ASTNode.copySubtree(ast, name));
      return methodInvocation;
    }
  }
  @NotNull
  public static MethodInvocation newAddRef(@NotNull ASTNode name, @NotNull ITypeBinding typeBinding) {
    AST ast = name.getAST();
    if (typeBinding.isArray()) {
      final String qualifiedName = typeBinding.getElementType().getQualifiedName();
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRefs"));
      methodInvocation.setExpression(newQualifiedName(ast, qualifiedName.split("\\.")));
      methodInvocation.arguments().add(ASTNode.copySubtree(ast, name));
      return methodInvocation;
    } else {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRef"));
      methodInvocation.setExpression((Expression) ASTNode.copySubtree(ast, name));
      return methodInvocation;
    }
  }

  @NotNull
  public static Expression wrapAddRef(@NotNull Expression expression, @Nullable ITypeBinding typeBinding) {
    AST ast = expression.getAST();
    if (null == typeBinding) {
      return expression;
    } else if (typeBinding.isArray()) {
      final String qualifiedName = typeBinding.getElementType().getQualifiedName();
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRefs"));
      methodInvocation.setExpression(newQualifiedName(ast, qualifiedName.split("\\.")));
      methodInvocation.arguments().add(ASTNode.copySubtree(ast, expression));
      return methodInvocation;
    } else {
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setName(ast.newSimpleName("addRef"));
      methodInvocation.setExpression((Expression) ASTNode.copySubtree(ast, expression));
      return methodInvocation;
    }
  }

  protected static class ModifyFieldSets extends FileAstVisitor {

    private ModifyFieldSets(CompilationUnit compilationUnit, File file) {
      super(compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull Assignment node) {
      if (node.getLeftHandSide() instanceof FieldAccess) {
        final FieldAccess fieldAccess = (FieldAccess) node.getLeftHandSide();
        final ASTNode parent = node.getParent();
        if (parent instanceof ExpressionStatement) {
          final ASTNode parent2 = parent.getParent();
          final ITypeBinding typeBinding = fieldAccess.resolveTypeBinding();
          if (parent2 instanceof Block) {
            final Block block = (Block) parent2;
            final int lineNumber = block.statements().indexOf(parent);
            final Expression rightHandSide = node.getRightHandSide();
            final AST ast = node.getAST();
            if (rightHandSide instanceof Name) {
              block.statements().add(lineNumber, freeRefStatement(fieldAccess));
              node.setRightHandSide(wrapAddRef(rightHandSide, typeBinding));
              info(node, "Simple field-set statement at line " + lineNumber);
            } else {
              final Block exchangeBlock = ast.newBlock();

              final String identifier = randomIdentifier(node);
              exchangeBlock.statements().add(newLocalVariable(identifier, rightHandSide, getType(ast, typeBinding.getName())));
              exchangeBlock.statements().add(freeRefStatement(fieldAccess));

              final Assignment assignment = ast.newAssignment();
              assignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, fieldAccess));
              assignment.setOperator(Assignment.Operator.ASSIGN);
              assignment.setRightHandSide(wrapAddRef(ast.newSimpleName(identifier), typeBinding));
              exchangeBlock.statements().add(ast.newExpressionStatement(assignment));

              block.statements().set(lineNumber, exchangeBlock);
              info(node, "Complex field-set statement at line " + lineNumber);
            }
          } else {
            info(node, "Non-block field-set statement: %s (%s)", parent.getClass(), parent);
          }
        } else {
          info(node, "Non-ExpressionStatement field-set statement: %s (%s)", parent.getClass(), parent);
        }
      }
      super.endVisit(node);
    }

    @NotNull
    public IfStatement freeRefStatement(@NotNull FieldAccess fieldAccess) {
      AST ast = fieldAccess.getAST();
      final IfStatement ifStatement = ast.newIfStatement();
      final InfixExpression infixExpression = ast.newInfixExpression();
      infixExpression.setLeftOperand(ast.newNullLiteral());
      infixExpression.setRightOperand((Expression) ASTNode.copySubtree(ast, fieldAccess));
      infixExpression.setOperator(InfixExpression.Operator.NOT_EQUALS);
      ifStatement.setExpression(infixExpression);
      final ITypeBinding typeBinding = fieldAccess.resolveTypeBinding();
      if (null == typeBinding) {
        info(fieldAccess, "Cannot add freeRef for field access %s (binding not resolved)", fieldAccess.getName());
      } else {
        ifStatement.setThenStatement(ast.newExpressionStatement(newFreeRef(fieldAccess, typeBinding)));
        info(fieldAccess, "Added freeRef for field access %s", fieldAccess.getName());
      }
      return ifStatement;
    }
  }

  protected static class RemoveRefs extends FileAstVisitor {

    private RemoveRefs(CompilationUnit compilationUnit, File file) {
      super(compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull TypeDeclaration node) {
      final ITypeBinding typeBinding = node.resolveBinding();
      if (derives(typeBinding, ReferenceCountingBase.class)) {
        removeMethods(node, "addRef");
        removeMethods(node, "freeRef");
        removeMethods(node, "_free");
        removeMethods(node, "addRefs");
        removeMethods(node, "freeRefs");
      }
    }

    @Override
    public void endVisit(@NotNull MethodInvocation node) {
      final String methodName = node.getName().toString();

      if (Arrays.asList("addRef", "freeRef", "addRefs", "freeRefs", "wrapInterface").contains(methodName)) {
        final AST ast = node.getAST();
        final Expression subject;
        if (Arrays.asList("addRefs", "freeRefs", "wrapInterface").contains(methodName)) {
          subject = (Expression) ASTNode.copySubtree(ast, (ASTNode) node.arguments().get(0));
        } else {
          subject = (Expression) ASTNode.copySubtree(ast, node.getExpression());
        }
//        replace(node, subject);
        final ASTNode parent = node.getParent();
        if (parent instanceof MethodInvocation) {
          final List arguments = ((MethodInvocation) parent).arguments();
          final int index = arguments.indexOf(node);
          if(index < 0) {
            warn(node, "%s not found as argument to %s", node, ((MethodInvocation) parent).getName());
          } else {
            arguments.set(index, subject);
            info(node, "%s removed as argument %s of %s", methodName, index, parent);
          }
        } else if (parent instanceof ExpressionStatement) {
          delete((ExpressionStatement) parent);
          info(node, "%s removed from %s", methodName, parent.toString().trim());
        } else if (parent instanceof ClassInstanceCreation) {
          final List arguments = ((ClassInstanceCreation) parent).arguments();
          final int index = arguments.indexOf(node);
          arguments.set(index, subject);
          info(node, "%s removed as argument %s of %s", methodName, index, parent);
        } else if (parent instanceof VariableDeclarationFragment) {
          ((VariableDeclarationFragment) parent).setInitializer(subject);
          info(node, "%s removed", methodName);
        } else if (parent instanceof Assignment) {
          ((Assignment) parent).setRightHandSide(subject);
          info(node, "%s removed", methodName);
        } else if (parent instanceof ArrayInitializer) {
          final List arguments = ((ArrayInitializer) parent).expressions();
          final int index = arguments.indexOf(node);
          arguments.set(index, subject);
          info(node, "%s removed as argument %s of %s", methodName, index, parent);
        } else {
          warn(node, "Cannot remove %s called in %s: %s", methodName, parent.getClass(), parent);
        }
      }
    }

    @Override
    public void endVisit(AnonymousClassDeclaration node) {
      final ITypeBinding typeBinding = node.resolveBinding();
      if (derives(typeBinding, ReferenceCountingBase.class)) {
        removeMethods(node, "_free");
      }
    }

    @Override
    public void endVisit(Block node) {
      if(node.statements().isEmpty() && node.getParent() instanceof Initializer) node.getParent().delete();
    }
  }

  protected static class InlineRefs extends FileAstVisitor {

    private InlineRefs(CompilationUnit compilationUnit, File file) {
      super(compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull Block node) {
      if (node.statements().size() == 1 && node.getParent() instanceof Block) {
        final Block parent = (Block) node.getParent();
        parent.statements().set(parent.statements().indexOf(node),
            ASTNode.copySubtree(node.getAST(), (ASTNode) node.statements().get(0)));
      }
    }

    @Override
    public void endVisit(@NotNull Assignment node) {
      Statement previousStatement = previousStatement(node);
      if (previousStatement != null) {
        if (previousStatement instanceof VariableDeclarationStatement) {
          final List fragments = ((VariableDeclarationStatement) previousStatement).fragments();
          if (1 == fragments.size()) {
            final VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(0);
            if (fragment.getName().toString().equals(node.getRightHandSide().toString())) {
              info(node, "Inlining %s", fragment.getName());
              node.setRightHandSide((Expression) ASTNode.copySubtree(node.getAST(), fragment.getInitializer()));
              previousStatement.delete();
            } else {
              warn(node, "previous variable %s is not used in %s", fragment.getName(), node.getRightHandSide());
            }
          } else {
            warn(node, "previous variable has multiple fragments");
          }
        } else {
          warn(node, "previous statement is %s", previousStatement.getClass().getSimpleName());
        }
      }
    }

    @Nullable
    public Statement previousStatement(@Nonnull ASTNode node) {
      if (node instanceof Statement) {
        final ASTNode statementParent = node.getParent();
        if (statementParent instanceof Block) {
          final List statements = ((Block) statementParent).statements();
          final int statementNumber = statements.indexOf(node);
          if (statementNumber > 0) {
            return (Statement) statements.get(statementNumber - 1);
          } else {
            //info(String.format("No previous statement for %s at %s", node.getClass().getSimpleName(), location(node)));
            return null;
          }
        } else {
          info(node, "No previous statement for %s", node.getClass().getSimpleName());
          return null;
        }
      } else {
        final ASTNode parent = node.getParent();
        if (null == parent) {
          info(node, "No previous statement for %s", node.getClass().getSimpleName());
          return null;
        } else {
          return previousStatement(parent);
        }
      }
    }

    @Override
    public void endVisit(@NotNull ReturnStatement node) {
      if (node.getExpression() instanceof Name) {
        Statement previousStatement = previousStatement(node);
        if (previousStatement != null) {
          if (previousStatement instanceof VariableDeclarationStatement) {
            final List fragments = ((VariableDeclarationStatement) previousStatement).fragments();
            if (1 == fragments.size()) {
              final VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragments.get(0);
              if (fragment.getName().toString().equals(node.getExpression().toString())) {
                info(node, "Inlining %s", fragment.getName());
                node.setExpression((Expression) ASTNode.copySubtree(node.getAST(), fragment.getInitializer()));
                previousStatement.delete();
              }
            }
          } else {
            info(node, "Cannot inline - Previous statement is %s", previousStatement.getClass().getSimpleName());
          }
        } else {
          info(node, "Cannot inline - No previous statement");
        }
      }
    }
  }

  protected static class InstrumentClosures extends FileAstVisitor {

    private final IndexSymbols.SymbolIndex index;

    private InstrumentClosures(CompilationUnit compilationUnit, File file, IndexSymbols.SymbolIndex index) {
      super(compilationUnit, file);
      this.index = index;
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

    public void addRefcounting(@Nonnull AnonymousClassDeclaration declaration, Map<IndexSymbols.BindingId, List<IndexSymbols.Span>> closures) {
      final AST ast = declaration.getAST();
      final Optional<MethodDeclaration> freeMethodOpt = findMethod(declaration, "_free");
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
      closures.keySet().stream().map(index.definitionNodes::get).filter(x -> x != null).forEach(closureNode->{
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

    public void wrapInterface(Expression node, Map<IndexSymbols.BindingId, List<IndexSymbols.Span>> closures) {
      AST ast = node.getAST();
      final MethodInvocation methodInvocation = ast.newMethodInvocation();
      methodInvocation.setExpression(newQualifiedName(ast, ReferenceCounting.class));
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

  }

  protected static class UnReplaceTypes extends FileAstVisitor {

    private UnReplaceTypes(CompilationUnit compilationUnit, File file) {
      super(compilationUnit, file);
    }

    @Override
    public void endVisit(QualifiedName node) {
      apply(node);
    }

    @Override
    public void endVisit(SimpleName node) {
      if(node.getParent() instanceof QualifiedName) return;
      apply(node);
    }

    public void apply(Name node) {
      if(skip(node)) return;
      final IBinding binding = node.resolveBinding();
      if(binding instanceof ITypeBinding) {
        final String className = ((ITypeBinding) binding).getBinaryName();
        if(className.equals(RefStream.class.getCanonicalName())) {
          replace(node, newQualifiedName(node.getAST(), Stream.class));
        }
        if(className.equals(RefArrays.class.getCanonicalName())) {
          replace(node, newQualifiedName(node.getAST(), Arrays.class));
        }
        if(className.equals(RefArrayList.class.getCanonicalName())) {
          replace(node, newQualifiedName(node.getAST(), ArrayList.class));
        }
        if(className.equals(RefHashMapV.class.getCanonicalName())) {
          replace(node, newQualifiedName(node.getAST(), HashMap.class));
        }
        if(className.equals(RefSet.class.getCanonicalName())) {
          replace(node, newQualifiedName(node.getAST(), HashSet.class));
        }
      }
    }

    public boolean skip(@NotNull ASTNode node) {
      return enclosingMethods(node).stream().filter(enclosingMethod -> {
        final String methodName = enclosingMethod.getName();
        return methodName.equals("addRefs") || methodName.equals("freeRefs");
      }).findFirst().isPresent();
    }

  }

  protected static class ReplaceTypes extends FileAstVisitor {

    private ReplaceTypes(CompilationUnit compilationUnit, File file) {
      super(compilationUnit, file);
    }

    @Override
    public void endVisit(QualifiedName node) {
      apply(node);
    }

    @Override
    public void endVisit(SimpleName node) {
      if(node.getParent() instanceof QualifiedName) return;
      apply(node);
    }

    public void apply(Name node) {
      if(skip(node)) return;
      final IBinding binding = node.resolveBinding();
      if(binding instanceof ITypeBinding) {
        final String className = ((ITypeBinding) binding).getBinaryName();
        if(className.equals(Stream.class.getCanonicalName())) {
          replace(node, newQualifiedName(node.getAST(), RefStream.class));
        }
        if(className.equals(Arrays.class.getCanonicalName())) {
          replace(node, newQualifiedName(node.getAST(), RefArrays.class));
        }
        if(className.equals(ArrayList.class.getCanonicalName())) {
          replace(node, newQualifiedName(node.getAST(), RefArrayList.class));
        }
        if(className.equals(HashMap.class.getCanonicalName())) {
          replace(node, newQualifiedName(node.getAST(), RefHashMapV.class));
        }
        if(className.equals(HashSet.class.getCanonicalName())) {
          replace(node, newQualifiedName(node.getAST(), RefSet.class));
        }
      }
    }

    public boolean skip(@NotNull ASTNode node) {
      return enclosingMethods(node).stream().filter(enclosingMethod -> {
        final String methodName = enclosingMethod.getName();
        return methodName.equals("addRefs") || methodName.equals("freeRefs");
      }).findFirst().isPresent();
    }

  }

  protected static class InsertFreeRefs extends FileAstVisitor {

    private InsertFreeRefs(CompilationUnit compilationUnit, File file) {
      super(compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull VariableDeclarationFragment declaration) {
      if(skip(declaration)) return;
      final ASTNode parent = declaration.getParent();
      if (parent instanceof VariableDeclarationStatement) {
        final ITypeBinding typeBinding = ((VariableDeclarationStatement) parent).getType().resolveBinding();
        if (null != typeBinding) {
          addFreeRef(declaration, typeBinding);
        } else {
          warn(declaration, "Cannot resolve type of %s", parent);
        }
      } else if (parent instanceof VariableDeclarationExpression) {
        final Type type = ((VariableDeclarationExpression) parent).getType();
        final ITypeBinding typeBinding = type.resolveBinding();
        if (null != typeBinding) {
          addFreeRef(declaration, typeBinding);
        } else {
          warn(declaration, "Cannot resolve type of %s", parent);
        }
      } else if (parent instanceof FieldDeclaration) {
        final ITypeBinding typeBinding = ((FieldDeclaration) parent).getType().resolveBinding();
        if (null != typeBinding) {
          addFreeRef(declaration, typeBinding);
        } else {
          warn(declaration, "Cannot resolve type of %s", parent);
        }
      } else if (parent instanceof LambdaExpression) {
        final LambdaExpression lambdaExpression = (LambdaExpression) parent;
        final IMethodBinding methodBinding = lambdaExpression.resolveMethodBinding();
        final int paramNumber = lambdaExpression.parameters().indexOf(declaration);
        if (methodBinding != null && paramNumber>=0) {
          addFreeRef(declaration, methodBinding.getParameterTypes()[paramNumber]);
        } else if (paramNumber < 0) {
          warn(declaration, "Cannot argument index of %s", parent);
        } else {
          warn(declaration, "Cannot method of %s", parent);
        }
      } else {
        warn(declaration, "Cannot handle %s", parent);
      }
    }

    @Override
    public void endVisit(@NotNull SingleVariableDeclaration declaration) {
      if(skip(declaration)) return;
      ITypeBinding typeBinding = declaration.resolveBinding().getType();
      if (null == typeBinding) {
        info(declaration, "Cannot add freeRef for field access %s (binding not resolved)", declaration.getName());
      } else {
        addFreeRef(declaration, typeBinding);
      }
    }

    @Override
    public void endVisit(@NotNull MethodInvocation node) {
      if(skip(node)) return;
      final IMethodBinding methodBinding = node.resolveMethodBinding();
      if (null != methodBinding) {
        if (modifyArg(methodBinding.getDeclaringClass()) && !node.getName().toString().equals("addRefs")) {
          final List arguments = node.arguments();
          for (int i = 0; i < arguments.size(); i++) {
            Object next = arguments.get(i);
            MethodInvocation methodInvocation = wrapAddRef((ASTNode) next);
            if (null != methodInvocation) {
              final SimpleName nodeName = node.getName();
              info(node, "Argument addRef for %s: %s", nodeName, nodeName.resolveTypeBinding().getQualifiedName());
              arguments.set(i, methodInvocation);
            }
          }
        }
      }
    }

    @Override
    public void endVisit(@NotNull ArrayInitializer node) {
      if(skip(node)) return;
      final ITypeBinding typeBinding = node.resolveTypeBinding();
      if (null != typeBinding) {
        if (modifyArg(typeBinding.getElementType())) {
          final List expressions = node.expressions();
          for (int i = 0; i < expressions.size(); i++) {
            Object next = expressions.get(i);
            MethodInvocation methodInvocation = wrapAddRef((ASTNode) next);
            if (null != methodInvocation) {
              info(node, "Argument addRef for %s", next);
              expressions.set(i, methodInvocation);
            }
          }
        }
      }
    }

    public boolean skip(@NotNull ASTNode node) {
      return enclosingMethods(node).stream().filter(enclosingMethod -> {
        final String methodName = enclosingMethod.getName();
        return methodName.equals("addRefs") || methodName.equals("freeRefs");
      }).findFirst().isPresent();
    }

    public void addFreeRef(@Nonnull VariableDeclaration declaration, @Nonnull ITypeBinding typeBinding) {
      if(skip(declaration)) return;
      if (derives(typeBinding, ReferenceCountingBase.class)) {
        final SimpleName name = declaration.getName();
        ASTNode parent = declaration.getParent();
        if (parent instanceof MethodDeclaration) {
          final MethodDeclaration node = (MethodDeclaration) parent;
          addFreeRef(typeBinding, name, node.getBody());
        } else if (parent instanceof LambdaExpression) {
          final LambdaExpression node = (LambdaExpression) parent;
          final ASTNode lambdaParent = node.getParent();
          final ASTNode body = node.getBody();
          if (body instanceof Block) {
            addFreeRef(typeBinding, name, (Block) body);
          } else {
            final AST ast = node.getAST();
            final Block block = ast.newBlock();
            if (hasReturnValue(lambdaParent, node)) {
              final ReturnStatement returnStatement = ast.newReturnStatement();
              returnStatement.setExpression((Expression) ASTNode.copySubtree(ast, body));
              block.statements().add(returnStatement);
            } else {
              block.statements().add(ast.newExpressionStatement((Expression) ASTNode.copySubtree(ast, body)));
            }
            node.setBody(block);
            addFreeRef(typeBinding, name, block);
          }
        } else if (parent instanceof VariableDeclarationStatement) {
          parent = parent.getParent();
          if (parent instanceof Block) {
            addFreeRef(typeBinding, name, (Block) parent);
          } else {
            warn(declaration, "Cannot add freeRef for %s (VariableDeclarationStatement) in %s : %s", name, parent.getClass(), parent.toString().trim());
          }
        } else if (parent instanceof FieldDeclaration) {
          add_freeRef_entry(declaration, typeBinding, name, parent);
        } else {
          warn(declaration, "Cannot add freeRef for %s in %s : %s",
              name,
              parent.getClass(), parent.toString().trim());
        }
      }
    }

    public void add_freeRef_entry(@Nonnull VariableDeclaration declaration, @Nonnull ITypeBinding typeBinding, SimpleName name, ASTNode parent) {
      final ASTNode fieldParent = parent.getParent();
      if (fieldParent instanceof TypeDeclaration) {
        final TypeDeclaration typeDeclaration = (TypeDeclaration) fieldParent;
        final Optional<MethodDeclaration> freeMethodOpt = findMethod(typeDeclaration, "_free");
        if (freeMethodOpt.isPresent()) {
          final AST ast = name.getAST();
          final MethodInvocation expression = newFreeRef(name, typeBinding);
          final ExpressionStatement expressionStatement = ast.newExpressionStatement(expression);
          final IfStatement ifStatement = ast.newIfStatement();
          final InfixExpression nullTest = ast.newInfixExpression();
          nullTest.setLeftOperand(ast.newNullLiteral());
          nullTest.setOperator(InfixExpression.Operator.NOT_EQUALS);
          nullTest.setRightOperand((Expression) ASTNode.copySubtree(ast,name));
          ifStatement.setExpression(nullTest);
          ifStatement.setThenStatement(expressionStatement);
          info(declaration, "Adding freeRef for %s::%s to %s", typeDeclaration.getName(), declaration.getName(), "(" + getLocation(name) + ")");
          freeMethodOpt.get().getBody().statements().add(0, ifStatement);
        } else {
          warn(declaration, "Cannot add freeRef for %s::%s - no _free method", typeDeclaration.getName(), declaration.getName());
        }
      } else {
        warn(declaration, "Cannot add freeRef for %s (FieldDeclaration) in %s : %s", name, fieldParent.getClass(), fieldParent.toString().trim());
      }
    }

    public boolean hasReturnValue(ASTNode lambdaParent, LambdaExpression node) {
      if (lambdaParent instanceof MethodInvocation) {
        final MethodInvocation methodInvocation = (MethodInvocation) lambdaParent;
        final int argIndex = methodInvocation.arguments().indexOf(node);
        final ITypeBinding targetClass = methodInvocation.resolveMethodBinding().getParameterTypes()[argIndex];
        if (derives(targetClass, Consumer.class)) {
          return false;
        } else if (derives(targetClass, Function.class)) {
          return true;
        } else if (derives(targetClass, Predicate.class)) {
          return true;
        } else {
          final List<IMethodBinding> methods = Arrays.stream(targetClass.getDeclaredMethods()).filter(x -> x.getDefaultValue() != null).collect(Collectors.toList());
          if (methods.size() == 1 && (methods.get(0).getReturnType()).equals(PrimitiveType.VOID)) {
            return false;
          } else {
            warn(node, "Cannot determine if %s returns a value", targetClass.getQualifiedName());
          }
        }
      }
      return false;
    }

    public void addFreeRef(@NotNull ITypeBinding typeBinding, @NotNull SimpleName name, @NotNull Block body) {
      final List<IndexSymbols.Mention> lastMentions = lastMentions(body, name.resolveBinding());
      lastMentions.stream().filter(x -> !x.isReturn())
          .forEach(insertFreeRef(typeBinding, name, body));
      lastMentions.stream().filter(x -> x.isComplexReturn())
          .forEach(insertAddRef_ComplexReturn(name, typeBinding));
    }

    @NotNull
    public Consumer<IndexSymbols.Mention> insertAddRef_ComplexReturn(@NotNull SimpleName node, @NotNull ITypeBinding typeBinding) {
      AST ast = node.getAST();
      return mention -> {
        final ReturnStatement returnStatement = (ReturnStatement) mention.statement;
        final String identifier = randomIdentifier(node);
        final List statements = mention.block.statements();
        statements.add(mention.line, newLocalVariable(identifier, returnStatement.getExpression()));
        ASTNode name1 = ast.newSimpleName(node.getIdentifier());
        statements.add(mention.line + 1, name1.getAST().newExpressionStatement(newFreeRef(name1, typeBinding)));
        final ReturnStatement newReturnStatement = ast.newReturnStatement();
        newReturnStatement.setExpression(ast.newSimpleName(identifier));
        statements.set(mention.line + 2, newReturnStatement);
        info(node, "Added freeRef for return value %s", node);
      };
    }

    @NotNull
    public Consumer<IndexSymbols.Mention> insertFreeRef(@NotNull ITypeBinding typeBinding, @NotNull SimpleName name, @NotNull Block body) {
      return lastMention -> {
        body.statements().add(lastMention.line + 1, name.getAST().newExpressionStatement(newFreeRef(name, typeBinding)));
        info(name, "Added freeRef for value %s (%s)", name, typeBinding.getQualifiedName());
      };
    }

    @Nullable
    public static MethodInvocation wrapAddRef(ASTNode node) {
      if (node instanceof SimpleName) {
        final SimpleName name = (SimpleName) node;
        if (derives(name.resolveTypeBinding(), ReferenceCountingBase.class)) {
          return (MethodInvocation) RefAutoCoder.wrapAddRef(name, name.resolveTypeBinding());
        }
      }
      return null;
    }

    public boolean modifyArg(@NotNull ITypeBinding declaringClass) {
      return AutoCoder.toString(declaringClass.getPackage()).startsWith("com.simiacryptus");
    }

  }

  protected static class InsertAddRefs extends FileAstVisitor {

    private InsertAddRefs(CompilationUnit compilationUnit, File file) {
      super(compilationUnit, file);
    }

    @Override
    public void endVisit(@NotNull ConstructorInvocation node) {
      if(skip(node)) return;
      final IMethodBinding methodBinding = node.resolveConstructorBinding();
      if (null != methodBinding) {
        if (methodsConsumeRefs(methodBinding, node) && node.arguments().size() > 0) {
          apply(node, node, node.arguments());
        }
      } else {
        warn(node, "Cannot resolve " + node);
      }
    }

    @Override
    public void endVisit(@NotNull ClassInstanceCreation node) {
      if(skip(node)) return;
      final IMethodBinding methodBinding = node.resolveConstructorBinding();
      if (null != methodBinding) {
        if (methodsConsumeRefs(methodBinding, node)) {
          if (node.arguments().size() > 0) {
            apply(node, node.getType(), node.arguments());
          } else {
            debug(node, "No args %s", node);
          }
        } else {
          info(node, "Non-refcounted arg %s", node);
        }
      } else {
        warn(node, "Cannot resolve %s", node);
      }
    }

    @Override
    public void endVisit(@NotNull MethodInvocation node) {
      if(skip(node)) return;
      final IMethodBinding methodBinding = node.resolveMethodBinding();
      if (null != methodBinding) {
        if (methodsConsumeRefs(methodBinding, node)) {
          apply(node, node.getName(), node.arguments());
        } else {
          debug(node, "Ignored method on %s", node);
        }
      } else {
        warn(node, "Unresolved binding on %s", node);
      }
    }

    public boolean skip(@NotNull ASTNode declaration) {
      return enclosingMethods(declaration).stream().filter(enclosingMethod -> {
        final String methodName = enclosingMethod.getName();
        return methodName.equals("addRefs") || methodName.equals("freeRefs");
      }).findFirst().isPresent();
    }

    public void apply(ASTNode methodNode, @NotNull ASTNode node, @NotNull List<ASTNode> arguments) {
      for (int i = 0; i < arguments.size(); i++) {
        ASTNode arg = arguments.get(i);
        if (arg instanceof ClassInstanceCreation) {
          debug(node, "Ignored argument type %s on %s", arg.getClass().getSimpleName(), methodNode);
        } else if (arg instanceof AnonymousClassDeclaration) {
          debug(node, "Ignored argument type %s on %s", arg.getClass().getSimpleName(), methodNode);
        } else if (arg instanceof MethodInvocation) {
          debug(node, "Ignored argument type %s on %s", arg.getClass().getSimpleName(), methodNode);
        } else if (arg instanceof ArrayCreation) {
          debug(node, "Ignored argument type %s on %s", arg.getClass().getSimpleName(), methodNode);
        } else if (arg instanceof Expression) {
          final Expression expression = (Expression) arg;
          final ITypeBinding resolveTypeBinding = expression.resolveTypeBinding();
          if (isRefCounted(resolveTypeBinding)) {
            arguments.set(i, addAddRef(expression, resolveTypeBinding));
            info(node, "Argument addRef for %s: %s (%s) defined", node, resolveTypeBinding.getQualifiedName(), expression);
          } else {
            info(node, "Non-refcounted arg %s", expression);
          }
        } else {
          warn(node, "Unexpected type %s", arg.getClass().getSimpleName());
        }
      }
    }

    @NotNull
    public MethodInvocation addAddRef(@NotNull Expression expression, @NotNull ITypeBinding type) {
      AST ast = expression.getAST();
      if (type.isArray()) {
        final String qualifiedName = type.getElementType().getQualifiedName();
        final MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setName(ast.newSimpleName("addRefs"));
        methodInvocation.setExpression(newQualifiedName(ast, qualifiedName.split("\\.")));
        methodInvocation.arguments().add(ASTNode.copySubtree(ast, expression));
        return methodInvocation;
      } else {
        final MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setName(ast.newSimpleName("addRef"));
        methodInvocation.setExpression((Expression) ASTNode.copySubtree(ast, expression));
        return methodInvocation;
      }
    }

    public boolean methodsConsumeRefs(IMethodBinding methodBinding, ASTNode node) {
      final String qualifiedName = methodBinding.getDeclaringClass().getQualifiedName();
      final String methodName = methodBinding.getName();
      if(qualifiedName.equals(String.class.getCanonicalName())) {
        if(methodName.equals("format")) {
          return false;
        }
      }
      //Arrays.toString
      if(methodName.equals("addRefs")) {
        return false;
      }
      if(methodName.equals("freeRefs")) {
        return false;
      }
      if(AutoCoder.toString(methodBinding.getDeclaringClass().getPackage()).startsWith("com.simiacryptus")) return true;
      warn(node, "Not sure if %s consumes refs", methodBinding);
      return true;
    }

  }

  protected static class InsertMethods extends FileAstVisitor {

    public InsertMethods(CompilationUnit cu, File file) {
      super(cu, file);
    }

    @Override
    public void endVisit(@NotNull TypeDeclaration node) {
      if (derives(node.resolveBinding(), ReferenceCountingBase.class)) {
        final AST ast = node.getAST();
        final List declarations = node.bodyDeclarations();
        declarations.add(method_free(ast));
        declarations.add(method_addRef(ast, node.getName()));
        declarations.add(method_addRefs(ast, node.getName()));
        declarations.add(method_freeRefs(ast, node.getName()));
      }
    }

    @Override
    public void endVisit(AnonymousClassDeclaration node) {
      if (derives(node.resolveBinding(), ReferenceCountingBase.class)) {
        final AST ast = node.getAST();
        final List declarations = node.bodyDeclarations();
        declarations.add(method_free(ast));
      }
    }

    @NotNull
    public MethodDeclaration method_free(@NotNull AST ast) {
      final MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
      methodDeclaration.setName(ast.newSimpleName("_free"));
      methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
      methodDeclaration.modifiers().add(annotation_override(ast));
      final Block body = ast.newBlock();
      final SuperMethodInvocation superCall = ast.newSuperMethodInvocation();
      superCall.setName(ast.newSimpleName("_free"));
      body.statements().add(ast.newExpressionStatement(superCall));
      methodDeclaration.setBody(body);
      return methodDeclaration;
    }

    @NotNull
    public MethodDeclaration method_addRef(@NotNull AST ast, @NotNull SimpleName name) {
      final String fqTypeName = name.getFullyQualifiedName();
      final MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
      methodDeclaration.setName(ast.newSimpleName("addRef"));
      methodDeclaration.setReturnType2(ast.newSimpleType(ast.newSimpleName(fqTypeName)));
      methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
      methodDeclaration.modifiers().add(annotation_override(ast));
      final Block block = ast.newBlock();
      final CastExpression castExpression = ast.newCastExpression();
      castExpression.setType(ast.newSimpleType(ast.newSimpleName(fqTypeName)));
      final SuperMethodInvocation superMethodInvocation = ast.newSuperMethodInvocation();
      superMethodInvocation.setName(ast.newSimpleName("addRef"));
      castExpression.setExpression(superMethodInvocation);
      final ReturnStatement returnStatement = ast.newReturnStatement();
      returnStatement.setExpression(castExpression);
      block.statements().add(returnStatement);
      methodDeclaration.setBody(block);
      return methodDeclaration;
    }

    @NotNull
    public MethodDeclaration method_freeRefs(@NotNull AST ast, @NotNull SimpleName name) {
      final String fqTypeName = name.getFullyQualifiedName();
      final MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
      methodDeclaration.setName(ast.newSimpleName("freeRefs"));

      methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
      methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));

      final SingleVariableDeclaration arg = ast.newSingleVariableDeclaration();
      arg.setType(arrayType(ast, fqTypeName));
      arg.setName(ast.newSimpleName("array"));
      methodDeclaration.parameters().add(arg);

      final MethodInvocation stream_invoke = ast.newMethodInvocation();
      stream_invoke.setExpression(newQualifiedName(ast, "java.util.Arrays".split("\\.")));
      stream_invoke.setName(ast.newSimpleName("stream"));
      stream_invoke.arguments().add(ast.newSimpleName("array"));

      final MethodInvocation filter_invoke = ast.newMethodInvocation();
      {
        filter_invoke.setExpression(stream_invoke);
        filter_invoke.setName(ast.newSimpleName("filter"));
        final LambdaExpression filter_lambda = ast.newLambdaExpression();
        final VariableDeclarationFragment variableDeclarationFragment = ast.newVariableDeclarationFragment();
        variableDeclarationFragment.setName(ast.newSimpleName("x"));
        filter_lambda.parameters().add(variableDeclarationFragment);
        final InfixExpression infixExpression = ast.newInfixExpression();
        infixExpression.setLeftOperand(ast.newSimpleName("x"));
        infixExpression.setOperator(InfixExpression.Operator.NOT_EQUALS);
        infixExpression.setRightOperand(ast.newNullLiteral());
        filter_lambda.setBody(infixExpression);
        filter_invoke.arguments().add(filter_lambda);
      }

      final MethodInvocation addref_invoke = ast.newMethodInvocation();
      {
        addref_invoke.setExpression(filter_invoke);
        addref_invoke.setName(ast.newSimpleName("forEach"));
        final ExpressionMethodReference body = ast.newExpressionMethodReference();
        body.setExpression(ast.newSimpleName(fqTypeName));
        body.setName(ast.newSimpleName("freeRef"));
        addref_invoke.arguments().add(body);
      }

      final Block block = ast.newBlock();
      block.statements().add(ast.newExpressionStatement(addref_invoke));
      methodDeclaration.setBody(block);
      return methodDeclaration;
    }

    @NotNull
    public MethodDeclaration method_addRefs(@NotNull AST ast, @NotNull SimpleName name) {
      final String fqTypeName = name.getFullyQualifiedName();
      final MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
      methodDeclaration.setName(ast.newSimpleName("addRefs"));

      methodDeclaration.setReturnType2(arrayType(ast, fqTypeName));
      methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
      methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));

      final SingleVariableDeclaration arg = ast.newSingleVariableDeclaration();
      arg.setType(arrayType(ast, fqTypeName));
      arg.setName(ast.newSimpleName("array"));
      methodDeclaration.parameters().add(arg);

      final MethodInvocation stream_invoke = ast.newMethodInvocation();
      stream_invoke.setExpression(newQualifiedName(ast, "java.util.Arrays".split("\\.")));
      stream_invoke.setName(ast.newSimpleName("stream"));
      stream_invoke.arguments().add(ast.newSimpleName("array"));

      final MethodInvocation filter_invoke = ast.newMethodInvocation();
      {
        filter_invoke.setExpression(stream_invoke);
        filter_invoke.setName(ast.newSimpleName("filter"));
        final LambdaExpression filter_lambda = ast.newLambdaExpression();
        final VariableDeclarationFragment variableDeclarationFragment = ast.newVariableDeclarationFragment();
        variableDeclarationFragment.setName(ast.newSimpleName("x"));
        filter_lambda.parameters().add(variableDeclarationFragment);
        final InfixExpression infixExpression = ast.newInfixExpression();
        infixExpression.setLeftOperand(ast.newSimpleName("x"));
        infixExpression.setOperator(InfixExpression.Operator.NOT_EQUALS);
        infixExpression.setRightOperand(ast.newNullLiteral());
        filter_lambda.setBody(infixExpression);
        filter_invoke.arguments().add(filter_lambda);
      }

      final MethodInvocation addref_invoke = ast.newMethodInvocation();
      {
        addref_invoke.setExpression(filter_invoke);
        addref_invoke.setName(ast.newSimpleName("map"));
        final ExpressionMethodReference body = ast.newExpressionMethodReference();
        body.setExpression(ast.newSimpleName(fqTypeName));
        body.setName(ast.newSimpleName("addRef"));
        addref_invoke.arguments().add(body);
      }

      final MethodInvocation toArray_invoke = ast.newMethodInvocation();
      {
        toArray_invoke.setExpression(addref_invoke);
        toArray_invoke.setName(ast.newSimpleName("toArray"));
        final LambdaExpression filter_lambda = ast.newLambdaExpression();
        final VariableDeclarationFragment variableDeclarationFragment = ast.newVariableDeclarationFragment();
        variableDeclarationFragment.setName(ast.newSimpleName("x"));
        filter_lambda.parameters().add(variableDeclarationFragment);

        final ArrayCreation arrayCreation = ast.newArrayCreation();
        arrayCreation.setType(arrayType(ast, fqTypeName));
        arrayCreation.dimensions().add(ast.newSimpleName("x"));

        filter_lambda.setBody(arrayCreation);
        toArray_invoke.arguments().add(filter_lambda);
      }

      final Block block = ast.newBlock();
      final ReturnStatement returnStatement = ast.newReturnStatement();
      returnStatement.setExpression(toArray_invoke);
      block.statements().add(returnStatement);
      methodDeclaration.setBody(block);
      return methodDeclaration;
    }

  }

}
