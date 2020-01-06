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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

@RefAware
@RefIgnore
@SuppressWarnings("unused")
public interface RefMap<K, V> extends ReferenceCounting, Map<K, V> {

  @NotNull
  public static <K, V> RefMap<K, V>[] addRefs(@NotNull RefMap<K, V>[] array) {
    return Arrays.stream(array).filter((x) -> x != null).map(RefMap::addRef).toArray((x) -> new RefMap[x]);
  }

  @NotNull
  RefMap<K, V> addRef();

  @Override
  default V computeIfAbsent(@RefAware K key, @NotNull @RefAware Function<? super K, ? extends V> mappingFunction) {
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

  default void forEach(@NotNull @RefAware BiConsumer<? super K, ? super V> action) {
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
  default V merge(@RefAware K key, @RefAware V value,
      @NotNull @RefAware BiFunction<? super V, ? super V, ? extends V> fn) {
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

  @Override
  void putAll(@RefAware @NotNull Map<? extends K, ? extends V> m);

  @Override
  boolean containsKey(@com.simiacryptus.ref.lang.RefAware Object key);

  @Override
  boolean containsValue(@com.simiacryptus.ref.lang.RefAware Object value);

  @Override
  V get(@com.simiacryptus.ref.lang.RefAware Object key);

  @Nullable
  @Override
  V put(@com.simiacryptus.ref.lang.RefAware K key, @com.simiacryptus.ref.lang.RefAware V value);

  @Override
  V remove(@com.simiacryptus.ref.lang.RefAware Object key);

  @Override
  default V getOrDefault(@com.simiacryptus.ref.lang.RefAware Object key,
      @com.simiacryptus.ref.lang.RefAware V defaultValue) {
    return null;
  }

  @Override
  default void replaceAll(@com.simiacryptus.ref.lang.RefAware BiFunction<? super K, ? super V, ? extends V> function) {

  }

  @Nullable
  @Override
  default V putIfAbsent(@com.simiacryptus.ref.lang.RefAware K key, @com.simiacryptus.ref.lang.RefAware V value) {
    return null;
  }

  @Override
  default boolean remove(@com.simiacryptus.ref.lang.RefAware Object key,
      @com.simiacryptus.ref.lang.RefAware Object value) {
    return false;
  }

  @Override
  default boolean replace(@com.simiacryptus.ref.lang.RefAware K key, @com.simiacryptus.ref.lang.RefAware V oldValue,
      @com.simiacryptus.ref.lang.RefAware V newValue) {
    return false;
  }

  @Nullable
  @Override
  default V replace(@com.simiacryptus.ref.lang.RefAware K key, @com.simiacryptus.ref.lang.RefAware V value) {
    return null;
  }

  @Override
  default V computeIfPresent(@com.simiacryptus.ref.lang.RefAware K key,
      @com.simiacryptus.ref.lang.RefAware BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    return null;
  }

  @Override
  default V compute(@com.simiacryptus.ref.lang.RefAware K key,
      @com.simiacryptus.ref.lang.RefAware BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    return null;
  }
}
