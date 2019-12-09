/*
 * Copyright (c) 2019 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.ref.lang;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@RefAware
@RefIgnore
public abstract class LazyVal<T extends ReferenceCounting> extends ReferenceCountingBase implements Supplier<T> {
  @Nullable
  private volatile T val = null;

  public static <T extends ReferenceCounting> LazyVal<T> wrap(@NotNull Supplier<T> fn) {
    return new LazyVal<T>() {
      @NotNull
      @Override
      protected T build() {
        return fn.get();
      }
    };
  }

  @Override
  protected void _free() {
    if (null != val) {
      val.freeRef();
      val = null;
    }
    super._free();
  }

  @NotNull
  protected abstract T build();

  @Nullable
  public T get() {
    if (null == val) {
      synchronized (this) {
        if (null == val) {
          val = build();
        }
      }
    }
    val.addRef();
    return val;
  }
}
