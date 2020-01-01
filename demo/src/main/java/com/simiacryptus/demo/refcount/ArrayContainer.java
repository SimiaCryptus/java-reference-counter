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
public @com.simiacryptus.ref.lang.RefAware class ArrayContainer extends ReferenceCountingBase {
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
    {
      com.simiacryptus.demo.refcount.BasicType[] temp_03_0001 = com.simiacryptus.demo.refcount.BasicType
          .addRefs(values);
      if (null != this.values)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(this.values);
      this.values = com.simiacryptus.demo.refcount.BasicType.addRefs(temp_03_0001);
      if (null != temp_03_0001)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_03_0001);
    }
    if (null != values)
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(values);
  }

  public @Override void _free() {
    if (null != values)
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(values);
    super._free();
  }

  /**
   * Test.
   */
  public void test() {
    com.simiacryptus.ref.wrappers.RefArrays.stream(com.simiacryptus.demo.refcount.BasicType.addRefs(this.values))
        .forEach(x -> {
          x.setValue(x.getValue() + 1);
          if (null != x)
            x.freeRef();
        });
  }

  @Override
  public String toString() {
    return "ArrayContainer{" + "values="
        + com.simiacryptus.ref.wrappers.RefArrays.toString(com.simiacryptus.demo.refcount.BasicType.addRefs(values))
        + '}';
  }

  /**
   * Use closures 1.
   *
   * @param right the right
   */
  public void useClosures1(BasicType right) {
    com.simiacryptus.ref.wrappers.RefArrays.stream(com.simiacryptus.demo.refcount.BasicType.addRefs(this.values))
        .forEach(
            (com.simiacryptus.ref.wrappers.RefConsumer<? super com.simiacryptus.demo.refcount.BasicType>) com.simiacryptus.ref.lang.RefUtil
                .wrapInterface(
                    (com.simiacryptus.ref.wrappers.RefConsumer<? super com.simiacryptus.demo.refcount.BasicType>) x -> {
                      x.setValue(x.getValue() + right.getValue());
                      if (null != x)
                        x.freeRef();
                    }, right == null ? null : right.addRef()));
    if (null != right)
      right.freeRef();
  }

  /**
   * Use closures 2.
   *
   * @param right the right
   */
  public void useClosures2(BasicType right) {
    com.simiacryptus.ref.wrappers.RefArrays.stream(com.simiacryptus.demo.refcount.BasicType.addRefs(this.values))
        .forEach(
            com.simiacryptus.ref.lang.RefUtil.wrapInterface(new com.simiacryptus.ref.wrappers.RefConsumer<BasicType>() {
              @Override
              public void accept(BasicType x) {
                x.setValue(x.getValue() + right.getValue());
                if (null != x)
                  x.freeRef();
              }

              public void _free() {
              }

            }, right == null ? null : right.addRef()));
    if (null != right)
      right.freeRef();
  }

  /**
   * Use closures 3.
   *
   * @param right the right
   */
  public void useClosures3(BasicType right) {
    com.simiacryptus.ref.wrappers.RefArrays.stream(com.simiacryptus.demo.refcount.BasicType.addRefs(this.values))
        .forEach(new RefAwareConsumer<BasicType>() {
          @Override
          public void accept(BasicType x) {
            x.setValue(x.getValue() + right.getValue());
            if (null != x)
              x.freeRef();
          }

          public @Override void _free() {
            if (null != right)
              right.freeRef();
            super._free();
          }

          {
            right.addRef();
          }
        });
    if (null != right)
      right.freeRef();
  }

  public @Override @SuppressWarnings("unused") ArrayContainer addRef() {
    return (ArrayContainer) super.addRef();
  }

  public static @SuppressWarnings("unused") ArrayContainer[] addRefs(ArrayContainer[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ArrayContainer::addRef)
        .toArray((x) -> new ArrayContainer[x]);
  }

  public static @SuppressWarnings("unused") ArrayContainer[][] addRefs(ArrayContainer[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ArrayContainer::addRefs)
        .toArray((x) -> new ArrayContainer[x][]);
  }
}
