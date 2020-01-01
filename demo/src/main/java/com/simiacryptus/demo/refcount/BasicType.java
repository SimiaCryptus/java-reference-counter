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

import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;

/**
 * The type Basic type.
 */
public @com.simiacryptus.ref.lang.RefAware class BasicType extends ReferenceCountingBase
    implements Comparable<BasicType> {
  /**
   * The Label.
   */
  public final String label;
  /**
   * The Value.
   */
  public int value;

  /**
   * Instantiates a new Basic type.
   */
  public BasicType() {
    this(Long.toHexString(TestOperations.random.nextLong()));
  }

  /**
   * Instantiates a new Basic type.
   *
   * @param label the label
   */
  public BasicType(String label) {
    this.setValue(1);
    this.label = label;
  }

  /**
   * Gets self.
   *
   * @return the self
   */
  public BasicType getSelf() {
    return this.addRef();
  }

  /**
   * Gets value.
   *
   * @return the value
   */
  public int getValue() {
    assertAlive();
    return value;
  }

  /**
   * Sets value.
   *
   * @param value the value
   */
  public void setValue(int value) {
    this.value = value;
  }

  public @Override void _free() {
    value = -1;
    super._free();
  }

  @Override
  public int compareTo(@NotNull BasicType o) {
    int temp_07_0001 = this.label.compareTo(o.label);
    if (null != o)
      o.freeRef();
    return temp_07_0001;
  }

  @RefIgnore
  @Override
  public boolean equals(Object o) {
    if (o == null)
      return false;
    if (!(o instanceof BasicType))
      return false;
    BasicType basicType = (BasicType) o;
    if (this == basicType) {
      return true;
    }
    return label == basicType.label;
  }

  @RefIgnore
  @Override
  public int hashCode() {
    return label.hashCode();
  }

  @Override
  public String toString() {
    return "BasicType{" + "values=" + getValue() + '}';
  }

  /**
   * Use.
   */
  public void use() {
    this.setValue(this.getValue() + 1);
  }

  /**
   * Wrap wrapper type.
   *
   * @return the wrapper type
   */
  public WrapperType<BasicType> wrap() {
    return new WrapperType<>(this.addRef());
  }

  public @Override @SuppressWarnings("unused") BasicType addRef() {
    return (BasicType) super.addRef();
  }

  public static @SuppressWarnings("unused") BasicType[] addRefs(BasicType[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(BasicType::addRef)
        .toArray((x) -> new BasicType[x]);
  }

  public static @SuppressWarnings("unused") BasicType[][] addRefs(BasicType[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(BasicType::addRefs)
        .toArray((x) -> new BasicType[x][]);
  }
}
