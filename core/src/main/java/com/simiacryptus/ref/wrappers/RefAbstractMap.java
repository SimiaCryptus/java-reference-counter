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

import com.simiacryptus.ref.lang.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;

@RefIgnore
@SuppressWarnings("unused")
public abstract class RefAbstractMap<K, V> extends ReferenceCountingBase
    implements RefMap<K, V>, Cloneable, Serializable {

  @Nonnull
  protected abstract Map<K, KeyValue<K, V>> getInner();

  @Override
  public boolean isEmpty() {
    assertAlive();
    return getInner().isEmpty();
  }

  @Nonnull
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
  public boolean containsKey(@RefAware Object key) {
    assertAlive();
    final boolean containsKey = getInner().containsKey(key);
    RefUtil.freeRef(key);
    return containsKey;
  }

  @Override
  public boolean containsValue(@RefAware Object value) {
    assertAlive();
    final boolean containsValue = getInner().values().stream().anyMatch(x -> x.value.equals(value));
    RefUtil.freeRef(value);
    return containsValue;
  }

  @Nonnull
  @Override
  public RefHashSet<Entry<K, V>> entrySet() {
    assertAlive();
    final RefHashSet<Entry<K, V>> refSet = new RefHashSet<>();
    final Map<K, KeyValue<K, V>> inner = getInner();
    assert !(inner instanceof ReferenceCounting);
    inner.values().stream().map(x -> new MapEntry(x)).forEach(refSet::add);
    return refSet;
  }

  @Nullable
  @Override
  public V get(@RefAware Object key) {
    assertAlive();
    final KeyValue<K, V> keyValue = getInner().get(key);
    RefUtil.freeRef(key);
    return RefUtil.addRef(null == keyValue ? null : keyValue.value);
  }

  @Nonnull
  @Override
  public RefSet<K> keySet() {
    assertAlive();
    return new RefHashSet<>(getInner().keySet());
  }

  @Override
  public V put(@RefAware K key, @RefAware V value) {
    assertAlive();
    final KeyValue<K, V> put = getInner().put(key, new KeyValue<>(key, value));
    if (null == put)
      return null;
    RefUtil.freeRef(put.key);
    return put.value;
  }

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
    m.forEach((k, v) -> {
      RefUtil.freeRef(put(k, v));
    });
    RefUtil.freeRef(m);
  }

  @Override
  public V remove(@RefAware Object key) {
    assertAlive();
    final KeyValue<K, V> removed = getInner().remove(key);
    if (null != removed) {
      RefUtil.freeRef(removed.key);
    }
    RefUtil.freeRef(key);
    assert removed != null;
    return removed.value;
  }

  @Override
  public int size() {
    assertAlive();
    return getInner().size();
  }

  @Nonnull
  @Override
  public RefHashSet<V> values() {
    assertAlive();
    final RefHashSet<V> hashSet = new RefHashSet<>();
    getInner().values().forEach(x -> {
      V value = x.value;
      hashSet.add(RefUtil.addRef(value));
    });
    return hashSet;
  }

  @Override
  protected void _free() {
    clear();
    super._free();
  }

  @RefIgnore
  protected static class KeyValue<K, V> {
    public final K key;
    public final V value;

    public KeyValue(@RefAware K key, @RefAware V value) {
      this.key = key;
      this.value = value;
    }
  }

  private class MapEntry extends RefEntry<K, V> {
    private final KeyValue<K, V> x;

    public MapEntry(KeyValue<K, V> x) {
      super(RefUtil.addRef(x.key), RefUtil.addRef(x.value));
      this.x = x;
    }

    @Nullable
    @Override
    public V setValue(@RefAware V value) {
      return put(RefUtil.addRef(x.key), value);
    }
  }
}
