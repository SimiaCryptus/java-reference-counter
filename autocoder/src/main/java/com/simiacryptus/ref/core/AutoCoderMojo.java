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

/**
 * This class is responsible for autocoding.
 *
 * @docgenVersion 9
 */
public abstract class AutoCoderMojo extends BaseMojo {
  public static final PrintStream ERR = System.err;

  /**
   * Returns a new ProjectInfo object containing the absolute path of the basedir, the sources, and the dependencies.
   *
   * @return ProjectInfo
   * @docgenVersion 9
   */
  @Nonnull
  protected ProjectInfo getProjectInfo() {
    return new ProjectInfo(basedir.getAbsolutePath(), getSources(), getDependencies());
  }

  /**
   * Executes the mojo.
   *
   * @throws MojoExecutionException if an error occurs
   * @docgenVersion 9
   */
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

  /**
   * Returns the AutoCoder for the given ProjectInfo.
   *
   * @param projectInfo the ProjectInfo
   * @return the AutoCoder
   * @throws NullPointerException if projectInfo is null
   * @docgenVersion 9
   */
  @Nonnull
  protected abstract AutoCoder getAutoCoder(ProjectInfo projectInfo);

  /**
   * Get a boolean value from the system properties.
   *
   * @param key          the key to look up
   * @param defaultValue the default value to use if the key is not found
   * @return the boolean value
   * @docgenVersion 9
   */
  protected boolean getBoolean(@Nonnull String key, boolean defaultValue) {
    return Boolean.parseBoolean(System.getProperty(key, Boolean.toString(defaultValue)));
  }

}
