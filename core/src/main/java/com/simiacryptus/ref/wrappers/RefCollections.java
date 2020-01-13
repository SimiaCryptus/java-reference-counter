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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@RefIgnore
@SuppressWarnings("unused")
public class RefCollections {

  public static <T> Stream<T> getInnerStream(@NotNull @RefAware Collection<T> c) {
    final Stream<T> stream = getInnerCollection(c).stream();
    assert !(stream instanceof RefStream);
    return stream;
  }

  public static <T> Collection<T> getInnerCollection(@NotNull @RefAware Collection<T> c) {
    if (c instanceof RefCollection) {
      return ((RefCollection<T>) c).getInner();
    } else {
      return c;
    }
  }

  @NotNull
  public static <K, V> RefMap<K, V> unmodifiableMap(RefMap<K, V> map) {
    return map; // TODO: Implement Me
  }

  public static <T> void shuffle(@NotNull @RefAware List<T> list) {
    Collections.shuffle(list);
  }

  public static <T> RefList<T> unmodifiableList(RefList<T> list) {
    return list; // TODO: Implement me
  }

  public static <T> RefList<T> synchronizedList(RefList<T> list) {
    return list; // TODO: Implement me
  }
}
