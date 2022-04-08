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
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import org.eclipse.jdt.core.dom.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class fixes variable declarations.
 *
 * @docgenVersion 9
 */
@RefIgnore
public class FixVariableDeclarations extends RefASTOperator {

  protected FixVariableDeclarations(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    super(projectInfo, compilationUnit, file);
  }

  /**
   * @param type        the type to apply
   * @param initializer the initializer to use, or null
   * @return the applied type, or null if unable to apply
   * @docgenVersion 9
   */
  @Nullable
  protected Type apply(@Nonnull Type type, @Nullable Expression initializer) {
    final ITypeBinding typeBinding = resolveBinding(type);
    if (null == typeBinding) {
      warn(type, "Unresolved binding for %s", type);
      return null;
    }
    if (null == initializer) {
      debug(type, "No initializer");
      return null;
    }
    if (initializer instanceof NullLiteral) {
      debug(type, "Null initializer");
      return null;
    }
    final ITypeBinding initializerType = resolveTypeBinding(initializer);
    if (null == initializerType) {
      warn(type, "Unresolved binding");
      return null;
    }
    final boolean initRefMarked = ASTUtil.derives(initializerType, ReferenceCounting.class);
    if (initializerType.isAssignmentCompatible(typeBinding) && ASTUtil.derives(typeBinding, ReferenceCounting.class) == initRefMarked) {
      return null;
    }
    Optional<ITypeBinding> chooseType = Optional.empty();
    List<ITypeBinding> typePath = typePath(initializerType);
    for (int i = typePath.size() - 1; i >= 0; i--) {
      ITypeBinding candidateBinding = typePath.get(i);
      if (candidateBinding.isAssignmentCompatible(typeBinding)
          && ASTUtil.derives(candidateBinding, ReferenceCounting.class) == initRefMarked) {
        chooseType = Optional.of(candidateBinding);
        break;
      }
    }
    if (chooseType.isPresent()) {
      Type newType = getType(type, RefUtil.get(chooseType).getQualifiedName(), true);
      debug(type, "Replaced variable type %s to %s", type, newType);
      return newType;
    } else {
      warn(type, "No candidates fit for %s", type);
      return null;
    }
  }

  /**
   * @param node            the first type
   * @param typeBinding     the second type
   * @param initializerType the type of the initializer
   * @return the common interface between node and typeBinding, or null if there is no common interface
   * @docgenVersion 9
   */
  @Nullable
  protected Type commonInterface(@Nonnull Type node, @Nonnull ITypeBinding typeBinding, @Nonnull ITypeBinding initializerType) {
    if (initializerType.isAssignmentCompatible(typeBinding)) {
      return getType(node, typeBinding.getQualifiedName(), true);
    }
    for (ITypeBinding interfaceBinding : initializerType.getInterfaces()) {
      final Type commonInterface = commonInterface(node, interfaceBinding, initializerType);
      if (null != commonInterface) return commonInterface;
    }
    return null;
  }

  /**
   * Returns the list of type bindings for the given type binding.
   *
   * @param typeBinding the type binding to get the list of type bindings for
   * @return the list of type bindings for the given type binding
   * @docgenVersion 9
   */
  protected List<ITypeBinding> typePath(@Nonnull ITypeBinding typeBinding) {
    final ArrayList<ITypeBinding> list = new ArrayList<>();
    list.add(typeBinding);
    final ITypeBinding superclass = typeBinding.getSuperclass();
    if (null != superclass) {
      list.addAll(typePath(superclass));
    }
    for (ITypeBinding xface : typeBinding.getInterfaces()) {
      typePath(xface).forEach(list::add);
    }
    return list.stream().distinct().collect(Collectors.toList());
  }

  /**
   * @param node            the node whose type is being resolved
   * @param typeBinding     the type of the node
   * @param initializerType the type of the initializer for the node
   * @return the common superclass of node and typeBinding, or null if none exists
   * @docgenVersion 9
   */
  @Nullable
  @SuppressWarnings("unused")
  protected Type commonSuperclass(@Nonnull Type node, @Nonnull ITypeBinding typeBinding, @Nonnull ITypeBinding initializerType) {
    if (initializerType.isAssignmentCompatible(typeBinding)) {
      return getType(node, typeBinding.getQualifiedName(), true);
    }
    final ITypeBinding superclass = initializerType.getSuperclass();
    if (null != superclass && !superclass.getQualifiedName().equals(Object.class)) {
      final Type commonSuperclass = commonSuperclass(node, superclass, initializerType);
      if (null != commonSuperclass) return commonSuperclass;
    }
    assert superclass != null;
    return commonInterface(node, superclass, initializerType);
  }

  /**
   * This class represents a statement that modifies a variable declaration.
   *
   * @docgenVersion 9
   */
  @RefIgnore
  public static class ModifyVariableDeclarationStatement extends FixVariableDeclarations {
    public ModifyVariableDeclarationStatement(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
      super(projectInfo, compilationUnit, file);
    }

    /**
     * This method is called when the end of a VariableDeclarationStatement is reached in the Java code.
     *
     * @param node the VariableDeclarationStatement that is ending
     * @docgenVersion 9
     */
    @Override
    public void endVisit(@Nonnull VariableDeclarationStatement node) {
      final Type type = node.getType();
      final List fragments = node.fragments();
      if (1 != fragments.size()) {
        warn(node, "%s fragments", fragments.size());
        return;
      }
      final Type newType = apply(type, ((VariableDeclarationFragment) fragments.get(0)).getInitializer());
      if (null != newType && type != newType) node.setType(newType);
    }
  }

  /**
   * This class is used to modify field declarations.
   *
   * @docgenVersion 9
   */
  @RefIgnore
  public static class ModifyFieldDeclaration extends FixVariableDeclarations {
    public ModifyFieldDeclaration(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
      super(projectInfo, compilationUnit, file);
    }

    /**
     * This method is called when the end of a field declaration is reached in the Java source code.
     *
     * @param node the field declaration node that is being visited
     * @docgenVersion 9
     */
    @Override
    public void endVisit(@Nonnull FieldDeclaration node) {
      final Type type = node.getType();
      final List<VariableDeclarationFragment> fragments = node.fragments();
      if (1 != fragments.size()) {
        warn(node, "%s fragments", fragments.size());
        return;
      }
      final Type newType = apply(type, fragments.get(0).getInitializer());
      if (null != newType && type != newType) node.setType(newType);
    }
  }

  /**
   * This class is responsible for modifying a variable declaration fragment.
   *
   * @docgenVersion 9
   */
  @RefIgnore
  public static class ModifyVariableDeclarationFragment extends FixVariableDeclarations {
    public ModifyVariableDeclarationFragment(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
      super(projectInfo, compilationUnit, file);
    }

    /**
     * @docgenVersion 9
     * @see org.eclipse.jdt.core.dom.ASTVisitor#endVisit(org.eclipse.jdt.core.dom.VariableDeclarationFragment)
     */
    @Override
    public void endVisit(VariableDeclarationFragment node) {
      super.endVisit(node);
    }
  }
}
