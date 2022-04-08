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

import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class BasicType
 *
 * @param label the label for the BasicType object
 * @param value the value for the BasicType object
 * @docgenVersion 9
 */
@SuppressWarnings("unused")
public class BasicType extends ReferenceCountingBase implements Comparable<BasicType> {
  public final String label;
  public int value;

  public BasicType() {
    this(Long.toHexString(TestOperations.random.nextLong()));
  }

  public BasicType(String label) {
    this.setValue(1);
    this.label = label;
  }

  /**
   * @return the BasicType object itself
   * @docgenVersion 9
   */
  @Nonnull
  public BasicType getSelf() {
    return this;
  }

  /**
   * Returns the value of the object.
   *
   * @throws IllegalStateException if the object is not alive
   * @docgenVersion 9
   */
  public int getValue() {
    assertAlive();
    return value;
  }

  /**
   * Sets the value of this object.
   *
   * @param value the new value of this object
   * @docgenVersion 9
   */
  public void setValue(int value) {
    this.value = value;
  }

  /**
   * This method overrides the free method and sets the value to -1.
   *
   * @docgenVersion 9
   */
  public @Override
  void _free() {
    value = -1;
    super._free();
  }

  /**
   * @Override public int compareTo(@Nonnull BasicType o) {
   * return this.label.compareTo(o.label);
   * }
   * @docgenVersion 9
   */
  @Override
  public int compareTo(@Nonnull BasicType o) {
    return this.label.compareTo(o.label);
  }

  /**
   * @RefIgnore
   * @Override public boolean equals(@Nullable Object o) {
   * if (o == null)
   * return false;
   * if (!(o instanceof BasicType))
   * return false;
   * BasicType basicType = (BasicType) o;
   * if (this == basicType) {
   * return true;
   * }
   * return label == basicType.label;
   * }
   * @docgenVersion 9
   */
  @RefIgnore
  @Override
  public boolean equals(@Nullable Object o) {
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

  /**
   * @RefIgnore
   * @Override public int hashCode() {
   * return label.hashCode();
   * }
   * @docgenVersion 9
   */
  @RefIgnore
  @Override
  public int hashCode() {
    return label.hashCode();
  }

  /**
   * @return A String representation of this BasicType, including its values.
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public String toString() {
    return "BasicType{" + "values=" + getValue() + '}';
  }

  /**
   * Use this method to increase the value by 1
   *
   * @docgenVersion 9
   */
  public void use() {
    this.setValue(this.getValue() + 1);
  }

  /**
   * Returns a WrapperType object that wraps this BasicType object.
   *
   * @return a WrapperType object that wraps this BasicType object
   * @docgenVersion 9
   */
  @Nonnull
  public WrapperType<BasicType> wrap() {
    return new WrapperType<>(this);
  }
}
