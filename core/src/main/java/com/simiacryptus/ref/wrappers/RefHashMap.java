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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RefAware
@RefIgnore
@SuppressWarnings("unused")
public class RefHashMap<K, V> extends RefAbstractMap<K, V> {
  @NotNull
  private final Map<K, KeyValue<K, V>> inner;

  public RefHashMap() {
    this.inner = new HashMap<>();
  }

  public RefHashMap(int length) {
    this.inner = new HashMap<>(length);
  }

  public RefHashMap(@NotNull Map<? extends K, ? extends V> values) {
    this();
    putAll(values);
  }

  @NotNull
  @Override
  public Map<K, KeyValue<K, V>> getInner() {
    return inner;
  }

  @NotNull
  public static <K, V> RefHashMap<K, V>[] addRefs(@NotNull RefHashMap<K, V>[] array) {
    return Arrays.stream(array).filter((x) -> x != null).map(RefHashMap::addRef)
        .toArray((x) -> new RefHashMap[x]);
  }

}
