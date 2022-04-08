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

/**
 * This is the RefCollection interface.
 *
 * @docgenVersion 9
 */
@RefIgnore
public interface RefCollection<T> extends ReferenceCounting, Collection<T> {

  /**
   * Returns a collection of type T. This collection will never be null.
   *
   * @docgenVersion 9
   */
  @Nonnull
  Collection<T> getInner();

  /**
   * Adds a reference to the collection.
   *
   * @return the collection with the added reference
   * @throws NullPointerException if the collection is null
   * @docgenVersion 9
   */
  @Nonnull
  RefCollection<T> addRef();

  /**
   * Performs the given action for each element of the {@link RefIterable}
   * until all elements have been processed or the action throws an
   * exception.  Exceptions thrown by the action are relayed to the
   * caller.
   *
   * @param action The action to be performed for each element
   * @throws NullPointerException if the specified action is null
   * @docgenVersion 9
   */
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

  /**
   * @return an iterator over the elements in this RefCollection
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefIterator<T> iterator();

  /**
   * Returns a parallel {@code RefStream} over the elements in this {@code RefCollection}.
   *
   * @return a parallel {@code RefStream} over the elements in this {@code RefCollection}
   * @docgenVersion 9
   */
  @Override
  default RefStream<T> parallelStream() {
    assertAlive();
    return RefStreamSupport.stream(spliterator(), true);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation creates a {@link RefSpliterator} that
   * wraps the given {@link Spliterator}. The spliterator is
   * tracked by this {@code RefStream} instance.
   *
   * @return the new spliterator
   * @docgenVersion 9
   */
  @Override
  default RefSpliterator<T> spliterator() {
    assertAlive();
    final Spliterator<T> spliterator = Spliterators.spliterator(getInner(), 0);
    return new RefSpliterator<>(spliterator, size()).track(this.addRef());
  }

  /**
   * Returns a stream over the elements in this container.
   *
   * @return a stream over the elements in this container
   * @docgenVersion 9
   */
  @Override
  default RefStream<T> stream() {
    assertAlive();
    return RefStreamSupport.stream(spliterator(), false);
  }

  /**
   * @param filter the predicate used to filter elements
   * @return {@code true} if any elements were removed
   * @throws NullPointerException if the specified filter is null
   * @docgenVersion 9
   */
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
