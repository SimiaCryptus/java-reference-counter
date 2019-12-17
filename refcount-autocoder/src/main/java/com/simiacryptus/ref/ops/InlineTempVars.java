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
import org.eclipse.jdt.core.dom.*;

import java.io.File;

@RefIgnore
public class InlineTempVars extends RefFileAstVisitor {

  public InlineTempVars(ProjectInfo projectInfo, CompilationUnit compilationUnit, File file) {
    super(projectInfo, compilationUnit, file);
  }

  @Override
  public void endVisit(VariableDeclarationStatement node) {
    if (node.fragments().size() > 1) return;
    final Object head = node.fragments().get(0);
    if (head instanceof VariableDeclarationFragment) {
      final VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) head;
      final SimpleName name = variableDeclarationFragment.getName();
      if (isTempIdentifier(name)) {
        final Expression expression = variableDeclarationFragment.getInitializer();
        final Block block = getBlock(node);
        delete(node);
        for (SimpleName match : findExpressions(block, name)) {
          replace(match, copyIfAttached(expression));
        }
      }
    }
  }


}
