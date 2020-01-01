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
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The type Ref linked hash map.
 *
 * @param <K> the type parameter
 * @param <V> the type parameter
 */
@RefAware
@RefIgnore
@SuppressWarnings("unused")
public class RefLinkedHashMap<K, V> extends RefAbstractMap<K, V> {
  @NotNull
  private final Map<K, KeyValue<K, V>> inner;

  /**
   * Instantiates a new Ref linked hash map.
   */
  public RefLinkedHashMap() {
    this.inner = new LinkedHashMap<>();
  }

  /**
   * Instantiates a new Ref linked hash map.
   *
   * @param values the values
   */
  public RefLinkedHashMap(@NotNull Map<? extends K, ? extends V> values) {
    this();
    putAll(values);
  }

  @NotNull
  @Override
  public Map<K, KeyValue<K, V>> getInner() {
    return inner;
  }

  /**
   * Add refs ref linked hash map [ ].
   *
   * @param <K>   the type parameter
   * @param <V>   the type parameter
   * @param array the array
   * @return the ref linked hash map [ ]
   */
  @NotNull
  public static <K, V> RefLinkedHashMap<K, V>[] addRefs(@NotNull RefLinkedHashMap<K, V>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefLinkedHashMap::addRef)
        .toArray((x) -> new RefLinkedHashMap[x]);
  }

}