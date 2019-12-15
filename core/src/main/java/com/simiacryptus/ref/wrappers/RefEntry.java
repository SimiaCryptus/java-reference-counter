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

import java.util.Map;

/**
 * The type Ref entry.
 *
 * @param <K> the type parameter
 * @param <V> the type parameter
 */
@RefAware
@RefIgnore
public abstract class RefEntry<K, V> extends ReferenceCountingBase implements Map.Entry<K, V> {

  private final K key;
  private V value;

  /**
   * Instantiates a new Ref entry.
   *
   * @param inner the inner
   */
  public RefEntry(Map.Entry<K, V> inner) {
    this(RefUtil.addRef(inner.getKey()), RefUtil.addRef(inner.getValue()));
  }

  /**
   * Instantiates a new Ref entry.
   *
   * @param key   the key
   * @param value the value
   */
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
  public abstract V setValue(V value);

}
