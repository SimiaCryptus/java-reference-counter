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
public @com.simiacryptus.ref.lang.RefAware
class SimpleContainer extends ReferenceCountingBase {
  /**
   * The Value.
   */
  public BasicType value;

  /**
   * Instantiates a new Simple container.
   */
  public SimpleContainer() {
    {
      value = new BasicType();
    }
  }

  /**
   * Instantiates a new Simple container.
   *
   * @param value the value
   */
  public SimpleContainer(BasicType value) {
    {
      this.value = value;
    }
  }

  public static SimpleContainer[] addRefs(SimpleContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(SimpleContainer::addRef)
        .toArray((x) -> new SimpleContainer[x]);
  }

  public static SimpleContainer[][] addRefs(SimpleContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(SimpleContainer::addRefs)
        .toArray((x) -> new SimpleContainer[x][]);
  }

  public @Override
  void _free() {
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

  public @Override
  SimpleContainer addRef() {
    return (SimpleContainer) super.addRef();
  }
}
