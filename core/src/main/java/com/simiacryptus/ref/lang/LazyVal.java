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

package com.simiacryptus.ref.lang;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

@RefIgnore
public abstract class LazyVal<T extends ReferenceCounting> extends ReferenceCountingBase implements Supplier<T> {
  @Nullable
  private volatile T val = null;

  @Nonnull
  public static <T extends ReferenceCounting> LazyVal<T> wrap(
      @Nonnull @RefAware Supplier<T> fn) {
    return new LazyVal<T>() {
      @Nonnull
      @Override
      protected T build() {
        return fn.get();
      }
    };
  }

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

  @Override
  protected void _free() {
    if (null != val) {
      val.freeRef();
      val = null;
    }
    super._free();
  }

  @Nonnull
  protected abstract T build();
}
