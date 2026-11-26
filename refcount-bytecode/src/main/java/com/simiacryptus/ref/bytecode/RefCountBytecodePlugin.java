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

package com.simiacryptus.ref.bytecode;

import com.simiacryptus.ref.lang.RefIgnore;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

@RefIgnore
public class RefCountBytecodePlugin implements Plugin<Project> {
  public static final String GROUP = "refcount";
  public static final String EXTENSION_NAME = "refcountBytecode";

  @Override
  public void apply(@Nonnull Project project) {
    RefCountBytecodeExtension ext = project.getExtensions().create(
        EXTENSION_NAME,
        RefCountBytecodeExtension.class
    );

    registerTask(project, "refcountBytecodeVerify", BytecodeVerifyTask.class,
        "Verify reference counting in compiled Java and non-Java JVM bytecode", ext, null);

    registerTask(project, "refcountBytecodeCheck", BytecodeCheckTask.class,
        "Check compiled bytecode type hierarchy and @MustCall / @RefAware contracts", ext, null);

    registerTask(project, "refcountBytecodeReport", BytecodeReportTask.class,
        "Generate text and JSON reference-counting bytecode analysis reports", ext,
        task -> task.getFailOnError().convention(false));
  }

  private <T extends AbstractBytecodeTask> void registerTask(
      @Nonnull Project project,
      @Nonnull String name,
      @Nonnull Class<T> type,
      @Nonnull String description,
      @Nonnull RefCountBytecodeExtension ext,
      @Nullable Action<T> extraConfig
  ) {
    project.getTasks().register(name, type, task -> {
      task.setGroup(GROUP);
      task.setDescription(description);
      task.getIncludeTests().convention(ext.getIncludeTests());
      task.getFailOnError().convention(ext.getFailOnError());
      task.getStrictBlindParameters().convention(ext.getStrictBlindParameters());
      task.getLogException().convention(ext.getLogException());
      task.getForce().convention(ext.getForce());
      task.getTargetFiles().from(ext.getTargetFiles());
      task.getExcludePackages().convention(ext.getExcludePackages());
      task.getExcludeClasses().convention(ext.getExcludeClasses());

      // Wire dependency on compiled sourceSet classes (Java, Kotlin, Scala, Groovy, etc.) when present
      SourceSetContainer sourceSets = project.getExtensions().findByType(SourceSetContainer.class);
      if (sourceSets != null) {
        for (SourceSet ss : sourceSets) {
          String ssName = ss.getName().toLowerCase(Locale.ROOT);
          if (!task.getIncludeTests().get() && ssName.contains("test")) {
            continue;
          }
          String classesTask = ss.getClassesTaskName();
          if (project.getTasks().getNames().contains(classesTask)) {
            task.dependsOn(classesTask);
          }
        }
      }

      if (extraConfig != null) {
        extraConfig.execute(task);
      }
    });
  }
}
