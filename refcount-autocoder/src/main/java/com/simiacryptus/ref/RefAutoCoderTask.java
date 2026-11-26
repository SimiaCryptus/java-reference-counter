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

import com.simiacryptus.ref.core.AutoCoderTask;
import com.simiacryptus.ref.lang.RefIgnore;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.File;

@RefIgnore
public abstract class RefAutoCoderTask extends AutoCoderTask {

  @Input
  public abstract Property<Boolean> getForce();

  protected RefAutoCoderTask() {
    getForce().convention(getBoolean("force", false));
  }

  private boolean hasRefCountCore() {
    for (Configuration config : getProject().getConfigurations()) {
      boolean foundInDeps = config.getAllDependencies().stream().anyMatch(dep ->
          "com.simiacryptus".equals(dep.getGroup()) && "refcount-core".equals(dep.getName())
              || "refcount-core".equals(dep.getName())
      );
      if (foundInDeps) {
        return true;
      }
    }
    for (String srcDir : getSources()) {
      if (new File(srcDir, "com/simiacryptus/ref/lang/ReferenceCounting.java").exists()) {
        return true;
      }
    }
    return false;
  }

  @Override
  @TaskAction
  public void executeTask() {
    if (!hasRefCountCore()) {
      getLogger().warn("Dependency not found: refcount-core");
      if (getForce().get()) {
        super.executeTask();
      }
    } else {
      getLogger().info("Dependency confirmed: refcount-core");
      super.executeTask();
    }
  }
}
