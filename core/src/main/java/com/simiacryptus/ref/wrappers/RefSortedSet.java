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
import java.util.SortedSet;

/**
 * This is the RefSortedSet interface.
 *
 * @docgenVersion 9
 */
public interface RefSortedSet<T> extends SortedSet<T>, RefSet<T> {
  /**
   * @return a view of the portion of this sorted set whose elements range from fromElement, inclusive, to toElement, exclusive
   * @throws ClassCastException   if fromElement or toElement cannot be compared to one another using this sorted set's comparator
   *                              (or, if the sorted set has no comparator, using natural ordering).
   * @throws NullPointerException if fromElement or toElement is null
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefSortedSet<T> subSet(@RefAware T fromElement,
                         @RefAware T toElement);

  /**
   * @return a view of the portion of this sorted set whose elements are strictly less than {@code toElement}
   * @throws ClassCastException   if {@code toElement} is not compatible
   *                              with this sorted set's comparator
   * @throws NullPointerException if {@code toElement} is null
   *                              and this sorted set does not permit null elements
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefSortedSet<T> headSet(@RefAware T toElement);

  /**
   * {@inheritDoc}
   *
   * @param fromElement the element to be used as the lower bound (inclusive) for the returned set
   * @return a view of the portion of this set whose elements are greater than or equal to {@code fromElement}
   * @throws ClassCastException       if {@code fromElement} is not compatible with this set's comparator (or,
   *                                  if the set has no comparator, if {@code fromElement} does not implement
   *                                  {@link Comparable}).
   *                                  Implementations may, but are not required to, throw this exception if {@code fromElement}
   *                                  cannot be compared to elements currently in the set.
   * @throws NullPointerException     if {@code fromElement} is null and this set does not permit null elements
   *                                  (<a href="Collection.html#optional-restrictions">optional</a>)
   * @throws IllegalArgumentException if some property of {@code fromElement} prevents it from being added to this set
   * @docgenVersion 9
   * @since 1.6
   */
  @Nonnull
  @Override
  RefSortedSet<T> tailSet(@RefAware T fromElement);

  /**
   * {@inheritDoc}
   *
   * <p>The default implementation creates a {@link RefSpliterator}
   * that traverses the elements of this set.
   *
   * @docgenVersion 9
   */
  @Override
  default RefSpliterator<T> spliterator() {
    return RefSet.super.spliterator();
  }
}
