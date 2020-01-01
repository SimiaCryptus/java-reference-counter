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
import java.util.Iterator;

/**
 * The type Ref iterator base.
 *
 * @param <T> the type parameter
 */
@RefAware
@RefIgnore
@SuppressWarnings("unused")
public abstract class RefIteratorBase<T> extends ReferenceCountingBase implements Iterator<T> {
  /**
   * The List.
   */
  protected final ArrayList<ReferenceCounting> list = new ArrayList<>();
  /**
   * The Current.
   */
  @Nullable
  protected T current;

  /**
   * Gets inner.
   *
   * @return the inner
   */
  @Nullable
  public Iterator<T> getInner() {
    return null;
  }

  @Override
  public boolean hasNext() {
    return getInner().hasNext();
  }

  @Nullable
  @Override
  public T next() {
    current = getInner().next();
    return RefUtil.addRef((T) current);
  }

  @Override
  public void remove() {
    getInner().remove();
    RefUtil.freeRef(current);
    current = null;
  }

  /**
   * Track ref iterator base.
   *
   * @param obj the obj
   * @return the ref iterator base
   */
  public @NotNull RefIteratorBase<T> track(ReferenceCounting obj) {
    list.add(obj);
    return this;
  }

  @NotNull
  @Override
  public RefIteratorBase<T> addRef() {
    return (RefIteratorBase<T>) super.addRef();
  }

  @Override
  protected void _free() {
    list.forEach(ReferenceCounting::freeRef);
    list.clear();
    super._free();
  }
}
