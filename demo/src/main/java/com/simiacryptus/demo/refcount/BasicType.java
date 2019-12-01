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
import org.jetbrains.annotations.NotNull;

public class BasicType extends ReferenceCountingBase implements Comparable<BasicType> {
  public static final boolean BUG_WORKAROUND = true;
  private final String label;
  private final double doubleLabel;
  public int value;

  public BasicType() {
    this(Double.toString(TestOperations.random.nextDouble()));
  }

  public BasicType(String label) {
    this.value = 1;
    this.doubleLabel = TestOperations.random.nextDouble();
    this.label = label;
  }

  public @Override
  void _free() {
    super._free();
  }

  @Override
  public int compareTo(@NotNull BasicType o) {
    return this.label.compareTo(o.label);
  }

  @Override
  public boolean equals(Object o) {
    if (BUG_WORKAROUND)
      return super.equals(o);
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    BasicType basicType = (BasicType) o;
    return label == basicType.label;
  }

  @Override
  public int hashCode() {
    if (BUG_WORKAROUND)
      return super.hashCode();
    return label.hashCode();
  }

  @Override
  public String toString() {
    return "BasicType{" + "values=" + value + '}';
  }

  public void use() {
    System.out.println(String.format("Increment %s", this));
    this.value++;
  }
}
