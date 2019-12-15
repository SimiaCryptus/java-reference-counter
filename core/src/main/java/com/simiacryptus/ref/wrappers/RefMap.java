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
import com.simiacryptus.ref.lang.ReferenceCounting;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The interface Ref map.
 *
 * @param <K> the type parameter
 * @param <V> the type parameter
 */
@RefAware
@RefIgnore
public interface RefMap<K, V> extends ReferenceCounting, Map<K, V> {

  /**
   * Add refs ref map [ ].
   *
   * @param <K>   the type parameter
   * @param <V>   the type parameter
   * @param array the array
   * @return the ref map [ ]
   */
  public static <K, V> RefMap<K, V>[] addRefs(@NotNull RefMap<K, V>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefMap::addRef)
        .toArray((x) -> new RefMap[x]);
  }

  RefMap<K, V> addRef();

  @Override
  default V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    V value = get(RefUtil.addRef(key));
    if (value == null) {
      value = mappingFunction.apply(RefUtil.addRef(key));
      if (value != null) {
        RefUtil.freeRef(mappingFunction);
        RefUtil.freeRef(put(key, RefUtil.addRef(value)));
        return value;
      }
    }
    RefUtil.freeRef(mappingFunction);
    RefUtil.freeRef(key);
    return value;
  }

  @NotNull
  @Override
  RefSet<Entry<K, V>> entrySet();

  default void forEach(BiConsumer<? super K, ? super V> action) {
    final RefSet<Entry<K, V>> entries = entrySet();
    entries.forEach(entry -> {
      final K key = entry.getKey();
      final V value = entry.getValue();
      RefUtil.freeRef(entry);
      action.accept(key, value);
    });
    entries.freeRef();
  }

  @NotNull
  @Override
  RefSet<K> keySet();

  @Override
  default V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> fn) {
    V oldValue = get(RefUtil.addRef(key));
    V newValue;
    if (oldValue == null) {
      newValue = value;
    } else {
      newValue = fn.apply(oldValue, value);
    }
    RefUtil.freeRef(fn);
    if (newValue == null) {
      RefUtil.freeRef(remove(key));
    } else {
      RefUtil.freeRef(put(key, RefUtil.addRef(newValue)));
    }
    return newValue;
  }

  @NotNull
  @Override
  RefCollection<V> values();
}
