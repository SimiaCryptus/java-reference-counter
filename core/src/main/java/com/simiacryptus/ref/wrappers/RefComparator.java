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
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

@RefAware
@RefIgnore
@SuppressWarnings("unused")
public interface RefComparator<T> extends Comparator<T> {

  public static <T> RefComparator<T> create(Comparator<T> inner) {
    return inner::compare;
  }

  @NotNull
  public static <T extends Comparable<T>> Comparator<? super T> naturalOrder() {
    return (a, b) -> {
      final int result = a.compareTo(RefUtil.addRef(b));
      RefUtil.freeRef(a);
      RefUtil.freeRef(b);
      return result;
    };
  }

  @NotNull
  public static <T, U extends Comparable<? super U>> RefComparator<? super T> comparing(@NotNull Function<? super T, ? extends U> fn) {
    return RefComparator.create((a, b) -> fn.apply(a).compareTo(RefUtil.addRef(fn.apply(b))));
  }

  @NotNull
  public static <T> RefComparator<T> comparingInt(@NotNull ToIntFunction<? super T> keyExtractor) {
    return RefComparator.create(Comparator.comparingInt(keyExtractor));
  }

  @NotNull
  public static <T> RefComparator<T> comparingLong(@NotNull ToLongFunction<? super T> keyExtractor) {
    return RefComparator.create(Comparator.comparingLong(keyExtractor));
  }

  @NotNull
  public static <T> RefComparator<T> comparingDouble(@NotNull ToDoubleFunction<? super T> keyExtractor) {
    return RefComparator.create(Comparator.comparingDouble(keyExtractor));
  }

  default <U extends Comparable<? super U>> RefComparator<T> thenComparing(Function<? super T, ? extends U> keyExtractor) {
    return thenComparing(comparing(keyExtractor));
  }

  @NotNull
  public default RefComparator<T> thenComparingInt(@NotNull ToIntFunction<? super T> keyExtractor) {
    return RefComparator.create(thenComparing(comparingInt(keyExtractor)));
  }

  @NotNull
  public default RefComparator<T> thenComparing(@NotNull Comparator<? super T> other) {
    return RefComparator.create((c1, c2) -> {
      int res = compare(c1, c2);
      return (res != 0) ? res : other.compare(c1, c2);
    });
  }

}
