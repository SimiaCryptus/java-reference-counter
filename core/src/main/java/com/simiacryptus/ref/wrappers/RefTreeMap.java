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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

@RefAware
@RefIgnore
@SuppressWarnings("unused")
public class RefTreeMap<K, V> extends RefAbstractMap<K, V> implements RefNavigableMap<K, V> {
  @NotNull
  private final TreeMap<K, KeyValue<K, V>> inner;

  public RefTreeMap() {
    this.inner = new TreeMap<>();
  }

  public RefTreeMap(@NotNull Map<? extends K, ? extends V> values) {
    this();
    putAll(values);
  }

  @NotNull
  @Override
  public TreeMap<K, KeyValue<K, V>> getInner() {
    return inner;
  }

  @NotNull
  public static <K, V> RefTreeMap<K, V>[] addRefs(@NotNull RefTreeMap<K, V>[] array) {
    return Arrays.stream(array).filter((x) -> x != null).map(RefTreeMap::addRef)
        .toArray((x) -> new RefTreeMap[x]);
  }

  @Override
  public Entry<K, V> lowerEntry(K key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public K lowerKey(K key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Entry<K, V> floorEntry(K key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public K floorKey(K key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Entry<K, V> ceilingEntry(K key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public K ceilingKey(K key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Entry<K, V> higherEntry(K key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public K higherKey(K key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Entry<K, V> firstEntry() {
    return wrap(inner.firstEntry());
  }

  public Entry<K, V> lastEntry() {
    return wrap(inner.lastEntry());
  }

  @Override
  public Entry<K, V> pollFirstEntry() {
    return wrap(inner.pollFirstEntry());
  }

  @Override
  public Entry<K, V> pollLastEntry() {
    return wrap(inner.pollLastEntry());
  }

  @Override
  public RefNavigableMap<K, V> descendingMap() {
    throw new UnsupportedOperationException();
  }

  @Override
  public RefNavigableSet<K> navigableKeySet() {
    throw new UnsupportedOperationException();
  }

  @Override
  public RefNavigableSet<K> descendingKeySet() {
    throw new UnsupportedOperationException();
  }

  @Override
  public RefNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RefNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RefNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Comparator<? super K> comparator() {
    return inner.comparator();
  }

  @Override
  public RefSortedMap<K, V> subMap(K fromKey, K toKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RefSortedMap<K, V> headMap(K toKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RefSortedMap<K, V> tailMap(K fromKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public K firstKey() {
    throw new UnsupportedOperationException();
  }

  @Override
  public K lastKey() {
    throw new UnsupportedOperationException();
  }

  @NotNull
  private Map.Entry<K, V> wrap(Entry<K, KeyValue<K, V>> entry) {
    final KeyValue<K, V> keyValue = entry.getValue();
    return new RefEntry<K, V>(RefUtil.addRef(keyValue.key), RefUtil.addRef(keyValue.value)) {
      @Override
      public Object setValue(Object value) {
        throw new UnsupportedOperationException();
      }
    };
  }
}
