/*
 * Copyright (c) 2019 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.ref.wrappers;

import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import java.util.ArrayList;
import java.util.PrimitiveIterator;

/**
 * The type Ref primitive iterator.
 *
 * @param <T>      the type parameter
 * @param <T_CONS> the type parameter
 */
public class RefPrimitiveIterator<T, T_CONS> extends RefIteratorBase<T> {

  private final PrimitiveIterator<T, T_CONS> inner;

  /**
   * Instantiates a new Ref primitive iterator.
   *
   * @param inner the inner
   */
  public RefPrimitiveIterator(PrimitiveIterator<T, T_CONS> inner) {
    this.inner = inner;
  }

  @Override
  public PrimitiveIterator<T, T_CONS> getInner() {
    return inner;
  }

  /**
   * The type Of int.
   */
  public static class OfInt extends ReferenceCountingBase implements PrimitiveIterator.OfInt {
    private final PrimitiveIterator.OfInt inner;
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
    protected void _free() {
      list.forEach(ReferenceCounting::freeRef);
      list.clear();
      super._free();
    }

    @Override
    public boolean hasNext() {
      return inner.hasNext();
    }

    @Override
    public int nextInt() {
      return inner.nextInt();
    }

    /**
     * Track ref primitive iterator . of int.
     *
     * @param obj the obj
     * @return the ref primitive iterator . of int
     */
    public RefPrimitiveIterator.OfInt track(ReferenceCounting obj) {
      list.add(obj);
      return this;
    }

  }

  /**
   * The type Of long.
   */
  public static class OfLong extends ReferenceCountingBase implements PrimitiveIterator.OfLong {
    private final PrimitiveIterator.OfLong inner;
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
    protected void _free() {
      list.forEach(ReferenceCounting::freeRef);
      list.clear();
      super._free();
    }

    @Override
    public boolean hasNext() {
      return inner.hasNext();
    }

    @Override
    public long nextLong() {
      return inner.nextLong();
    }

    /**
     * Track ref primitive iterator . of long.
     *
     * @param obj the obj
     * @return the ref primitive iterator . of long
     */
    public RefPrimitiveIterator.OfLong track(ReferenceCounting obj) {
      list.add(obj);
      return this;
    }

  }

  /**
   * The type Of double.
   */
  public static class OfDouble extends ReferenceCountingBase implements PrimitiveIterator.OfDouble {
    private final PrimitiveIterator.OfDouble inner;
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
    protected void _free() {
      list.forEach(ReferenceCounting::freeRef);
      list.clear();
      super._free();
    }

    @Override
    public boolean hasNext() {
      return inner.hasNext();
    }

    @Override
    public double nextDouble() {
      return inner.nextDouble();
    }

    /**
     * Track ref primitive iterator . of double.
     *
     * @param obj the obj
     * @return the ref primitive iterator . of double
     */
    public RefPrimitiveIterator.OfDouble track(ReferenceCounting obj) {
      list.add(obj);
      return this;
    }

  }

}
