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

package com.simiacryptus.ref.core.ops;

import com.simiacryptus.ref.core.ProjectInfo;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * The type Ast scanner.
 */
public class ASTScanner extends ASTEditor {
  /**
   * Instantiates a new Ast scanner.
   *
   * @param projectInfo     the project info
   * @param compilationUnit the compilation unit
   * @param file            the file
   * @param record          the record
   */
  public ASTScanner(ProjectInfo projectInfo, @NotNull CompilationUnit compilationUnit, @NotNull File file, boolean record) {
    super(projectInfo, compilationUnit, file, record);
  }

  @Override
  public boolean write(boolean format) {
    return false;
  }

}
