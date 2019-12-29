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

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * The type Ref collections.
 */
public class RefCollections {

  /**
   * Gets inner stream.
   *
   * @param <T> the type parameter
   * @param c   the c
   * @return the inner stream
   */
  public static <T> Stream<T> getInnerStream(@NotNull Collection<T> c) {
    final Stream<T> stream = getInnerCollection(c).stream();
    assert !(stream instanceof RefStream);
    return stream;
  }

  /**
   * Gets inner collection.
   *
   * @param <T> the type parameter
   * @param c   the c
   * @return the inner collection
   */
  public static <T> Collection<T> getInnerCollection(@NotNull Collection<T> c) {
    if (c instanceof RefCollection) {
      return ((RefCollection<T>) c).getInner();
    } else {
      return c;
    }
  }

  /**
   * Unmodifiable map ref map.
   *
   * @param <K> the type parameter
   * @param <V> the type parameter
   * @param map the map
   * @return the ref map
   */
  public static <K, V> RefMap<K, V> unmodifiableMap(RefMap<K, V> map) {
    throw new RuntimeException();
  }

  public static <T> void shuffle(List<T> list) {
    Collections.shuffle(list);
  }
}
