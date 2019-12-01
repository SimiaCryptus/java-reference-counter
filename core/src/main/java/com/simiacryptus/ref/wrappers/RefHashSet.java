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

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefCoderIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RefAware
@RefCoderIgnore
public class RefHashSet<T> extends RefAbstractSet<T> {

  private final Map<T, T> inner;

  public RefHashSet() {
    this(new HashMap<>());
  }

  RefHashSet(@Nonnull HashMap<T, T> inner) {
    if (inner instanceof ReferenceCounting) throw new IllegalArgumentException("inner class cannot be ref-aware");
    this.inner = inner;
    this.getInner().keySet().forEach(RefUtil::addRef);
  }

  public RefHashSet(@NotNull Collection<T> values) {
    this();
    addAll(values);
  }

  public static <T> RefHashSet<T>[] addRefs(@NotNull RefHashSet<T>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefHashSet::addRef)
        .toArray((x) -> new RefHashSet[x]);
  }

  @Override
  public Map<T, T> getInner() {
    return inner;
  }

}
