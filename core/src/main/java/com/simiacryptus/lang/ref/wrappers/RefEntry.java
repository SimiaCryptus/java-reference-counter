package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.ReferenceCounting;
import com.simiacryptus.lang.ref.ReferenceCountingBase;

import java.util.Map;

public class RefEntry<K, V> extends ReferenceCountingBase implements Map.Entry<K, V> {

  private final Map.Entry<K, V> inner;

  public RefEntry(Map.Entry<K, V> inner) {
    assert !(inner instanceof ReferenceCounting);
    this.inner = inner;
  }

  @Override
  public K getKey() {
    return inner.getKey();
  }

  @Override
  public V getValue() {
    return inner.getValue();
  }

  @Override
  public V setValue(V value) {
    return inner.setValue(value);
  }

  @Override
  protected void _free() {
    free(inner.getValue());
    free(inner.getKey());
    super._free();
  }

}
