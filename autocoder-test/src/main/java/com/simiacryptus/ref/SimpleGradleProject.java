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

package com.simiacryptus.ref;

import com.simiacryptus.ref.core.ProjectInfo;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimpleGradleProject {
  public final String projectRoot;
  private final String[] classpath;

  public SimpleGradleProject(@Nonnull String projectRoot, @Nonnull String[] classpath) {
    this.projectRoot = projectRoot;
    this.classpath = classpath;
  }

  @Nonnull
  public static SimpleGradleProject load(@Nonnull String root) throws IOException {
    String canonicalRoot = new File(root).getCanonicalPath();
    String cpProp = System.getProperty("demo.classpath", System.getProperty("java.class.path", ""));
    String[] cp = Arrays.stream(cpProp.split(File.pathSeparator))
        .filter(s -> !s.isEmpty() && new File(s).exists())
        .distinct()
        .toArray(String[]::new);
    return new SimpleGradleProject(canonicalRoot, cp);
  }

  @Nonnull
  public String[] getSources() {
    List<String> sources = new ArrayList<>();
    for (String rel : new String[]{"src/main/java", "src/test/java"}) {
      File dir = new File(projectRoot, rel);
      if (dir.exists()) {
        sources.add(dir.getAbsolutePath());
      }
    }
    return sources.toArray(new String[0]);
  }

  @Nonnull
  public String[] getDependencies() {
    return classpath;
  }

  @Nonnull
  public ProjectInfo getProjectInfo() {
    return new ProjectInfo(this.projectRoot, getSources(), getDependencies());
  }
}
