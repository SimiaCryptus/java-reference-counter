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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;

@RefIgnore
public class RefIterator<T> extends RefIteratorBase<T> {

  @Nullable
  private final Iterator<T> inner;

  public RefIterator(@RefAware Iterator<T> inner) {
    if (inner instanceof RefIterator) {
      this.inner = ((RefIterator<T>) inner).getInner();
    } else {
      this.inner = inner;
    }
  }

  @Override
  public Iterator<T> getInner() {
    return inner;
  }

  public @Nonnull
  RefIterator<T> track(ReferenceCounting obj) {
    super.track(obj);
    return this;
  }

  @Nonnull
  @Override
  public RefIterator<T> addRef() {
    return (RefIterator<T>) super.addRef();
  }
}
