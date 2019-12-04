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

import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.Spliterator;
import java.util.Spliterators;

public abstract class RefAbstractCollection<T> extends ReferenceCountingBase implements Collection<T>, Cloneable, Serializable {

  @NotNull
  public @Override
  RefAbstractCollection<T> addRef() {
    return (RefAbstractCollection<T>) super.addRef();
  }

  @Override
  public synchronized final void clear() {
    getInner().forEach(RefUtil::freeRef);
    getInner().clear();
  }

  @Override
  public final boolean contains(Object o) {
    final boolean returnValue = getInner().contains(o);
    RefUtil.freeRef(o);
    return returnValue;
  }

  @Override
  public final boolean containsAll(@NotNull Collection<?> c) {
    if (c instanceof ReferenceCounting) {
      final boolean returnValue = c.stream().filter(o -> {
        final boolean b = !getInner().contains(o);
        RefUtil.freeRef(o);
        return b;
      }).count() == 0;
      ((ReferenceCounting) c).freeRef();
      return returnValue;
    } else {
      return c.stream().filter(o -> {
        final boolean b = !getInner().contains(o);
        RefUtil.freeRef(o);
        return b;
      }).count() == 0;
    }
  }

  protected abstract Collection<T> getInner();

  @Override
  public final boolean isEmpty() {
    return getInner().isEmpty();
  }

  @NotNull
  @Override
  public final RefIterator<T> iterator() {
    return new RefIterator<>(getInner().iterator());
  }

  @Override
  public synchronized final boolean removeAll(@NotNull Collection<?> c) {
    if (c instanceof ReferenceCounting) {
      final boolean returnValue = c.stream().map(o -> remove(o)).reduce((a, b) -> a || b).orElse(false);
      ((ReferenceCounting) c).freeRef();
      return returnValue;
    } else {
      return c.stream().map(o -> remove(RefUtil.addRef(o))).reduce((a, b) -> a || b).orElse(false);
    }
  }

  @Override
  public final int size() {
    return getInner().size();
  }

  @Override
  public RefSpliterator<T> spliterator() {
    return new RefSpliterator<>(Spliterators.spliterator(getInner(), Spliterator.ORDERED));
  }

  @Override
  public RefStream<T> stream() {
    return new RefStream<T>(getInner().stream());
  }

  @NotNull
  @Override
  public final Object[] toArray() {
    final @NotNull Object[] returnValue = getInner().toArray();
    for (Object x : returnValue) {
      RefUtil.addRef(x);
    }
    return returnValue;
  }

  @NotNull
  @Override
  public final <T1> T1[] toArray(@NotNull T1[] a) {
    final @NotNull T1[] returnValue = getInner().toArray(a);
    for (T1 x : returnValue) {
      RefUtil.addRef(x);
    }
    return returnValue;
  }
}
