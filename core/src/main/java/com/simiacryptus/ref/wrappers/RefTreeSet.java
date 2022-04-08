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

import com.simiacryptus.ref.lang.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * This class is a reference-based implementation of the {@link Set} interface,
 * backed by a {@link TreeMap}. It does not permit {@code null} elements.
 *
 * @param <T> the type of elements maintained by this set
 * @author Josh Bloch
 * @docgenVersion 9
 * @see Set
 * @see HashSet
 * @see TreeSet
 * @see SortedSet
 * @see NavigableSet
 * @see Collection
 * @see Arrays#asList(Object[])
 * @see Objects#requireNonNull
 * @since 1.6
 */
@RefIgnore
@SuppressWarnings("unused")
public class RefTreeSet<T> extends RefAbstractSet<T> implements RefNavigableSet<T> {

  @Nonnull
  private final TreeMap<T, T> inner;
  private final Comparator<? super T> comparator;

  public RefTreeSet() {
    this((a, b) -> {
      final int result;
      if (a instanceof ReferenceCountingBase) {
        result = ((Comparable<T>) a).compareTo(RefUtil.addRef(b));
      } else {
        result = ((Comparable<T>) a).compareTo(b);
      }
      return result;
    });
  }

  public RefTreeSet(@RefAware Comparator<? super T> comparator) {
    this(new TreeMap<>(comparator));
  }

  RefTreeSet(@Nonnull @RefAware TreeMap<T, T> inner) {
    if (inner instanceof ReferenceCounting)
      throw new IllegalArgumentException("inner class cannot be ref-aware");
    this.inner = inner;
    this.getInnerMap().keySet().forEach(value -> RefUtil.addRef(value));
    comparator = inner.comparator();
  }

  public RefTreeSet(@Nonnull @RefAware Collection<T> values) {
    this();
    addAll(values);
  }

  /**
   * Returns the inner map.
   *
   * @return the inner map
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  protected Map<T, T> getInnerMap() {
    return inner;
  }

  /**
   * @return a new RefTreeSet with a reference added
   * @docgenVersion 9
   */
  @Nonnull
  public @Override
  RefTreeSet<T> addRef() {
    return (RefTreeSet<T>) super.addRef();
  }

  /**
   * @Nullable
   * @Override public T lower(@RefAware T t) {
   * throw new UnsupportedOperationException();
   * }
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public T lower(@RefAware T t) {
    throw new UnsupportedOperationException();
  }

  /**
   * @Nullable
   * @Override public T floor(@RefAware T t) {
   * throw new UnsupportedOperationException();
   * }
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public T floor(@RefAware T t) {
    throw new UnsupportedOperationException();
  }

  /**
   * @Nullable
   * @Override public T ceiling(@RefAware T t) {
   * throw new UnsupportedOperationException();
   * }
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public T ceiling(@RefAware T t) {
    throw new UnsupportedOperationException();
  }

  /**
   * @param t the element to be matched
   * @return the least element in this set greater than or equal to the given element, or {@code null} if there is no such element
   * @throws UnsupportedOperationException if the {@code higher} operation is not supported by this set
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public T higher(@RefAware T t) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return the first key in the map, or null if the map is empty
   * @docgenVersion 9
   */
  @Nullable
  public T pollFirst() {
    Map.Entry<T, T> entry = inner.pollFirstEntry();
    return entry.getKey();
  }

  /**
   * @return the last element, or {@code null} if this deque is empty
   * @throws UnsupportedOperationException if this operation is not supported
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public T pollLast() {
    throw new UnsupportedOperationException();
  }

  /**
   * @return a navigable set view of the elements in this set, in descending order
   * @throws UnsupportedOperationException if the descendingSet operation is not supported by this set
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefNavigableSet<T> descendingSet() {
    throw new UnsupportedOperationException();
  }

  /**
   * @return an iterator over the elements in this list in reverse order
   * @throws UnsupportedOperationException if the {@code descendingIterator}
   *                                       operation is not supported by this list
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Iterator<T> descendingIterator() {
    throw new UnsupportedOperationException();
  }

  /**
   * @return a navigable set view of the portion of this set whose elements range from {@code fromElement} to {@code toElement}
   * @throws UnsupportedOperationException if this operation is not supported
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefNavigableSet<T> subSet(@RefAware T fromElement, boolean fromInclusive,
                                   @RefAware T toElement, boolean toInclusive) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return a navigable set view of the elements in this set, whose elements are strictly less than (or equal to, if {@code inclusive} is true) {@code toElement}
   * @throws ClassCastException       if {@code toElement} is not compatible with this set's comparator (or, if the set has no comparator, if {@code toElement} does not implement {@link Comparable}).
   * @throws NullPointerException     if {@code toElement} is null and this set does not permit null elements
   * @throws IllegalArgumentException if this set itself has a restricted range, and {@code toElement} lies outside the bounds of the range
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefNavigableSet<T> headSet(@RefAware T toElement, boolean inclusive) {
    throw new UnsupportedOperationException();
  }

  /**
   * @param fromElement the element to be used as the lower bound (inclusive)
   * @param inclusive   whether the specified element should be included in the returned set
   * @return a view of the portion of this set whose elements are greater than or equal to <tt>fromElement</tt>
   * @throws UnsupportedOperationException if the <tt>tailSet</tt> operation
   *                                       is not supported by this set
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefNavigableSet<T> tailSet(@RefAware T fromElement, boolean inclusive) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return a comparator for the elements in this queue, or {@code null} if this
   * queue uses the {@linkplain Comparable natural ordering} of its
   * elements
   * @throws UnsupportedOperationException if the comparator cannot be obtained
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public Comparator<? super T> comparator() {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   *
   * @param fromElement the lower bound (inclusive) of the returned set
   * @param toElement   the upper bound (exclusive) of the returned set
   * @return a view of the portion of this set whose elements range from
   * {@code fromElement}, inclusive, to {@code toElement}, exclusive
   * @throws UnsupportedOperationException if the {@code subSet} operation
   *                                       is not supported by this set
   * @docgenVersion 9
   * @since 1.6
   */
  @Nonnull
  @Override
  public RefSortedSet<T> subSet(@RefAware T fromElement,
                                @RefAware T toElement) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return a view of the portion of this set whose elements are strictly less than toElement
   * @throws UnsupportedOperationException if the headSet operation is not supported by this set
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefSortedSet<T> headSet(@RefAware T toElement) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return a view of the portion of this set whose elements are greater than or equal to {@code fromElement}
   * @throws ClassCastException       if {@code fromElement} is not compatible with this set's comparator (or, if the set has no comparator, if {@code fromElement} does not implement {@link Comparable}).
   * @throws NullPointerException     if {@code fromElement} is null and this set does not permit null elements
   * @throws IllegalArgumentException if this set itself has a restricted range, and {@code fromElement} lies outside the bounds of the range
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefSortedSet<T> tailSet(@RefAware T fromElement) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return the first element in this list
   * @throws UnsupportedOperationException if the <tt>first</tt> operation
   *                                       is not supported by this list
   * @docgenVersion 9
   * @since 1.6
   */
  @Nonnull
  @Override
  public T first() {
    throw new UnsupportedOperationException();
  }

  /**
   * @return the last element in the list
   * @throws UnsupportedOperationException if the list is empty
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public T last() {
    throw new UnsupportedOperationException();
  }

  /**
   * Frees resources used by this object.
   *
   * @docgenVersion 9
   * @see Ref#freeRef()
   */
  @Override
  protected void _free() {
    RefUtil.freeRef(comparator);
    super._free();
  }
}
