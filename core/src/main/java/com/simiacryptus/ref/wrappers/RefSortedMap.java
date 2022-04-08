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
import java.util.SortedMap;

/**
 * This is the RefSortedMap interface.
 *
 * @docgenVersion 9
 */
public interface RefSortedMap<K, V> extends SortedMap<K, V>, RefMap<K, V> {
  /**
   * Returns a view of the portion of this map whose keys range from
   * {@code fromKey}, inclusive, to {@code toKey}, exclusive.  (If
   * {@code fromKey} and {@code toKey} are equal, the returned map
   * is empty.)  The returned map is backed by this map, so changes
   * in the returned map are reflected in this map, and vice-versa.
   * The returned map supports all optional map operations that this
   * map supports.
   *
   * <p>The returned map will throw an {@code IllegalArgumentException}
   * on an attempt to insert a key outside of its range, or to construct
   * a submap either of whose endpoints lie outside its range.
   *
   * @param fromKey low endpoint (inclusive) of the keys in the returned map
   * @param toKey   high endpoint (exclusive) of the keys in the returned map
   * @return a view of the portion of this map whose keys range from
   * {@code fromKey}, inclusive, to {@code toKey}, exclusive
   * @throws ClassCastException       if {@code fromKey} and {@code toKey}
   *                                  cannot be compared to one another using this map's comparator
   *                                  (or, if the map has no comparator, using natural ordering).
   *                                  Implementations may, but are not required to, throw this
   *                                  exception if {@code fromKey} or {@code toKey} cannot be
   *                                  compared to keys currently in the map.
   * @throws NullPointerException     if {@code fromKey} or {@code toKey}
   *                                  is null and this map does not permit null keys
   * @throws IllegalArgumentException if some property of {@code fromKey}
   *                                  or {@code toKey} prevents it from being compared to keys
   *                                  currently in the map
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefSortedMap<K, V> subMap(@RefAware K fromKey, @RefAware K toKey);

  /**
   * Returns a view of the portion of this map whose keys are strictly less than {@code toKey}. The returned map is backed by this map, so changes in the returned map are reflected in this map, and vice-versa. The returned map supports all optional map operations that this map supports.
   *
   * <p>The returned map will throw an {@code IllegalArgumentException} on an attempt to insert a key outside its range ({@code null} or {@code toKey}).
   *
   * @param toKey high endpoint (exclusive) of the keys in the returned map
   * @return a view of the portion of this map whose keys are strictly less than {@code toKey}
   * @throws ClassCastException       if {@code toKey} is not compatible with this map's comparator (or, if the map has no comparator, if {@code toKey} does not implement {@link Comparable}).
   * @throws NullPointerException     if {@code toKey} is {@code null} and this map does not permit {@code null} keys
   * @throws IllegalArgumentException if this map itself has a restricted range, and {@code toKey} lies outside the bounds of the range
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefSortedMap<K, V> headMap(@RefAware K toKey);

  /**
   * Returns a view of the portion of this map whose keys are greater than or equal to {@code fromKey}. The returned map is backed by this map, so changes in the returned map are reflected in this map, and vice-versa. The returned map supports all optional map operations that this map supports.
   *
   * <p>The returned map will throw an {@code IllegalArgumentException} on an attempt to insert a key less than {@code fromKey} or a key that is equal to {@code fromKey} and the map uses natural ordering, or its comparator does not permit keys less than {@code fromKey}.
   *
   * @param fromKey low endpoint (inclusive) of the keys in the returned map
   * @return a view of the portion of this map whose keys are greater than or equal to {@code fromKey}
   * @throws ClassCastException       if {@code fromKey} is not compatible with this map's comparator (or, if the map has no comparator, if {@code fromKey} does not implement {@link Comparable}).
   * @throws NullPointerException     if {@code fromKey} is null and this map does not permit null keys
   * @throws IllegalArgumentException if this map itself has a restricted range, and {@code fromKey} lies outside the bounds of the range
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefSortedMap<K, V> tailMap(@RefAware K fromKey);
}
