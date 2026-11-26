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

import com.simiacryptus.ref.core.PrintAST;
import com.simiacryptus.ref.lang.RefIgnore;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import javax.annotation.Nonnull;

@RefIgnore
public class RefCountAutocoderPlugin implements Plugin<Project> {
  public static final String GROUP = "refcount";

  @Override
  public void apply(@Nonnull Project project) {
    RefCountAutocoderExtension ext = project.getExtensions().create(
        "refcountAutocoder",
        RefCountAutocoderExtension.class
    );

    registerRefTask(project, "refcountInsert", Insert.class,
        "Insert reference counting operations into source code", ext,
        task -> task.getModifyAPI().convention(ext.getModifyAPI()));

    registerRefTask(project, "refcountRemove", Remove.class,
        "Remove reference counting operations from source code", ext,
        task -> task.getModifyAPI().convention(ext.getModifyAPI()));

    registerRefTask(project, "refcountVerify", Verify.class,
        "Verify that reference counting is correctly implemented", ext, null);

    registerRefTask(project, "refcountCheck", Check.class,
        "Check code type hierarchy for reference counting issues", ext, null);

    registerRefTask(project, "refcountUpdate", Update.class,
        "Update reference counting annotations and verify", ext, null);

    registerRefTask(project, "refcountModifyAPI", ModifyAPI.class,
        "Convert standard Java collection types to reference-counted wrappers", ext, null);

    registerRefTask(project, "refcountRevertAPI", RevertAPI.class,
        "Revert reference-counted collection wrappers back to standard Java types", ext, null);

    registerRefTask(project, "refcountAnnotate", AnnotateAll.class,
        "Annotate all reference-counted method parameters", ext, null);

    registerRefTask(project, "refcountTemp", Temp.class,
        "Remove RefAware type annotations", ext, null);

    project.getTasks().register("refcountPrintAST", PrintAST.class, task -> {
      task.setGroup(GROUP);
      task.setDescription("Print the Eclipse JDT AST of project Java source files");
      task.getParallel().convention(ext.getParallel());
      task.getLogException().convention(ext.getLogException());
    });
  }

  private <T extends RefAutoCoderTask> void registerRefTask(
      Project project,
      String name,
      Class<T> type,
      String description,
      RefCountAutocoderExtension ext,
      org.gradle.api.Action<T> extraConfig
  ) {
    project.getTasks().register(name, type, task -> {
      task.setGroup(GROUP);
      task.setDescription(description);
      task.getParallel().convention(ext.getParallel());
      task.getLogException().convention(ext.getLogException());
      task.getForce().convention(ext.getForce());
      if (extraConfig != null) {
        extraConfig.execute(task);
      }
    });
  }
}
