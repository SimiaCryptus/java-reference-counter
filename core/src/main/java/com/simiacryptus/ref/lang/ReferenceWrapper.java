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

@RefIgnore
public class ReferenceWrapper<T> {
  final T obj;
  final Consumer<T> destructor;
  final AtomicBoolean isFinalized = new AtomicBoolean(false);

  public ReferenceWrapper(final @RefAware T obj,
                          final @RefAware Consumer<T> destructor) {
    this.obj = obj;
    this.destructor = destructor;
  }

  public void destroy() {
    if (!isFinalized.getAndSet(true)) {
      destructor.accept(obj);
    }
  }

  public T peek() {
    return obj;
  }

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
