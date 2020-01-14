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

import org.apache.maven.plugin.MojoExecutionException;

import javax.annotation.Nonnull;
import java.io.PrintStream;

public abstract class AutoCoderMojo extends BaseMojo {
  public static final PrintStream ERR = System.err;

  @Nonnull
  protected ProjectInfo getProjectInfo() {
    return new ProjectInfo(basedir.getAbsolutePath(), getSources(), getDependencies());
  }

  public void execute() throws MojoExecutionException {
    if (getBoolean("logException", true)) {
      try {
        getAutoCoder(getProjectInfo()).rewrite();
      } catch (RuntimeException e) {
        e.printStackTrace(ERR);
        throw e;
      } catch (Throwable e) {
        e.printStackTrace(ERR);
        throw new RuntimeException(e);
      }
    } else {
      getAutoCoder(getProjectInfo()).rewrite();
    }
  }

  @Nonnull
  protected abstract AutoCoder getAutoCoder(ProjectInfo projectInfo);

  protected boolean getBoolean(@Nonnull String key, boolean defaultValue) {
    return Boolean.parseBoolean(System.getProperty(key, Boolean.toString(defaultValue)));
  }

}
