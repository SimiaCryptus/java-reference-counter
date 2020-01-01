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

import com.simiacryptus.ref.lang.ReferenceCountingBase;

/**
 * The type Ref aware consumer.
 *
 * @param <T> the type parameter
 */
public abstract @com.simiacryptus.ref.lang.RefAware class RefAwareConsumer<T> extends ReferenceCountingBase
    implements com.simiacryptus.ref.wrappers.RefConsumer<T> {
  public @Override void _free() {
    super._free();
  }

  public @Override @SuppressWarnings("unused") RefAwareConsumer<T> addRef() {
    return (RefAwareConsumer<T>) super.addRef();
  }

  public static @SuppressWarnings("unused") RefAwareConsumer[] addRefs(RefAwareConsumer[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefAwareConsumer::addRef)
        .toArray((x) -> new RefAwareConsumer[x]);
  }

  public static @SuppressWarnings("unused") RefAwareConsumer[][] addRefs(RefAwareConsumer[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(RefAwareConsumer::addRefs)
        .toArray((x) -> new RefAwareConsumer[x][]);
  }
}
