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
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

@RefIgnore
public abstract class RefAbstractCollection<T> extends ReferenceCountingBase
    implements RefCollection<T>, Cloneable, Serializable {

  @Nonnull
  public abstract Collection<T> getInner();

  @Override
  public final boolean isEmpty() {
    assertAlive();
    return getInner().isEmpty();
  }

  @Nonnull
  public @Override
  RefAbstractCollection<T> addRef() {
    return (RefAbstractCollection<T>) super.addRef();
  }

  @Override
  public synchronized void clear() {
    getInner().forEach(value -> RefUtil.freeRef(value));
    getInner().clear();
  }

  @Override
  public final boolean contains(@RefAware Object o) {
    assertAlive();
    final boolean returnValue = getInner().contains(o);
    RefUtil.freeRef(o);
    return returnValue;
  }

  @Override
  public final boolean containsAll(@Nonnull @RefAware Collection<?> c) {
    assertAlive();
    final Collection<?> c_inner;
    if (c instanceof RefAbstractCollection) {
      c_inner = ((RefAbstractCollection<?>) c).getInner();
    } else {
      c_inner = c;
    }
    final Collection<T> inner = getInner();
    final boolean b = !c_inner.stream().anyMatch(o -> !inner.contains(o));
    RefUtil.freeRef(c);
    return b;
  }

  @Nonnull
  @Override
  public final RefIterator<T> iterator() {
    assertAlive();
    return new RefIterator<>(getInner().iterator()).track(this.addRef());
  }

  @Override
  public synchronized boolean removeAll(@Nonnull @RefAware Collection<?> c) {
    assertAlive();
    final Collection<?> c_inner;
    if (c instanceof RefAbstractCollection) {
      c_inner = ((RefAbstractCollection<?>) c).getInner();
    } else {
      c_inner = c;
    }
    boolean b = false;
    final Iterator<T> iterator = getInner().iterator();
    while (iterator.hasNext()) {
      final T next = iterator.next();
      if (c_inner.contains(next)) {
        iterator.remove();
        RefUtil.freeRef(next);
        b = true;
      }
    }
    RefUtil.freeRef(c);
    return b;
  }

  @Override
  public boolean retainAll(@Nonnull @RefAware Collection<?> c) {
    assertAlive();
    final Collection<?> c_inner;
    if (c instanceof RefAbstractCollection) {
      c_inner = ((RefAbstractCollection<?>) c).getInner();
    } else {
      c_inner = c;
    }
    boolean b = false;
    final Iterator<T> iterator = getInner().iterator();
    while (iterator.hasNext()) {
      final T next = iterator.next();
      if (!c_inner.contains(next)) {
        iterator.remove();
        RefUtil.freeRef(next);
        b = true;
      }
    }
    RefUtil.freeRef(c);
    return b;
  }

  @Override
  public final int size() {
    assertAlive();
    return getInner().size();
  }

  @Override
  public RefSpliterator<T> spliterator() {
    return RefCollection.super.spliterator();
  }

  @Override
  public RefStream<T> stream() {
    return RefCollection.super.stream();
  }

  @Nonnull
  @Override
  @RefAware
  public final Object[] toArray() {
    assertAlive();
    final @Nonnull Object[] returnValue = getInner().toArray();
    for (Object x : returnValue) {
      RefUtil.addRef(x);
    }
    return returnValue;
  }

  @Nonnull
  @Override
  @RefAware
  public final <T1> T1[] toArray(@Nonnull @RefAware T1[] a) {
    assertAlive();
    final @Nonnull T1[] returnValue = getInner().toArray(a);
    for (T1 x : returnValue) {
      RefUtil.addRef(x);
    }
    return returnValue;
  }
}
