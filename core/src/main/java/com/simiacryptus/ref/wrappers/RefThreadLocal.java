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
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import javax.annotation.Nonnull;

@RefIgnore
@SuppressWarnings("unused")
public class RefThreadLocal<T> extends ThreadLocal<T> implements ReferenceCounting {

  private final ReferenceCountingBase refCounter = new ReferenceCountingBase() {
    @Override
    protected void _free() {
      super._free();
      RefThreadLocal.this._free();
    }
  };

  @Override
  public boolean isFreed() {
    return refCounter.isFreed();
  }

  @Override
  public ReferenceCounting addRef() {
    return refCounter.addRef();
  }

  @Override
  public boolean assertAlive() {
    return refCounter.assertAlive();
  }

  @Override
  public int currentRefCount() {
    return refCounter.currentRefCount();
  }

  @Nonnull
  @Override
  public ReferenceCounting detach() {
    return refCounter.detach();
  }

  @Override
  public int freeRef() {
    return refCounter.freeRef();
  }

  @Override
  public boolean tryAddRef() {
    return refCounter.tryAddRef();
  }

  public T get() {
    T t = super.get();
    if (null == t) return null;
    if (t instanceof ReferenceCounting) {
      if (!((ReferenceCounting) t).tryAddRef()) {
        this.remove();
        return get();
      }
    }
    return t;

  }

  public void set(T t) {
    RefUtil.freeRef(super.get());
    super.set(t);
  }

  @Override
  public void remove() {
    set(null);
  }

  protected void _free() {
  }
}
