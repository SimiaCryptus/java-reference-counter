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

package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A simple container class that can hold a value of any BasicType.
 * The value can be null.
 *
 * @docgenVersion 9
 */
@SuppressWarnings("unused")
public class SimpleContainer extends ReferenceCountingBase {
  @Nullable
  public BasicType value;

  public SimpleContainer() {
    value = new BasicType();
  }

  public SimpleContainer(BasicType value) {
    this.value = value;
  }

  /**
   * Frees this object from memory.
   *
   * @docgenVersion 9
   */
  public @Override
  void _free() {
    value = null;
    super._free();
  }

  /**
   * Tests that the value is not null and then increments the value.
   *
   * @docgenVersion 9
   */
  public void test() {
    assert this.value != null;
    this.value.setValue(this.value.getValue() + 1);
  }

  /**
   * Returns a string representation of this SimpleContainer.
   *
   * @return a string representation of this SimpleContainer.
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public String toString() {
    return "SimpleContainer{" + "values=" + value + '}';
  }
}
