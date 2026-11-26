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

package com.simiacryptus.ref.bytecode.model;

import com.simiacryptus.ref.lang.RefIgnore;

import javax.annotation.Nonnull;

@RefIgnore
public final class MethodContract {
  private final boolean resolved;
  private final boolean methodRefIgnore;
  private final boolean returnRefAware;
  private final boolean mustCall;
  private final boolean varargs;
  @Nonnull
  private final boolean[] paramRefAware;
  @Nonnull
  private final boolean[] paramRefIgnore;
  @Nonnull
  private final boolean[] paramSynthetic;

  public MethodContract(
      boolean resolved,
      boolean methodRefIgnore,
      boolean returnRefAware,
      boolean mustCall,
      boolean varargs,
      @Nonnull boolean[] paramRefAware,
      @Nonnull boolean[] paramRefIgnore,
      @Nonnull boolean[] paramSynthetic
  ) {
    this.resolved = resolved;
    this.methodRefIgnore = methodRefIgnore;
    this.returnRefAware = returnRefAware;
    this.mustCall = mustCall;
    this.varargs = varargs;
    this.paramRefAware = paramRefAware;
    this.paramRefIgnore = paramRefIgnore;
    this.paramSynthetic = paramSynthetic;
  }

  public boolean isResolved() {
    return resolved;
  }

  public boolean isMethodRefIgnore() {
    return methodRefIgnore;
  }

  public boolean isReturnRefAware() {
    return returnRefAware;
  }

  public boolean isMustCall() {
    return mustCall;
  }

  public boolean isVarargs() {
    return varargs;
  }

  public boolean isParamRefAware(int index) {
    if (paramRefAware.length == 0) return false;
    int idx = Math.min(index, paramRefAware.length - 1);
    return idx >= 0 && paramRefAware[idx];
  }

  public boolean isParamRefIgnore(int index) {
    if (paramRefIgnore.length == 0) return false;
    int idx = Math.min(index, paramRefIgnore.length - 1);
    return idx >= 0 && paramRefIgnore[idx];
  }

  public boolean isParamSynthetic(int index) {
    return index >= 0 && index < paramSynthetic.length && paramSynthetic[index];
  }
}
