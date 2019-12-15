/*
 * Copyright (c) 2019 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Map;

/**
 * The type Ref abstract map.
 *
 * @param <K> the type parameter
 * @param <V> the type parameter
 */
@RefAware
@RefIgnore
public abstract class RefAbstractMap<K, V> extends ReferenceCountingBase implements RefMap<K, V>, Cloneable, Serializable {

  @Override
  protected void _free() {
    clear();
    super._free();
  }

  @NotNull
  public @Override
  RefAbstractMap<K, V> addRef() {
    return (RefAbstractMap<K, V>) super.addRef();
  }

  @Override
  public synchronized void clear() {
    getInner().forEach((k, v) -> {
      RefUtil.freeRef(v.key);
      RefUtil.freeRef(v.value);
    });
    getInner().clear();
  }

  @Override
  public boolean containsKey(Object key) {
    final boolean containsKey = getInner().containsKey(key);
    RefUtil.freeRef(key);
    return containsKey;
  }

  @Override
  public boolean containsValue(Object value) {
    final boolean containsValue = getInner().values().stream().anyMatch(x -> x.value.equals(value));
    RefUtil.freeRef(value);
    return containsValue;
  }

  @NotNull
  @Override
  public RefHashSet<Entry<K, V>> entrySet() {
    final RefHashSet<Entry<K, V>> refSet = new RefHashSet<>();
    getInner().values().stream().map(x -> new RefEntry<K, V>(RefUtil.addRef(x.key), RefUtil.addRef(x.value)) {
      @Override
      public V setValue(V value) {
        return put(RefUtil.addRef(x.key), value);
      }
    }).forEach(refSet::add);
    return refSet;
  }

  @Override
  public V get(Object key) {
    final KeyValue<K, V> keyValue = getInner().get(key);
    RefUtil.freeRef(key);
    return RefUtil.addRef(null == keyValue ? null : keyValue.value);
  }

  /**
   * Gets inner.
   *
   * @return the inner
   */
  protected abstract Map<K, KeyValue<K, V>> getInner();

  @Override
  public boolean isEmpty() {
    return getInner().isEmpty();
  }

  @NotNull
  @Override
  public RefSet<K> keySet() {
    return new RefHashSet<>(getInner().keySet());
  }

  @Override
  public V put(K key, V value) {
    final KeyValue<K, V> put = getInner().put(key, new KeyValue<>(key, value));
    if (null == put) return null;
    RefUtil.freeRef(put.key);
    return put.value;
  }

  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> m) {
    final Map<? extends K, ? extends V> m_inner;
    if (m instanceof RefAbstractMap) {
      m_inner = ((RefAbstractMap) m).getInner();
    } else {
      m_inner = m;
    }
    m_inner.forEach((k, v) -> {
      RefUtil.freeRef(put(RefUtil.addRef(k), RefUtil.addRef(v)));
    });
    RefUtil.freeRef(m);
  }

  @Override
  public V remove(Object key) {
    final KeyValue<K, V> removed = getInner().remove(key);
    if (null != removed) {
      RefUtil.freeRef(removed.key);
    }
    RefUtil.freeRef(key);
    return removed.value;
  }

  @Override
  public int size() {
    return getInner().size();
  }

  @NotNull
  @Override
  public RefHashSet<V> values() {
    final RefHashSet<V> hashSet = new RefHashSet<>();
    getInner().values().forEach(x -> hashSet.add(x.value));
    return hashSet;
  }

  /**
   * The type Key value.
   *
   * @param <K> the type parameter
   * @param <V> the type parameter
   */
  protected static class KeyValue<K, V> {
    /**
     * The Key.
     */
    public final K key;
    /**
     * The Value.
     */
    public final V value;

    /**
     * Instantiates a new Key value.
     *
     * @param key   the key
     * @param value the value
     */
    public KeyValue(K key, V value) {
      this.key = key;
      this.value = value;
    }
  }

}
