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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;

@RefAware
@RefIgnore
@SuppressWarnings("unused")
public class RefTreeSet<T> extends RefAbstractSet<T> implements RefNavigableSet<T> {

  @NotNull
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

  public RefTreeSet(Comparator<? super T> comparator) {
    this(new TreeMap<>(comparator));
  }

  RefTreeSet(@Nonnull TreeMap<T, T> inner) {
    if (inner instanceof ReferenceCounting) throw new IllegalArgumentException("inner class cannot be ref-aware");
    this.inner = inner;
    this.getInnerMap().keySet().forEach(RefUtil::addRef);
  }

  public RefTreeSet(@NotNull Collection<T> values) {
    this();
    addAll(values);
  }

  @NotNull
  @Override
  public Map<T, T> getInnerMap() {
    return inner;
  }

  @NotNull
  public static <T> RefTreeSet<T>[] addRefs(@NotNull RefTreeSet<T>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefTreeSet::addRef)
        .toArray((x) -> new RefTreeSet[x]);
  }

  @NotNull
  public @Override
  RefTreeSet<T> addRef() {
    return (RefTreeSet<T>) super.addRef();
  }

  @Nullable
  @Override
  public T lower(T t) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public T floor(T t) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public T ceiling(T t) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public T higher(T t) {
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

  @NotNull
  @Override
  public RefNavigableSet<T> descendingSet() {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public Iterator<T> descendingIterator() {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public RefNavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public RefNavigableSet<T> headSet(T toElement, boolean inclusive) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public RefNavigableSet<T> tailSet(T fromElement, boolean inclusive) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public Comparator<? super T> comparator() {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public RefSortedSet<T> subSet(T fromElement, T toElement) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public RefSortedSet<T> headSet(T toElement) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public RefSortedSet<T> tailSet(T fromElement) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T first() {
    throw new UnsupportedOperationException();
  }

  @Override
  public T last() {
    throw new UnsupportedOperationException();
  }
}
