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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

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

  @Nonnull
  @Override
  public TreeMap<K, KeyValue<K, V>> getInner() {
    return inner;
  }

  @Nonnull
  public static <K, V> RefTreeMap<K, V>[] addRefs(@Nonnull RefTreeMap<K, V>[] array) {
    return Arrays.stream(array).filter((x) -> x != null).map(RefTreeMap::addRef).toArray((x) -> new RefTreeMap[x]);
  }

  @Nonnull
  @Override
  public Entry<K, V> lowerEntry(@RefAware K key) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public K lowerKey(@RefAware K key) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public Entry<K, V> floorEntry(@RefAware K key) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public K floorKey(@RefAware K key) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public Entry<K, V> ceilingEntry(@RefAware K key) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public K ceilingKey(@RefAware K key) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public Entry<K, V> higherEntry(@RefAware K key) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public K higherKey(@RefAware K key) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public Entry<K, V> firstEntry() {
    return wrap(inner.firstEntry());
  }

  @Nonnull
  public Entry<K, V> lastEntry() {
    return wrap(inner.lastEntry());
  }

  @Nonnull
  @Override
  public Entry<K, V> pollFirstEntry() {
    return wrap(inner.pollFirstEntry());
  }

  @Nonnull
  @Override
  public Entry<K, V> pollLastEntry() {
    return wrap(inner.pollLastEntry());
  }

  @Nonnull
  @Override
  public RefNavigableMap<K, V> descendingMap() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RefNavigableSet<K> navigableKeySet() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RefNavigableSet<K> descendingKeySet() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RefNavigableMap<K, V> subMap(@RefAware K fromKey, boolean fromInclusive,
                                      @RefAware K toKey, boolean toInclusive) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RefNavigableMap<K, V> headMap(@RefAware K toKey, boolean inclusive) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RefNavigableMap<K, V> tailMap(@RefAware K fromKey, boolean inclusive) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Comparator<? super K> comparator() {
    return inner.comparator();
  }

  @Nonnull
  @Override
  public RefSortedMap<K, V> subMap(@RefAware K fromKey,
                                   @RefAware K toKey) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RefSortedMap<K, V> headMap(@RefAware K toKey) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RefSortedMap<K, V> tailMap(@RefAware K fromKey) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public K firstKey() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public K lastKey() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  private Map.Entry<K, V> wrap(@Nonnull Entry<K, KeyValue<K, V>> entry) {
    final KeyValue<K, V> keyValue = entry.getValue();
    return new RefEntry<K, V>(RefUtil.addRef(keyValue.key), RefUtil.addRef(keyValue.value)) {
      @Nonnull
      @Override
      public Object setValue(@RefAware Object value) {
        throw new UnsupportedOperationException();
      }
    };
  }
}
