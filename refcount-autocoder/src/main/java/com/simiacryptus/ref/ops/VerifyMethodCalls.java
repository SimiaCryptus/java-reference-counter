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
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RefIgnore
public class VerifyMethodCalls extends RefASTOperator {

  final HashMap<SymbolIndex.BindingID, Set<Integer>> missingAttributes;

  public VerifyMethodCalls(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
    this(projectInfo, compilationUnit, file, null);
  }

  public VerifyMethodCalls(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file, HashMap<SymbolIndex.BindingID, Set<Integer>> missingAttributes) {
    super(projectInfo, compilationUnit, file);
    this.missingAttributes = missingAttributes;
  }

  @Override
  public void endVisit(MethodDeclaration node) {
    final IMethodBinding methodBinding = node.resolveBinding();
    if (null == methodBinding) {
      warn(node, "Unresolved binding");
      return;
    }
    final List<IMethodBinding> superMethods = ASTUtil.superMethods(methodBinding);
    for (int i = 0; i < methodBinding.getParameterTypes().length; i++) {
      if (!ASTUtil.findAnnotation(RefAware.class, methodBinding.getParameterAnnotations(i)).isPresent()) {
        for (IMethodBinding superMethod : superMethods) {
          if (ASTUtil.findAnnotation(RefAware.class, superMethod.getParameterAnnotations(i)).isPresent()) {
            ITypeBinding parameterType = methodBinding.getParameterTypes()[i];
            if (!isRefCounted(node, parameterType) && Modifier.isFinal(parameterType.getModifiers())) continue;
            fail(node, methodBinding, i);
            break;
          }
        }
      }
    }
  }

  @Override
  public void endVisit(MethodInvocation node) {
    final IMethodBinding methodBinding = node.resolveMethodBinding();
    if (null == methodBinding) {
      warn(node, "Unresolved binding");
      return;
    }
    if(isRefCounted(node, methodBinding.getReturnType()) || ASTUtil.hasAnnotation(methodBinding, RefAware.class)) {
      assertResultConfumed(node);
    }
    final List<Expression> arguments = node.arguments();
    ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
    final int numberOfDeclaredArguments = parameterTypes.length;
    final int numberOfArguments = arguments.size();
    for (int i = 0; i < numberOfArguments; i++) {
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
    }
  }

  private void assertResultConfumed(ASTNode node) {
    ASTNode parent = node.getParent();
    if(parent instanceof MethodInvocation) {
      MethodInvocation methodInvocation = (MethodInvocation) parent;
      if(methodInvocation.getExpression() == node) {
        if(methodInvocation.getName().toString().equals("freeRef")) return;
        fatal(parent, "Reference tracked type used in chained method call");
      }
    } else if(parent instanceof Assignment) {
      // OK
    } else if(parent instanceof VariableDeclarationFragment) {
      // OK
    } else if(parent instanceof SingleVariableDeclaration) {
      // OK
    } else if(parent instanceof ReturnStatement) {
      // OK
    } else if(parent instanceof LambdaExpression) {
      // OK
    } else if(parent instanceof SuperConstructorInvocation) {
      // OK
    } else if(parent instanceof ConstructorInvocation) {
      // OK
    } else if(parent instanceof ExpressionStatement) {
      ASTNode parent1 = parent.getParent();
      if(parent1 instanceof Block) {
        ASTNode parent2 = parent1.getParent();
        if(parent2 instanceof Initializer)
          return;
      }
      fatal(parent, "Reference tracked type discarded without freeRef");
    } else if(parent instanceof ArrayAccess) {
      fatal(parent, "Reference tracked array access directly upon creation");
    } else if(parent instanceof InfixExpression) {
      fatal(parent, "Reference tracked type used as conditional expression");
    } else if(parent instanceof ClassInstanceCreation) {
      // OK
    } else if(parent instanceof SuperMethodInvocation) {
      // OK
    } else if(parent instanceof CastExpression) {
      assertResultConfumed(parent);
    } else if(parent instanceof ParenthesizedExpression) {
      assertResultConfumed(parent);
    } else if(parent instanceof ArrayInitializer) {
      assertResultConfumed(parent);
    } else if(parent instanceof ArrayCreation) {
      assertResultConfumed(parent);
    } else if(parent instanceof InstanceofExpression) {
      fatal(parent, "Reference tracked type used by InstanceofExpression");
    } else if(parent instanceof EnhancedForStatement) {
      fatal(parent, "Reference tracked type used by EnhancedForStatement");
    } else if(parent instanceof ConditionalExpression) {
      if(((ConditionalExpression) parent).getExpression() == node) {
        fatal(parent, "Reference tracked type used as conditional expression");
      } else {
        assertResultConfumed(parent);
      }
    } else {
      fatal(node, "Unhandled MethodInvocation parent %s", parent.getClass().getSimpleName());
    }
  }

  private void fail(ASTNode node, IMethodBinding methodBinding, int i) {
    if (null == missingAttributes) {
      fatal(node, "Argument %s of %s is not @RefAware", i, methodBinding.getName());
    } else {
      final SymbolIndex.BindingID bindingID = SymbolIndex.getBindingID(methodBinding);
      missingAttributes.computeIfAbsent(bindingID, x -> new HashSet<>()).add(i);
      warn(node, "Argument %s of %s is not @RefAware", i, methodBinding.getName());
    }
  }
}
