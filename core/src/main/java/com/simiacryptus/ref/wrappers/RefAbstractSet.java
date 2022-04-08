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

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This class represents an abstract set.
 *
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public abstract class RefAbstractSet<T> extends RefAbstractCollection<T> implements RefSet<T>, Cloneable, Serializable {
  /**
   * Returns a collection of the inner map's keys.
   *
   * @return a collection of the inner map's keys
   * @docgenVersion 9
   */
  @Nonnull
  public Collection<T> getInner() {
    return getInnerMap().keySet();
  }

  /**
   * Returns the inner map.
   *
   * @return the inner map
   * @docgenVersion 9
   */
  @Nonnull
  protected abstract Map<T, T> getInnerMap();

  /**
   * Adds the specified element to this set if it is not already present. More formally, adds the specified element e to this set if the set contains no element e2 such that (o==null ? e2==null : o.equals(e2)). If this set already contains the element, the call leaves the set unchanged and returns false.
   *
   * @param o element to be added to this set
   * @return true if this set did not already contain the specified element
   * @throws ClassCastException       if the class of the specified element prevents it from being added to this set
   * @throws NullPointerException     if the specified element is null and this set does not permit null elements
   * @throws IllegalArgumentException if some property of the specified element prevents it from being added to this set
   * @docgenVersion 9
   */
  @Override
  public final boolean add(@RefAware T o) {
    assertAlive();
    final T replaced = getInnerMap().put(o, o);
    if (null != replaced) {
      RefUtil.freeRef(replaced);
      return false;
    } else {
      return true;
    }
  }

  /**
   * Adds all of the elements in the specified collection to this
   * collection. The behavior of this operation is undefined if the
   * specified collection is modified while the operation is in
   * progress. (This implies that the behavior of this call is undefined
   * if the specified collection is this collection, and this
   * collection is nonempty.)
   *
   * @param c collection containing elements to be added to this collection
   * @return {@code true} if this collection changed as a result of the call
   * @throws UnsupportedOperationException if the {@code addAll} operation
   *                                       is not supported by this collection
   * @throws ClassCastException            if the class of an element of the specified
   *                                       collection prevents it from being added to this collection
   * @throws NullPointerException          if the specified collection contains one
   *                                       or more null elements and this collection does not permit null
   *                                       elements, or if the specified collection is null
   * @throws IllegalArgumentException      if some property of an element of the
   *                                       specified collection prevents it from being added to this
   *                                       collection
   * @docgenVersion 9
   * @see #add(Object)
   */
  @Override
  public final boolean addAll(@Nonnull @RefAware Collection<? extends T> c) {
    assertAlive();
    final Collection<? extends T> c_inner;
    if (c instanceof RefAbstractCollection) {
      c_inner = ((RefAbstractCollection<? extends T>) c).getInner();
    } else {
      c_inner = c;
    }
    final boolean returnValue = c_inner.stream().map(o -> add(RefUtil.addRef(o))).reduce((a, b) -> a || b)
        .orElse(false);
    RefUtil.freeRef(c);
    return returnValue;
  }

  /**
   * Adds a reference to this object.
   *
   * @return a reference to this object
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  RefAbstractSet<T> addRef() {
    return (RefAbstractSet<T>) super.addRef();
  }

  /**
   * Clears the map.
   *
   * @docgenVersion 9
   */
  @Override
  public synchronized final void clear() {
    getInnerMap().keySet().forEach(value -> RefUtil.freeRef(value));
    getInnerMap().clear();
  }

  /**
   * Performs the given action for each element of the Iterable until all elements
   * have been processed or the action throws an exception.  Unless otherwise
   * specified by the implementing class, actions are performed in the order of
   * iteration (if an iteration order is specified).  Exceptions thrown by the
   * action are relayed to the caller.
   *
   * @param action The action to be performed for each element
   * @throws NullPointerException if the specified action is null
   * @docgenVersion 9
   */
  @Override
  public void forEach(@Nonnull @RefAware Consumer<? super T> action) {
    assertAlive();
    try {
      for (T t : getInnerMap().keySet()) {
        action.accept(RefUtil.addRef(t));
      }
    } finally {
      RefUtil.freeRef(action);
    }
  }

  /**
   * @param o
   * @return
   * @docgenVersion 9
   * @see java.util.Collection#remove(java.lang.Object)
   */
  @Override
  public final boolean remove(@RefAware Object o) {
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

  /**
   * @Override public synchronized final boolean removeAll(@Nonnull @RefAware Collection<?> c);
   * @docgenVersion 9
   */
  @Override
  public synchronized final boolean removeAll(@Nonnull @RefAware Collection<?> c) {
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

  /**
   * @Override public synchronized final boolean retainAll(@Nonnull @RefAware Collection<?> c);
   * @docgenVersion 9
   */
  @Override
  public synchronized final boolean retainAll(@Nonnull @RefAware Collection<?> c) {
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

  /**
   * This method clears the data and then calls the super method.
   *
   * @docgenVersion 9
   */
  @Override
  protected void _free() {
    clear();
    super._free();
  }

}
