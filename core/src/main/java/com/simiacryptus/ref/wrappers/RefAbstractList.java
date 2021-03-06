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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

@RefIgnore
@SuppressWarnings("unused")
public abstract class RefAbstractList<T> extends RefAbstractCollection<T> implements RefList<T> {
  @Nonnull
  public abstract List<T> getInner();

  @Override
  public void add(int index, @RefAware T element) {
    assertAlive();
    getInner().add(index, element);
  }

  @Override
  public final boolean add(@RefAware T o) {
    assertAlive();
    return getInner().add(o);
  }

  @Override
  public boolean addAll(int index, @Nonnull @RefAware Collection<? extends T> c) {
    assertAlive();
    final boolean changed = getInner().addAll(index, c);
    RefUtil.freeRef(c);
    return changed;
  }

  @Override
  public final boolean addAll(@Nonnull @RefAware Collection<? extends T> collection) {
    assertAlive();
    try {
      if (collection.isEmpty()) return false;
      Iterator<? extends T> iterator = collection.iterator();
      if (iterator instanceof ReferenceCounting) {
        while (iterator.hasNext()) {
          add(iterator.next());
        }
        ((ReferenceCounting) iterator).freeRef();
      } else {
        while (iterator.hasNext()) {
          add(RefUtil.addRef(iterator.next()));
        }
      }
      return true;
    } finally {
      RefUtil.freeRef(collection);
    }
  }

  @Nonnull
  public @Override
  RefAbstractList<T> addRef() {
    assertAlive();
    return (RefAbstractList<T>) super.addRef();
  }

  @Nullable
  @Override
  @RefIgnore
  @RefAware
  public T get(int index) {
    assertAlive();
    return RefUtil.addRef(getInner().get(index));
  }

  @Override
  public int indexOf(@RefAware Object o) {
    assertAlive();
    final int index = getInner().indexOf(o);
    RefUtil.freeRef(o);
    return index;
  }

  @Override
  public int lastIndexOf(@RefAware Object o) {
    assertAlive();
    final int index = getInner().lastIndexOf(o);
    RefUtil.freeRef(o);
    return index;
  }

  @Nonnull
  @Override
  public RefListIterator<T> listIterator() {
    assertAlive();
    return new RefListIterator<>(getInner().listIterator()).track(this.addRef());
  }

  @Nonnull
  @Override
  public RefListIterator<T> listIterator(int index) {
    assertAlive();
    return new RefListIterator<>(getInner().listIterator(index));
  }

  @Override
  @RefIgnore
  @RefAware
  public T remove(int index) {
    assertAlive();
    return getInner().remove(index);
  }

  @Override
  public final boolean remove(@RefAware Object item) {
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
  public synchronized final boolean retainAll(@Nonnull @RefAware Collection<?> c) {
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
  @RefIgnore
  @RefAware
  public T set(int index, @RefAware T element) {
    assertAlive();
    return getInner().set(index, element);
  }

  @Nonnull
  @Override
  public RefArrayList<T> subList(int fromIndex, int toIndex) {
    assertAlive();
    return new RefArrayList<T>(getInner().subList(fromIndex, toIndex));
  }

  @Override
  protected void _free() {
    clear();
    super._free();
  }

}
