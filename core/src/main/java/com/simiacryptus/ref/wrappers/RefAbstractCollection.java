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
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

/**
 * This class is the abstract superclass of all collections.
 *
 * @docgenVersion 9
 */
@RefIgnore
public abstract class RefAbstractCollection<T> extends ReferenceCountingBase
    implements RefCollection<T>, Cloneable, Serializable {

  /**
   * Returns a collection of type T. This collection cannot be null.
   *
   * @docgenVersion 9
   */
  @Nonnull
  public abstract Collection<T> getInner();

  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException if this {@code Optional} is empty
   * @docgenVersion 9
   */
  @Override
  public final boolean isEmpty() {
    assertAlive();
    return getInner().isEmpty();
  }

  /**
   * @return RefAbstractCollection<T>
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  RefAbstractCollection<T> addRef() {
    return (RefAbstractCollection<T>) super.addRef();
  }

  /**
   * Clears the inner map.
   *
   * @docgenVersion 9
   */
  @Override
  public synchronized void clear() {
    getInner().forEach(value -> RefUtil.freeRef(value));
    getInner().clear();
  }

  /**
   * @param o The object to check for containment
   * @return True if the object is contained in this container, false otherwise
   * @docgenVersion 9
   */
  @Override
  public final boolean contains(@RefAware Object o) {
    assertAlive();
    final boolean returnValue = getInner().contains(o);
    RefUtil.freeRef(o);
    return returnValue;
  }

  /**
   * @docgenVersion 9
   * @see Collection#containsAll(Collection)
   */
  @Override
  public final boolean containsAll(@Nonnull @RefAware Collection<?> c) {
    assertAlive();
    final Collection<?> c_inner;
    if (c instanceof RefAbstractCollection) {
      c_inner = ((RefAbstractCollection<?>) c).getInner();
    } else {
      c_inner = c;
    }
    final Collection<T> inner = getInner();
    final boolean b = !c_inner.stream().anyMatch(o -> !inner.contains(o));
    RefUtil.freeRef(c);
    return b;
  }

  /**
   * @return an iterator over the elements in this list in proper sequence
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public final RefIterator<T> iterator() {
    assertAlive();
    return new RefIterator<>(getInner().iterator()).track(this.addRef());
  }

  /**
   * @Override public synchronized boolean removeAll(@Nonnull @RefAware Collection<?> c);
   * @docgenVersion 9
   */
  @Override
  public synchronized boolean removeAll(@Nonnull @RefAware Collection<?> c) {
    assertAlive();
    final Collection<?> c_inner;
    if (c instanceof RefAbstractCollection) {
      c_inner = ((RefAbstractCollection<?>) c).getInner();
    } else {
      c_inner = c;
    }
    boolean b = false;
    final Iterator<T> iterator = getInner().iterator();
    while (iterator.hasNext()) {
      final T next = iterator.next();
      if (c_inner.contains(next)) {
        iterator.remove();
        RefUtil.freeRef(next);
        b = true;
      }
    }
    RefUtil.freeRef(c);
    return b;
  }

  /**
   * @Override public boolean retainAll(@Nonnull @RefAware Collection<?> c);
   * @docgenVersion 9
   */
  @Override
  public boolean retainAll(@Nonnull @RefAware Collection<?> c) {
    assertAlive();
    final Collection<?> c_inner;
    if (c instanceof RefAbstractCollection) {
      c_inner = ((RefAbstractCollection<?>) c).getInner();
    } else {
      c_inner = c;
    }
    boolean b = false;
    final Iterator<T> iterator = getInner().iterator();
    while (iterator.hasNext()) {
      final T next = iterator.next();
      if (!c_inner.contains(next)) {
        iterator.remove();
        RefUtil.freeRef(next);
        b = true;
      }
    }
    RefUtil.freeRef(c);
    return b;
  }

  /**
   * @Override public final int size() {
   * assertAlive();
   * return getInner().size();
   * }
   * @docgenVersion 9
   */
  @Override
  public final int size() {
    assertAlive();
    return getInner().size();
  }

  /**
   * {@inheritDoc}
   *
   * @return a {@code RefSpliterator} over the elements in this collection
   * @docgenVersion 9
   */
  @Override
  public RefSpliterator<T> spliterator() {
    return RefCollection.super.spliterator();
  }

  /**
   * @return a stream of the elements in this collection
   * @docgenVersion 9
   */
  @Override
  public RefStream<T> stream() {
    return RefCollection.super.stream();
  }

  /**
   * @return an array containing all of the elements in this list in proper sequence (from first to last element);
   * the runtime type of the returned array is that of the specified array.
   * If the list fits in the specified array, it is returned therein.
   * Otherwise, a new array is allocated with the runtime type of the specified array and the size of this list.
   * @throws NullPointerException if the specified array is null
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  @RefAware
  public final Object[] toArray() {
    assertAlive();
    final @Nonnull Object[] returnValue = getInner().toArray();
    for (Object x : returnValue) {
      RefUtil.addRef(x);
    }
    return returnValue;
  }

  /**
   * @param a the array into which the elements of this list are to
   *          be stored, if it is big enough; otherwise, a new array of the
   *          same runtime type is allocated for this purpose.
   * @return an array containing the elements of this list
   * @throws NullPointerException if the specified array is null
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  @RefAware
  public final <T1> T1[] toArray(@Nonnull @RefAware T1[] a) {
    assertAlive();
    return RefUtil.addRef(getInner().toArray(a));
  }
}
