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
import org.jetbrains.annotations.NotNull;

import java.util.NavigableSet;
import java.util.SortedSet;

@RefAware
public interface RefNavigableSet<T> extends NavigableSet<T>, RefSet<T> {
  @NotNull
  @Override
  RefNavigableSet<T> descendingSet();

  @NotNull
  @Override
  RefNavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive);

  @NotNull
  @Override
  RefNavigableSet<T> headSet(T toElement, boolean inclusive);

  @NotNull
  @Override
  RefNavigableSet<T> tailSet(T fromElement, boolean inclusive);

  @NotNull
  @Override
  RefSortedSet<T> subSet(T fromElement, T toElement);

  @NotNull
  @Override
  RefSortedSet<T> headSet(T toElement);

  @NotNull
  @Override
  RefSortedSet<T> tailSet(T fromElement);

  @Override
  default RefSpliterator<T> spliterator() {
    return RefSet.super.spliterator();
  }
}
