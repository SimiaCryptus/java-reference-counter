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
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

@RefIgnore
public abstract class AbstractBytecodeTask extends DefaultTask {
  public static final PrintStream ERR = System.err;

  @Input
  public abstract Property<Boolean> getIncludeTests();

  @Input
  public abstract Property<Boolean> getFailOnError();

  @Input
  public abstract Property<Boolean> getStrictBlindParameters();

  @Input
  public abstract Property<Boolean> getLogException();

  @Input
  public abstract Property<Boolean> getForce();

  @InputFiles
  @Optional
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract ConfigurableFileCollection getTargetFiles();

  @Input
  public abstract ListProperty<String> getExcludePackages();

  @Input
  public abstract ListProperty<String> getExcludeClasses();

  protected AbstractBytecodeTask() {
    getIncludeTests().convention(getBoolean("includeTests", true));
    getFailOnError().convention(getBoolean("failOnError", true));
    getStrictBlindParameters().convention(getBoolean("strictBlindParameters", true));
    getLogException().convention(getBoolean("logException", true));
    getForce().convention(getBoolean("force", false));
    getExcludePackages().convention(Collections.emptyList());
    getExcludeClasses().convention(Collections.emptyList());
  }

  protected abstract boolean checkOnlyMode();

  protected void onResult(@Nonnull VerificationResult result) throws IOException {
    // Subclasses such as BytecodeReportTask may override to emit reports
  }

  @Nonnull
  public List<File> resolveTargetClassLocations() {
    Set<File> targets = new LinkedHashSet<>();
    for (File explicit : getTargetFiles().getFiles()) {
      if (explicit != null && explicit.exists()) {
        targets.add(explicit);
      }
    }
    SourceSetContainer sourceSets = getProject().getExtensions().findByType(SourceSetContainer.class);
    boolean includeTests = getIncludeTests().get();
    if (sourceSets != null) {
      for (SourceSet sourceSet : sourceSets) {
        String name = sourceSet.getName().toLowerCase(Locale.ROOT);
        if (!includeTests && name.contains("test")) {
          continue;
        }
        // Automatically picks up Java, Kotlin, Scala, Groovy, and custom JVM class output directories
        for (File classesDir : sourceSet.getOutput().getClassesDirs().getFiles()) {
          if (classesDir != null && classesDir.exists()) {
            targets.add(classesDir);
          }
        }
        for (File extraDir : sourceSet.getOutput().getDirs().getFiles()) {
          if (extraDir != null && extraDir.exists()) {
            targets.add(extraDir);
          }
        }
      }
    }
    return new ArrayList<>(targets);
  }

  @Nonnull
  public List<File> resolveClasspathLocations() {
    Set<File> classpath = new LinkedHashSet<>();
    String[] configs = getIncludeTests().get()
        ? new String[]{"compileClasspath", "runtimeClasspath", "testCompileClasspath", "testRuntimeClasspath"}
        : new String[]{"compileClasspath", "runtimeClasspath"};
    for (String configName : configs) {
      Configuration config = getProject().getConfigurations().findByName(configName);
      if (config != null && config.isCanBeResolved()) {
        for (File file : config.getFiles()) {
          if (file != null && file.exists()) {
            classpath.add(file);
          }
        }
      }
    }
    classpath.addAll(resolveTargetClassLocations());
    return new ArrayList<>(classpath);
  }

  private boolean hasRefCountCore() {
    if (!getTargetFiles().isEmpty()) {
      return true;
    }
    for (Configuration config : getProject().getConfigurations()) {
      boolean foundInDeps = config.getAllDependencies().stream().anyMatch(dep ->
          ("com.simiacryptus".equals(dep.getGroup()) && "refcount-core".equals(dep.getName()))
              || "refcount-core".equals(dep.getName())
      );
      if (foundInDeps) {
        return true;
      }
    }
    for (File dir : resolveTargetClassLocations()) {
      if (new File(dir, "com/simiacryptus/ref/lang/ReferenceCounting.class").exists()) {
        return true;
      }
    }
    return false;
  }

  @TaskAction
  public void executeTask() {
    if (!hasRefCountCore() && !getForce().get()) {
      getLogger().warn("Dependency not found: refcount-core (skipping {}; set force=true to run anyway)", getName());
      return;
    }
    try {
      VerifierConfig config = VerifierConfig.builder()
          .checkOnly(checkOnlyMode())
          .strictBlindParameters(getStrictBlindParameters().get())
          .excludePackages(getExcludePackages().get())
          .excludeClasses(getExcludeClasses().get())
          .classLoader(getClass().getClassLoader())
          .build();

      BytecodeVerifier verifier = new BytecodeVerifier(config);
      VerificationResult result = verifier.analyze(resolveTargetClassLocations(), resolveClasspathLocations());
      getLogger().lifecycle("{}", result.formatSummary());
      onResult(result);

      if (getFailOnError().get() && result.hasErrors()) {
        BytecodeVerificationException ex = new BytecodeVerificationException(result);
        if (getLogException().get()) {
          ERR.println(ex.getMessage());
        }
        throw ex;
      }
    } catch (BytecodeVerificationException e) {
      throw e;
    } catch (IOException e) {
      if (getLogException().get()) {
        e.printStackTrace(ERR);
      }
      throw new RuntimeException("Failed to execute bytecode verification", e);
    }
  }

  protected static boolean getBoolean(@Nonnull String key, boolean defaultValue) {
    return Boolean.parseBoolean(System.getProperty(key, Boolean.toString(defaultValue)));
  }
}
