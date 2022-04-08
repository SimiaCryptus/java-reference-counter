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
import com.simiacryptus.ref.lang.RefUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Queue;

/**
 * This class represents an abstract queue data structure.
 *
 * @param <E> the type of elements in this queue
 * @author John Doe
 * @docgenVersion 9
 * @since 1.0
 */
public abstract class RefAbstractQueue<T> extends RefAbstractCollection<T> implements RefQueue<T> {
  /**
   * @return the inner queue
   * @throws NullPointerException if the inner queue is null
   * @docgenVersion 9
   */
  @Nonnull
  public abstract Queue<T> getInner();

  /**
   * Adds a reference to this object.
   *
   * @return a reference to this object
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  RefAbstractQueue<T> addRef() {
    return (RefAbstractQueue<T>) super.addRef();
  }

  /**
   * @return the element at the top of the stack, or null if the stack is empty
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public T element() {
    assertAlive();
    return RefUtil.addRef(getInner().element());
  }

  /**
   * @return the element at the top of the stack without removing it, or null if the stack is empty
   * @docgenVersion 9
   */
  @Nullable
  @Override
  @RefAware
  public T peek() {
    assertAlive();
    return RefUtil.addRef(getInner().peek());
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException if this {@linkplain BlockingQueue} is no longer {@linkplain #isAlive() alive}
   * @docgenVersion 9
   */
  @Override
  @RefAware
  public T poll() {
    assertAlive();
    return getInner().poll();
  }

  /**
   * @param o the object to be removed from this list, if present
   * @return {@code true} if an element was removed as a result of this call
   * @throws ClassCastException            if the type of the specified element is incompatible with this
   *                                       list (<a href="Collection.html#optional-restrictions">optional</a>)
   * @throws NullPointerException          if the specified element is null and this list does not permit
   *                                       null elements (<a href="Collection.html#optional-restrictions">optional</a>)
   * @throws UnsupportedOperationException if the {@code remove} operation is not supported by this
   *                                       list
   * @docgenVersion 9
   */
  @Override
  public boolean remove(@RefAware Object o) {
    assertAlive();
    final boolean remove = getInner().remove(o);
    if (remove)
      RefUtil.freeRef(o);
    RefUtil.freeRef(o);
    return remove;
  }

  /**
   * @Override
   * @RefAware public T remove() {
   * assertAlive();
   * return getInner().remove();
   * }
   * @docgenVersion 9
   */
  @Override
  @RefAware
  public T remove() {
    assertAlive();
    return getInner().remove();
  }
}
