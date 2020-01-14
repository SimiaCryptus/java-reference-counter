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

@RefIgnore
@SuppressWarnings("unused")
public class RefTreeSet<T> extends RefAbstractSet<T> implements RefNavigableSet<T> {

  @Nonnull
  private final TreeMap<T, T> inner;

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
    new TreeSet<>();
  }

  public RefTreeSet(@RefAware Comparator<? super T> comparator) {
    this(new TreeMap<>(comparator));
  }

  RefTreeSet(@Nonnull @RefAware TreeMap<T, T> inner) {
    if (inner instanceof ReferenceCounting)
      throw new IllegalArgumentException("inner class cannot be ref-aware");
    this.inner = inner;
    this.getInnerMap().keySet().forEach(RefUtil::addRef);
  }

  public RefTreeSet(@Nonnull @RefAware Collection<T> values) {
    this();
    addAll(values);
  }

  @Nonnull
  @Override
  public Map<T, T> getInnerMap() {
    return inner;
  }

  @Nonnull
  public static <T> RefTreeSet<T>[] addRefs(@Nonnull RefTreeSet<T>[] array) {
    return Arrays.stream(array).filter((x) -> x != null).map(RefTreeSet::addRef).toArray((x) -> new RefTreeSet[x]);
  }

  @Nonnull
  public @Override
  RefTreeSet<T> addRef() {
    return (RefTreeSet<T>) super.addRef();
  }

  @Nullable
  @Override
  public T lower(@RefAware T t) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public T floor(@RefAware T t) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public T ceiling(@RefAware T t) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public T higher(@RefAware T t) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  public T pollFirst() {
    return RefUtil.addRef(inner.pollFirstEntry().getKey());
  }

  @Nullable
  @Override
  public T pollLast() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RefNavigableSet<T> descendingSet() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public Iterator<T> descendingIterator() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RefNavigableSet<T> subSet(@RefAware T fromElement, boolean fromInclusive,
                                   @RefAware T toElement, boolean toInclusive) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RefNavigableSet<T> headSet(@RefAware T toElement, boolean inclusive) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RefNavigableSet<T> tailSet(@RefAware T fromElement, boolean inclusive) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public Comparator<? super T> comparator() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RefSortedSet<T> subSet(@RefAware T fromElement,
                                @RefAware T toElement) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RefSortedSet<T> headSet(@RefAware T toElement) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public RefSortedSet<T> tailSet(@RefAware T fromElement) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public T first() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public T last() {
    throw new UnsupportedOperationException();
  }
}
