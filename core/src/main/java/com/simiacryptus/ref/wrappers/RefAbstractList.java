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

/**
 * This is the RefAbstractList class.
 *
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public abstract class RefAbstractList<T> extends RefAbstractCollection<T> implements RefList<T> {
  /**
   * Returns a list of the inner type.
   *
   * @return a list of the inner type
   * @docgenVersion 9
   */
  @Nonnull
  public abstract List<T> getInner();

  /**
   * Adds the specified element at the specified index in this list.
   *
   * @param index   the index at which to add the element
   * @param element the element to add
   * @throws IndexOutOfBoundsException if the index is out of range
   *                                   ({@code index < 0 || index > size()})
   * @throws NullPointerException      if the specified element is null
   * @docgenVersion 9
   */
  @Override
  public void add(int index, @RefAware T element) {
    assertAlive();
    getInner().add(index, element);
  }

  /**
   * Adds the specified element to this set if it is not already present
   * (optional operation).  More formally, adds the specified element
   * {@code e} to this set if the set contains no element {@code e2}
   * such that
   * <tt>(e==null&nbsp;?&nbsp;e2==null&nbsp;:&nbsp;e.equals(e2))</tt>.
   * If this set already contains the element, the call leaves the set
   * unchanged and returns {@code false}.  In combination with the
   * restriction on constructors, this ensures that sets never contain
   * duplicate elements.<p>
   * <p>
   * The stipulation above does not imply that sets must accept all
   * elements; sets may refuse to add any particular element, including
   * {@code null}, and throw an exception, as described in the
   * specification for {@link Collection#add Collection.add}.
   * Individual set implementations should clearly document any
   * restrictions on the elements that they may contain.
   *
   * @param o element to be added to this set
   * @return {@code true} if this set did not already contain the specified
   * element
   * @throws UnsupportedOperationException if the {@code add} operation
   *                                       is not supported by this set
   * @throws ClassCastException            if the class of the specified element
   *                                       prevents it from being added to this set
   * @throws NullPointerException          if the specified element is null and this
   *                                       set does not permit null elements
   * @throws IllegalArgumentException      if some property of the specified element
   *                                       prevents it from being added to this set
   * @docgenVersion 9
   * @see #add(Object)
   */
  @Override
  public final boolean add(@RefAware T o) {
    assertAlive();
    return getInner().add(o);
  }

  /**
   * @param index
   * @param c
   * @return
   * @docgenVersion 9
   */
  @Override
  public boolean addAll(int index, @Nonnull @RefAware Collection<? extends T> c) {
    assertAlive();
    final boolean changed = getInner().addAll(index, c);
    RefUtil.freeRef(c);
    return changed;
  }

  /**
   * Adds all of the elements in the specified collection to this
   * collection. The behavior of this operation is undefined if the
   * specified collection is modified while the operation is in
   * progress. (This implies that the behavior of this call is undefined
   * if the specified collection is this collection, and this
   * collection is nonempty.)
   *
   * @param collection collection containing elements to be added to this collection
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

  /**
   * Adds a reference to this object.
   *
   * @return a reference to this object
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  RefAbstractList<T> addRef() {
    assertAlive();
    return (RefAbstractList<T>) super.addRef();
  }

  /**
   * @return the element at the specified position in this list
   * @throws IndexOutOfBoundsException if the index is out of range
   *                                   (<tt>index &lt; 0 || index &gt;= size()</tt>)
   * @docgenVersion 9
   */
  @Nullable
  @Override
  @RefIgnore
  @RefAware
  public T get(int index) {
    assertAlive();
    return RefUtil.addRef(getInner().get(index));
  }

  /**
   * @param o The object to search for.
   * @return The index of the first occurrence of the object, or -1 if not found.
   * @docgenVersion 9
   */
  @Override
  public int indexOf(@RefAware Object o) {
    assertAlive();
    final int index = getInner().indexOf(o);
    RefUtil.freeRef(o);
    return index;
  }

  /**
   * {@inheritDoc}
   *
   * @param o the object to be searched for
   * @return the index of the last occurrence of the specified object in this
   * list
   * @docgenVersion 9
   */
  @Override
  public int lastIndexOf(@RefAware Object o) {
    assertAlive();
    final int index = getInner().lastIndexOf(o);
    RefUtil.freeRef(o);
    return index;
  }

  /**
   * @return an iterator over the elements in this list in proper sequence
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefListIterator<T> listIterator() {
    assertAlive();
    return new RefListIterator<>(getInner().listIterator()).track(this.addRef());
  }

  /**
   * Returns a list iterator over the elements in this list (in proper sequence), starting at the specified position in the list.
   * The specified index indicates the first element that would be returned by an initial call to the next method.
   * An initial call to the previous method would return the element with the specified index minus one.
   *
   * @param index index of the first element to be returned from the list iterator (by a call to the next method)
   * @return a list iterator over the elements in this list (in proper sequence), starting at the specified position in the list
   * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index > size()})
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefListIterator<T> listIterator(int index) {
    assertAlive();
    return new RefListIterator<>(getInner().listIterator(index));
  }

  /**
   * @Override
   * @RefIgnore
   * @RefAware public T remove(int index) {
   * assertAlive();
   * return getInner().remove(index);
   * }
   * @docgenVersion 9
   */
  @Override
  @RefIgnore
  @RefAware
  public T remove(int index) {
    assertAlive();
    return getInner().remove(index);
  }

  /**
   * @Override public final boolean remove(@RefAware Object item);
   * @docgenVersion 9
   */
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

  /**
   * @Override public synchronized final boolean retainAll(@Nonnull @RefAware Collection<?> c);
   * @docgenVersion 9
   */
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

  /**
   * @Override
   * @RefIgnore
   * @RefAware public T set(int index, @RefAware T element) {
   * assertAlive();
   * return getInner().set(index, element);
   * }
   * @docgenVersion 9
   */
  @Override
  @RefIgnore
  @RefAware
  public T set(int index, @RefAware T element) {
    assertAlive();
    return getInner().set(index, element);
  }

  /**
   * Returns a view of the portion of this list between the specified fromIndex, inclusive, and toIndex, exclusive.
   * (If fromIndex and toIndex are equal, the returned list is empty.) The returned list is backed by this list, so non-structural changes in the returned list are reflected in this list, and vice-versa. The returned list supports all of the optional list operations supported by this list.
   *
   * @param fromIndex low endpoint (inclusive) of the subList
   * @param toIndex   high endpoint (exclusive) of the subList
   * @return a view of the specified range within this list
   * @throws IndexOutOfBoundsException if an endpoint index value is out of range {@code (fromIndex < 0 || toIndex > size)}
   * @throws IllegalArgumentException  if the endpoint indices are out of order {@code (fromIndex > toIndex)}
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefArrayList<T> subList(int fromIndex, int toIndex) {
    assertAlive();
    return new RefArrayList<T>(getInner().subList(fromIndex, toIndex));
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
