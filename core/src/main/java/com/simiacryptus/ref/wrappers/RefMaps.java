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
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.annotation.Nonnull;

/**
 * The RefMaps class contains methods for manipulating reference maps.
 *
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public class RefMaps {

  /**
   * Transforms the entries of a map by using the provided {@link EntryTransformer}.
   *
   * @param fromMap     the map to transform
   * @param transformer the {@link EntryTransformer} to use
   * @param <K>         the type of keys in the map
   * @param <V1>        the type of values in the map
   * @param <V2>        the type of values in the resulting map
   * @return a new map with the transformed entries
   * @docgenVersion 9
   */
  @Nonnull
  public static <K, V1, V2> RefMap<K, V2> transformEntries(@Nonnull RefMap<K, V1> fromMap,
                                                           @Nonnull @RefAware EntryTransformer<? super K, ? super V1, V2> transformer) {
    final RefHashMap<K, V2> refHashMap = new RefHashMap<>();
    fromMap.forEach((k, v) -> refHashMap.put(RefUtil.addRef(k), transformer.transformEntry(k, v)));
    return refHashMap;
  }

  /**
   * EntryTransformer is an interface that is used to transform an entry.
   *
   * @docgenVersion 9
   */
  @FunctionalInterface
  public interface EntryTransformer<K, V1, V2> {
    /**
     * Transforms an entry in the map.
     *
     * @param var1 the key for the entry
     * @param var2 the value for the entry
     * @return the transformed value
     * @docgenVersion 9
     */
    V2 transformEntry(@Nullable @RefAware K var1,
                      @Nullable @RefAware V1 var2);
  }
}
