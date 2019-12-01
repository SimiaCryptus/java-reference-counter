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
import com.simiacryptus.ref.lang.RefUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@RefAware
public class RefHashMap<K, V> extends RefAbstractMap<K, V> {
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
  public Map<K, V> getInner() {
    return inner;
  }

}
