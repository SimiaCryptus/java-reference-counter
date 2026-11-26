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
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.annotation.Nonnull;
import java.util.Collections;

@RefIgnore
public abstract class RefCountBytecodeExtension {

  public abstract Property<Boolean> getIncludeTests();

  public abstract Property<Boolean> getFailOnError();

  public abstract Property<Boolean> getStrictBlindParameters();

  public abstract Property<Boolean> getLogException();

  public abstract Property<Boolean> getForce();

  public abstract ConfigurableFileCollection getTargetFiles();

  public abstract ListProperty<String> getExcludePackages();

  public abstract ListProperty<String> getExcludeClasses();

  public RefCountBytecodeExtension() {
    getIncludeTests().convention(getBoolean("includeTests", true));
    getFailOnError().convention(getBoolean("failOnError", true));
    getStrictBlindParameters().convention(getBoolean("strictBlindParameters", true));
    getLogException().convention(getBoolean("logException", true));
    getForce().convention(getBoolean("force", false));
    getExcludePackages().convention(Collections.emptyList());
    getExcludeClasses().convention(Collections.emptyList());
  }

  private static boolean getBoolean(@Nonnull String key, boolean defaultValue) {
    return Boolean.parseBoolean(System.getProperty(key, Boolean.toString(defaultValue)));
  }
}
