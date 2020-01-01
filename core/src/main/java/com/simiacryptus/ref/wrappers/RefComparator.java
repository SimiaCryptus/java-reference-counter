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

/**
 * The type Ref comparator.
 *
 * @param <T> the type parameter
 */
@RefAware
@RefIgnore
@SuppressWarnings("unused")
public class RefComparator<T> implements Comparator<T> {

  private final Comparator<T> inner;

  /**
   * Instantiates a new Ref comparator.
   *
   * @param inner the inner
   */
  public RefComparator(Comparator<T> inner) {
    this.inner = inner;
  }

  /**
   * Natural order comparator.
   *
   * @param <T> the type parameter
   * @return the comparator
   */
  @NotNull
  public static <T extends Comparable<T>> Comparator<? super T> naturalOrder() {
    return (a, b) -> {
      final int result = a.compareTo(RefUtil.addRef(b));
      RefUtil.freeRef(a);
      RefUtil.freeRef(b);
      return result;
    };
  }

  /**
   * Comparing ref comparator.
   *
   * @param <T> the type parameter
   * @param <U> the type parameter
   * @param fn  the fn
   * @return the ref comparator
   */
  @NotNull
  public static <T, U extends Comparable<? super U>> RefComparator<? super T> comparing(@NotNull Function<? super T, ? extends U> fn) {
    return new RefComparator<>((a, b) -> fn.apply(a).compareTo(RefUtil.addRef(fn.apply(b))));
  }

  /**
   * Comparing int ref comparator.
   *
   * @param <T>          the type parameter
   * @param keyExtractor the key extractor
   * @return the ref comparator
   */
  @NotNull
  public static <T> RefComparator<T> comparingInt(@NotNull ToIntFunction<? super T> keyExtractor) {
    return new RefComparator<>(Comparator.comparingInt(keyExtractor));
  }

  /**
   * Comparing long ref comparator.
   *
   * @param <T>          the type parameter
   * @param keyExtractor the key extractor
   * @return the ref comparator
   */
  @NotNull
  public static <T> RefComparator<T> comparingLong(@NotNull ToLongFunction<? super T> keyExtractor) {
    return new RefComparator<>(Comparator.comparingLong(keyExtractor));
  }

  /**
   * Comparing double ref comparator.
   *
   * @param <T>          the type parameter
   * @param keyExtractor the key extractor
   * @return the ref comparator
   */
  @NotNull
  public static <T> RefComparator<T> comparingDouble(@NotNull ToDoubleFunction<? super T> keyExtractor) {
    return new RefComparator<>(Comparator.comparingDouble(keyExtractor));
  }

  @NotNull
  public RefComparator<T> thenComparingInt(@NotNull ToIntFunction<? super T> keyExtractor) {
    return new RefComparator<>(thenComparing(comparingInt(keyExtractor)));
  }

  @NotNull
  public RefComparator<T> thenComparing(@NotNull Comparator<? super T> other) {
    return new RefComparator<>((c1, c2) -> {
      int res = compare(c1, c2);
      return (res != 0) ? res : other.compare(c1, c2);
    });
  }

  @Override
  public int compare(T o1, T o2) {
    return inner.compare(o1, o2);
  }
}
