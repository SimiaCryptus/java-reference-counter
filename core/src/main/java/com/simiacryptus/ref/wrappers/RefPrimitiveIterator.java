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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.PrimitiveIterator;

@RefAware
@RefIgnore
@SuppressWarnings("unused")
public class RefPrimitiveIterator<T, T_CONS> extends RefIteratorBase<T> {

  private final PrimitiveIterator<T, T_CONS> inner;

  public RefPrimitiveIterator(@RefAware PrimitiveIterator<T, T_CONS> inner) {
    this.inner = inner;
  }

  @Override
  public PrimitiveIterator<T, T_CONS> getInner() {
    return inner;
  }

  @RefAware
  @RefIgnore
  public static class OfInt extends ReferenceCountingBase implements PrimitiveIterator.OfInt {
    private final PrimitiveIterator.OfInt inner;
    private final ArrayList<ReferenceCounting> list = new ArrayList<>();

    public OfInt(@RefAware OfInt inner) {
      this.inner = inner;
    }

    @Override
    public boolean hasNext() {
      return inner.hasNext();
    }

    @Override
    public int nextInt() {
      return inner.nextInt();
    }

    @NotNull
    public RefPrimitiveIterator.OfInt track(ReferenceCounting obj) {
      list.add(obj);
      return this;
    }

    @Override
    protected void _free() {
      list.forEach(ReferenceCounting::freeRef);
      list.clear();
      super._free();
    }

  }

  @RefAware
  @RefIgnore
  public static class OfLong extends ReferenceCountingBase implements PrimitiveIterator.OfLong {
    private final PrimitiveIterator.OfLong inner;
    private final ArrayList<ReferenceCounting> list = new ArrayList<>();

    public OfLong(@RefAware OfLong inner) {
      this.inner = inner;
    }

    @Override
    public boolean hasNext() {
      return inner.hasNext();
    }

    @Override
    public long nextLong() {
      return inner.nextLong();
    }

    @NotNull
    public RefPrimitiveIterator.OfLong track(ReferenceCounting obj) {
      list.add(obj);
      return this;
    }

    @Override
    protected void _free() {
      list.forEach(ReferenceCounting::freeRef);
      list.clear();
      super._free();
    }

  }

  @RefAware
  @RefIgnore
  public static class OfDouble extends ReferenceCountingBase implements PrimitiveIterator.OfDouble {
    private final PrimitiveIterator.OfDouble inner;
    private final ArrayList<ReferenceCounting> list = new ArrayList<>();

    public OfDouble(@RefAware OfDouble inner) {
      this.inner = inner;
    }

    @Override
    public boolean hasNext() {
      return inner.hasNext();
    }

    @Override
    public double nextDouble() {
      return inner.nextDouble();
    }

    @NotNull
    public RefPrimitiveIterator.OfDouble track(ReferenceCounting obj) {
      list.add(obj);
      return this;
    }

    @Override
    protected void _free() {
      list.forEach(ReferenceCounting::freeRef);
      list.clear();
      super._free();
    }

  }

}
