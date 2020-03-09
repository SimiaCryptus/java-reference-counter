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
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

@RefIgnore
@SuppressWarnings("unused")
public class RefComparator<T> extends ReferenceCountingBase implements Comparator<T> {

  private final Comparator<T> inner;
  private final RefArrayList watched = new RefArrayList();

  public RefComparator(@Nonnull @RefAware Comparator<T> inner) {
    this.inner = inner;
  }

  public RefComparator() {
    this.inner = null;
  }

  @Nonnull
  public static <T extends Comparable<T>> RefComparator<? super T> naturalOrder() {
    return new RefComparator<>(Comparable::compareTo);
  }

  @Nonnull
  public static <T, U extends Comparable<? super U>> RefComparator<? super T> comparing(
      @Nonnull @RefAware Function<? super T, ? extends U> fn) {
    return new RefComparator(Comparator.comparing(fn::apply)).watch(fn);
  }

  @Nonnull
  public static <T> RefComparator<T> comparingInt(
      @Nonnull @RefAware ToIntFunction<? super T> keyExtractor) {
    return new RefComparator(Comparator.comparingInt(keyExtractor)).watch(keyExtractor);
  }

  @Nonnull
  public static <T> RefComparator<T> comparingLong(
      @Nonnull @RefAware ToLongFunction<? super T> keyExtractor) {
    return new RefComparator(Comparator.comparingLong(keyExtractor)).watch(keyExtractor);
  }

  @Nonnull
  public static <T> RefComparator<T> comparingDouble(
      @Nonnull @RefAware ToDoubleFunction<? super T> keyExtractor) {
    return new RefComparator(Comparator.comparingDouble(keyExtractor)).watch(keyExtractor);
  }

  public static <T> RefComparator<T> reversed(RefComparator<T> comparator) {
    return new RefComparator<T>(comparator::compare).watch(comparator);
  }

  public static <T, U> Comparator<T> comparing(
      Function<? super T, ? extends U> keyExtractor,
      Comparator<? super U> keyComparator) {
    return new RefComparator<T>(Comparator.comparing(keyExtractor, keyComparator)).watch(keyComparator).watch(keyExtractor);
  }

  @Override
  public int compare(T o1, T o2) {
    return inner.compare(o1, o2);
  }

  @Override
  public RefComparator<T> reversed() {
    return reversed(addRef());
  }

  @Override
  public synchronized RefComparator<T> addRef() {
    return (RefComparator<T>) super.addRef();
  }

  @Nonnull
  public <U extends Comparable<? super U>> RefComparator<T> thenComparing(
      @Nonnull @RefAware Function<? super T, ? extends U> keyExtractor) {
    return thenComparing(comparing(keyExtractor));
  }

  @Nonnull
  public RefComparator<T> thenComparingInt(
      @Nonnull @RefAware ToIntFunction<? super T> keyExtractor) {
    return thenComparing(comparingInt(keyExtractor));
  }

  @Override
  public <U> Comparator<T> thenComparing(Function<? super T, ? extends U> keyExtractor, Comparator<? super U> keyComparator) {
    return thenComparing(comparing(keyExtractor, keyComparator));
  }

  @Override
  public Comparator<T> thenComparingLong(ToLongFunction<? super T> keyExtractor) {
    return thenComparing(comparingLong(keyExtractor));
  }

  @Override
  public Comparator<T> thenComparingDouble(ToDoubleFunction<? super T> keyExtractor) {
    return thenComparing(comparingDouble(keyExtractor));
  }

  @Nonnull
  public RefComparator<T> thenComparing(
      @Nonnull @RefAware Comparator<? super T> other) {
    return new RefComparator<T>((c1, c2) -> {
      int res = compare(c1, c2);
      return res != 0 ? res : other.compare(c1, c2);
    }).watch(other);
  }

  RefComparator<T> watch(Object obj) {
    watched.add(obj);
    return this;
  }

  @Override
  protected void _free() {
    super._free();
    RefUtil.freeRef(inner);
    watched.freeRef();
  }

}
