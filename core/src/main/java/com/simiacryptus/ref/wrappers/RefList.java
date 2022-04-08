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
import com.simiacryptus.ref.lang.ReferenceCounting;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * This is the RefList interface.
 *
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public interface RefList<T> extends ReferenceCounting, List<T>, RefCollection<T> {

  /**
   * Returns an empty {@code RefList} instance.
   *
   * @param <T> the type of elements in the list
   * @return an empty {@code RefList}
   * @docgenVersion 9
   */
  static <T> RefList<T> empty() {
    return new RefArrayList();
  }

  /**
   * Returns a list of type T. This list will never be null.
   *
   * @return List<T>
   * @docgenVersion 9
   */
  @Nonnull
  List<T> getInner();

  /**
   * Adds a reference to the list.
   *
   * @return the list with the added reference
   * @throws NullPointerException if the reference is null
   * @docgenVersion 9
   */
  @Nonnull
  RefList<T> addRef();

  /**
   * @return an iterator over the elements in this list in proper sequence
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefListIterator<T> listIterator();

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
  RefListIterator<T> listIterator(int index);

  /**
   * Returns a {@code Spliterator} over the elements in this {@code RefStream}.
   *
   * <p>The {@code Spliterator} reports {@link Spliterator#SIZED},
   * {@link Spliterator#SUBSIZED}, and {@link Spliterator#ORDERED}.
   * Implementations should document the reporting of additional characteristic
   * values.
   *
   * @return a {@code Spliterator} over the elements in this {@code RefStream}
   * @implSpec The default implementation creates a
   * <em><a href="Spliterator.html#binding">late-binding</a></em> spliterator
   * from the stream's {@code Supplier}.  The spliterator inherits the
   * <em>fail-fast</em> properties of the stream's source.  The spliterator's
   * comparator is the same as or imposes the same total ordering as the
   * stream's source.
   * @implNote The default implementation should usually be overridden.  The
   * spliterator returned by the default implementation has poor splitting
   * capabilities, is unsized, and does not report any spliterator characteristics.
   * Implementations should document better splitting capabilities.
   * @docgenVersion 9
   */
  @Override
  RefSpliterator<T> spliterator();

  /**
   * Returns a stream of the elements in this container.
   *
   * @return a stream of the elements in this container
   * @docgenVersion 9
   */
  @Override
  RefStream<T> stream();

  /**
   * Returns a view of the portion of this list between the specified fromIndex,
   * inclusive, and toIndex, exclusive. (If fromIndex and toIndex are equal, the
   * returned list is empty.) The returned list is backed by this list, so
   * non-structural changes in the returned list are reflected in this list, and
   * vice-versa. The returned list supports all of the optional list operations.
   *
   * @param fromIndex low endpoint (inclusive) of the subList
   * @param toIndex   high endpoint (exclusive) of the subList
   * @return a view of the specified range within this list
   * @throws IndexOutOfBoundsException {@inheritDoc}
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  RefList<T> subList(int fromIndex, int toIndex);

  /**
   * @Override boolean contains(@RefAware Object o);
   * @docgenVersion 9
   */
  @Override
  boolean contains(@RefAware Object o);

  /**
   * @Nonnull
   * @Override <T1> T1[] toArray(@Nonnull @RefAware T1[] a);
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  <T1> T1[] toArray(@Nonnull @RefAware T1[] a);

  /**
   * @Override boolean add(@RefAware T t);
   * @docgenVersion 9
   */
  @Override
  boolean add(@RefAware T t);

  /**
   * @Override boolean remove(@RefAware Object o);
   * @docgenVersion 9
   */
  @Override
  boolean remove(@RefAware Object o);

  /**
   * @param c the collection to be checked for containment in this collection
   * @return {@code true} if this collection contains all of the elements in the
   * specified collection
   * @throws ClassCastException   if the types of one or more elements
   *                              in the specified collection are incompatible with this
   *                              collection (optional)
   * @throws NullPointerException if the specified collection contains one
   *                              or more null elements and this collection does not permit null
   *                              elements (optional), or if the specified collection is null
   * @throws NullPointerException if elements of the specified collection are
   *                              null and this collection does not permit null elements
   *                              (optional), or if the specified collection is null
   * @docgenVersion 9
   * @see java.util.Collection#containsAll(java.util.Collection)
   * @since 1.6
   */
  @Override
  boolean containsAll(@Nonnull @RefAware Collection<?> c);

  /**
   * Adds all of the elements in the specified collection to this
   * collection, if they're not already present (optional operation).
   * If the specified collection is also a {@link java.util.Set}, the
   * {@code addAll} operation effectively modifies this set so that
   * its value is the <i>union</i> of the two sets.
   *
   * @param c the collection containing elements to be added to this collection
   * @return {@code true} if this collection changed as a result of the
   * {@code addAll} operation, {@code false} otherwise
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
  boolean addAll(@Nonnull @RefAware Collection<? extends T> c);

  /**
   * Adds all of the elements in the specified collection to this
   * list, starting at the specified position.  Shifts the element
   * currently at that position (if any) and any subsequent elements to
   * the right (increases their indices).  The new elements will appear
   * in the list in the order that they are returned by the
   * specified collection's iterator.  The behavior of this operation is
   * undefined if the specified collection is modified while the
   * operation is in progress.  (This implies that the behavior of this
   * call is undefined if the specified collection is this list, and
   * this list is nonempty.)
   *
   * @param index index at which to insert the first element from the
   *              specified collection
   * @param c     collection containing elements to be added to this list
   * @return {@code true} if this list changed as a result of the call
   * @throws IndexOutOfBoundsException if the index is out of range
   *                                   ({@code index < 0 || index > size()})
   * @throws NullPointerException      if the specified collection is null
   * @docgenVersion 9
   */
  @Override
  boolean addAll(int index, @Nonnull @RefAware Collection<? extends T> c);

  /**
   * @Override boolean removeAll(@Nonnull @RefAware Collection<?> c);
   * @docgenVersion 9
   */
  @Override
  boolean removeAll(@Nonnull @RefAware Collection<?> c);

  /**
   * @Override boolean retainAll(@Nonnull @RefAware Collection<?> c);
   * @docgenVersion 9
   */
  @Override
  boolean retainAll(@Nonnull @RefAware Collection<?> c);

  /**
   * {@inheritDoc}
   *
   * <p>This implementation throws an {@link UnsupportedOperationException}.
   *
   * @param operator the operator to apply to each element
   * @throws UnsupportedOperationException if this operation is not supported
   * @throws NullPointerException          if the specified operator is null
   * @docgenVersion 9
   */
  @Override
  default void replaceAll(@RefAware UnaryOperator<T> operator) {
    throw new RuntimeException("Not Implemented");
  }

  /**
   * {@inheritDoc}
   *
   * @throws RuntimeException if not implemented
   * @docgenVersion 9
   */
  @Override
  default void sort(@RefAware Comparator<? super T> c) {
    throw new RuntimeException("Not Implemented");
  }

  /**
   * @Override T set(int index, @RefAware T element);
   * @docgenVersion 9
   */
  @Override
  T set(int index, @RefAware T element);

  /**
   * Adds the specified element at the specified index.
   *
   * @param index   the index at which to add the element
   * @param element the element to add
   * @docgenVersion 9
   */
  @Override
  void add(int index, @RefAware T element);

  /**
   * @Override int indexOf(@RefAware Object o);
   * @docgenVersion 9
   */
  @Override
  int indexOf(@RefAware Object o);

  /**
   * @Override int lastIndexOf(@RefAware Object o);
   * @docgenVersion 9
   */
  @Override
  int lastIndexOf(@RefAware Object o);

  /**
   * Returns the element at the specified index.
   *
   * @param index the index of the element to return
   * @return the element at the specified index
   * @throws IndexOutOfBoundsException if the index is out of range
   * @docgenVersion 9
   */
  @RefAware
  T get(int index);
}
