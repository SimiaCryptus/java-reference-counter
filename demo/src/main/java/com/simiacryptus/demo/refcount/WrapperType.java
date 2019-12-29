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

package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

/**
 * The type Wrapper type.
 *
 * @param <T> the type parameter
 */
public @com.simiacryptus.ref.lang.RefAware class WrapperType<T extends ReferenceCounting>
    extends ReferenceCountingBase {
  private T inner;

  /**
   * Instantiates a new Wrapper type.
   *
   * @param inner the inner
   */
  public WrapperType(T inner) {
    this.setInner(inner);
  }

  public void _free() {
  }

  /**
   * Gets inner.
   *
   * @return the inner
   */
  public T getInner() {
    return inner;
  }

  /**
   * Sets inner.
   *
   * @param inner the inner
   * @return the inner
   */
  public WrapperType<T> setInner(T inner) {
    {
      this.inner = inner;
    }
    return this;
  }

  public @Override WrapperType<T> addRef() {
    return (WrapperType<T>) super.addRef();
  }

  public static WrapperType[] addRefs(WrapperType[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(WrapperType::addRef)
        .toArray((x) -> new WrapperType[x]);
  }

  public static WrapperType[][] addRefs(WrapperType[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(WrapperType::addRefs)
        .toArray((x) -> new WrapperType[x][]);
  }
}
