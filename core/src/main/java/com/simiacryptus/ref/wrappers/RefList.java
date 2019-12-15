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
import com.simiacryptus.ref.lang.ReferenceCounting;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The interface Ref list.
 *
 * @param <T> the type parameter
 */
@RefAware
@RefIgnore
public interface RefList<T> extends ReferenceCounting, List<T>, RefCollection<T> {

  /**
   * Add refs ref list [ ].
   *
   * @param <T>   the type parameter
   * @param array the array
   * @return the ref list [ ]
   */
  public static <T> RefList<T>[] addRefs(@NotNull RefList<T>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefList::addRef)
        .toArray((x) -> new RefList[x]);
  }

  RefList<T> addRef();

  List<T> getInner();

  @NotNull
  @Override
  RefListIterator<T> listIterator();

  @NotNull
  @Override
  RefListIterator<T> listIterator(int index);

  @Override
  RefSpliterator<T> spliterator();

  @Override
  RefStream<T> stream();

  @NotNull
  @Override
  RefList<T> subList(int fromIndex, int toIndex);
}
