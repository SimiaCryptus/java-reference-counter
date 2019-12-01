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

package com.simiacryptus.devutil.ref;

import com.simiacryptus.lang.ref.RefAware;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.io.File;
import java.util.Iterator;

class RemoveAnnotations extends RefFileAstVisitor {

  RemoveAnnotations(CompilationUnit compilationUnit, File file) {
    super(compilationUnit, file);
  }

  @Override
  public void endVisit(TypeDeclaration node) {
    final Iterator iterator = node.modifiers().iterator();
    while (iterator.hasNext()) {
      final Object next = iterator.next();
      if (next instanceof MarkerAnnotation) {
        if (((MarkerAnnotation) next).getTypeName().getFullyQualifiedName().equals(RefAware.class.getCanonicalName())) {
          info(node, "Removed @RefAware from %s", node.getName());
          iterator.remove();
        }
      }
    }
  }
}
