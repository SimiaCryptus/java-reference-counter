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
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

@RefIgnore
@SuppressWarnings("unused")
public class RefConcurrentLinkedDeque<T> extends RefAbstractQueue<T> implements RefDeque<T> {

  @Nonnull
  private final ConcurrentLinkedDeque<T> inner;

  public RefConcurrentLinkedDeque() {
    inner = new ConcurrentLinkedDeque<>();
  }

  @Nullable
  @Override
  public T getFirst() {
    assertAlive();
    return RefUtil.addRef(getInner().getFirst());
  }

  @Nonnull
  @Override
  public Deque<T> getInner() {
    return inner;
  }

  @Nullable
  @Override
  public T getLast() {
    assertAlive();
    return RefUtil.addRef(getInner().getLast());
  }

  @Nonnull
  public @Override
  RefConcurrentLinkedDeque<T> addRef() {
    return (RefConcurrentLinkedDeque<T>) super.addRef();
  }

  @Override
  public boolean add(@RefAware T t) {
    assertAlive();
    return getInner().add(t);
  }

  @Override
  public boolean addAll(@Nonnull @RefAware Collection<? extends T> c) {
    assertAlive();
    final Deque<T> inner = getInner();
    final boolean b = c.stream().allMatch(e -> inner.add(e));
    RefUtil.freeRef(c);
    return b;
  }

  @Override
  public void addFirst(@RefAware T t) {
    assertAlive();
    getInner().addFirst(t);
  }

  @Override
  public void addLast(@RefAware T t) {
    assertAlive();
    getInner().addLast(t);
  }

  @Nonnull
  @Override
  public RefIterator<T> descendingIterator() {
    assertAlive();
    return new RefIterator(getInner().descendingIterator()).track(this.addRef());
  }

  @Override
  public boolean offer(@RefAware T t) {
    assertAlive();
    final boolean b = getInner().offer(t);
    if (!b)
      RefUtil.freeRef(t);
    return b;
  }

  @Override
  public boolean offerFirst(@RefAware T t) {
    assertAlive();
    final boolean b = getInner().offerFirst(t);
    if (!b)
      RefUtil.freeRef(t);
    return b;
  }

  @Override
  public boolean offerLast(@RefAware T t) {
    assertAlive();
    final boolean b = getInner().offerLast(t);
    if (!b)
      RefUtil.freeRef(t);
    return b;
  }

  @Nullable
  @Override
  public T peekFirst() {
    assertAlive();
    return RefUtil.addRef(getInner().peekFirst());
  }

  @Nullable
  @Override
  public T peekLast() {
    assertAlive();
    return RefUtil.addRef(getInner().peekLast());
  }

  @Nullable
  @Override
  public T pollFirst() {
    assertAlive();
    return getInner().pollFirst();
  }

  @Nullable
  @Override
  public T pollLast() {
    assertAlive();
    return getInner().pollLast();
  }

  @Override
  public T pop() {
    assertAlive();
    return getInner().pop();
  }

  @Override
  public void push(@RefAware T t) {
    assertAlive();
    getInner().push(t);
  }

  @Override
  public T removeFirst() {
    assertAlive();
    return getInner().removeFirst();
  }

  @Override
  public boolean removeFirstOccurrence(@RefAware Object o) {
    assertAlive();
    final boolean remove = getInner().removeFirstOccurrence(o);
    if (remove)
      RefUtil.freeRef(o);
    RefUtil.freeRef(o);
    return remove;
  }

  @Override
  public T removeLast() {
    assertAlive();
    return getInner().removeLast();
  }

  @Override
  public boolean removeLastOccurrence(@RefAware Object o) {
    assertAlive();
    final boolean remove = getInner().removeLastOccurrence(o);
    if (remove)
      RefUtil.freeRef(o);
    RefUtil.freeRef(o);
    return remove;
  }

  @Override
  protected void _free() {
    inner.forEach(value -> RefUtil.freeRef(value));
    inner.clear();
    super._free();
  }

}
