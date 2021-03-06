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

@RefIgnore
public class RefListIterator<T> extends RefIteratorBase<T> implements ListIterator<T> {

  private final ListIterator<T> inner;

  public RefListIterator(@RefAware ListIterator<T> inner) {
    this.inner = inner;
  }

  public ListIterator<T> getInner() {
    return inner;
  }

  @Override
  public void add(@RefAware T t) {
    assert getInner() != null;
    getInner().add(t);
  }

  @Override
  public boolean hasPrevious() {
    assert getInner() != null;
    return getInner().hasPrevious();
  }

  @Override
  public int nextIndex() {
    assert getInner() != null;
    return getInner().nextIndex();
  }

  @Nullable
  @Override
  public T previous() {
    assert getInner() != null;
    current = getInner().previous();
    return RefUtil.addRef(current);
  }

  @Override
  public int previousIndex() {
    assert getInner() != null;
    return getInner().previousIndex();
  }

  @Override
  public void set(@RefAware T t) {
    assert getInner() != null;
    getInner().set(t);
    RefUtil.freeRef(current);
    current = null;
  }

  public @Nonnull
  RefListIterator<T> track(ReferenceCounting obj) {
    super.track(obj);
    return this;
  }
}
