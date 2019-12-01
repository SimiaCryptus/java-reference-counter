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

import com.simiacryptus.ref.lang.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

@RefAware
@RefCoderIgnore
public abstract class RefAbstractSet<T> extends ReferenceCountingBase implements RefSet<T>, Cloneable, Serializable {
  @Override
  protected void _free() {
    clear();
    super._free();
  }

  @Override
  public final boolean add(T o) {
    final T replaced = getInner().put(o, o);
    if (null != replaced) {
      RefUtil.freeRef(replaced);
      return false;
    } else {
      return true;
    }
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
  RefHashSet<T> addRef() {
    return (RefHashSet<T>) super.addRef();
  }

  @Override
  public synchronized final void clear() {
    getInner().keySet().forEach(RefUtil::freeRef);
    getInner().clear();
  }

  @Override
  public void forEach(Consumer<? super T> action) {
    for (T t : getInner().keySet()) {
      action.accept(RefUtil.addRef(t));
    }
    RefUtil.freeRef(action);
  }

  @Override
  public final boolean contains(Object o) {
    final boolean returnValue = getInner().containsKey(o);
    RefUtil.freeRef(o);
    return returnValue;
  }

  @Override
  public final boolean containsAll(@NotNull Collection<?> c) {
    if (c instanceof ReferenceCounting) {
      final boolean returnValue = c.stream().filter(o -> {
        final boolean b = !getInner().containsKey(o);
        RefUtil.freeRef(o);
        return b;
      }).count() == 0;
      ((ReferenceCounting) c).freeRef();
      return returnValue;
    } else {
      return c.stream().filter(o -> !getInner().containsKey(o)).count() == 0;
    }
  }

  public abstract Map<T, T> getInner();

  @Override
  public final boolean isEmpty() {
    return getInner().isEmpty();
  }

  @NotNull
  @Override
  public final Iterator<T> iterator() {
    return new RefIterator<>(getInner().keySet().iterator());
  }

  @Override
  public final boolean remove(Object o) {
    final T removed = getInner().remove(o);
    RefUtil.freeRef(o);
    if (null != removed) {
      RefUtil.freeRef(removed);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public synchronized final boolean removeAll(@NotNull Collection<?> c) {
    if (c instanceof ReferenceCounting) {
      final boolean returnValue = ((RefStream<?>) c.stream()).map(o -> {
        final T remove = getInner().remove(o);
        RefUtil.freeRef(o);
        final boolean b = remove != null;
        if (b) RefUtil.freeRef(o);
        return b;
      }).reduce((a, b) -> a || b).orElse(false);
      ((ReferenceCounting) c).freeRef();
      return returnValue;
    } else {
      return c.stream().map(o -> {
        final T remove = getInner().remove(o);
        final boolean found = remove != null;
        if (found) RefUtil.freeRef(o);
        return found;
      }).reduce((a, b) -> a || b).orElse(false);
    }
  }

  @Override
  public synchronized final boolean retainAll(@NotNull Collection<?> c) {
    final Object[] toRemove;
    if (c instanceof ReferenceCounting) {
      toRemove = getInner().keySet().stream().filter(o -> !c.contains(RefUtil.addRef(o))).toArray();
      ((ReferenceCounting) c).freeRef();
    } else {
      toRemove = getInner().keySet().stream().filter(o -> !c.contains(o)).map(RefUtil::addRef).toArray();
    }
    for (Object o : toRemove) {
      getInner().remove(o);
      RefUtil.freeRef(o);
    }
    return 0 < toRemove.length;
  }

  @Override
  public final int size() {
    return getInner().size();
  }

  @Override
  public final RefStream<T> stream() {
    return new RefStream<T>(getInner().keySet().stream());
  }

  @NotNull
  @Override
  public final Object[] toArray() {
    final @NotNull Object[] returnValue = getInner().keySet().toArray();
    for (Object x : returnValue) {
      RefUtil.addRef(x);
    }
    return returnValue;
  }

  @NotNull
  @Override
  public final <T1> T1[] toArray(@NotNull T1[] a) {
    final @NotNull T1[] returnValue = getInner().keySet().toArray(a);
    for (T1 x : returnValue) {
      RefUtil.addRef(x);
    }
    return returnValue;
  }
}
