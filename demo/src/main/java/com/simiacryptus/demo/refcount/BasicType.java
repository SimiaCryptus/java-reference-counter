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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  @NotNull
  public BasicType getSelf() {
    return this;
  }

  public int getValue() {
    assertAlive();
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }

  public @Override void _free() {
    value = -1;
    super._free();
  }

  @Override
  public int compareTo(@NotNull BasicType o) {
    return this.label.compareTo(o.label);
  }

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

  @RefIgnore
  @Override
  public int hashCode() {
    return label.hashCode();
  }

  @NotNull
  @Override
  public String toString() {
    return "BasicType{" + "values=" + getValue() + '}';
  }

  public void use() {
    this.setValue(this.getValue() + 1);
  }

  @NotNull
  public WrapperType<BasicType> wrap() {
    return new WrapperType<>(this);
  }
}
