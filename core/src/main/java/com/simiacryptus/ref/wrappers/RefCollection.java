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

import java.util.Collection;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * The interface Ref collection.
 *
 * @param <T> the type parameter
 */
@RefAware
@RefIgnore
public interface RefCollection<T> extends ReferenceCounting, Collection<T> {

  /**
   * Add refs ref collection [ ].
   *
   * @param <T>   the type parameter
   * @param array the array
   * @return the ref collection [ ]
   */
  public static <T> RefCollection<T>[] addRefs(@NotNull RefCollection<T>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefCollection::addRef)
        .toArray((x) -> new RefCollection[x]);
  }

  RefCollection<T> addRef();

  default void forEach(Consumer<? super T> action) {
    final RefIterator<T> iterator = iterator();
    while (iterator.hasNext()) {
      action.accept(iterator.next());
    }
    RefUtil.freeRef(action);
    iterator.freeRef();
  }

  /**
   * Gets inner.
   *
   * @return the inner
   */
  Collection<T> getInner();

  @NotNull
  @Override
  RefIterator<T> iterator();

  @Override
  default RefStream<T> parallelStream() {
    assertAlive();
    return RefStreamSupport.stream(spliterator(), true);
  }

  @Override
  default RefSpliterator<T> spliterator() {
    assertAlive();
    final Spliterator<T> spliterator = Spliterators.spliterator(getInner(), 0);
    if (spliterator instanceof RefSpliterator) {
      return ((RefSpliterator<T>) spliterator).addRef();
    } else {
      return new RefSpliterator<>(spliterator, size()).track(this.addRef());
    }
  }

  @Override
  default RefStream<T> stream() {
    assertAlive();
    return RefStreamSupport.stream(spliterator(), false);
  }

}
