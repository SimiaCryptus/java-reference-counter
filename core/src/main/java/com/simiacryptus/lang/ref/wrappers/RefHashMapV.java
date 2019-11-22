package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.ReferenceCounting;
import com.simiacryptus.lang.ref.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RefHashMapV<K, V extends ReferenceCounting> extends ReferenceCountingBase implements Map<K, V>, Cloneable, Serializable {
  private final HashMap<K, V> inner;

  public RefHashMapV() {
    this(new HashMap<>());
  }

  public RefHashMapV(HashMap<K, V> inner) {
    this.inner = inner;
  }

  @Override
  public int size() {
    return inner.size();
  }

  @Override
  public boolean isEmpty() {
    return inner.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return inner.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return inner.containsValue(value);
  }

  @Override
  public V get(Object key) {
    return inner.get(key);
  }

  @Nullable
  @Override
  public V put(K key, V value) {
    return inner.put(key, value);
  }

  @Override
  public V remove(Object key) {
    return inner.remove(key);
  }

  @Override
  public void putAll(@NotNull Map<? extends K, ? extends V> m) {
    inner.putAll(m);
  }

  @Override
  public void clear() {
    inner.clear();
  }

  @NotNull
  @Override
  public Set<K> keySet() {
    return inner.keySet();
  }

  @NotNull
  @Override
  public RefCollection<V> values() {
    return new RefCollection<V>(inner.values());
  }

  @NotNull
  @Override
  public Set<Entry<K, V>> entrySet() {
    return (Set<Entry<K, V>>) (Set<?>) new RefSet<>(inner.entrySet().stream()
        .map(x -> new RefEntry(x)).collect(Collectors.toSet()));
  }

}
