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
import com.simiacryptus.ref.lang.RefUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Queue;

public abstract class RefAbstractQueue<T> extends RefAbstractCollection<T> implements RefQueue<T> {
  @Nonnull
  public abstract Queue<T> getInner();

  @Nonnull
  public @Override
  RefAbstractQueue<T> addRef() {
    return (RefAbstractQueue<T>) super.addRef();
  }

  @Nullable
  @Override
  public T element() {
    assertAlive();
    return RefUtil.addRef(getInner().element());
  }

  @Nullable
  @Override
  public T peek() {
    assertAlive();
    return RefUtil.addRef(getInner().peek());
  }

  @Override
  public T poll() {
    assertAlive();
    return getInner().poll();
  }

  @Override
  public boolean remove(@RefAware Object o) {
    assertAlive();
    final boolean remove = getInner().remove(o);
    if (remove)
      RefUtil.freeRef(o);
    RefUtil.freeRef(o);
    return remove;
  }

  @Override
  public T remove() {
    assertAlive();
    return getInner().remove();
  }
}
