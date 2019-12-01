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

import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public abstract class RefAbstractMap<K, V> extends ReferenceCountingBase implements RefMap<K, V>, Cloneable, Serializable {
  @Override
  protected void _free() {
    clear();
    super._free();
  }

  @NotNull
  public @Override
  RefHashMap<K, V> addRef() {
    return (RefHashMap<K, V>) super.addRef();
  }

  @Override
  public synchronized void clear() {
    getInner().forEach((k, v) -> {
      RefUtil.freeRef(k);
      RefUtil.freeRef(v);
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
    final boolean containsValue = getInner().containsValue(value);
    RefUtil.freeRef(value);
    return containsValue;
  }

  @NotNull
  @Override
  public RefHashSet<Entry<K, V>> entrySet() {
    final RefHashSet<Entry<K, V>> refSet = new RefHashSet<>();
    getInner().entrySet().stream().map(x -> new RefEntry<K, V>(x)).forEach(refSet::add);
    return refSet;
  }

  @Override
  public V get(Object key) {
    final V value = RefUtil.addRef(getInner().get(key));
    RefUtil.freeRef(key);
    return value;
  }

  public abstract Map<K, V> getInner();

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
    return getInner().put(key, value);
  }

  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> m) {
    if (m instanceof ReferenceCounting) {
      final Set<? extends Entry<? extends K, ? extends V>> entrySet = m.entrySet();
      entrySet.stream().forEach(t -> {
        final V put = put(t.getKey(), t.getValue());
        RefUtil.freeRef(put);
        RefUtil.freeRef(t);
      });
      RefUtil.freeRef(entrySet);
      ((ReferenceCounting) m).freeRef();
    } else {
      m.forEach((k, v) -> put(RefUtil.addRef(k), RefUtil.addRef(v)));
    }
  }

  @Override
  public V remove(Object key) {
    final V removed = getInner().remove(key);
    RefUtil.freeRef(key);
    return removed;
  }

  @Override
  public int size() {
    return getInner().size();
  }

  @NotNull
  @Override
  public RefHashSet<V> values() {
    return new RefHashSet<V>(getInner().values());
  }
}
