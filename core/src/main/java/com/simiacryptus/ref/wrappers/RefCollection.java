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
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

@RefIgnore
public interface RefCollection<T> extends ReferenceCounting, Collection<T> {

  @Nonnull
  Collection<T> getInner();

  @Nonnull
  RefCollection<T> addRef();

  default void forEach(@Nonnull @RefAware Consumer<? super T> action) {
    final RefIterator<T> iterator = iterator();
    try {
      while (iterator.hasNext()) {
        action.accept(iterator.next());
      }
    } finally {
      iterator.freeRef();
      RefUtil.freeRef(action);
    }
  }

  @Nonnull
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
    return new RefSpliterator<>(spliterator, size()).track(this.addRef());
  }

  @Override
  default RefStream<T> stream() {
    assertAlive();
    return RefStreamSupport.stream(spliterator(), false);
  }

  default boolean removeIf(@Nonnull @RefAware Predicate<? super T> filter) {
    Objects.requireNonNull(filter);
    boolean removed = false;
    final Iterator<T> each = getInner().iterator();
    while (each.hasNext()) {
      final T next = each.next();
      if (filter.test(RefUtil.addRef(next))) {
        RefUtil.freeRef(next);
        each.remove();
        removed = true;
      }
    }
    return removed;
  }
}
