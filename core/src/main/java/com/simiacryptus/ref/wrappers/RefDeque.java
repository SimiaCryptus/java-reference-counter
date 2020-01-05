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

import java.util.Arrays;
import java.util.Deque;

@RefAware
@RefIgnore
@SuppressWarnings("unused")
public interface RefDeque<T> extends RefQueue<T>, Deque<T> {

  @NotNull
  public static <T> RefDeque<T>[] addRefs(@NotNull RefDeque<T>[] array) {
    return Arrays.stream(array).filter((x) -> x != null).map(RefDeque::addRef)
        .toArray((x) -> new RefDeque[x]);
  }

  @NotNull RefDeque<T> addRef();
}
