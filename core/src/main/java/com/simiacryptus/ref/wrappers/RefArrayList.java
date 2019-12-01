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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@RefAware
@RefCoderIgnore
public class RefArrayList<T> extends RefAbstractList<T> {
  @NotNull
  private final List<T> inner;

  public RefArrayList() {
    this.inner = new ArrayList<>();
  }

  public RefArrayList(@NotNull List<T> list) {
    this();
    this.addAll(list);
  }

  public static <T> RefArrayList<T>[] addRefs(@NotNull RefArrayList<T>[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefArrayList::addRef)
        .toArray((x) -> new RefArrayList[x]);
  }

  @Override
  public List<T> getInner() {
    return inner;
  }

}
