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
import java.util.NavigableSet;

/**
 * This is the RefNavigableSet interface.
 *
 * @docgenVersion 9
 */
public interface RefNavigableSet<T> extends NavigableSet<T>, RefSet<T> {
  /**
   * Returns a reverse order view of the elements contained in this set.
   * The descending set is backed by this set, so changes to the set are
   * reflected in the descending set, and vice-versa.  If either set is
   * modified while an iteration over a collection view of either set is
   * in progress (except through the iterator's own {@code remove}
   * operation), the results of the iteration are undefined.
   *
   * @return a reverse order view of this set
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefNavigableSet<T> descendingSet();

  /**
   * Returns a view of the portion of this set whose elements range from
   * {@code fromElement} to {@code toElement}.  If {@code fromElement} and
   * {@code toElement} are equal, the returned set is empty unless {@code
   * fromInclusive} and {@code toInclusive} are both true.  The returned set
   * is backed by this set, so changes in the returned set are reflected in
   * this set, and vice-versa.  The returned set supports all optional set
   * operations that this set supports.
   *
   * <p>The returned set will throw an {@code IllegalArgumentException}
   * on an attempt to insert an element outside its range.
   *
   * @param fromElement   low endpoint (inclusive) of the returned set
   * @param fromInclusive {@code true} if the low endpoint
   *                      is to be included in the returned view
   * @param toElement     high endpoint (inclusive) of the returned set
   * @param toInclusive   {@code true} if the high endpoint
   *                      is to be included in the returned view
   * @return a view of the portion of this set whose elements range from
   * {@code fromElement}, inclusive, to {@code toElement}, inclusive
   * @throws ClassCastException       if {@code fromElement} or {@code toElement}
   *                                  cannot be compared with one another using this set's comparator
   *                                  (or, if the set has no comparator, using natural ordering).
   *                                  Implementations may, but are not required to, throw this
   *                                  exception if {@code fromElement} or {@code toElement} cannot be
   *                                  compared with elements of the set.
   * @throws NullPointerException     if {@code fromElement} or {@code toElement}
   *                                  is null and this set does not permit null elements
   * @throws IllegalArgumentException if some property of {@code fromElement}
   *                                  or {@code toElement} prevents it from being compared with elements
   *                                  of the set
   * @docgenVersion 9
   * @see #comparator()
   */
  @Nonnull
  @Override
  RefNavigableSet<T> subSet(@RefAware T fromElement, boolean fromInclusive,
                            @RefAware T toElement, boolean toInclusive);

  /**
   * Returns a view of the portion of this set whose elements are less than (or equal to, if {@code inclusive} is true) {@code toElement}.
   * The returned set is backed by this set, so changes in the returned set are reflected in this set, and vice-versa.
   * The returned set supports all optional set operations that this set supports.
   *
   * @param toElement high endpoint (exclusive) of the returned set
   * @param inclusive true if the high endpoint is to be included in the returned view
   * @return a view of the portion of this set whose elements are less than (or equal to, if {@code inclusive} is true) {@code toElement}
   * @throws ClassCastException       if {@code toElement} is not compatible with this set's comparator (or, if the set has no comparator, if {@code toElement} does not implement {@link Comparable}).
   * @throws NullPointerException     if {@code toElement} is null and this set does not permit null elements
   * @throws IllegalArgumentException if this set itself has a restricted range, and {@code toElement} lies outside the bounds of the range
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefNavigableSet<T> headSet(@RefAware T toElement, boolean inclusive);

  /**
   * Returns a view of the portion of this set whose elements are greater than or equal to {@code fromElement}. The returned set is backed by this set, so changes in the returned set are reflected in this set, and vice-versa. The returned set supports all optional set operations that this set supports.
   *
   * <p>The returned set will throw an {@code IllegalArgumentException} on an attempt to insert an element outside its range ({@code fromElement} and {@code toElement}).
   *
   * @param fromElement low endpoint (inclusive) of the returned set
   * @param inclusive   {@code true} if the low endpoint is to be included in the returned view
   * @return a view of the portion of this set whose elements are greater than or equal to {@code fromElement}
   * @throws ClassCastException       if {@code fromElement} is not compatible with this set's comparator (or, if the set has no comparator, if {@code fromElement} does not implement {@link Comparable}).
   * @throws NullPointerException     if {@code fromElement} is null and this set does not permit null elements
   * @throws IllegalArgumentException if this set itself has a restricted range, and {@code fromElement} lies outside the bounds of the range
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefNavigableSet<T> tailSet(@RefAware T fromElement, boolean inclusive);

  /**
   * Returns a view of the portion of this set whose elements range from
   * {@code fromElement}, inclusive, to {@code toElement}, exclusive.  (If
   * {@code fromElement} and {@code toElement} are equal, the returned set is
   * empty.)  The returned set is backed by this set, so changes in the
   * returned set are reflected in this set, and vice-versa.  The returned set
   * supports all optional set operations that this set supports.
   *
   * <p>The returned set will throw an {@code IllegalArgumentException}
   * on an attempt to insert an element outside its range.
   *
   * @param fromElement low endpoint (inclusive) of the returned set
   * @param toElement   high endpoint (exclusive) of the returned set
   * @return a view of the specified range within this set
   * @throws ClassCastException       if {@code fromElement} or {@code toElement}
   *                                  cannot be compared with one another using this set's comparator
   *                                  (or, if the set has no comparator, using natural ordering).
   *                                  Implementations may, but are not required to, throw this
   *                                  exception if {@code fromElement} or {@code toElement} cannot be
   *                                  compared with one another.
   * @throws NullPointerException     if {@code fromElement} or {@code toElement}
   *                                  is null and this set does not permit null elements
   * @throws IllegalArgumentException if some property of {@code fromElement}
   *                                  or {@code toElement} prevents it from being compared with one
   *                                  another in this set, or if {@code fromElement} is greater than
   *                                  {@code toElement}; or if this set itself has a restricted
   *                                  range, and {@code fromElement} or {@code toElement} lies
   *                                  outside the bounds of the range
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefSortedSet<T> subSet(@RefAware T fromElement,
                         @RefAware T toElement);

  /**
   * Returns a view of the portion of this set whose elements are strictly less than toElement.
   * The returned set is backed by this set, so changes in the returned set are reflected in this set, and vice-versa.
   * The returned set supports all optional set operations that this set supports.
   *
   * @param toElement - high endpoint (exclusive) of the returned set
   * @return a view of the portion of this set whose elements are strictly less than toElement
   * @throws ClassCastException       - if toElement is not compatible with this set's comparator (or, if the set has no comparator, if toElement does not implement Comparable).
   * @throws NullPointerException     - if toElement is null and this set does not permit null elements
   * @throws IllegalArgumentException - if this set itself has a restricted range, and toElement lies outside the bounds of the range
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefSortedSet<T> headSet(@RefAware T toElement);

  /**
   * Returns a view of the portion of this set whose elements are greater than or equal to {@code fromElement}. The returned set is backed by this set, so changes in the returned set are reflected in this set, and vice-versa. The returned set supports all optional set operations that this set supports.
   *
   * <p>The returned set will throw an {@code IllegalArgumentException} on an attempt to insert an element outside its range ({@code fromElement} to {@code null}).
   *
   * @param fromElement low endpoint (inclusive) of the returned set
   * @return a view of the portion of this set whose elements are greater than or equal to {@code fromElement}
   * @throws ClassCastException       if {@code fromElement} is not compatible with this set's comparator (or, if the set has no comparator, if {@code fromElement} does not implement {@link Comparable}).
   * @throws NullPointerException     if {@code fromElement} is null and this set does not permit null elements
   * @throws IllegalArgumentException if this set itself has a restricted range, and {@code fromElement} lies outside the bounds of the range
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefSortedSet<T> tailSet(@RefAware T fromElement);

  /**
   * {@inheritDoc}
   *
   * <p>The default implementation creates a {@link RefSpliterator}
   * that traverses the elements in this set.
   *
   * @return a {@code RefSpliterator} over the elements in this set
   * @docgenVersion 9
   * @since 1.8
   */
  @Override
  default RefSpliterator<T> spliterator() {
    return RefSet.super.spliterator();
  }
}
