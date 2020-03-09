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

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

@RefIgnore
@SuppressWarnings("unused")
public class RefLinkedHashMap<K, V> extends RefAbstractMap<K, V> {
  @Nonnull
  private final Map<K, KeyValue<K, V>> inner;

  public RefLinkedHashMap() {
    this.inner = new LinkedHashMap<>();
  }

  public RefLinkedHashMap(@Nonnull @RefAware Map<? extends K, ? extends V> values) {
    this();
    putAll(values);
  }

  @Nonnull
  @Override
  public Map<K, KeyValue<K, V>> getInner() {
    return inner;
  }

  @Nonnull
  @Override
  public RefLinkedHashMap<K, V> addRef() {
    return (RefLinkedHashMap<K, V>) super.addRef();
  }
}
