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

package com.simiacryptus.ref.lang;

import org.jetbrains.annotations.NotNull;

@RefAware
@RefIgnore
public interface ReferenceCounting {

  default boolean isFinalized() {
    return false;
  }

  static <T extends ReferenceCounting> void freeRefs(@NotNull T[] array) {
    java.util.Arrays.stream(array).filter((x) -> x != null).forEach(ReferenceCounting::freeRef);
  }

  static <T extends ReferenceCounting> void freeRefs(@NotNull T[][] array) {
    java.util.Arrays.stream(array).filter((x) -> x != null).forEach(ReferenceCounting::freeRefs);
  }

  static <T extends ReferenceCounting> void freeRefs(@NotNull T[][][] array) {
    java.util.Arrays.stream(array).filter((x) -> x != null).forEach(ReferenceCounting::freeRefs);
  }

  default ReferenceCounting addRef() {
    return this;
  }

  default boolean assertAlive() {
    return true;
  }

  default int currentRefCount() {
    return 1;
  }

  @NotNull
  default ReferenceCounting detach() {
    return this;
  }

  default int freeRef() {
    return 0;
  }

  default void freeRefAsync() {
  }

  default boolean tryAddRef() {
    return true;
  }
}
