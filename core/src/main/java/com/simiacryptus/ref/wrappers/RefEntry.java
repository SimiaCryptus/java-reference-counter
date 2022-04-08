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
import java.util.Map;

/**
 * This class represents an entry in a reference map.
 *
 * @param <K> the type of the key
 * @param <V> the type of the value
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public abstract class RefEntry<K, V> extends ReferenceCountingBase implements Map.Entry<K, V> {

  protected final K key;
  protected V value;

  public RefEntry(@Nonnull Map.Entry<K, V> inner) {
    this(RefUtil.addRef(inner.getKey()), RefUtil.addRef(inner.getValue()));
  }

  public RefEntry(@RefAware K key, @RefAware V value) {
    this.key = key;
    this.value = value;
  }

  /**
   * @return the key, or null if it does not exist
   * @docgenVersion 9
   */
  @Nullable
  @Override
  @RefAware
  public K getKey() {
    return RefUtil.addRef(key);
  }

  /**
   * @return the value, or null if there is no value
   * @docgenVersion 9
   */
  @Nullable
  @Override
  @RefAware
  public V getValue() {
    return RefUtil.addRef(value);
  }

  /**
   * @Nullable
   * @Override
   * @RefAware public abstract V setValue(@RefAware V value);
   * @docgenVersion 9
   */
  @Nullable
  @Override
  @RefAware
  public abstract V setValue(@RefAware V value);

  /**
   * Frees resources used by this object.
   *
   * @docgenVersion 9
   * @see Ref#freeRef()
   */
  @Override
  protected void _free() {
    RefUtil.freeRef(value);
    RefUtil.freeRef(key);
    super._free();
  }

}
