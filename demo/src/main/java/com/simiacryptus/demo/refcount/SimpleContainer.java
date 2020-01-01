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
 * The type Simple container.
 */
public @com.simiacryptus.ref.lang.RefAware class SimpleContainer extends ReferenceCountingBase {
  /**
   * The Value.
   */
  public BasicType value;

  /**
   * Instantiates a new Simple container.
   */
  public SimpleContainer() {
    {
      com.simiacryptus.demo.refcount.BasicType temp_01_0001 = new BasicType();
      if (null != value)
        value.freeRef();
      value = temp_01_0001 == null ? null : temp_01_0001.addRef();
      if (null != temp_01_0001)
        temp_01_0001.freeRef();
    }
  }

  /**
   * Instantiates a new Simple container.
   *
   * @param value the value
   */
  public SimpleContainer(BasicType value) {
    {
      com.simiacryptus.demo.refcount.BasicType temp_01_0002 = value == null ? null : value.addRef();
      if (null != this.value)
        this.value.freeRef();
      this.value = temp_01_0002 == null ? null : temp_01_0002.addRef();
      if (null != temp_01_0002)
        temp_01_0002.freeRef();
    }
    if (null != value)
      value.freeRef();
  }

  public @Override void _free() {
    if (null != value)
      value.freeRef();
    super._free();
  }

  /**
   * Test.
   */
  public void test() {
    this.value.setValue(this.value.getValue() + 1);
  }

  @Override
  public String toString() {
    return "SimpleContainer{" + "values=" + value + '}';
  }

  public @Override @SuppressWarnings("unused") SimpleContainer addRef() {
    return (SimpleContainer) super.addRef();
  }

  public static @SuppressWarnings("unused") SimpleContainer[] addRefs(SimpleContainer[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(SimpleContainer::addRef)
        .toArray((x) -> new SimpleContainer[x]);
  }

  public static @SuppressWarnings("unused") SimpleContainer[][] addRefs(SimpleContainer[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(SimpleContainer::addRefs)
        .toArray((x) -> new SimpleContainer[x][]);
  }
}
