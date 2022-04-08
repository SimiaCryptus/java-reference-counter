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
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * This class provides a skeletal implementation of the Map
 * interface, to minimize the effort required to implement this interface.
 *
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public abstract class RefAbstractMap<K, V> extends ReferenceCountingBase
    implements RefMap<K, V>, Cloneable, Serializable {

  /**
   * Returns the inner map.
   *
   * @return the inner map
   * @docgenVersion 9
   */
  @Nonnull
  protected abstract Map<K, KeyValue<K, V>> getInner();

  /**
   * @Override public boolean isEmpty() {
   * assertAlive();
   * return getInner().isEmpty();
   * }
   * @docgenVersion 9
   */
  @Override
  public boolean isEmpty() {
    assertAlive();
    return getInner().isEmpty();
  }

  /**
   * Returns a reference to this {@code RefAbstractMap} instance.
   *
   * @return a reference to this {@code RefAbstractMap} instance
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  RefAbstractMap<K, V> addRef() {
    return (RefAbstractMap<K, V>) super.addRef();
  }

  /**
   * Replaces each entry's value with the result of invoking the given
   * function on that entry until all entries have been processed or the
   * function throws an exception.  Exceptions thrown by the function are
   * relayed to the caller.
   *
   * @param function the function to apply to each entry
   * @throws UnsupportedOperationException if this map is unmodifiable
   * @throws ClassCastException            if the class of a replacement value
   *                                       prevents it from being stored in this map
   * @throws NullPointerException          if the specified function is null or
   *                                       the specified replacement value is null
   * @throws ClassCastException            if a replacement value is of an inappropriate
   *                                       type for this map
   *                                       (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
   * @throws NullPointerException          if function or a replacement value is null
   * @throws IllegalArgumentException      if some property of a replacement value
   *                                       prevents it from being stored in this map
   *                                       (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
   * @throws RuntimeException              or Error if the function fails unexpectedly
   * @implSpec The default implementation is equivalent to the following steps for this {@code map},
   * then returning the current value or {@code null} if now absent:
   *
   * <pre> {@code
   * for (Map.Entry<K, V> entry : map.entrySet())
   *     entry.setValue(function.apply(entry.getKey(), entry.getValue()));
   * }</pre>
   *
   * <p>The default implementation makes no guarantees about synchronization
   * or atomicity properties of this method. Any implementation providing
   * atomicity guarantees must override this method and document its
   * concurrency properties. In particular, all implementations of
   * subinterface {@link java.util.concurrent.ConcurrentMap} must document
   * whether the function is applied once atomically only if the value is not
   * present.
   * @docgenVersion 9
   * @since 1.8
   */
  @Override
  public void replaceAll(@RefAware BiFunction<? super K, ? super V, ? extends V> function) {
    RefHashSet<Entry<K, V>> entries = entrySet();
    try {
      entries.forEach(entry -> {
        K key = entry.getKey();
        V value = entry.getValue();
        RefUtil.freeRef(entry);
        RefUtil.freeRef(put(key, function.apply(RefUtil.addRef(key), value)));
      });
    } finally {
      entries.freeRef();
    }
  }


  /**
   * Clears the map.
   *
   * @docgenVersion 9
   */
  @Override
  public synchronized void clear() {
    getInner().forEach((k, v) -> {
      RefUtil.freeRef(v.key);
      RefUtil.freeRef(v.value);
    });
    getInner().clear();
  }

  /**
   * @param key The key to check for
   * @return True if the key is in the map, false otherwise
   * @docgenVersion 9
   */
  @Override
  public boolean containsKey(@RefAware Object key) {
    assertAlive();
    final boolean containsKey = getInner().containsKey(key);
    RefUtil.freeRef(key);
    return containsKey;
  }

  /**
   * {@inheritDoc}
   *
   * @param value the value whose presence in this map is to be tested
   * @return {@code true} if this map maps one or more keys to the specified value
   * @throws ClassCastException   if the value is of an inappropriate type for this map
   *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
   * @throws NullPointerException if the specified value is null and this map does not permit null
   *                              values
   *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
   * @docgenVersion 9
   */
  @Override
  public boolean containsValue(@RefAware Object value) {
    assertAlive();
    final boolean containsValue = getInner().values().stream().anyMatch(x -> x.value.equals(value));
    RefUtil.freeRef(value);
    return containsValue;
  }

  /**
   * Returns a set of entries in the map.
   *
   * @return a set of entries in the map.
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefHashSet<Entry<K, V>> entrySet() {
    assertAlive();
    final RefHashSet<Entry<K, V>> refSet = new RefHashSet<>();
    getInner().values().stream().map(x -> new MapEntry(x)).forEach(o -> refSet.add(o));
    return refSet;
  }

  /**
   * Performs the given action for each entry in this map until all entries
   * have been processed or the action throws an exception.
   * Exceptions thrown by the action are relayed to the caller.
   *
   * @param action The action to be performed for each entry
   * @throws NullPointerException if the specified action is null
   * @docgenVersion 9
   */
  public void forEach(@Nonnull @RefAware BiConsumer<? super K, ? super V> action) {
    try {
      getInner().values().forEach(entry -> {
        action.accept(RefUtil.addRef(entry.key), RefUtil.addRef(entry.value));
      });
    } finally {
      RefUtil.freeRef(action);
    }
  }

  /**
   * @param key The key to look up
   * @return The value associated with the key, or null if the key is not present
   * @docgenVersion 9
   */
  @Nullable
  @Override
  @RefAware
  public V get(@RefAware Object key) {
    assertAlive();
    final KeyValue<K, V> keyValue = getInner().get(key);
    RefUtil.freeRef(key);
    return null == keyValue ? null : RefUtil.addRef(keyValue.value);
  }

  /**
   * Returns a {@link RefSet} of the keys contained in this {@link RefMap}.
   *
   * @return a {@link RefSet} of the keys contained in this {@link RefMap}
   * @throws IllegalStateException if this {@link RefMap} has been {@link #destroy() destroyed}
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefSet<K> keySet() {
    assertAlive();
    return new RefHashSet<>(getInner().keySet());
  }

  /**
   * @Override
   * @RefAware public V put(@RefAware K key, @RefAware V value);
   * @docgenVersion 9
   */
  @Override
  @RefAware
  public V put(@RefAware K key, @RefAware V value) {
    assertAlive();
    final KeyValue<K, V> put = getInner().put(key, new KeyValue<>(key, value));
    if (null == put)
      return null;
    RefUtil.freeRef(put.key);
    if (put.value == value) {
      RefUtil.freeRef(put.value);
      return null;
    }
    return put.value;
  }

  /**
   * @param m the map to be stored in this map
   * @throws NullPointerException if the specified map is null
   * @docgenVersion 9
   * @see Map#putAll(Map)
   */
  @Override
  public void putAll(@Nonnull @RefAware Map<? extends K, ? extends V> m) {
    assertAlive();
//    if (m instanceof RefAbstractMap) {
//      final Map<K, KeyValue<K, V>> m_inner = ((RefAbstractMap<K,V>) m).getInner();
//      m_inner.forEach((k, v) -> {
//        RefUtil.freeRef(put(RefUtil.addRef(k), RefUtil.addRef(v.value)));
//      });
//    } else {
//    }
    try {
      m.forEach((k, v) -> {
        RefUtil.freeRef(put(k, v));
      });
    } finally {
      RefUtil.freeRef(m);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param key {@link RefAware}
   * @return {@link RefAware}
   * @docgenVersion 9
   */
  @Override
  @RefAware
  public V remove(@RefAware Object key) {
    assertAlive();
    final KeyValue<K, V> removed = getInner().remove(key);
    RefUtil.freeRef(key);
    if (null == removed) {
      return null;
    } else {
      RefUtil.freeRef(removed.key);
      return removed.value;
    }
  }

  /**
   * @Override public int size() {
   * assertAlive();
   * return getInner().size();
   * }
   * @docgenVersion 9
   */
  @Override
  public int size() {
    assertAlive();
    return getInner().size();
  }

  /**
   * @return a {@link RefHashSet} of all values in this {@link RefMap}
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefHashSet<V> values() {
    assertAlive();
    final RefHashSet<V> hashSet = new RefHashSet<>();
    getInner().values().forEach(x -> {
      hashSet.add(RefUtil.addRef(x.value));
    });
    return hashSet;
  }

  /**
   * @param key          the key for which the corresponding value is to be returned
   * @param defaultValue the value to be returned in the event that this map contains no mapping for the given key
   * @return the value to which the specified key is mapped, or the default value if this map contains no mapping for the given key
   * @docgenVersion 9
   */
  @Nullable
  @Override
  @RefAware
  public V getOrDefault(@RefAware Object key,
                        @RefAware V defaultValue) {
    Map<K, KeyValue<K, V>> inner = getInner();
    KeyValue<K, V> value = inner.get(key);
    if (null == value) return defaultValue;
    else RefUtil.freeRef(defaultValue);
    return RefUtil.addRef(value.value);
  }

  /**
   * This method clears the data and then calls the super method.
   *
   * @docgenVersion 9
   */
  @Override
  protected void _free() {
    clear();
    super._free();
  }

  /**
   * This class represents a key-value pair.
   *
   * @param <K> the type of the key
   * @param <V> the type of the value
   * @docgenVersion 9
   */
  @RefIgnore
  protected static class KeyValue<K, V> {
    public final K key;
    public final V value;

    public KeyValue(@RefAware K key, @RefAware V value) {
      this.key = key;
      this.value = value;
    }
  }

  /**
   * The MapEntry class represents an entry in a map.
   *
   * @docgenVersion 9
   */
  private class MapEntry extends RefEntry<K, V> {
    public MapEntry(KeyValue<K, V> x) {
      super(RefUtil.addRef(x.key), RefUtil.addRef(x.value));
    }

    /**
     * @param value the value to be set
     * @return the previous value of the specified key, or null if there was no mapping for the key
     * @docgenVersion 9
     */
    @Nullable
    @Override
    public V setValue(@RefAware V value) {
      return put(getKey(), value);
    }
  }
}
