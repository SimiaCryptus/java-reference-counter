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

package com.simiacryptus.ref.core;

import com.simiacryptus.ref.lang.RefCoderIgnore;
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
import java.util.List;

@RefCoderIgnore
public class ProjectInfo {
  private final String projectRoot;
  private final String[] sourcepathEntries;
  private final String[] classpathEntries;

  public ProjectInfo(String projectRoot, String[] sourcepathEntries, String[] classpathEntries) {
    this.projectRoot = projectRoot;
    this.sourcepathEntries = sourcepathEntries;
    this.classpathEntries = classpathEntries;
  }

  public List<String> getClasspathEntries() {
    return Arrays.asList(classpathEntries);
  }

  public String getProjectRoot() {
    return projectRoot;
  }

  public List<String> getSourcepathEntries() {
    return Arrays.asList(sourcepathEntries);
  }

  @NotNull
  public HashMap<File, CompilationUnit> parse() {
    ASTParser astParser = ASTParser.newParser(AST.JLS11);
    astParser.setKind(ASTParser.K_COMPILATION_UNIT);
    astParser.setResolveBindings(true);
    HashMap<String, String> compilerOptions = new HashMap<>();
    compilerOptions.put(CompilerOptions.OPTION_Source, CompilerOptions.versionFromJdkLevel(ClassFileConstants.JDK1_8));
    compilerOptions.put(CompilerOptions.OPTION_DocCommentSupport, CompilerOptions.ENABLED);
    astParser.setCompilerOptions(compilerOptions);
    astParser.setEnvironment(classpathEntries, sourcepathEntries, null, true);
    final ASTParser parser = astParser;
    HashMap<File, CompilationUnit> results = new HashMap<>();
    HashMap<String, File> fileMap = new HashMap<>();
    parser.createASTs(
        FileUtils.listFiles(new File(projectRoot), new String[]{"java"}, true).stream().map(file -> {
          final String absolutePath = file.getAbsolutePath();
          fileMap.put(absolutePath, file);
          return absolutePath;
        }).toArray(i -> new String[i]),
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
