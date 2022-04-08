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
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.PrimitiveIterator;

/**
 * This class is an iterator for primitives.
 *
 * @param <T>      the type of primitive being iterated over
 * @param <T_CONS> the type of primitive consumer
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public class RefPrimitiveIterator<T, T_CONS> extends RefIteratorBase<T> {

  private final PrimitiveIterator<T, T_CONS> inner;

  public RefPrimitiveIterator(@RefAware PrimitiveIterator<T, T_CONS> inner) {
    this.inner = inner;
  }

  /**
   * @Override public PrimitiveIterator<T, T_CONS> getInner() {
   * return inner;
   * }
   * @docgenVersion 9
   */
  @Override
  public PrimitiveIterator<T, T_CONS> getInner() {
    return inner;
  }

  /**
   * Class OfInt
   *
   * @author Author
   * @version 1.0
   * @docgenVersion 9
   * @since 1.0
   */
  @RefIgnore
  public static class OfInt extends ReferenceCountingBase implements PrimitiveIterator.OfInt {
    private final PrimitiveIterator.OfInt inner;
    private final ArrayList<ReferenceCounting> list = new ArrayList<>();

    public OfInt(@RefAware OfInt inner) {
      this.inner = inner;
    }

    /**
     * @return whether the inner iterator has a next element
     * @docgenVersion 9
     */
    @Override
    public boolean hasNext() {
      return inner.hasNext();
    }

    /**
     * @Override public int nextInt() {
     * return inner.nextInt();
     * }
     * @docgenVersion 9
     */
    @Override
    public int nextInt() {
      return inner.nextInt();
    }

    /**
     * @param obj the object to track
     * @return this iterator
     * @throws NullPointerException if obj is null
     * @docgenVersion 9
     */
    @Nonnull
    public RefPrimitiveIterator.OfInt track(ReferenceCounting obj) {
      list.add(obj);
      return this;
    }

    /**
     * This method clears the list of reference countings and calls the super method.
     *
     * @docgenVersion 9
     */
    @Override
    protected void _free() {
      list.forEach(referenceCounting -> referenceCounting.freeRef());
      list.clear();
      super._free();
    }
  }

  /**
   * This class is a wrapper for the PrimitiveIterator.OfLong class.
   * It keeps track of a list of ReferenceCounting objects.
   *
   * @docgenVersion 9
   */
  @RefIgnore
  public static class OfLong extends ReferenceCountingBase implements PrimitiveIterator.OfLong {
    private final PrimitiveIterator.OfLong inner;
    private final ArrayList<ReferenceCounting> list = new ArrayList<>();

    public OfLong(@RefAware OfLong inner) {
      this.inner = inner;
    }

    /**
     * @return whether the inner iterator has a next element
     * @docgenVersion 9
     */
    @Override
    public boolean hasNext() {
      return inner.hasNext();
    }

    /**
     * @Override public long nextLong() {
     * return inner.nextLong();
     * }
     * @docgenVersion 9
     */
    @Override
    public long nextLong() {
      return inner.nextLong();
    }

    /**
     * @param obj the object to track
     * @return this iterator
     * @throws NullPointerException if obj is null
     * @docgenVersion 9
     */
    @Nonnull
    public RefPrimitiveIterator.OfLong track(ReferenceCounting obj) {
      list.add(obj);
      return this;
    }

    /**
     * This method clears the list of reference countings and calls the super method.
     *
     * @docgenVersion 9
     */
    @Override
    protected void _free() {
      list.forEach(referenceCounting -> referenceCounting.freeRef());
      list.clear();
      super._free();
    }
  }

  /**
   * This class represents an iterator of double values.
   * The inner iterator is used to store the double values.
   * The list is used to store the reference counting objects.
   *
   * @docgenVersion 9
   */
  @RefIgnore
  public static class OfDouble extends ReferenceCountingBase implements PrimitiveIterator.OfDouble {
    private final PrimitiveIterator.OfDouble inner;
    private final ArrayList<ReferenceCounting> list = new ArrayList<>();

    public OfDouble(@RefAware OfDouble inner) {
      this.inner = inner;
    }

    /**
     * @return whether the inner iterator has a next element
     * @docgenVersion 9
     */
    @Override
    public boolean hasNext() {
      return inner.hasNext();
    }

    /**
     * @Override public double nextDouble() {
     * return inner.nextDouble();
     * }
     * @docgenVersion 9
     */
    @Override
    public double nextDouble() {
      return inner.nextDouble();
    }

    /**
     * @param obj the object to track
     * @return this iterator
     * @throws NullPointerException if obj is null
     * @docgenVersion 9
     */
    @Nonnull
    public RefPrimitiveIterator.OfDouble track(ReferenceCounting obj) {
      list.add(obj);
      return this;
    }

    /**
     * This method clears the list of reference countings and calls the super method.
     *
     * @docgenVersion 9
     */
    @Override
    protected void _free() {
      list.forEach(referenceCounting -> referenceCounting.freeRef());
      list.clear();
      super._free();
    }
  }

}
