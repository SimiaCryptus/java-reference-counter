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

import com.simiacryptus.ref.core.ProjectInfo;
import com.simiacryptus.ref.lang.RefIgnore;
import org.eclipse.jdt.core.dom.CompilationUnit;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;

@RefIgnore
public class DistinctImports extends RefASTOperator {

  public DistinctImports(ProjectInfo projectInfo, @Nonnull CompilationUnit compilationUnit, @Nonnull File file) {
    super(projectInfo, compilationUnit, file);
  }

  @Override
  public void endVisit(@Nonnull CompilationUnit node) {
    final Iterator iterator = node.imports().iterator();
    final HashSet<String> set = new HashSet<>();
    while (iterator.hasNext()) {
      if (!set.add(iterator.next().toString())) {
        iterator.remove();
      }
    }
    super.endVisit(node);
  }


}
