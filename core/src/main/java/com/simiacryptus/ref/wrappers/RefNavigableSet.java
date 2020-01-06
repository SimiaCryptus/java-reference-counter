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

@RefAware
public interface RefNavigableSet<T> extends NavigableSet<T>, RefSet<T> {
  @NotNull
  @Override
  RefNavigableSet<T> descendingSet();

  @NotNull
  @Override
  RefNavigableSet<T> subSet(@RefAware T fromElement, boolean fromInclusive,
                            @RefAware T toElement, boolean toInclusive);

  @NotNull
  @Override
  RefNavigableSet<T> headSet(@RefAware T toElement, boolean inclusive);

  @NotNull
  @Override
  RefNavigableSet<T> tailSet(@RefAware T fromElement, boolean inclusive);

  @NotNull
  @Override
  RefSortedSet<T> subSet(@RefAware T fromElement,
      @RefAware T toElement);

  @NotNull
  @Override
  RefSortedSet<T> headSet(@RefAware T toElement);

  @NotNull
  @Override
  RefSortedSet<T> tailSet(@RefAware T fromElement);

  @Override
  default RefSpliterator<T> spliterator() {
    return RefSet.super.spliterator();
  }
}
