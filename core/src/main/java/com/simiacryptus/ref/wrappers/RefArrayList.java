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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RefAware
@RefIgnore
@SuppressWarnings("unused")
public class RefArrayList<T> extends RefAbstractList<T> {
  @NotNull
  private final List<T> inner;

  public RefArrayList() {
    this.inner = new ArrayList<>();
  }

  public RefArrayList(int length) {
    this.inner = new ArrayList<>(length);
  }

  public RefArrayList(@NotNull Collection<T> list) {
    this();
    addAll(list);
  }

  @NotNull
  @Override
  public List<T> getInner() {
    return inner;
  }

  @NotNull
  public static <T> RefArrayList<T>[] addRefs(@NotNull RefArrayList<T>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefArrayList::addRef)
        .toArray((x) -> new RefArrayList[x]);
  }

  @NotNull
  public @Override
  RefArrayList<T> addRef() {
    return (RefArrayList<T>) super.addRef();
  }

}
