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

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class AutoCoderTask extends DefaultTask {
  public static final PrintStream ERR = System.err;

  @Input
  public abstract Property<Boolean> getLogException();

  @Input
  public abstract Property<Boolean> getParallel();

  protected AutoCoderTask() {
    getLogException().convention(getBoolean("logException", true));
    getParallel().convention(getBoolean("parallel", false));
  }

  @Internal
  @Nonnull
  public String[] getDependencies() {
    Set<String> classpath = new LinkedHashSet<>();
    for (String configName : new String[]{"compileClasspath", "testCompileClasspath"}) {
      Configuration config = getProject().getConfigurations().findByName(configName);
      if (config != null && config.isCanBeResolved()) {
        for (File file : config.getFiles()) {
          if (file != null && file.exists()) {
            classpath.add(file.getAbsolutePath());
          }
        }
      }
    }
    return classpath.toArray(new String[0]);
  }

  @Internal
  @Nonnull
  public String[] getSources() {
    Set<String> sources = new LinkedHashSet<>();
    SourceSetContainer sourceSets = getProject().getExtensions().findByType(SourceSetContainer.class);
    if (sourceSets != null) {
      for (SourceSet sourceSet : sourceSets) {
        for (File dir : sourceSet.getJava().getSrcDirs()) {
          if (dir.exists()) {
            sources.add(dir.getAbsolutePath());
          }
        }
      }
    }
    return sources.toArray(new String[0]);
  }

  @Internal
  @Nonnull
  protected ProjectInfo getProjectInfo() {
    return new ProjectInfo(
        getProject().getProjectDir().getAbsolutePath(),
        getSources(),
        getDependencies()
    );
  }

  @TaskAction
  public void executeTask() {
    AutoCoder coder = getAutoCoder(getProjectInfo());
    coder.setParallel(getParallel().get());
    if (getLogException().get()) {
      try {
        coder.rewrite();
      } catch (CollectableException e) {
        ERR.println(e.getMessage());
        throw e;
      } catch (RuntimeException e) {
        e.printStackTrace(ERR);
        throw e;
      } catch (Throwable e) {
        e.printStackTrace(ERR);
        throw new RuntimeException(e);
      }
    } else {
      coder.rewrite();
    }
  }

  @Nonnull
  protected abstract AutoCoder getAutoCoder(ProjectInfo projectInfo);

  protected static boolean getBoolean(@Nonnull String key, boolean defaultValue) {
    return Boolean.parseBoolean(System.getProperty(key, Boolean.toString(defaultValue)));
  }
}
