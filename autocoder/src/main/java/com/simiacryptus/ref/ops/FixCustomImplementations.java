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

import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.wrappers.RefIterator;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.Iterator;

@RefIgnore
public class FixCustomImplementations extends RefFileAstVisitor {

  public FixCustomImplementations(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
    super(projectInfo, compilationUnit, file);
  }

  @Override
  public void endVisit(AnonymousClassDeclaration node) {
    final ITypeBinding typeBinding = node.resolveBinding();
    if (null == typeBinding) {
      warn(node, "Unresolved binding");
      return;
    }
    if (replace(node, typeBinding, RefIterator.class, Iterator.class)) return;
  }

  protected boolean replace(AnonymousClassDeclaration node, ITypeBinding typeBinding, Class<RefIterator> match, Class<Iterator> replace) {
    if (typeBinding.getSuperclass().getBinaryName().equals(match.getName())) {
      info(node, "RefIterator anonymous class");
      final AST ast = node.getAST();
      final ClassInstanceCreation parent = (ClassInstanceCreation) node.getParent();
      final SimpleType simpleType = ast.newSimpleType(newQualifiedName(ast, replace));
      final Type parentType = parent.getType();
      if (parentType instanceof ParameterizedType) {
        final ParameterizedType type = ast.newParameterizedType(simpleType);
        ((ParameterizedType) parentType).typeArguments().forEach(astNode -> type.typeArguments().add(ASTNode.copySubtree(ast, (ASTNode) astNode)));
        parent.setType(type);
      } else {
        parent.setType(simpleType);
      }
      return true;
    } else {
      return false;
    }
  }

}
