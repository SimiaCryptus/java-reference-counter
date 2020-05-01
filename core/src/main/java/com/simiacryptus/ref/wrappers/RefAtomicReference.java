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

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

@RefIgnore
@SuppressWarnings("unused")
public class RefAtomicReference<V> extends ReferenceCountingBase {
  AtomicReference<V> inner;

  public RefAtomicReference() {
    inner = new AtomicReference<>();
  }

  public RefAtomicReference(@RefAware V obj) {
    inner = new AtomicReference<>(obj);
  }

  public static <T> T get(RefAtomicReference<T> atomicReference) {
    try {
      return atomicReference.get();
    } finally {
      atomicReference.freeRef();
    }
  }

  public @RefAware
  V updateAndGet(UnaryOperator<V> o) {
    synchronized (inner) {
      return RefUtil.addRef(inner.updateAndGet(x -> o.apply(x)));
    }
  }

  public @RefAware
  V getAndSet(@RefAware V value) {
    return inner.getAndSet(value);
  }

  @Override
  public RefAtomicReference<V> addRef() {
    return (RefAtomicReference<V>) super.addRef();
  }

  @RefAware
  public V get() {
    assertAlive();
    return RefUtil.addRef(inner.get());
  }

  public void set(@RefAware V newValue) {
    assertAlive();
    if (null != newValue) RefUtil.assertAlive(newValue);
    //RefUtil.watch(newValue);
    RefUtil.freeRef(inner.getAndSet(newValue));
  }

  protected void _free() {
    RefUtil.freeRef(inner.get());
  }

//  public final void lazySet(@RefAware V newValue) {
//    RefUtil.freeRef(inner.getAndSet(newValue));
//  }
//
//  public final boolean compareAndSet(@RefAware V expect, @RefAware V update) {
//    return inner.compareAndSet(expect, update);
//  }
//
//  public final boolean weakCompareAndSet(@RefAware V expect, @RefAware V update) {
//    return inner.weakCompareAndSet(expect, update);
//  }
//
//  @RefAware
//  public final V getAndSet(@RefAware V newValue) {
//    return inner.getAndSet(newValue);
//  }
//
//  @RefAware
//  public final V getAndUpdate(@RefAware UnaryOperator<V> updateFunction) {
//    return inner.getAndUpdate(updateFunction);
//  }
//
//  public final V updateAndGet(@RefAware UnaryOperator<V> updateFunction) {
//    return inner.updateAndGet(updateFunction);
//  }
//
//  public final V getAndAccumulate(@RefAware V x, @RefAware BinaryOperator<V> accumulatorFunction) {
//    return inner.getAndAccumulate(x,accumulatorFunction);
//  }
//
//  public final V accumulateAndGet(@RefAware V x, @RefAware BinaryOperator<V> accumulatorFunction) {
//    return inner.accumulateAndGet(x,accumulatorFunction);
//  }

}
