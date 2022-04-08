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

import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class provides a base implementation for iterating over a list of reference-counted objects.
 *
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public abstract class RefIteratorBase<T> extends ReferenceCountingBase implements Iterator<T> {
  protected final ArrayList<ReferenceCounting> list = new ArrayList<>();
  @Nullable
  protected T current;

  /**
   * @return an iterator over the innermost elements of this structure, or null if there are no innermost elements
   * @docgenVersion 9
   */
  @Nullable
  public Iterator<T> getInner() {
    return null;
  }

  /**
   * @return whether the inner iterator has a next element
   * @docgenVersion 9
   */
  @Override
  public boolean hasNext() {
    return getInner().hasNext();
  }

  /**
   * @return the next element in the iteration, or null if there are no more elements
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public T next() {
    final Iterator<T> inner = getInner();
    assert !(inner instanceof ReferenceCounting);
    assert inner != null;
    current = inner.next();
    return RefUtil.addRef(current);
  }

  /**
   * Remove the current element from the iterator.
   *
   * @docgenVersion 9
   */
  @Override
  public void remove() {
    getInner().remove();
    RefUtil.freeRef(current);
    current = null;
  }

  /**
   * @param obj the object to track
   * @return this RefIteratorBase
   * @throws NullPointerException if obj is null
   * @docgenVersion 9
   */
  public @Nonnull
  RefIteratorBase<T> track(ReferenceCounting obj) {
    list.add(obj);
    return this;
  }

  /**
   * Adds a reference to this object.
   *
   * @return A reference to this object.
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefIteratorBase<T> addRef() {
    return (RefIteratorBase<T>) super.addRef();
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
