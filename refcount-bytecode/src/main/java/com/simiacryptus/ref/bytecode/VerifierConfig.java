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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@RefIgnore
public final class VerifierConfig {
  private final boolean checkOnly;
  private final boolean strictBlindParameters;
  private final boolean verifySyntheticLambdas;
  @Nonnull
  private final Set<String> excludedPackagePrefixes;
  @Nonnull
  private final Set<String> excludedClasses;
  @Nullable
  private final ClassLoader classLoader;

  private VerifierConfig(Builder builder) {
    this.checkOnly = builder.checkOnly;
    this.strictBlindParameters = builder.strictBlindParameters;
    this.verifySyntheticLambdas = builder.verifySyntheticLambdas;
    this.excludedPackagePrefixes = Collections.unmodifiableSet(new LinkedHashSet<>(builder.excludedPackagePrefixes));
    this.excludedClasses = Collections.unmodifiableSet(new LinkedHashSet<>(builder.excludedClasses));
    this.classLoader = builder.classLoader;
  }

  @Nonnull
  public static VerifierConfig defaults() {
    return builder().build();
  }

  @Nonnull
  public static Builder builder() {
    return new Builder();
  }

  public boolean isCheckOnly() {
    return checkOnly;
  }

  public boolean isStrictBlindParameters() {
    return strictBlindParameters;
  }

  public boolean isVerifySyntheticLambdas() {
    return verifySyntheticLambdas;
  }

  @Nonnull
  public Set<String> getExcludedPackagePrefixes() {
    return excludedPackagePrefixes;
  }

  @Nonnull
  public Set<String> getExcludedClasses() {
    return excludedClasses;
  }

  @Nullable
  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public boolean isExcluded(@Nonnull String internalClassName) {
    String binaryName = internalClassName.replace('/', '.');
    if (excludedClasses.contains(binaryName) || excludedClasses.contains(internalClassName)) {
      return true;
    }
    for (String prefix : excludedPackagePrefixes) {
      String normalized = prefix.endsWith(".") ? prefix : prefix + ".";
      if (binaryName.startsWith(normalized) || binaryName.equals(prefix)) {
        return true;
      }
    }
    return false;
  }

  @RefIgnore
  public static final class Builder {
    private boolean checkOnly = false;
    private boolean strictBlindParameters = true;
    private boolean verifySyntheticLambdas = true;
    private final Set<String> excludedPackagePrefixes = new LinkedHashSet<>();
    private final Set<String> excludedClasses = new LinkedHashSet<>();
    private ClassLoader classLoader = VerifierConfig.class.getClassLoader();

    @Nonnull
    public Builder checkOnly(boolean checkOnly) {
      this.checkOnly = checkOnly;
      return this;
    }

    @Nonnull
    public Builder strictBlindParameters(boolean strictBlindParameters) {
      this.strictBlindParameters = strictBlindParameters;
      return this;
    }

    @Nonnull
    public Builder verifySyntheticLambdas(boolean verifySyntheticLambdas) {
      this.verifySyntheticLambdas = verifySyntheticLambdas;
      return this;
    }

    @Nonnull
    public Builder excludePackages(@Nonnull Collection<String> prefixes) {
      this.excludedPackagePrefixes.addAll(prefixes);
      return this;
    }

    @Nonnull
    public Builder excludePackage(@Nonnull String prefix) {
      this.excludedPackagePrefixes.add(prefix);
      return this;
    }

    @Nonnull
    public Builder excludeClasses(@Nonnull Collection<String> classNames) {
      this.excludedClasses.addAll(classNames);
      return this;
    }

    @Nonnull
    public Builder excludeClass(@Nonnull String className) {
      this.excludedClasses.add(className);
      return this;
    }

    @Nonnull
    public Builder classLoader(@Nullable ClassLoader classLoader) {
      this.classLoader = classLoader;
      return this;
    }

    @Nonnull
    public VerifierConfig build() {
      return new VerifierConfig(this);
    }
  }
}
