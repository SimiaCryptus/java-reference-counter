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

package com.simiacryptus.ref.wrappers;

import com.simiacryptus.ref.lang.RefAware;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Function;

public interface RefFunction<F, T> extends Function<F, T> {
  @Nonnull
  @Override
  @RefAware
  T apply(@RefAware F f);

  default <V> RefFunction<F, V> andThen(RefFunction<? super T, ? extends V> after) {
    Objects.requireNonNull(after);
    return (F t) -> after.apply(apply(t));
  }

}
