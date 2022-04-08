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

package com.simiacryptus.lang;

import com.simiacryptus.ref.lang.RefIgnore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * This class represents a lazy value.
 *
 * @param <T> the type of the lazy value
 * @docgenVersion 9
 */
@RefIgnore
public abstract class LazyVal<T> implements Supplier<T> {
  @Nullable
  private volatile T val = null;

  /**
   * @param fn  A function that supplies a value of type {@code T}
   * @param <T> The type of the value supplied by {@code fn}
   * @return A {@link LazyVal} that wraps the given function {@code fn}
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> LazyVal<T> wrap(
      @Nonnull Supplier<T> fn) {
    return new LazyVal<T>() {
      /**
       * Builds the object.
       *
       * @return the built object
       *
       *   @docgenVersion 9
       */
      @Nonnull
      @Override
      protected T build() {
        return fn.get();
      }
    };
  }

  /**
   * Returns the value, if it exists. If it does not exist, it will create it.
   *
   * @return the value
   * @docgenVersion 9
   */
  @Nullable
  public T get() {
    if (null == val) {
      synchronized (this) {
        if (null == val) {
          val = build();
        }
      }
    }
    return val;
  }

  /**
   * @return the built object
   * @throws NullPointerException if the built object is null
   * @docgenVersion 9
   */
  @Nonnull
  protected abstract T build();
}
