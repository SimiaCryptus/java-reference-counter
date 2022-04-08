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
import com.simiacryptus.ref.lang.ReferenceCounting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ListIterator;

/**
 * This class is an iterator for a list of references.
 *
 * @param <T> the type of reference in the list
 * @docgenVersion 9
 */
@RefIgnore
public class RefListIterator<T> extends RefIteratorBase<T> implements ListIterator<T> {

  private final ListIterator<T> inner;

  public RefListIterator(@RefAware ListIterator<T> inner) {
    this.inner = inner;
  }

  /**
   * Returns the inner list iterator.
   *
   * @return the inner list iterator
   * @docgenVersion 9
   */
  public ListIterator<T> getInner() {
    return inner;
  }

  /**
   * Adds the specified element to this set if it is not already present.
   *
   * @param t element to be added to this set
   * @return {@code true} if this set did not already contain the specified element
   * @throws NullPointerException if the specified element is null
   * @docgenVersion 9
   */
  @Override
  public void add(@RefAware T t) {
    assert getInner() != null;
    getInner().add(t);
  }

  /**
   * {@inheritDoc}
   *
   * @docgenVersion 9
   */
  @Override
  public boolean hasPrevious() {
    assert getInner() != null;
    return getInner().hasPrevious();
  }

  /**
   * @return the next index
   * @docgenVersion 9
   */
  @Override
  public int nextIndex() {
    assert getInner() != null;
    return getInner().nextIndex();
  }

  /**
   * @return the previous element in the list, or null if the list is empty
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public T previous() {
    assert getInner() != null;
    current = getInner().previous();
    return RefUtil.addRef(current);
  }

  /**
   * @return the index of the previous element, or -1 if there is no such element
   * @docgenVersion 9
   */
  @Override
  public int previousIndex() {
    assert getInner() != null;
    return getInner().previousIndex();
  }

  /**
   * @param t
   * @docgenVersion 9
   */
  @Override
  public void set(@RefAware T t) {
    assert getInner() != null;
    getInner().set(t);
    RefUtil.freeRef(current);
    current = null;
  }

  /**
   * @param obj the object to track
   * @return this RefListIterator
   * @docgenVersion 9
   */
  public @Nonnull
  RefListIterator<T> track(ReferenceCounting obj) {
    super.track(obj);
    return this;
  }
}
