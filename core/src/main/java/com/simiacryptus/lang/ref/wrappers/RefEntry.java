package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.RefUtil;
import com.simiacryptus.lang.ref.ReferenceCountingBase;

import java.util.Map;

public class RefEntry<K, V> extends ReferenceCountingBase implements Map.Entry<K, V> {

  private final K key;
  private V value;

  public RefEntry(Map.Entry<K, V> inner) {
    this(RefUtil.addRef(inner.getKey()), RefUtil.addRef(inner.getValue()));
  }

  public RefEntry(K key, V value) {
    this.key = key;
    this.value = value;
  }

  @Override
  protected void _free() {
    RefUtil.freeRef(value);
    RefUtil.freeRef(key);
    super._free();
  }

  @Override
  public K getKey() {
    return RefUtil.addRef(key);
  }

  @Override
  public V getValue() {
    return RefUtil.addRef(value);
  }

  @Override
  public V setValue(V value) {
    final V oldValue = this.value;
    this.value = value;
    return oldValue;
  }

}
