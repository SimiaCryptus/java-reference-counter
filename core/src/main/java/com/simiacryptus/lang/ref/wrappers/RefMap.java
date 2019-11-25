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
import java.util.stream.Collectors;

public class RefMap<K, V> extends ReferenceCountingBase implements Map<K, V>, Cloneable, Serializable {
  private final Map<K, V> inner;

  public RefMap() {
    this.inner = new HashMap<>();
    this.inner.forEach((k, v) -> {
      RefUtil.addRef(k);
      RefUtil.addRef(v);
    });
  }

  @Override
  protected void _free() {
    clear();
    super._free();
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
  public RefSet<Entry<K, V>> entrySet() {
    final Set<Entry<K, V>> entrySet = inner.entrySet().stream()
        .map(x -> new RefEntry<K, V>(x))
        .collect(Collectors.toSet());
    final RefSet<Entry<K, V>> refSet = new RefSet<>(entrySet);
    entrySet.stream().forEach(RefUtil::freeRef);
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
  public Set<K> keySet() {
    return new RefSet<>(inner.keySet());
  }

  @Nullable
  @Override
  public V put(K key, V value) {
    final V replaced = inner.put(key, value);
    if (null != replaced) {
      RefUtil.freeRef(key);
    }
    return replaced;
  }

  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> m) {
    if (m instanceof ReferenceCounting) {
      m.forEach((k, v) -> put(k, v));
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
  public RefSet<V> values() {
    return new RefSet<V>(inner.values());
  }

  public @Override RefMap<K,V> addRef() {
    return (RefMap<K,V>) super.addRef();
  }

  public static <K,V> RefMap<K,V>[] addRefs(RefMap<K,V>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefMap::addRef)
        .toArray((x) -> new RefMap[x]);
  }
}
