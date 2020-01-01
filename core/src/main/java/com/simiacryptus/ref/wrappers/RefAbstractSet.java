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
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The type Ref abstract set.
 *
 * @param <T> the type parameter
 */
@RefAware
@RefIgnore
public abstract class RefAbstractSet<T> extends RefAbstractCollection<T> implements RefSet<T>, Cloneable, Serializable {
  public Collection<T> getInner() {
    return getInnerMap().keySet();
  }

  /**
   * Gets inner map.
   *
   * @return the inner map
   */
  public abstract Map<T, T> getInnerMap();

  @Override
  public final boolean add(T o) {
    assertAlive();
    final T replaced = getInnerMap().put(o, o);
    if (null != replaced) {
      RefUtil.freeRef(replaced);
      return false;
    } else {
      return true;
    }
  }

  @Override
  public final boolean addAll(@NotNull Collection<? extends T> c) {
    assertAlive();
    final Collection<? extends T> c_inner;
    if (c instanceof RefAbstractCollection) {
      c_inner = ((RefAbstractCollection<? extends T>) c).getInner();
    } else {
      c_inner = c;
    }
    final boolean returnValue = c_inner.stream().map(o -> add(RefUtil.addRef(o))).reduce((a, b) -> a || b).orElse(false);
    RefUtil.freeRef(c);
    return returnValue;
  }

  @NotNull
  public @Override
  RefAbstractSet<T> addRef() {
    return (RefAbstractSet<T>) super.addRef();
  }

  @Override
  public synchronized final void clear() {
    getInnerMap().keySet().forEach(RefUtil::freeRef);
    getInnerMap().clear();
  }

  @Override
  public void forEach(Consumer<? super T> action) {
    assertAlive();
    for (T t : getInnerMap().keySet()) {
      action.accept(RefUtil.addRef(t));
    }
    RefUtil.freeRef(action);
  }

  @Override
  public final boolean remove(Object o) {
    assertAlive();
    final T removed = getInnerMap().remove(o);
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
    assertAlive();
    final Collection<?> c_inner;
    if (c instanceof RefAbstractCollection) {
      c_inner = ((RefAbstractCollection<?>) c).getInner();
    } else {
      c_inner = c;
    }
    final Iterator<Map.Entry<T, T>> iterator = getInnerMap().entrySet().iterator();
    boolean b = false;
    while (iterator.hasNext()) {
      final Map.Entry<T, T> next = iterator.next();
      if (c_inner.contains(next.getKey())) {
        iterator.remove();
        RefUtil.freeRef(next.getKey());
        b = true;
      }
    }
    RefUtil.freeRef(c);
    return b;
  }

  @Override
  public synchronized final boolean retainAll(@NotNull Collection<?> c) {
    assertAlive();
    final Collection<?> c_inner;
    if (c instanceof RefAbstractCollection) {
      c_inner = ((RefAbstractCollection<?>) c).getInner();
    } else {
      c_inner = c;
    }
    final Iterator<Map.Entry<T, T>> iterator = getInnerMap().entrySet().iterator();
    boolean b = false;
    while (iterator.hasNext()) {
      final Map.Entry<T, T> next = iterator.next();
      if (!c_inner.contains(next.getKey())) {
        iterator.remove();
        RefUtil.freeRef(next.getKey());
        b = true;
      }
    }
    RefUtil.freeRef(c);
    return b;
  }

  @Override
  protected void _free() {
    clear();
    super._free();
  }

}
