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

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * A RefTreeMap is a TreeMap with inner KeyValue pairs.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public class RefTreeMap<K, V> extends RefAbstractMap<K, V> implements RefNavigableMap<K, V> {
  @Nonnull
  private final TreeMap<K, KeyValue<K, V>> inner;

  public RefTreeMap() {
    this.inner = new TreeMap<>();
  }

  public RefTreeMap(@Nonnull @RefAware Map<? extends K, ? extends V> values) {
    this();
    putAll(values);
  }

  /**
   * Returns the inner TreeMap.
   *
   * @return the inner TreeMap
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public TreeMap<K, KeyValue<K, V>> getInner() {
    return inner;
  }

  /**
   * @return the entry with the greatest key less than the specified key, or
   * {@code null} if there is no such key
   * @throws ClassCastException   if the specified key cannot be compared with the
   *                              keys currently in the map
   * @throws NullPointerException if the specified key is null and the map
   *                              does not permit null keys
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Entry<K, V> lowerEntry(@RefAware K key) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   *
   * @param key the key
   * @return the lower key, or {@code null} if there is no such key
   * @throws UnsupportedOperationException if this operation is not supported
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public K lowerKey(@RefAware K key) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   *
   * @param key the key
   * @return the entry with the greatest key less than or equal to {@code key},
   * or {@code null} if there is no such key
   * @throws UnsupportedOperationException if the {@code floorEntry} operation
   *                                       is not supported by this map
   * @docgenVersion 9
   * @since 1.6
   */
  @Nonnull
  @Override
  public Entry<K, V> floorEntry(@RefAware K key) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   *
   * @param key the key
   * @return the floor key
   * @throws UnsupportedOperationException if this operation is not supported
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public K floorKey(@RefAware K key) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   *
   * @param key the key
   * @return the corresponding entry
   * @throws UnsupportedOperationException if the {@code ceilingEntry} operation
   *                                       is not supported by this map
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Entry<K, V> ceilingEntry(@RefAware K key) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException if this operation is not supported
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public K ceilingKey(@RefAware K key) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return the entry with the least key strictly greater than the given key,
   * or {@code null} if there is no such key
   * @throws UnsupportedOperationException if the {@code higherEntry} operation
   *                                       is not supported by this map
   * @throws ClassCastException            if the specified key cannot be compared
   *                                       with the keys currently in the map
   * @throws NullPointerException          if the specified key is {@code null}
   *                                       and this map does not permit {@code null} keys
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Entry<K, V> higherEntry(@RefAware K key) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   *
   * @param key the key
   * @return the least key greater than {@code key}, or {@code null} if there
   * is no such key
   * @throws UnsupportedOperationException if the {@code higherKey} operation
   *                                       is not supported by this map
   * @docgenVersion 9
   * @see SortedMap#higherKey(Object)
   */
  @Nonnull
  @Override
  public K higherKey(@RefAware K key) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   *
   * @return the first entry in the map, or {@code null} if the map is empty
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Entry<K, V> firstEntry() {
    return wrap(inner.firstEntry());
  }

  /**
   * Returns the last (highest) entry in this map.
   *
   * @return the last entry in this map
   * @throws NoSuchElementException if this map is empty
   * @docgenVersion 9
   */
  @Nonnull
  public Entry<K, V> lastEntry() {
    return wrap(inner.lastEntry());
  }

  /**
   * @return the entry corresponding to the first key in the sorted map,
   * or {@code null} if the sorted map is empty
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Entry<K, V> pollFirstEntry() {
    return wrap(inner.pollFirstEntry());
  }

  /**
   * {@inheritDoc}
   *
   * @return the last entry in the map, or {@code null} if the map is empty
   * @throws NullPointerException if the specified key is null
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Entry<K, V> pollLastEntry() {
    return wrap(inner.pollLastEntry());
  }

  /**
   * @return a navigable map whose keys are in reverse order
   * @throws UnsupportedOperationException if the operation is not supported
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefNavigableMap<K, V> descendingMap() {
    throw new UnsupportedOperationException();
  }

  /**
   * @return a {@code RefNavigableSet} of the keys contained in this map
   * @throws UnsupportedOperationException if the {@code navigableKeySet} operation
   *                                       is not supported by this map
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefNavigableSet<K> navigableKeySet() {
    throw new UnsupportedOperationException();
  }

  /**
   * @return a navigable set view of the keys contained in this map, in
   * descending key order
   * @throws UnsupportedOperationException if the {@code descendingKeySet}
   *                                       operation is not supported by this map
   * @docgenVersion 9
   * @since 1.8
   */
  @Nonnull
  @Override
  public RefNavigableSet<K> descendingKeySet() {
    throw new UnsupportedOperationException();
  }

  /**
   * @param fromKey       the key from which the submap starts
   * @param fromInclusive whether the key from which the submap starts should be included in the submap
   * @param toKey         the key at which the submap ends
   * @param toInclusive   whether the key at which the submap ends should be included in the submap
   * @return a navigable map that is a submap of this map
   * @throws UnsupportedOperationException always
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefNavigableMap<K, V> subMap(@RefAware K fromKey, boolean fromInclusive,
                                      @RefAware K toKey, boolean toInclusive) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @docgenVersion 9
   * @deprecated Unsupported operation
   */
  @Nonnull
  @Override
  public RefNavigableMap<K, V> headMap(@RefAware K toKey, boolean inclusive) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException {@inheritDoc}
   * @docgenVersion 9
   * @see NavigableMap#tailMap(Object, boolean)
   */
  @Nonnull
  @Override
  public RefNavigableMap<K, V> tailMap(@RefAware K fromKey, boolean inclusive) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return a comparator that compares the keys in this map
   * @docgenVersion 9
   */
  @Override
  public Comparator<? super K> comparator() {
    return inner.comparator();
  }

  /**
   * @throws UnsupportedOperationException always
   * @docgenVersion 9
   * @see NavigableMap#subMap(Object, boolean, Object, boolean)
   * @see SortedMap#subMap(Object, Object)
   */
  @Nonnull
  @Override
  public RefSortedMap<K, V> subMap(@RefAware K fromKey,
                                   @RefAware K toKey) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return a view of the portion of this map whose keys are strictly less than {@code toKey}
   * @throws ClassCastException       if {@code toKey} is not compatible with this map's comparator (or, if the map has no comparator, if {@code toKey} does not implement {@link Comparable}).
   * @throws NullPointerException     if {@code toKey} is null
   * @throws IllegalArgumentException if this map itself has a restricted range, and {@code toKey} lies outside the bounds of the range
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefSortedMap<K, V> headMap(@RefAware K toKey) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return a view of the portion of this map whose keys are greater than or equal to {@code fromKey}
   * @throws UnsupportedOperationException if this operation is not supported
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefSortedMap<K, V> tailMap(@RefAware K fromKey) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return the first (lowest) key in this map
   * @throws UnsupportedOperationException if this map is empty
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public K firstKey() {
    throw new UnsupportedOperationException();
  }

  /**
   * @return the last (highest) key currently in this sorted map
   * @throws UnsupportedOperationException if this operation is not supported by the map
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public K lastKey() {
    throw new UnsupportedOperationException();
  }

  /**
   * @param entry the entry to wrap
   * @return a map entry wrapping the specified entry
   * @throws NullPointerException if entry is null
   * @docgenVersion 9
   */
  @Nonnull
  private Map.Entry<K, V> wrap(@Nonnull Entry<K, KeyValue<K, V>> entry) {
    final KeyValue<K, V> keyValue = entry.getValue();
    return new RefEntry<K, V>(RefUtil.addRef(keyValue.key), RefUtil.addRef(keyValue.value)) {
      /**
       * @param value the value to be set
       * @return the old value of the property
       * @throws UnsupportedOperationException if the operation is not supported
       *
       *   @docgenVersion 9
       */
      @Nonnull
      @Override
      public Object setValue(@RefAware Object value) {
        throw new UnsupportedOperationException();
      }
    };
  }
}
