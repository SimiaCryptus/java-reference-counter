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

package com.simiacryptus.ref.core;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ProjectInfo {
  private final String projectRoot;
  private final String[] sourcepathEntries;
  private final String[] classpathEntries;
  private ASTParser astParser = null;

  public ProjectInfo(String projectRoot, String[] sourcepathEntries, String[] classpathEntries) {
    this.projectRoot = projectRoot;
    this.sourcepathEntries = sourcepathEntries;
    this.classpathEntries = classpathEntries;
    //this.setAstParser(newAstParser());
  }

  public ASTParser getAstParser() {
    return astParser == null ? newAstParser() : astParser;
  }

  public void setAstParser(ASTParser astParser) {
    this.astParser = astParser;
  }

  @NotNull
  public ASTParser newAstParser() {
    HashMap<String, String> compilerOptions = new HashMap<>();
    compilerOptions.put(CompilerOptions.OPTION_Source, CompilerOptions.versionFromJdkLevel(ClassFileConstants.JDK1_8));
    ASTParser astParser = ASTParser.newParser(AST.JLS11);
    astParser.setCompilerOptions(compilerOptions);
    astParser.setKind(ASTParser.K_COMPILATION_UNIT);
    astParser.setResolveBindings(true);
    astParser.setBindingsRecovery(true);
    astParser.setStatementsRecovery(true);
    astParser.setEnvironment(classpathEntries, sourcepathEntries, null, true);
    return astParser;
  }

  @NotNull
  public HashMap<File, CompilationUnit> parse() {
    return read(sourceFiles());
  }

  public @NotNull HashMap<File, CompilationUnit> read(File... files) {
    final Map<String, File> fileMap = new HashMap<>();
    for (File file : files) {
      fileMap.put(file.getAbsolutePath(), file);
    }
    HashMap<File, CompilationUnit> results = new HashMap<>();
    getAstParser().createASTs(
        fileMap.keySet().toArray(new String[]{}),
        null,
        new String[]{},
        new FileASTRequestor() {
          @Override
          public void acceptAST(final String source, final CompilationUnit ast) {
            results.put(fileMap.get(source), ast);
          }
        },
        new NullProgressMonitor()
    );
    return results;
  }

  @NotNull
  public File[] sourceFiles() {
    return FileUtils.listFiles(new File(projectRoot), new String[]{"java"}, true)
        .stream().map(File::getAbsoluteFile).distinct().toArray(i -> new File[i]);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ProjectInfo{");
    sb.append("projectRoot='").append(projectRoot).append('\'');
    sb.append(", sourcepathEntries=").append(Arrays.toString(sourcepathEntries));
    sb.append(", classpathEntries=").append(Arrays.toString(classpathEntries));
    sb.append('}');
    return sb.toString();
  }
}
