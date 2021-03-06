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

import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

public class RefWeakReference<T> {
  private final WeakReference<T> inner;

  public RefWeakReference(@RefIgnore T inner) {
    this(new WeakReference<>(inner));
  }

  protected RefWeakReference(WeakReference<T> inner) {
    this.inner = inner;
  }

  @SuppressWarnings("unused")
  public boolean isEnqueued() {
    return inner.isEnqueued();
  }

  public static <T extends ReferenceCountingBase> RefWeakReference<T> wrap(T obj) {
    RefWeakReference<T> reference = new RefWeakReference<>(obj);
    obj.freeRef();
    return reference;
  }

  @Nullable
  @SuppressWarnings("unused")
  public T get() {
    final T t = inner.get();
    if (t instanceof ReferenceCounting) {
      final ReferenceCounting referenceCounting = (ReferenceCounting) t;
      synchronized (referenceCounting) {
        if (referenceCounting.isFreed()) return null;
        return (T) referenceCounting.addRef();
      }
    }
    return t;
  }

  @SuppressWarnings("unused")
  public void clear() {
    inner.clear();
  }

  @SuppressWarnings("unused")
  public boolean enqueue() {
    return inner.enqueue();
  }
}
