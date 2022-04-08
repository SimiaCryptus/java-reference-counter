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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A RefLinkedBlockingQueue is a LinkedBlockingQueue with an additional reference to the underlying data structure.
 *
 * @param <T> the type of elements held in this queue
 * @author John Doe
 * @docgenVersion 9
 * @since 1.0
 */
@RefIgnore
@SuppressWarnings("unused")
public class RefLinkedBlockingQueue<T> extends RefAbstractQueue<T> implements RefBlockingQueue<T> {

  @Nonnull
  private final LinkedBlockingQueue<T> inner;

  public RefLinkedBlockingQueue() {
    inner = new LinkedBlockingQueue<>();
  }

  /**
   * @return the inner BlockingQueue
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public BlockingQueue<T> getInner() {
    return inner;
  }

  /**
   * Adds the specified element to this set if it is not already present
   * and returns true if this set changed as a result of the call.
   *
   * @param t element to be added to this set
   * @return true if this set changed as a result of the call
   * @throws NullPointerException if the specified element is null
   * @docgenVersion 9
   */
  @Override
  public boolean add(@Nonnull @RefAware T t) {
    assertAlive();
    RefUtil.assertAlive(t);
    return getInner().add(t);
  }

  /**
   * Adds all of the elements in the specified collection to this
   * queue.  Attempts to add all of the elements in the specified
   * collection to this queue.  The behavior of this operation is
   * undefined if the specified collection is modified while the
   * operation is in progress.
   *
   * @param c the elements to be added to this queue
   * @return {@code true} if all the elements were added to this queue,
   * else {@code false}
   * @throws ClassCastException       if the class of an element of the
   *                                  specified collection prevents it from being added to this
   *                                  queue
   * @throws NullPointerException     if the specified collection contains a
   *                                  null element and this queue does not permit null elements,
   *                                  or if the specified collection is null
   * @throws IllegalArgumentException if some property of an element of
   *                                  the specified collection prevents it from being added to
   *                                  this queue, or if the specified collection is this queue
   * @throws IllegalStateException    if not all the elements can be added
   *                                  at this time due to insertion restrictions
   * @docgenVersion 9
   * @see #add(Object)
   */
  @Override
  public boolean addAll(@Nonnull @RefAware Collection<? extends T> c) {
    assertAlive();
    final BlockingQueue<T> inner = getInner();
    final boolean b = c.stream().allMatch(e -> inner.add(e));
    RefUtil.freeRef(c);
    return b;
  }

  /**
   * @param t the element to add
   * @return {@code true} if the element was added to this queue, else
   * {@code false}
   * @throws NullPointerException if the specified element is null
   * @docgenVersion 9
   */
  @Override
  public boolean offer(@Nonnull @RefAware T t) {
    assertAlive();
    final boolean b = getInner().offer(t);
    if (!b)
      RefUtil.freeRef(t);
    return b;
  }

  /**
   * @Override public void put(@RefAware @Nonnull T t) throws InterruptedException {
   * getInner().put(t);
   * }
   * @docgenVersion 9
   */
  @Override
  public void put(@RefAware @Nonnull T t) throws InterruptedException {
    getInner().put(t);
  }

  /**
   * {@inheritDoc}
   *
   * @param t       the element to add
   * @param timeout how long to wait before giving up, in units of {@code unit}
   * @param unit    a {@code TimeUnit} determining how to interpret the {@code timeout} parameter
   * @return {@code true} if the element was added to this queue, else {@code false}
   * @throws InterruptedException if interrupted while waiting
   * @throws NullPointerException if the specified element is null
   * @docgenVersion 9
   */
  @Override
  public boolean offer(@RefAware T t, long timeout, @Nonnull @RefAware TimeUnit unit)
      throws InterruptedException {
    return getInner().offer(t, timeout, unit);
  }

  /**
   * @return the next available element
   * @throws InterruptedException if interrupted while waiting
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  @RefAware
  public T take() throws InterruptedException {
    return getInner().take();
  }

  /**
   * @Nullable
   * @Override
   * @RefAware public T poll(long timeout, @Nonnull @RefAware TimeUnit unit) throws InterruptedException {
   * return getInner().poll(timeout, unit);
   * }
   * @docgenVersion 9
   */
  @Nullable
  @Override
  @RefAware
  public T poll(long timeout, @Nonnull @RefAware TimeUnit unit) throws InterruptedException {
    return getInner().poll(timeout, unit);
  }

  /**
   * {@inheritDoc}
   *
   * @return the remaining capacity
   * @docgenVersion 9
   */
  @Override
  public int remainingCapacity() {
    return getInner().remainingCapacity();
  }

  /**
   * Drains a maximum of {@link #size()} elements from this queue into the given collection {@code c}.
   *
   * @param c the collection to transfer elements into
   * @return the number of elements transferred
   * @docgenVersion 9
   */
  @Override
  public int drainTo(@RefAware @Nonnull Collection<? super T> c) {
    int drain = getInner().drainTo(c);
    RefUtil.freeRef(c);
    return drain;
  }

  /**
   * @Override public int drainTo(@RefAware @Nonnull Collection<? super T> c, int maxElements) {
   * return getInner().drainTo(c, maxElements);
   * }
   * @docgenVersion 9
   */
  @Override
  public int drainTo(@RefAware @Nonnull Collection<? super T> c, int maxElements) {
    return getInner().drainTo(c, maxElements);
  }

  /**
   * {@inheritDoc}
   *
   * @return a new {@code RefLinkedBlockingQueue} instance
   * @docgenVersion 9
   */
  @Override
  @Nonnull
  public RefLinkedBlockingQueue<T> addRef() {
    return (RefLinkedBlockingQueue) super.addRef();
  }

  /**
   * Frees the resources used by this object.
   *
   * @docgenVersion 9
   */
  @Override
  protected void _free() {
    ArrayList<T> list = new ArrayList<>(inner.size());
    inner.drainTo(list);
    list.forEach(value -> RefUtil.freeRef(value));
    super._free();
  }
}
