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

import javax.annotation.Nonnull;

/**
 * This interface is used for reference counting.
 *
 * @docgenVersion 9
 */
@RefIgnore
public interface ReferenceCounting {

  /**
   * Returns whether or not the object is freed.
   *
   * @return {@code false}
   * @docgenVersion 9
   */
  default boolean isFreed() {
    return false;
  }

  /**
   * Adds a reference to this object.
   *
   * @return this object
   * @docgenVersion 9
   */
  default ReferenceCounting addRef() {
    return this;
  }

  /**
   * Asserts that the object is alive.
   *
   * @return true if the object is alive; false otherwise
   * @docgenVersion 9
   */
  default boolean assertAlive() {
    return true;
  }

  /**
   * Returns the current reference count.
   *
   * @return the current reference count
   * @docgenVersion 9
   */
  default int currentRefCount() {
    return 1;
  }

  /**
   * @return a new {@link ReferenceCounting} instance with the same contents as this instance, but with a reference count of 1.
   * @docgenVersion 9
   */
  @Nonnull
  default ReferenceCounting detach() {
    return this;
  }

  /**
   * This is the default freeRef method.
   *
   * @return 0
   * @docgenVersion 9
   */
  default int freeRef() {
    return 0;
  }

  /**
   * Tries to add a reference to the object.
   *
   * @return true if the reference was added, false otherwise
   * @docgenVersion 9
   */
  default boolean tryAddRef() {
    return true;
  }
}
