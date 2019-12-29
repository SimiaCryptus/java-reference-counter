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

package com.simiacryptus.ref.lang;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * The interface Reference counting.
 */
@RefAware
@RefIgnore
public interface ReferenceCounting {

  /**
   * Gets object id.
   *
   * @return the object id
   */
  default UUID getObjectId() {
    throw new RuntimeException("Not Implemented");
  }

  /**
   * Is finalized boolean.
   *
   * @return the boolean
   */
  default boolean isFinalized() {
    return false;
  }

  /**
   * Free refs.
   *
   * @param <T>   the type parameter
   * @param array the array
   */
  static <T extends ReferenceCounting> void freeRefs(@NotNull T[] array) {
    java.util.Arrays.stream(array).filter((x) -> x != null).forEach(ReferenceCounting::freeRef);
  }

  /**
   * Free refs.
   *
   * @param <T>   the type parameter
   * @param array the array
   */
  static <T extends ReferenceCounting> void freeRefs(@NotNull T[][] array) {
    java.util.Arrays.stream(array).filter((x) -> x != null).forEach(ReferenceCounting::freeRefs);
  }

  /**
   * Free refs.
   *
   * @param <T>   the type parameter
   * @param array the array
   */
  static <T extends ReferenceCounting> void freeRefs(@NotNull T[][][] array) {
    java.util.Arrays.stream(array).filter((x) -> x != null).forEach(ReferenceCounting::freeRefs);
  }

  /**
   * Add ref reference counting.
   *
   * @return the reference counting
   */
  default ReferenceCounting addRef() {
    return this;
  }

  /**
   * Assert alive boolean.
   *
   * @return the boolean
   */
  default boolean assertAlive() {
    return true;
  }

  /**
   * Current ref count int.
   *
   * @return the int
   */
  default int currentRefCount() {
    return 1;
  }

  /**
   * Detach reference counting.
   *
   * @return the reference counting
   */
  default ReferenceCounting detach() {
    return this;
  }

  /**
   * Free ref int.
   *
   * @return the int
   */
  default int freeRef() {
    return 0;
  }

  /**
   * Free ref async.
   */
  default void freeRefAsync() {
  }

  /**
   * Try add ref boolean.
   *
   * @return the boolean
   */
  default boolean tryAddRef() {
    return true;
  }
}
