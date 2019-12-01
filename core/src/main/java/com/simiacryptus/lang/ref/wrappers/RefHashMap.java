package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.RefUtil;
import com.simiacryptus.lang.ref.ReferenceCounting;
import com.simiacryptus.lang.ref.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RefHashMap<K, V> extends ReferenceCountingBase implements RefMap<K, V>, Cloneable, Serializable {
  @NotNull
  private final Map<K, V> inner;

  public RefHashMap() {
    this.inner = new HashMap<>();
  }

  public RefHashMap(RefHashMap<? extends K, ? extends V> values) {
    this();
    final RefHashSet<? extends Entry<? extends K, ? extends V>> entries = values.entrySet();
    entries.stream().forEach(t -> {
      put(t.getKey(), t.getValue());
      RefUtil.freeRef(t);
    });
    entries.freeRef();
    RefUtil.freeRef(values);
  }

  public static <K, V> RefHashMap<K, V>[] addRefs(@NotNull RefHashMap<K, V>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefHashMap::addRef)
        .toArray((x) -> new RefHashMap[x]);
  }

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
    inner.forEach((k, v) -> {
      RefUtil.freeRef(k);
      RefUtil.freeRef(v);
    });
    inner.clear();
  }

  @Override
  public boolean containsKey(Object key) {
    final boolean containsKey = inner.containsKey(key);
    RefUtil.freeRef(key);
    return containsKey;
  }

  @Override
  public boolean containsValue(Object value) {
    final boolean containsValue = inner.containsValue(value);
    RefUtil.freeRef(value);
    return containsValue;
  }

  @NotNull
  @Override
  public RefHashSet<Entry<K, V>> entrySet() {
    final RefHashSet<Entry<K, V>> refSet = new RefHashSet<>();
    inner.entrySet().stream().map(x -> new RefEntry<K, V>(x)).forEach(refSet::add);
    return refSet;
  }

  @Override
  public V get(Object key) {
    final V value = RefUtil.addRef(inner.get(key));
    RefUtil.freeRef(key);
    return value;
  }

  @Override
  public boolean isEmpty() {
    return inner.isEmpty();
  }

  @NotNull
  @Override
  public RefSet<K> keySet() {
    return new RefHashSet<>(inner.keySet());
  }

  @Nullable
  @Override
  public V put(K key, V value) {
    return inner.put(key, value);
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
    final V removed = inner.remove(key);
    RefUtil.freeRef(key);
    return removed;
  }

  @Override
  public int size() {
    return inner.size();
  }

  @NotNull
  @Override
  public RefHashSet<V> values() {
    return new RefHashSet<V>(inner.values());
  }
}
