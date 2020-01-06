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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@RefAware
@RefIgnore
@SuppressWarnings("unused")
public class RefLinkedBlockingQueue<T> extends RefAbstractQueue<T> implements RefBlockingQueue<T> {

  @NotNull
  private final LinkedBlockingQueue<T> inner;

  public RefLinkedBlockingQueue() {
    inner = new LinkedBlockingQueue<>();
  }

  @NotNull
  @Override
  public BlockingQueue<T> getInner() {
    return inner;
  }

  @NotNull
  public static <T> RefLinkedBlockingQueue<T>[] addRefs(@NotNull RefLinkedBlockingQueue<T>[] array) {
    return Arrays.stream(array).filter((x) -> x != null).map(RefLinkedBlockingQueue::addRef)
        .toArray((x) -> new RefLinkedBlockingQueue[x]);
  }

  @Override
  public boolean add(@RefAware T t) {
    assertAlive();
    return getInner().add(t);
  }

  @Override
  public boolean addAll(@NotNull @RefAware Collection<? extends T> c) {
    assertAlive();
    final BlockingQueue<T> inner = getInner();
    final boolean b = c.stream().allMatch(inner::add);
    RefUtil.freeRef(c);
    return b;
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
  public void put(@RefAware @NotNull T t) throws InterruptedException {
    getInner().put(t);
  }

  @Override
  public boolean offer(@RefAware T t, long timeout, @NotNull @com.simiacryptus.ref.lang.RefAware TimeUnit unit)
      throws InterruptedException {
    return getInner().offer(t, timeout, unit);
  }

  @NotNull
  @Override
  public T take() throws InterruptedException {
    return getInner().take();
  }

  @Nullable
  @Override
  public T poll(long timeout, @NotNull @com.simiacryptus.ref.lang.RefAware TimeUnit unit) throws InterruptedException {
    return getInner().poll(timeout, unit);
  }

  @Override
  public int remainingCapacity() {
    return getInner().remainingCapacity();
  }

  @Override
  public int drainTo(@RefAware @NotNull Collection<? super T> c) {
    return getInner().drainTo(c);
  }

  @Override
  public int drainTo(@RefAware @NotNull Collection<? super T> c, int maxElements) {
    return getInner().drainTo(c, maxElements);
  }

  @Override
  protected void _free() {
    inner.forEach(RefUtil::freeRef);
    inner.clear();
    super._free();
  }

  @Override
  public @NotNull RefLinkedBlockingQueue<T> addRef() {
    return (RefLinkedBlockingQueue) super.addRef();
  }
}
