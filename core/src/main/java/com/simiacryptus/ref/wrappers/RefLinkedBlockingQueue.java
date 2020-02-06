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
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@RefIgnore
@SuppressWarnings("unused")
public class RefLinkedBlockingQueue<T> extends RefAbstractQueue<T> implements RefBlockingQueue<T> {

  @Nonnull
  private final LinkedBlockingQueue<T> inner;

  public RefLinkedBlockingQueue() {
    inner = new LinkedBlockingQueue<>();
  }

  @Nonnull
  @Override
  public BlockingQueue<T> getInner() {
    return inner;
  }

  @Override
  public boolean add(@Nonnull @RefAware T t) {
    assertAlive();
    return getInner().add(t);
  }

  @Override
  public boolean addAll(@Nonnull @RefAware Collection<? extends T> c) {
    assertAlive();
    final BlockingQueue<T> inner = getInner();
    final boolean b = c.stream().allMatch(e -> inner.add(e));
    RefUtil.freeRef(c);
    return b;
  }

  @Override
  public boolean offer(@Nonnull @RefAware T t) {
    assertAlive();
    final boolean b = getInner().offer(t);
    if (!b)
      RefUtil.freeRef(t);
    return b;
  }

  @Override
  public void put(@RefAware @Nonnull T t) throws InterruptedException {
    getInner().put(t);
  }

  @Override
  public boolean offer(@RefAware T t, long timeout, @Nonnull @RefAware TimeUnit unit)
      throws InterruptedException {
    return getInner().offer(t, timeout, unit);
  }

  @Nonnull
  @Override
  @RefAware
  public T take() throws InterruptedException {
    return getInner().take();
  }

  @Nullable
  @Override
  @RefAware
  public T poll(long timeout, @Nonnull @RefAware TimeUnit unit) throws InterruptedException {
    return getInner().poll(timeout, unit);
  }

  @Override
  public int remainingCapacity() {
    return getInner().remainingCapacity();
  }

  @Override
  public int drainTo(@RefAware @Nonnull Collection<? super T> c) {
    int drain = getInner().drainTo(c);
    RefUtil.freeRef(c);
    return drain;
  }

  @Override
  public int drainTo(@RefAware @Nonnull Collection<? super T> c, int maxElements) {
    return getInner().drainTo(c, maxElements);
  }

  @Override @Nonnull
  public RefLinkedBlockingQueue<T> addRef() {
    return (RefLinkedBlockingQueue) super.addRef();
  }

  @Override
  protected void _free() {
    ArrayList<T> list = new ArrayList<>(inner.size());
    inner.drainTo(list);
    list.forEach(value -> RefUtil.freeRef(value));
    super._free();
  }
}
