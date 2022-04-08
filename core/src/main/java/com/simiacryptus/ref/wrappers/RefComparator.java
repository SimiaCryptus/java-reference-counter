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

/**
 * This class is a comparator that uses a reference array list.
 *
 * @param inner   The comparator to use.
 * @param watched The reference array list.
 * @docgenVersion 9
 */
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

  /**
   * Returns a {@link RefComparator} that compares {@link Comparable} objects in the
   * natural order.
   *
   * @param <T> the type of the objects that this comparator can compare
   * @return a {@link RefComparator} that compares {@link Comparable} objects in the
   * natural order
   * @docgenVersion 9
   */
  @Nonnull
  public static <T extends Comparable<T>> RefComparator<? super T> naturalOrder() {
    return new RefComparator<>(Comparable::compareTo);
  }

  /**
   * Returns a {@link RefComparator} that compares {@link Ref} objects by using the
   * given {@link Function} to extract a {@link Comparable} sort key from each {@link Ref}.
   *
   * <p>The returned comparator is {@link Serializable}.
   *
   * @param <T> the type of the {@link Ref} objects to be compared
   * @param <U> the type of the sort key
   * @param fn  the {@link Function} used to extract the sort key
   * @return a {@link RefComparator} that compares {@link Ref} objects by using the
   * given {@link Function} to extract a {@link Comparable} sort key
   * @throws NullPointerException if the given {@link Function} is {@code null}
   * @docgenVersion 9
   */
  @Nonnull
  public static <T, U extends Comparable<? super U>> RefComparator<? super T> comparing(
      @Nonnull @RefAware Function<? super T, ? extends U> fn) {
    return new RefComparator(Comparator.comparing(fn::apply)).watch(fn);
  }

  /**
   * Returns a {@link RefComparator} comparing {@linkplain Comparable natural
   * ordering} on an {@code int} extracted by specified
   * {@linkplain ToIntFunction key extractor function}.
   *
   * <p>The returned comparator is {@linkplain Serializable} if the specified
   * key extractor function and the comparator it produces are both
   * {@linkplain Serializable}.
   *
   * @param <T>          the type of the element to be compared
   * @param keyExtractor the function used to extract the {@code int} sort key
   * @return a comparator that compares {@linkplain Comparable natural ordering}
   * on an {@code int} extracted by the supplied {@code keyExtractor}
   * @throws NullPointerException if the argument is null
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> RefComparator<T> comparingInt(
      @Nonnull @RefAware ToIntFunction<? super T> keyExtractor) {
    return new RefComparator(Comparator.comparingInt(keyExtractor)).watch(keyExtractor);
  }

  /**
   * Returns a {@link RefComparator} comparing {@linkplain Comparable natural
   * ordering} on an extracted long value.
   *
   * <p>The returned comparator is {@linkplain Serializable} if the specified
   * key extractor is also {@linkplain Serializable}.
   *
   * @param <T>          the type of element to be compared
   * @param keyExtractor the long value to be compared
   * @return a comparator that compares long values
   * @throws NullPointerException if the argument is null
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> RefComparator<T> comparingLong(
      @Nonnull @RefAware ToLongFunction<? super T> keyExtractor) {
    return new RefComparator(Comparator.comparingLong(keyExtractor)).watch(keyExtractor);
  }

  /**
   * Returns a {@link RefComparator} comparing {@link Double} objects extracted by given {@link ToDoubleFunction}.
   *
   * @param keyExtractor the {@link ToDoubleFunction} to extract a {@link Double} sort key
   * @param <T>          the type of the objects to be compared
   * @return a {@link RefComparator} comparing the sort keys
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> RefComparator<T> comparingDouble(
      @Nonnull @RefAware ToDoubleFunction<? super T> keyExtractor) {
    return new RefComparator(Comparator.comparingDouble(keyExtractor)).watch(keyExtractor);
  }

  /**
   * Returns a {@link RefComparator} that reverses the order of the given
   * {@link RefComparator}. If the given {@link RefComparator} is {@code null},
   * then the returned {@link RefComparator} is equivalent to {@link
   * Collections#reverseOrder()}.
   *
   * @param <T>        the type of the objects that may be compared by the given
   *                   {@link RefComparator}
   * @param comparator the {@link RefComparator} to be reversed, or {@code null}
   * @return a {@link RefComparator} that reverses the order of the given
   * {@link RefComparator}
   * @docgenVersion 9
   * @see Collections#reverseOrder()
   */
  public static <T> RefComparator<T> reversed(RefComparator<T> comparator) {
    return new RefComparator<T>(comparator::compare).watch(comparator);
  }

  /**
   * Returns a comparator that compares {@linkplain Comparable natural ordering} of {@code keyExtractor} applied to the
   * input elements. The comparator is serializable if the specified key extractor and comparator are both serializable.
   *
   * <p>The returned comparator is equivalent to {@code comparing(keyExtractor, naturalOrder())}.
   *
   * @param <T>          the type of the input elements
   * @param <U>          the type of the sort key
   * @param keyExtractor the function used to extract the sort key
   * @return a comparator that compares by an extracted key in the natural order
   * @throws NullPointerException if the argument is null
   * @docgenVersion 9
   */
  public static <T, U> Comparator<T> comparing(
      Function<? super T, ? extends U> keyExtractor,
      Comparator<? super U> keyComparator) {
    return new RefComparator<T>(Comparator.comparing(keyExtractor, keyComparator)).watch(keyComparator).watch(keyExtractor);
  }

  /**
   * @Override public int compare(T o1, T o2) {
   * return inner.compare(o1, o2);
   * }
   * @docgenVersion 9
   */
  @Override
  public int compare(T o1, T o2) {
    return inner.compare(o1, o2);
  }

  /**
   * Returns a comparator that compares {@link Ref} objects in the
   * reverse order of the given comparator.
   *
   * @param addRef the comparator to be reversed
   * @return a comparator that compares {@link Ref} objects in the
   * reverse order of the given comparator
   * @docgenVersion 9
   */
  @Override
  public RefComparator<T> reversed() {
    return reversed(addRef());
  }

  /**
   * Adds a reference to this RefComparator.
   *
   * @return a RefComparator with a reference added
   * @docgenVersion 9
   */
  @Override
  public synchronized RefComparator<T> addRef() {
    return (RefComparator<T>) super.addRef();
  }

  /**
   * Returns a comparator that compares by the given key extractor function,
   * then compares the results using the given comparator.
   *
   * @param keyExtractor  the function to extract the key
   * @param keyComparator the comparator to compare the keys
   * @return the comparator
   * @docgenVersion 9
   */
  @Nonnull
  public <U extends Comparable<? super U>> RefComparator<T> thenComparing(
      @Nonnull @RefAware Function<? super T, ? extends U> keyExtractor) {
    return thenComparing(comparing(keyExtractor));
  }

  /**
   * Returns a comparator that compares by the given key function,
   * then compares the results using the given comparator.
   *
   * @param keyExtractor  the function to extract the sort key
   * @param keyComparator the comparator to compare sort keys
   * @return the resulting comparator
   * @throws NullPointerException if the argument is null
   * @docgenVersion 9
   */
  @Nonnull
  public RefComparator<T> thenComparingInt(
      @Nonnull @RefAware ToIntFunction<? super T> keyExtractor) {
    return thenComparing(comparingInt(keyExtractor));
  }

  /**
   * Returns a comparator that compares by the given key function and then compares by the given key comparator if the first comparison is a tie.
   *
   * @param keyExtractor  the given key function
   * @param keyComparator the given key comparator
   * @return a comparator that compares by the given key function and then compares by the given key comparator if the first comparison is a tie
   * @docgenVersion 9
   */
  @Override
  public <U> Comparator<T> thenComparing(Function<? super T, ? extends U> keyExtractor, Comparator<? super U> keyComparator) {
    return thenComparing(comparing(keyExtractor, keyComparator));
  }

  /**
   * Returns a comparator that compares by the given long key function and
   * then by this comparator.
   *
   * @param keyExtractor the long key function
   * @return a comparator that compares by the given long key function and
   * then by this comparator
   * @throws NullPointerException if the argument is null
   * @docgenVersion 9
   */
  @Override
  public Comparator<T> thenComparingLong(ToLongFunction<? super T> keyExtractor) {
    return thenComparing(comparingLong(keyExtractor));
  }

  /**
   * Returns a comparator that compares by the given key function,
   * then compares the Double results of the given key function by their natural order.
   *
   * @param keyExtractor the given key function
   * @return a comparator that compares by the given key function,
   * then compares the Double results of the given key function by their natural order
   * @throws NullPointerException if the argument is null
   * @docgenVersion 9
   */
  @Override
  public Comparator<T> thenComparingDouble(ToDoubleFunction<? super T> keyExtractor) {
    return thenComparing(comparingDouble(keyExtractor));
  }

  /**
   * Returns a comparator that compares two objects using the first comparator,
   * and if the result of that comparison is zero, uses the second comparator.
   *
   * @param other the other comparator to be used when the first comparator
   *              compares two objects that are equal
   * @return a comparator that compares two objects with the comparators in the
   * order that they are provided
   * @docgenVersion 9
   */
  @Nonnull
  public RefComparator<T> thenComparing(
      @Nonnull @RefAware Comparator<? super T> other) {
    return new RefComparator<T>((c1, c2) -> {
      int res = compare(c1, c2);
      return res != 0 ? res : other.compare(c1, c2);
    }).watch(other);
  }

  /**
   * RefComparator#watch(Object obj)
   *
   * @param obj the object to watch
   * @return this RefComparator
   * @docgenVersion 9
   */
  RefComparator<T> watch(Object obj) {
    watched.add(obj);
    return this;
  }

  /**
   * Frees the resources held by this object. This method calls the {@link #_free()} method of its superclass, then calls {@link RefUtil#freeRef(Object)} on the {@link #inner} field, and finally calls {@link Ref#freeRef()} on the {@link #watched} field.
   *
   * @docgenVersion 9
   */
  @Override
  protected void _free() {
    super._free();
    RefUtil.freeRef(inner);
    watched.freeRef();
  }

}
