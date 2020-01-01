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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * The type Reference wrapper.
 *
 * @param <T> the type parameter
 */
@RefAware
@RefIgnore
public class ReferenceWrapper<T> {
  /**
   * The Obj.
   */
  final T obj;
  /**
   * The Destructor.
   */
  final Consumer<T> destructor;
  /**
   * The Is finalized.
   */
  final AtomicBoolean isFinalized = new AtomicBoolean(false);

  /**
   * Instantiates a new Reference wrapper.
   *
   * @param obj        the obj
   * @param destructor the destructor
   */
  public ReferenceWrapper(final T obj, final Consumer<T> destructor) {
    this.obj = obj;
    this.destructor = destructor;
  }

  /**
   * Destroy.
   */
  public void destroy() {
    if (!isFinalized.getAndSet(true)) {
      destructor.accept(obj);
    }
  }

  /**
   * Peek t.
   *
   * @return the t
   */
  public T peek() {
    return obj;
  }

  /**
   * Unwrap t.
   *
   * @return the t
   */
  public T unwrap() {
    if (isFinalized.getAndSet(true)) {
      throw new IllegalStateException();
    }
    return obj;
  }

  @Override
  protected void finalize() throws Throwable {
    destroy();
    super.finalize();
  }
}
