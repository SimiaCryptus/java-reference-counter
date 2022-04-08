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
import java.util.NavigableMap;

/**
 * This is the RefNavigableMap interface.
 *
 * @docgenVersion 9
 */
public interface RefNavigableMap<K, V> extends NavigableMap<K, V>, RefMap<K, V> {

  /**
   * Returns a navigable map whose keys are reverse ordered.
   *
   * @return a navigable map whose keys are reverse ordered
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefNavigableMap<K, V> descendingMap();

  /**
   * Returns a {@link RefNavigableSet} view of the keys contained in this map.
   * The set's iterator returns the keys in ascending order.
   * The set is backed by the map, so changes to the map are
   * reflected in the set, and vice-versa.  If the map is modified
   * while an iteration over the set is in progress (except through
   * the iterator's own {@code remove} operation), the results of
   * the iteration are undefined.  The set supports element removal,
   * which removes the corresponding mapping from the map, via the
   * {@code Iterator.remove}, {@code Set.remove},
   * {@code removeAll}, {@code retainAll}, and {@code clear}
   * operations.  It does not support the {@code add} or {@code addAll}
   * operations.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefNavigableSet<K> navigableKeySet();

  /**
   * Returns a reverse order NavigableSet view of the keys contained in this map.
   *
   * @return a reverse order NavigableSet view of the keys contained in this map
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefNavigableSet<K> descendingKeySet();

  /**
   * Returns a view of the portion of this map whose keys range from
   * {@code fromKey} to {@code toKey}.  If {@code fromKey} and
   * {@code toKey} are equal, the returned map is empty unless
   * {@code fromInclusive} and {@code toInclusive} are both true.  The
   * returned map is backed by this map, so changes in the returned map are
   * reflected in this map, and vice-versa.  The returned map supports all
   * optional map operations that this map supports.
   *
   * <p>The returned map will throw an {@code IllegalArgumentException}
   * on an attempt to insert a key outside of its range, or to construct a
   * submap either of whose endpoints lie outside its range.
   *
   * @param fromKey       low endpoint (inclusive) of the keys in the returned map
   * @param fromInclusive {@code true} if the low endpoint
   *                      is to be included in the returned view
   * @param toKey         high endpoint (inclusive) of the keys in the returned map
   * @param toInclusive   {@code true} if the high endpoint
   *                      is to be included in the returned view
   * @return a view of the portion of this map whose keys range from
   * {@code fromKey} to {@code toKey}
   * @throws ClassCastException       if {@code fromKey} and {@code toKey}
   *                                  cannot be compared to one another using this map's comparator
   *                                  (or, if the map has no comparator, using natural ordering).
   *                                  Implementations may, but are not required to, throw this
   *                                  exception if {@code fromKey} or {@code toKey}
   *                                  cannot be compared to keys currently in the map.
   * @throws NullPointerException     if {@code fromKey} or {@code toKey}
   *                                  is null and this map does not permit null keys
   * @throws IllegalArgumentException if {@code fromKey} is greater than
   *                                  {@code toKey}; or if this map itself has a restricted
   *                                  range, and {@code fromKey} or {@code toKey} lies
   *                                  outside the bounds of the range
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefNavigableMap<K, V> subMap(@RefAware K fromKey, boolean fromInclusive,
                               @RefAware K toKey, boolean toInclusive);

  /**
   * Returns a view of the portion of this map whose keys are less than (or equal to, if {@code inclusive} is true) {@code toKey}. The returned map is backed by this map, so changes in the returned map are reflected in this map, and vice-versa. The returned map supports all optional map operations that this map supports.
   *
   * <p>The returned map will throw an {@code IllegalArgumentException} on an attempt to insert a key outside its range ({@code toKey}).
   *
   * @param toKey     high endpoint of the keys in the returned map
   * @param inclusive {@code true} if the high endpoint
   *                  {@code toKey} is to be included in the returned view
   * @return a view of the portion of this map whose keys are less than
   * or equal to {@code toKey}
   * @throws ClassCastException       if {@code toKey} is not compatible
   *                                  with this map's comparator (or, if the map has no comparator,
   *                                  if {@code toKey} does not implement {@link Comparable}).
   *                                  Implementations may, but are not required to, throw this
   *                                  exception if {@code toKey} cannot be compared to keys
   *                                  currently in the map.
   * @throws NullPointerException     if {@code toKey} is null and
   *                                  this map does not permit null keys
   * @throws IllegalArgumentException if this map itself has a
   *                                  restricted range, and {@code toKey} lies outside the
   *                                  bounds of the range
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefNavigableMap<K, V> headMap(@RefAware K toKey, boolean inclusive);

  /**
   * Returns a view of the portion of this map whose keys are greater than or equal to {@code fromKey}. The returned map is backed by this map, so changes in the returned map are reflected in this map, and vice-versa. The returned map supports all optional map operations that this map supports.
   *
   * <p>The returned map will throw an {@code IllegalArgumentException} on an attempt to insert a key less than {@code fromKey} if {@code fromKey} is not an inclusive lower bound. Similarly, the returned map will throw an {@code IllegalArgumentException} on an attempt to insert a key greater than or equal to {@code fromKey} if {@code fromKey} is an exclusive upper bound.
   *
   * @param fromKey   low endpoint (inclusive) of the keys in the returned map
   * @param inclusive {@code true} if the low endpoint is to be included in the returned view
   * @return a view of the portion of this map whose keys are greater than or equal to {@code fromKey}
   * @throws ClassCastException       if {@code fromKey} is not compatible with this map's comparator (or, if the map has no comparator, if {@code fromKey} does not implement {@link Comparable}).
   * @throws NullPointerException     if {@code fromKey} is null and this map does not permit null keys
   * @throws IllegalArgumentException if this map itself has a restricted range, and {@code fromKey} lies outside the bounds of the range
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefNavigableMap<K, V> tailMap(@RefAware K fromKey, boolean inclusive);

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
   * @throws NullPointerException     if this map does not permit {@code null} keys and {@code toKey} is {@code null}
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
