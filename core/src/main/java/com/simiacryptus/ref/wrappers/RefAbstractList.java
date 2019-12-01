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
import java.util.*;
import java.util.stream.IntStream;

public abstract class RefAbstractList<T> extends ReferenceCountingBase implements RefList<T>, Cloneable, Serializable {
  @Override
  protected void _free() {
    clear();
    super._free();
  }

  @Override
  public void add(int index, T element) {
    getInner().add(index, element);
  }

  @Override
  public final boolean add(T o) {
    return getInner().add(o);
  }

  @Override
  public boolean addAll(int index, @NotNull Collection<? extends T> c) {
    final boolean changed = getInner().addAll(index, c);
    RefUtil.freeRef(c);
    return changed;
  }

  @Override
  public final boolean addAll(@NotNull Collection<? extends T> c) {
    if (c instanceof ReferenceCounting) {
      final boolean returnValue = c.stream().map(o -> add(o)).reduce((a, b) -> a || b).orElse(false);
      ((ReferenceCounting) c).freeRef();
      return returnValue;
    } else {
      return c.stream().map(o -> add(RefUtil.addRef(o))).reduce((a, b) -> a || b).orElse(false);
    }

  }

  @NotNull
  public @Override
  RefArrayList<T> addRef() {
    return (RefArrayList<T>) super.addRef();
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

  @Override
  public T get(int index) {
    return RefUtil.addRef(getInner().get(index));
  }

  public abstract List<T> getInner();

  @Override
  public int indexOf(Object o) {
    final int index = getInner().indexOf(o);
    RefUtil.freeRef(o);
    return index;
  }

  @Override
  public final boolean isEmpty() {
    return getInner().isEmpty();
  }

  @NotNull
  @Override
  public final Iterator<T> iterator() {
    return new RefIterator<>(getInner().iterator());
  }

  @Override
  public int lastIndexOf(Object o) {
    final int index = getInner().lastIndexOf(o);
    RefUtil.freeRef(o);
    return index;
  }

  @NotNull
  @Override
  public RefListIterator<T> listIterator() {
    return new RefListIterator<>(getInner().listIterator());
  }

  @NotNull
  @Override
  public RefListIterator<T> listIterator(int index) {
    return new RefListIterator<>(getInner().listIterator(index));
  }

  @Override
  public T remove(int index) {
    return getInner().remove(index);
  }

  @Override
  public final boolean remove(Object item) {
    final int index = getInner().indexOf(item);
    if (index >= 0) {
      final T remove = getInner().remove(index);
      assert null != remove;
      RefUtil.freeRef(remove);
      RefUtil.freeRef(item);
      return true;
    } else {
      RefUtil.freeRef(item);
      return false;
    }
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
  public synchronized final boolean retainAll(@NotNull Collection<?> c) {
    final Object[] toRemove;
    final int[] indicesToRemove;
    if (c instanceof ReferenceCounting) {
      indicesToRemove = IntStream.range(0, size()).filter(idx -> {
        return !c.contains(RefUtil.addRef(getInner().get(idx)));
      }).toArray();
      ((ReferenceCounting) c).freeRef();
    } else {
      indicesToRemove = IntStream.range(0, size()).filter(idx -> {
        return !c.contains(getInner().get(idx));
      }).toArray();
    }
    Arrays.stream(indicesToRemove).mapToObj(x -> -x).sorted().map(x -> -x).forEachOrdered(idx -> remove(idx));
    return 0 < indicesToRemove.length;
  }

  @Override
  public T set(int index, T element) {
    return getInner().set(index, element);
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
  public final RefStream<T> stream() {
    return new RefStream<T>(getInner().stream());
  }

  @NotNull
  @Override
  public RefArrayList<T> subList(int fromIndex, int toIndex) {
    return new RefArrayList<T>(getInner().subList(fromIndex, toIndex));
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
