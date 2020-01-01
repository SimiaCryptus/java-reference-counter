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

import com.simiacryptus.ref.lang.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * The type Ref spliterator.
 *
 * @param <T> the type parameter
 */
@RefAware
@RefIgnore
public class RefSpliterator<T> extends ReferenceCountingBase implements Spliterator<T> {
  private final Spliterator<T> inner;
  private final StackTraceElement[] createdBy = Thread.currentThread().getStackTrace();
  private final ArrayList<ReferenceCounting> list = new ArrayList<>();
  private long size;

  /**
   * Instantiates a new Ref spliterator.
   *
   * @param inner the inner
   */
  public RefSpliterator(Spliterator<T> inner) {
    this(inner, Long.MAX_VALUE);

  }

  /**
   * Instantiates a new Ref spliterator.
   *
   * @param inner the inner
   * @param size  the size
   */
  public RefSpliterator(Spliterator<T> inner, long size) {
    if (inner instanceof RefSpliterator) {
      this.inner = ((RefSpliterator) inner).getInner();
      assert !(this.inner instanceof RefSpliterator);
      this.size = size;
    } else {
      this.inner = inner;
      this.size = size;
    }
  }

  /**
   * Gets inner.
   *
   * @return the inner
   */
  public Spliterator<T> getInner() {
    return inner;
  }

  /**
   * Add refs ref spliterator [ ].
   *
   * @param <T>   the type parameter
   * @param array the array
   * @return the ref spliterator [ ]
   */
  public static <T> RefSpliterator<T>[] addRefs(@NotNull RefSpliterator<T>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefSpliterator::addRef)
        .toArray((x) -> new RefSpliterator[x]);
  }

  public RefSpliterator<T> addRef() {
    return (RefSpliterator<T>) super.addRef();
  }

  @Override
  public int characteristics() {
    return this.getInner().characteristics();
  }

  @Override
  public long estimateSize() {
    return size;
  }

  /**
   * Track ref spliterator.
   *
   * @param obj the obj
   * @return the ref spliterator
   */
  public RefSpliterator<T> track(ReferenceCounting obj) {
    list.add(obj);
    return this;
  }

  @Override
  public boolean tryAdvance(@NotNull Consumer<? super T> action) {
    return getInner().tryAdvance(t -> action.accept(getRef(t)));
  }

  @Nullable
  @Override
  public Spliterator<T> trySplit() {
    return this.getInner().trySplit();
//    final Spliterator<T> trySplit = this.inner.trySplit();
//    return null == trySplit ? null : new RefSpliterator<>(trySplit, size).track(this.addRef());
  }

  @Override
  protected void _free() {
    list.forEach(ReferenceCounting::freeRef);
    list.clear();
    RefUtil.freeRef(this.inner);
    //RefUtil.freeInternals(inner);
    super._free();
  }

  @Nullable
  protected T getRef(T t) {
    return RefUtil.addRef(t);
  }

  /**
   * The type Of double.
   *
   * @param <T> the type parameter
   */
  @RefAware
  @RefIgnore
  public static class OfDouble<T> extends ReferenceCountingBase implements Spliterator.OfDouble {

    private final Spliterator.OfDouble inner;
    private final ArrayList<ReferenceCounting> list = new ArrayList<>();

    /**
     * Instantiates a new Of double.
     *
     * @param inner the inner
     */
    public OfDouble(OfDouble inner) {
      this.inner = inner;
    }

    @Override
    public int characteristics() {
      return this.inner.characteristics();
    }

    @Override
    public long estimateSize() {
      return this.inner.estimateSize();
    }

    /**
     * Track ref spliterator . of double.
     *
     * @param obj the obj
     * @return the ref spliterator . of double
     */
    public RefSpliterator.OfDouble track(ReferenceCounting obj) {
      list.add(obj);
      return this;
    }

    @Override
    public boolean tryAdvance(DoubleConsumer action) {
      return this.inner.tryAdvance(action);
    }

    @Override
    public RefSpliterator.OfDouble trySplit() {
      return new RefSpliterator.OfDouble(this.inner.trySplit());
    }

    @Override
    protected void _free() {
      list.forEach(ReferenceCounting::freeRef);
      list.clear();
      super._free();
    }

  }

  /**
   * The type Of long.
   */
  @RefAware
  @RefIgnore
  public static class OfLong extends ReferenceCountingBase implements Spliterator.OfLong {

    private final Spliterator.OfLong inner;
    private final ArrayList<ReferenceCounting> list = new ArrayList<>();

    /**
     * Instantiates a new Of long.
     *
     * @param inner the inner
     */
    public OfLong(OfLong inner) {
      this.inner = inner;
    }

    @Override
    public int characteristics() {
      return this.inner.characteristics();
    }

    @Override
    public long estimateSize() {
      return this.inner.estimateSize();
    }

    /**
     * Track ref spliterator . of long.
     *
     * @param obj the obj
     * @return the ref spliterator . of long
     */
    public RefSpliterator.OfLong track(ReferenceCounting obj) {
      list.add(obj);
      return this;
    }

    ;

    @Override
    public boolean tryAdvance(LongConsumer action) {
      return this.inner.tryAdvance(action);
    }

    @Override
    public RefSpliterator.OfLong trySplit() {
      return new RefSpliterator.OfLong(this.inner.trySplit());
    }

    @Override
    protected void _free() {
      list.forEach(ReferenceCounting::freeRef);
      list.clear();
      super._free();
    }


  }

  /**
   * The type Of int.
   */
  @RefAware
  @RefIgnore
  public static class OfInt extends ReferenceCountingBase implements Spliterator.OfInt {

    private final Spliterator.OfInt inner;
    private final ArrayList<ReferenceCounting> list = new ArrayList<>();

    /**
     * Instantiates a new Of int.
     *
     * @param inner the inner
     */
    public OfInt(OfInt inner) {
      this.inner = inner;
    }

    @Override
    public int characteristics() {
      return this.inner.characteristics();
    }

    @Override
    public long estimateSize() {
      return this.inner.estimateSize();
    }

    /**
     * Track ref spliterator . of int.
     *
     * @param obj the obj
     * @return the ref spliterator . of int
     */
    public RefSpliterator.OfInt track(ReferenceCounting obj) {
      list.add(obj);
      return this;
    }

    @Override
    public boolean tryAdvance(IntConsumer action) {
      return this.inner.tryAdvance(action);
    }

    @Override
    public RefSpliterator.OfInt trySplit() {
      return new RefSpliterator.OfInt(this.inner.trySplit());
    }

    @Override
    protected void _free() {
      list.forEach(ReferenceCounting::freeRef);
      list.clear();
      super._free();
    }


  }
}
