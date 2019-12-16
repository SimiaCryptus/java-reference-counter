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
 * The type Array container.
 */
public class ArrayContainer extends ReferenceCountingBase {
  /**
   * The Values.
   */
  public BasicType[] values;

  /**
   * Instantiates a new Array container.
   *
   * @param values the values
   */
  public ArrayContainer(BasicType... values) {
    this.values = values;
  }

  public @Override void _free() {
    super._free();
  }

  /**
   * Test.
   */
  public void test() {
    java.util.Arrays.stream(this.values)
        .forEach(x -> {
          x.setValue(x.getValue() + 1);
        });
  }

  @Override
  public String toString() {
    return "ArrayContainer{" + "values="
        + java.util.Arrays.toString(values)
        + '}';
  }

  /**
   * Use closures 1.
   *
   * @param right the right
   */
  public void useClosures1(BasicType right) {
    java.util.Arrays.stream(this.values)
        .forEach(
            (java.util.function.Consumer<? super com.simiacryptus.demo.refcount.BasicType>) x -> {
			  x.setValue(x.getValue() + right.getValue());
			});
  }

  /**
   * Use closures 2.
   *
   * @param right the right
   */
  public void useClosures2(BasicType right) {
    java.util.Arrays.stream(this.values)
        .forEach(
            new java.util.function.Consumer<BasicType>() {
              @Override
              public void accept(BasicType x) {
                x.setValue(x.getValue() + right.getValue());
              }
            });
  }

  /**
   * Use closures 3.
   *
   * @param right the right
   */
  public void useClosures3(BasicType right) {
    java.util.Arrays.stream(this.values)
        .forEach(new RefAwareConsumer<BasicType>() {
          @Override
          public void accept(BasicType x) {
            x.setValue(x.getValue() + right.getValue());
          }
        });
  }
}
