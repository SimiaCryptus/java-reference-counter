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
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Queue;

@RefIgnore
@SuppressWarnings("unused")
public interface RefQueue<T> extends ReferenceCounting, Queue<T> {

  @NotNull
  public static <T> RefQueue<T>[] addRefs(@NotNull RefQueue<T>[] array) {
    return Arrays.stream(array).filter((x) -> x != null).map(RefQueue::addRef)
        .toArray((x) -> new RefQueue[x]);
  }

  @NotNull RefQueue<T> addRef();
}
