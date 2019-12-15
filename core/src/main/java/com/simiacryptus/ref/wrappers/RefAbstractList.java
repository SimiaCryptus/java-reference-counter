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

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The type Ref abstract list.
 *
 * @param <T> the type parameter
 */
@RefAware
@RefIgnore
public abstract class RefAbstractList<T> extends RefAbstractCollection<T> implements RefList<T> {
  @Override
  protected void _free() {
    clear();
    super._free();
  }

  @Override
  public void add(int index, T element) {
    assertAlive();
    getInner().add(index, element);
  }

  @Override
  public final boolean add(T o) {
    assertAlive();
    return getInner().add(o);
  }

  @Override
  public boolean addAll(int index, @NotNull Collection<? extends T> c) {
    assertAlive();
    final boolean changed = getInner().addAll(index, c);
    RefUtil.freeRef(c);
    return changed;
  }

  @Override
  public final boolean addAll(@NotNull Collection<? extends T> c) {
    assertAlive();
    final Boolean returnValue = RefCollections.getInnerStream(c)
        .map(o -> add(RefUtil.addRef(o)))
        .reduce((a, b) -> a || b)
        .orElse(false);
    RefUtil.freeRef(c);
    return returnValue;

  }

  @NotNull
  public @Override
  RefAbstractList<T> addRef() {
    assertAlive();
    return (RefAbstractList<T>) super.addRef();
  }

  @Override
  public T get(int index) {
    assertAlive();
    return RefUtil.addRef(getInner().get(index));
  }

  public abstract List<T> getInner();

  @Override
  public int indexOf(Object o) {
    assertAlive();
    final int index = getInner().indexOf(o);
    RefUtil.freeRef(o);
    return index;
  }

  @Override
  public int lastIndexOf(Object o) {
    assertAlive();
    final int index = getInner().lastIndexOf(o);
    RefUtil.freeRef(o);
    return index;
  }

  @NotNull
  @Override
  public RefListIterator<T> listIterator() {
    assertAlive();
    return new RefListIterator<>(getInner().listIterator()).track(this.addRef());
  }


  @NotNull
  @Override
  public RefListIterator<T> listIterator(int index) {
    assertAlive();
    return new RefListIterator<>(getInner().listIterator(index));
  }

  @Override
  public T remove(int index) {
    assertAlive();
    return getInner().remove(index);
  }

  @Override
  public final boolean remove(Object item) {
    assertAlive();
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
  public synchronized final boolean retainAll(@NotNull Collection<?> c) {
    assertAlive();
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
    assertAlive();
    return getInner().set(index, element);
  }

  @NotNull
  @Override
  public RefArrayList<T> subList(int fromIndex, int toIndex) {
    assertAlive();
    return new RefArrayList<T>(getInner().subList(fromIndex, toIndex));
  }

}
