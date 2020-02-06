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
import com.simiacryptus.ref.lang.ReferenceCounting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

@RefIgnore
@SuppressWarnings("unused")
public interface RefMap<K, V> extends ReferenceCounting, Map<K, V> {

  @Nonnull
  RefMap<K, V> addRef();

  @Nullable
  @Override
  @RefAware
  default V computeIfAbsent(@RefAware K key, @Nonnull @RefAware Function<? super K, ? extends V> mappingFunction) {
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

  @Nonnull
  @Override
  RefSet<Entry<K, V>> entrySet();

  void forEach(@Nonnull @RefAware BiConsumer<? super K, ? super V> action);

  @Nonnull
  @Override
  RefSet<K> keySet();

  @Override
  @RefAware
  default V merge(@RefAware K key, @RefAware V value,
                  @Nonnull @RefAware BiFunction<? super V, ? super V, ? extends V> fn) {
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

  @Nonnull
  @Override
  RefCollection<V> values();

  @Override
  void putAll(@RefAware @Nonnull Map<? extends K, ? extends V> m);

  @Override
  boolean containsKey(@RefAware Object key);

  @Override
  @RefAware
  boolean containsValue(@RefAware Object value);

  @Nullable
  @Override
  @RefAware
  V get(@RefAware Object key);

  @Nullable
  @Override
  @RefAware
  V put(@RefAware K key, @RefAware V value);

  @Override
  @RefAware
  V remove(@RefAware Object key);

  @Nullable
  @Override
  @RefAware
  V getOrDefault(@RefAware Object key,
                         @RefAware V defaultValue);

  @Override
  void replaceAll(@RefAware BiFunction<? super K, ? super V, ? extends V> function);

  @Nullable
  @Override
  @RefAware
  default V putIfAbsent(@RefAware K key, @RefAware V value) {
    throw new RuntimeException("Not Implemented");
  }

  @Override
  default boolean remove(@RefAware Object key,
                         @RefAware Object value) {
    throw new RuntimeException("Not Implemented");
  }

  @Override
  default boolean replace(@RefAware K key, @RefAware V oldValue,
                          @RefAware V newValue) {
    throw new RuntimeException("Not Implemented");
  }

  @Nullable
  @Override
  @RefAware
  default V replace(@RefAware K key, @RefAware V value) {
    throw new RuntimeException("Not Implemented");
  }

  @Nullable
  @Override
  @RefAware
  default V computeIfPresent(@RefAware K key,
                             @RefAware BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    throw new RuntimeException("Not Implemented");
  }

  @Nullable
  @Override
  @RefAware
  default V compute(@RefAware K key,
                    @RefAware BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    throw new RuntimeException("Not Implemented");
  }
}
