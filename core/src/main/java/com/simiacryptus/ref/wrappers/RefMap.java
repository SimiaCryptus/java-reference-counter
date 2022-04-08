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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This is the RefMap interface.
 *
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public interface RefMap<K, V> extends ReferenceCounting, Map<K, V> {

  /**
   * Returns a list of all keys in this map.
   *
   * @return a list of all keys in this map
   * @docgenVersion 9
   */
  @NotNull
  default RefList<K> keyList() {
    RefList<K> keys = new RefArrayList<>();
    this.forEach((k, v) -> {
      keys.add(k);
      RefUtil.freeRef(v);
    });
    return keys;
  }

  /**
   * Returns a list of all values in the map.
   *
   * @return a list of all values in the map
   * @docgenVersion 9
   */
  @NotNull
  default RefList<V> valueList() {
    RefList<V> values = new RefArrayList<>();
    this.forEach((k, v) -> {
      values.add(v);
      RefUtil.freeRef(k);
    });
    return values;
  }

  /**
   * Maps the values in this {@link RefMap} to new values using the given {@link RefFunction}.
   *
   * @param mapper the {@link RefFunction} to use for mapping values
   * @param <D>    the type of the mapped values
   * @return a new {@link RefMap} containing the mapped values
   * @docgenVersion 9
   */
  @NotNull
  default <D> RefMap<K, V> mapValues(RefFunction<V, D> mapper) {
    RefHashMap<K, V> map = new RefHashMap<>();
    forEach((k, v) -> {
      RefUtil.freeRef(map.put(k, (V) mapper.apply(v)));
    });
    RefUtil.freeRef(mapper);
    return map;
  }

  /**
   * Adds a reference to the map.
   *
   * @return the map with the added reference
   * @throws NullPointerException if the map is null
   * @docgenVersion 9
   */
  @Nonnull
  RefMap<K, V> addRef();

  /**
   * @Nullable
   * @Override
   * @RefAware default V computeIfAbsent(@RefAware K key, @Nonnull @RefAware Function<? super K, ? extends V> mappingFunction)
   * @docgenVersion 9
   */
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

  /**
   * Returns a {@link RefSet} view of the mappings contained in this map.
   * The set is backed by the map, so changes to the map are
   * reflected in the set, and vice-versa.  If the map is modified
   * while an iteration over the set is in progress (except through
   * the iterator's own {@code remove} operation), the results of
   * the iteration are undefined.  The set supports element removal,
   * which removes the corresponding mapping from the map, via the
   * {@code Iterator.remove}, {@code Set.remove},
   * {@code removeAll}, {@code retainAll}, and {@code clear}
   * operations.  It does not support the {@code add} or {@code addAll}
   * operations.
   *
   * @return a set view of the mappings contained in this map
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefSet<Entry<K, V>> entrySet();

  /**
   * Performs the given action for each entry in this map until all entries
   * have been processed or the action throws an exception. Unless
   * otherwise specified by the implementing class, actions are performed
   * in the order of entry set iteration (if an iteration order is specified.)
   * Exceptions thrown by the action are relayed to the caller.
   *
   * @param action The action to be performed for each entry
   * @throws NullPointerException if the specified action is null
   * @docgenVersion 9
   */
  void forEach(@Nonnull @RefAware BiConsumer<? super K, ? super V> action);

  /**
   * @return a {@link RefSet} of all keys in this {@link RefMap}
   * @throws NullPointerException if the returned {@link RefSet} is null
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefSet<K> keySet();

  /**
   * @Override
   * @RefAware default V merge(@RefAware K key, @RefAware V value, @Nonnull @RefAware BiFunction<? super V, ? super V, ? extends V> fn);
   * @docgenVersion 9
   */
  @Override
  @RefAware
  default V merge(@RefAware K key,
                  @RefAware V value,
                  @Nonnull @RefAware BiFunction<? super V, ? super V, ? extends V> fn) {
    synchronized (this) {
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
  }

  /**
   * Returns a {@link RefCollection} containing the values in this map.
   * The collection is backed by the map, so changes to the map are
   * reflected in the collection, and vice-versa.  If the map is
   * modified while an iteration over the collection is in progress
   * (except through the iterator's own {@code remove} operation),
   * the results of the iteration are undefined.  The collection
   * supports element removal, which removes the corresponding
   * mapping from the map, via the {@code Iterator.remove},
   * {@code Collection.remove}, {@code removeAll},
   * {@code clear} operations.  It does not support the
   * {@code add} or {@code addAll} operations.
   *
   * @return a collection view of the values contained in this map
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefCollection<V> values();

  /**
   * @Override void putAll(@RefAware @Nonnull Map<? extends K, ? extends V> m);
   * @docgenVersion 9
   */
  @Override
  void putAll(@RefAware @Nonnull Map<? extends K, ? extends V> m);

  /**
   * @Override boolean containsKey(@RefAware Object key);
   * @docgenVersion 9
   */
  @Override
  boolean containsKey(@RefAware Object key);

  /**
   * @Override
   * @RefAware boolean containsValue(@RefAware Object value);
   * @docgenVersion 9
   */
  @Override
  @RefAware
  boolean containsValue(@RefAware Object value);

  /**
   * Returns the value to which the specified key is mapped, or null if this map contains no mapping for the key.
   *
   * @param key the key whose associated value is to be returned
   * @return the value to which the specified key is mapped, or null if this map contains no mapping for the key
   * @docgenVersion 9
   */
  @Nullable
  @Override
  @RefAware
  V get(@RefAware Object key);

  /**
   * @Nullable
   * @Override
   * @RefAware V put(@RefAware K key, @RefAware V value);
   * @docgenVersion 9
   */
  @Nullable
  @Override
  @RefAware
  V put(@RefAware K key, @RefAware V value);

  /**
   * @Override
   * @RefAware V remove(@RefAware Object key);
   * @docgenVersion 9
   */
  @Override
  @RefAware
  V remove(@RefAware Object key);

  /**
   * Returns the value to which the specified key is mapped, or the specified defaultValue if this map contains no mapping for the key.
   *
   * @param key          the key whose associated value is to be returned
   * @param defaultValue the default mapping of the key
   * @return the value to which the specified key is mapped, or the specified defaultValue if this map contains no mapping for the key
   * @docgenVersion 9
   */
  @Nullable
  @Override
  @RefAware
  V getOrDefault(@RefAware Object key,
                 @RefAware V defaultValue);

  /**
   * Replaces each entry's value with the result of invoking the given
   * function on that entry until all entries have been processed or the
   * function throws an exception.  Exceptions thrown by the function are
   * relayed to the caller.
   *
   * @param function the function to apply to each entry
   * @throws NullPointerException          if the specified function is null
   * @throws UnsupportedOperationException if this map does not support the {@code put} operation
   * @throws ClassCastException            if the class of a replacement value
   *                                       prevents it from being stored in this map
   * @throws NullPointerException          if a replacement value is null,
   *                                       and this map does not permit null values
   * @throws NullPointerException          if function or a replacement value is null
   * @throws IllegalArgumentException      if some property of a replacement value
   *                                       prevents it from being stored in this map
   * @throws ClassCastException            if the specified function is not an instance
   *                                       of {@link BiFunction}
   * @docgenVersion 9
   */
  @Override
  void replaceAll(@RefAware BiFunction<? super K, ? super V, ? extends V> function);

  /**
   * @Nullable
   * @Override
   * @RefAware default V putIfAbsent(@RefAware K key, @RefAware V value) {
   * throw new RuntimeException("Not Implemented");
   * }
   * @docgenVersion 9
   */
  @Nullable
  @Override
  @RefAware
  default V putIfAbsent(@RefAware K key, @RefAware V value) {
    throw new RuntimeException("Not Implemented");
  }

  /**
   * {@inheritDoc}
   *
   * @throws RuntimeException if not implemented
   * @docgenVersion 9
   */
  @Override
  default boolean remove(@RefAware Object key,
                         @RefAware Object value) {
    throw new RuntimeException("Not Implemented");
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation always throws an {@link UnsupportedOperationException}.
   *
   * @param key      key with which the specified value is to be associated
   * @param oldValue value expected to be associated with the specified key
   * @param newValue value to be associated with the specified key
   * @return {@code true} if the value was replaced
   * @throws UnsupportedOperationException if the {@code put} operation
   *                                       is not supported by this map
   *                                       (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
   * @throws ClassCastException            if the class of a specified key or value
   *                                       prevents it from being stored in this map
   *                                       (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
   * @throws NullPointerException          if a specified key or newValue is null,
   *                                       and this map does not permit null keys or values
   *                                       (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
   * @throws NullPointerException          if oldValue is null and this map does not
   *                                       permit null values
   *                                       (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
   * @throws IllegalArgumentException      if some property of a specified key
   *                                       or value prevents it from being stored in this map
   *                                       (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
   * @throws IllegalArgumentException      if oldValue is not equal to the
   *                                       {@linkplain Map#get(Object) current value} of the specified key
   *                                       (<a href="{@docRoot}/java/util/Map.html#optional-restrictions">optional</a>)
   * @docgenVersion 9
   * @since 1.8
   */
  @Override
  default boolean replace(@RefAware K key, @RefAware V oldValue,
                          @RefAware V newValue) {
    throw new RuntimeException("Not Implemented");
  }

  /**
   * @Nullable
   * @Override
   * @RefAware default V replace(@RefAware K key, @RefAware V value) {
   * throw new RuntimeException("Not Implemented");
   * }
   * @docgenVersion 9
   */
  @Nullable
  @Override
  @RefAware
  default V replace(@RefAware K key, @RefAware V value) {
    throw new RuntimeException("Not Implemented");
  }

  /**
   * @Nullable
   * @Override
   * @RefAware default V computeIfPresent(@RefAware K key, @RefAware BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
   * throw new RuntimeException("Not Implemented");
   * }
   * @docgenVersion 9
   */
  @Nullable
  @Override
  @RefAware
  default V computeIfPresent(@RefAware K key,
                             @RefAware BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    throw new RuntimeException("Not Implemented");
  }

  /**
   * @Nullable
   * @Override
   * @RefAware default V compute(@RefAware K key, @RefAware BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
   * throw new RuntimeException("Not Implemented");
   * }
   * @docgenVersion 9
   */
  @Nullable
  @Override
  @RefAware
  default V compute(@RefAware K key,
                    @RefAware BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    throw new RuntimeException("Not Implemented");
  }
}
