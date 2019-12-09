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

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

@RefAware
@RefIgnore
public class RefTreeSet<T> extends RefAbstractSet<T> {

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

  public static <T> RefTreeSet<T>[] addRefs(@NotNull RefTreeSet<T>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefTreeSet::addRef)
        .toArray((x) -> new RefTreeSet[x]);
  }

  @NotNull
  public @Override
  RefTreeSet<T> addRef() {
    return (RefTreeSet<T>) super.addRef();
  }

  @Override
  public Map<T, T> getInnerMap() {
    return inner;
  }

  public T pollFirst() {
    return RefUtil.addRef(inner.pollFirstEntry().getKey());
  }
}
