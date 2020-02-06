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
import com.simiacryptus.ref.lang.MustCall;
import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;
import org.eclipse.jdt.core.dom.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;
import java.util.function.Predicate;

@RefIgnore
public class VerifyMethodCalls extends RefASTOperator {

  final HashMap<SymbolIndex.BindingID, Set<Integer>> missingAttributes;

  public VerifyMethodCalls(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    this(projectInfo, compilationUnit, file, null);
  }

  public VerifyMethodCalls(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file, HashMap<SymbolIndex.BindingID, Set<Integer>> missingAttributes) {
    super(projectInfo, compilationUnit, file);
    this.missingAttributes = missingAttributes;
  }

  @Override
  public void endVisit(@Nonnull MethodDeclaration node) {
    final IMethodBinding methodBinding = node.resolveBinding();
    if (null == methodBinding) {
      warn(node, "Unresolved binding");
      return;
    }
    if (contains(ASTUtil.superMethods(methodBinding), binding -> ASTUtil.hasAnnotation(binding, MustCall.class))) {
      Block body = node.getBody();
      if(null != body) {
        if (!contains((List<Statement>) body.statements(), statement -> {
          if (statement instanceof ExpressionStatement) {
            Expression expression = ((ExpressionStatement) statement).getExpression();
            if (expression instanceof SuperMethodInvocation) {
              return true;
            }
          }
          return false;
        })) {
          fatal(node, "Method does not call @MustCall super");
        }
      }
    }

    ArrayList<CollectableException> exceptions = new ArrayList<>();
    for (int i = 0; i < methodBinding.getParameterTypes().length; i++) {
      try {
        if (!ASTUtil.findAnnotation(RefAware.class, methodBinding.getParameterAnnotations(i)).isPresent()) {
          for (IMethodBinding superMethod : ASTUtil.superMethods(methodBinding)) {
            if (ASTUtil.findAnnotation(RefAware.class, superMethod.getParameterAnnotations(i)).isPresent()) {
              ITypeBinding parameterType = methodBinding.getParameterTypes()[i];
              if (!isRefCounted(node, parameterType) && Modifier.isFinal(parameterType.getModifiers())) continue;
              fail(node, methodBinding, i);
              break;
            }
          }
        }
      } catch (CollectableException e) {
        exceptions.add(e);
      }
    }
    if (!exceptions.isEmpty()) {
      throw CollectableException.combine(exceptions);
    }
  }

  public static <T> boolean contains(List<T> superMethods, Predicate<T> predicate) {
    return superMethods.stream().filter(predicate).findAny().isPresent();
  }

  @Override
  public void endVisit(ArrayAccess node) {
    ITypeBinding typeBinding = node.resolveTypeBinding();
    if (null == typeBinding) {
      warn(node, "Unresolved binding");
      return;
    }
    if (_isRefCounted(node, typeBinding)) {
      assertResultNotConsumed(node, false);
    }
  }

  @Override
  public void endVisit(FieldAccess node) {
    if (node.getExpression() instanceof ThisExpression) return;
    ITypeBinding typeBinding = node.resolveTypeBinding();
    if (null == typeBinding) {
      warn(node, "Unresolved binding");
      return;
    }
    if (_isRefCounted(node, typeBinding)) {
      assertResultNotConsumed(node, false);
    }
  }

  @Override
  public void endVisit(ThisExpression node) {
    ITypeBinding typeBinding = node.resolveTypeBinding();
    if (null == typeBinding) {
      warn(node, "Unresolved binding");
      return;
    }
    if (_isRefCounted(node, typeBinding)) {
      assertResultConsumed(node, true);
    }
    super.endVisit(node);
  }

  public boolean _isRefCounted(ASTNode node, ITypeBinding typeBinding) {
    if (typeBinding.isPrimitive()) return false;
    return isRefCounted(node, typeBinding) || ASTUtil.hasAnnotation(typeBinding, RefAware.class);
  }

  @Override
  public void endVisit(@Nonnull MethodInvocation node) {
    if (node.getName().toString().equals("equals")) return;
    final IMethodBinding methodBinding = node.resolveMethodBinding();
    if (null == methodBinding) {
      warn(node, "Unresolved binding");
      return;
    }
    if (isRefCounted(node, methodBinding)) {
      assertResultConsumed(node, false);
    }
    verifyArguments(methodBinding, node.arguments());
  }

  @Override
  public void endVisit(@Nonnull SuperMethodInvocation node) {
    if (node.getName().toString().equals("equals")) return;
    final IMethodBinding methodBinding = node.resolveMethodBinding();
    if (null == methodBinding) {
      warn(node, "Unresolved binding");
      return;
    }
    if (isRefCounted(node, methodBinding)) {
      assertResultConsumed(node, false);
    }
    verifyArguments(methodBinding, node.arguments());
  }

  public void verifyArguments(IMethodBinding methodBinding, List<Expression> arguments) {
    ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
    final int numberOfDeclaredArguments = parameterTypes.length;
    final int numberOfArguments = arguments.size();
    ArrayList<CollectableException> exceptions = new ArrayList<>();
    for (int i = 0; i < numberOfArguments; i++) {
      try {
        final Expression argument = arguments.get(i);
        final ITypeBinding parameterType;
        final IAnnotationBinding[] parameterAnnotations;
        if (numberOfArguments > numberOfDeclaredArguments && i >= numberOfDeclaredArguments) {
          parameterType = parameterTypes[numberOfDeclaredArguments - 1].getElementType();
          parameterAnnotations = methodBinding.getParameterAnnotations(numberOfDeclaredArguments - 1);
        } else {
          parameterType = parameterTypes[i];
          parameterAnnotations = methodBinding.getParameterAnnotations(i);
        }
        final ITypeBinding resolveTypeBinding = argument.resolveTypeBinding();
        if (null == resolveTypeBinding) {
          warn(argument, "Cannot resolve type binding");
          continue;
        }
        if (isRefCounted(argument, resolveTypeBinding)) {
          if (!isRefCounted(argument, parameterType) && !ASTUtil.findAnnotation(RefAware.class, parameterAnnotations).isPresent()) {
            fail(argument, methodBinding, i);
          }
        }
      } catch (CollectableException e) {
        exceptions.add(e);
      }
    }
    if (!exceptions.isEmpty()) {
      throw CollectableException.combine(exceptions);
    }
  }

  public boolean isRefCounted(@Nonnull ASTNode node, IMethodBinding methodBinding) {
    if (methodBinding.getReturnType().isPrimitive()) return false;
    return isRefCounted(node, methodBinding.getReturnType()) || ASTUtil.hasAnnotation(methodBinding, RefAware.class);
  }

  private void assertResultConsumed(@Nonnull ASTNode node, boolean allowTerminalAccess) {
    Boolean resultConsumed = isResultConsumed(node, allowTerminalAccess);
    if (null != resultConsumed && !resultConsumed) {
      fatal(node, "Reference to %s is not consumed", node);
    }
  }

  private void assertResultNotConsumed(@Nonnull ASTNode node, boolean allowTerminalAccess) {
    Boolean resultConsumed = isResultConsumed(node, allowTerminalAccess);
    if (null != resultConsumed && resultConsumed) {
      fatal(node, "Reference to %s is consumed", node);
    }
  }

  private Boolean isResultConsumed(@Nonnull ASTNode node, boolean allowTerminalAccess) {
    ASTNode parent = node.getParent();
    if (parent instanceof MethodInvocation) {
      MethodInvocation methodInvocation = (MethodInvocation) parent;
      if (methodInvocation.getExpression() == node) {
        if (methodInvocation.getName().toString().equals("freeRef")) return true;
        if (allowTerminalAccess) return null;
        if (node instanceof MethodInvocation) {
          Expression expression = ((MethodInvocation) node).getExpression();
          if (null != expression) {
            ITypeBinding typeBinding = expression.resolveTypeBinding();
            if (null != typeBinding) {
              if (typeBinding.getQualifiedName().startsWith(Map.Entry.class.getCanonicalName()) ||
                  typeBinding.getQualifiedName().startsWith(Optional.class.getCanonicalName())) {
                return true;
              }
            }
          }
        }
        //fatal(parent, "Reference tracked type used in chained method call");
        return false;
      } else {
        if (methodInvocation.getName().toString().equals("addRefs")) return false;
        int index = methodInvocation.arguments().indexOf(node);
        IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
        if (null == methodBinding) {
          warn(methodInvocation, "Unresolved binding");
          return true;
        }
        if (index >= methodBinding.getParameterTypes().length) index = methodBinding.getParameterTypes().length - 1;
        boolean isRefCounted = isRefCounted(node, methodBinding.getParameterTypes()[index]);
        boolean hasRefAware = ASTUtil.findAnnotation(RefAware.class, methodBinding.getParameterAnnotations(index)).isPresent();
        boolean hasRefIgnore = ASTUtil.findAnnotation(RefIgnore.class, methodBinding.getParameterAnnotations(index)).isPresent();
        if (!isRefCounted && !hasRefAware || hasRefIgnore) {
          //fatal(node, "Reference tracked type passed to non-RefAware method parameter");
          return false;
        }
      }
    } else if (parent instanceof ExpressionMethodReference) {
      if (allowTerminalAccess) return null;
      //fatal(parent, "Reference tracked type used as ExpressionMethodReference");
      return false;
    } else if (parent instanceof Assignment) {
      // OK
    } else if (parent instanceof VariableDeclarationFragment) {
      // OK
    } else if (parent instanceof SingleVariableDeclaration) {
      // OK
    } else if (parent instanceof ReturnStatement) {
      // OK
    } else if (parent instanceof LambdaExpression) {
      IMethodBinding methodBinding = ((LambdaExpression) parent).resolveMethodBinding();
      ITypeBinding returnType = methodBinding.getReturnType();
      if(returnType.isPrimitive()) {
        return false;
      } else {
        return true;
      }
    } else if (parent instanceof SuperConstructorInvocation) {
      // OK
    } else if (parent instanceof ConstructorInvocation) {
      // OK
    } else if (parent instanceof ExpressionStatement) {
      ASTNode parent1 = parent.getParent();
      if (parent1 instanceof Block) {
        ASTNode parent2 = parent1.getParent();
        if (parent2 instanceof Initializer)
          return true;
      }
      //fatal(parent, "Reference tracked type discarded without freeRef");
      return false;
    } else if (parent instanceof ArrayAccess) {
      //fatal(parent, "Reference tracked array access directly upon creation");
      return false;
    } else if (parent instanceof InfixExpression) {
      if (allowTerminalAccess) return null;
      //fatal(parent, "Reference tracked type used as conditional expression");
      return false;
    } else if (parent instanceof ClassInstanceCreation) {
      // OK
    } else if (parent instanceof SuperMethodInvocation) {
      // OK
    } else if (parent instanceof CastExpression) {
      return isResultConsumed(parent, allowTerminalAccess);
    } else if (parent instanceof ParenthesizedExpression) {
      return isResultConsumed(parent, allowTerminalAccess);
    } else if (parent instanceof SynchronizedStatement) {
      if (allowTerminalAccess) return null;
      //fatal(parent, "Reference tracked type used for syncronization");
      return false;
    } else if (parent instanceof ArrayInitializer) {
      return isResultConsumed(parent, allowTerminalAccess);
    } else if (parent instanceof ArrayCreation) {
      return isResultConsumed(parent, allowTerminalAccess);
    } else if (parent instanceof FieldAccess) {
      if (allowTerminalAccess) return null;
      //fatal(parent, "Reference tracked type used only for field access");
      return false;
    } else if (parent instanceof InstanceofExpression) {
      //fatal(parent, "Reference tracked type used by InstanceofExpression");
      return false;
    } else if (parent instanceof EnhancedForStatement) {
      //fatal(parent, "Reference tracked type used by EnhancedForStatement");
      return false;
    } else if (parent instanceof ConditionalExpression) {
      if (((ConditionalExpression) parent).getExpression() == node) {
        //fatal(parent, "Reference tracked type used as conditional expression");
        return false;
      } else {
        return isResultConsumed(parent, allowTerminalAccess);
      }
    } else {
      fatal(node, "Unhandled MethodInvocation parent %s", parent.getClass().getSimpleName());
    }
    return true;
  }

  private void fail(@Nonnull ASTNode node, @Nonnull IMethodBinding methodBinding, int i) {
    if (null == missingAttributes) {
      fatal(node, "Argument %s of %s is not @RefAware", i, methodBinding.getName());
    } else {
      final SymbolIndex.BindingID bindingID = SymbolIndex.getBindingID(methodBinding);
      missingAttributes.computeIfAbsent(bindingID, x -> new HashSet<>()).add(i);
      warn(node, "Argument %s of %s is not @RefAware", i, methodBinding.getName());
    }
  }
}
