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
import java.util.Collection;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A class that wraps a {@link ConcurrentLinkedDeque} and provides
 * reference-based access to it.
 *
 * @param <T> the type of elements in the deque
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public class RefConcurrentLinkedDeque<T> extends RefAbstractQueue<T> implements RefDeque<T> {

  @Nonnull
  private final ConcurrentLinkedDeque<T> inner;

  public RefConcurrentLinkedDeque() {
    inner = new ConcurrentLinkedDeque<>();
  }

  /**
   * @return the first element in the list, or null if the list is empty
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public T getFirst() {
    assertAlive();
    return RefUtil.addRef(getInner().getFirst());
  }

  /**
   * @return the inner deque
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Deque<T> getInner() {
    return inner;
  }

  /**
   * @return the last element in the list, or null if the list is empty
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public T getLast() {
    assertAlive();
    return RefUtil.addRef(getInner().getLast());
  }

  /**
   * Returns a new RefConcurrentLinkedDeque with the same contents as this RefConcurrentLinkedDeque.
   *
   * @return a new RefConcurrentLinkedDeque with the same contents as this RefConcurrentLinkedDeque.
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  RefConcurrentLinkedDeque<T> addRef() {
    return (RefConcurrentLinkedDeque<T>) super.addRef();
  }

  /**
   * Adds the specified element to this set if it is not already present
   * (optional operation).  More formally, adds the specified element
   * {@code e} to this set if the set contains no element {@code e2}
   * such that
   * {@code (e==null ? e2==null : e.equals(e2))}.
   * If this set already contains the specified element, the call leaves
   * the set unchanged and returns {@code false}.
   *
   * @param e element to be added to this set
   * @return {@code true} if this set did not already contain the specified
   * element
   * @throws UnsupportedOperationException if the {@code add} operation
   *                                       is not supported by this set
   * @throws ClassCastException            if the class of the specified element
   *                                       prevents it from being added to this set
   * @throws NullPointerException          if the specified element is null and this
   *                                       set does not permit null elements
   * @throws IllegalArgumentException      if some property of the specified element
   *                                       prevents it from being added to this set
   * @docgenVersion 9
   */
  @Override
  public boolean add(@RefAware T t) {
    assertAlive();
    return getInner().add(t);
  }

  /**
   * Adds all of the elements in the specified collection to this deque.
   *
   * @param c the elements to be added to this deque
   * @return true if the deque changed as a result of the call
   * @docgenVersion 9
   */
  @Override
  public boolean addAll(@Nonnull @RefAware Collection<? extends T> c) {
    assertAlive();
    final Deque<T> inner = getInner();
    final boolean b = c.stream().allMatch(e -> inner.add(e));
    RefUtil.freeRef(c);
    return b;
  }

  /**
   * Adds the given element to the front of this deque.
   *
   * @param t the element to add
   * @throws NullPointerException if the given element is null
   * @docgenVersion 9
   */
  @Override
  public void addFirst(@RefAware T t) {
    assertAlive();
    getInner().addFirst(t);
  }

  /**
   * Adds the specified element to the end of this list.
   *
   * @param t the element to add
   * @throws NullPointerException if the specified element is null
   * @docgenVersion 9
   */
  @Override
  public void addLast(@RefAware T t) {
    assertAlive();
    getInner().addLast(t);
  }

  /**
   * @return a new RefIterator that iterates over the elements of this RefList in reverse order
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefIterator<T> descendingIterator() {
    assertAlive();
    return new RefIterator(getInner().descendingIterator()).track(this.addRef());
  }

  /**
   * @param t the element to add
   * @return {@code true} if the element was added to this queue, else
   * {@code false}
   * @throws NullPointerException if the specified element is null
   * @docgenVersion 9
   */
  @Override
  public boolean offer(@RefAware T t) {
    assertAlive();
    final boolean b = getInner().offer(t);
    if (!b)
      RefUtil.freeRef(t);
    return b;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns {@code getInner().offerFirst(t)}.
   *
   * @param t the element to add
   * @return {@code true} if it was possible to add the element to this queue, else
   * {@code false}
   * @throws ClassCastException       if the class of the specified element prevents it
   *                                  from being added to this queue
   * @throws NullPointerException     if the specified element is null and this queue
   *                                  does not permit null elements
   * @throws IllegalArgumentException if some property of the specified element
   *                                  prevents it from being added to this queue
   * @docgenVersion 9
   */
  @Override
  public boolean offerFirst(@RefAware T t) {
    assertAlive();
    final boolean b = getInner().offerFirst(t);
    if (!b)
      RefUtil.freeRef(t);
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
  public boolean offerLast(@RefAware T t) {
    assertAlive();
    final boolean b = getInner().offerLast(t);
    if (!b)
      RefUtil.freeRef(t);
    return b;
  }

  /**
   * @return the first element of this deque, or {@code null} if this deque is empty
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public T peekFirst() {
    assertAlive();
    return RefUtil.addRef(getInner().peekFirst());
  }

  /**
   * @return the last element in the queue, or null if the queue is empty
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public T peekLast() {
    assertAlive();
    return RefUtil.addRef(getInner().peekLast());
  }

  /**
   * @return the first element of this deque, or {@code null} if it is empty
   * @throws IllegalStateException if this deque is no longer active
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public T pollFirst() {
    assertAlive();
    return getInner().pollFirst();
  }

  /**
   * @return the last element, or null if empty
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public T pollLast() {
    assertAlive();
    return getInner().pollLast();
  }

  /**
   * Pops an element from the stack.
   *
   * @return the popped element
   * @docgenVersion 9
   */
  @Override
  public T pop() {
    assertAlive();
    return getInner().pop();
  }

  /**
   * Pushes an element onto the stack.
   *
   * @param t the element to push
   * @docgenVersion 9
   */
  @Override
  public void push(@RefAware T t) {
    assertAlive();
    getInner().push(t);
  }

  /**
   * @Override public T removeFirst() {
   * assertAlive();
   * return getInner().removeFirst();
   * }
   * @docgenVersion 9
   */
  @Override
  public T removeFirst() {
    assertAlive();
    return getInner().removeFirst();
  }

  /**
   * @param o The object to remove.
   * @return True if the object was removed.
   * @docgenVersion 9
   */
  @Override
  public boolean removeFirstOccurrence(@RefAware Object o) {
    assertAlive();
    final boolean remove = getInner().removeFirstOccurrence(o);
    if (remove)
      RefUtil.freeRef(o);
    RefUtil.freeRef(o);
    return remove;
  }

  /**
   * @Override public T removeLast() {
   * assertAlive();
   * return getInner().removeLast();
   * }
   * @docgenVersion 9
   */
  @Override
  public T removeLast() {
    assertAlive();
    return getInner().removeLast();
  }

  /**
   * @param o The object to remove.  This object will be freed.
   * @return True if the object was removed.
   * @docgenVersion 9
   */
  @Override
  public boolean removeLastOccurrence(@RefAware Object o) {
    assertAlive();
    final boolean remove = getInner().removeLastOccurrence(o);
    if (remove)
      RefUtil.freeRef(o);
    RefUtil.freeRef(o);
    return remove;
  }

  /**
   * Frees the resources used by this object.
   *
   * @docgenVersion 9
   */
  @Override
  protected void _free() {
    inner.forEach(value -> RefUtil.freeRef(value));
    inner.clear();
    super._free();
  }

}
