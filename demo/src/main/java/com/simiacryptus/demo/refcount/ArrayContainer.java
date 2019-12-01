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

import java.util.function.Consumer;

public class ArrayContainer extends ReferenceCountingBase {
  public BasicType[] values;

  public ArrayContainer(BasicType... values) {
    this.values = values;
  }

  public @Override
  void _free() {
    super._free();
  }

  @Override
  public String toString() {
    return "ArrayContainer{" + "values=" + java.util.Arrays.toString(values) + '}';
  }

  public void use() {
    System.out.println(String.format("Increment %s", this));
    java.util.Arrays.stream(this.values).forEach(x -> {
      x.value++;
    });
  }

  public void useClosures1(BasicType right) {
    System.out.println(String.format("Increment %s", this));
    java.util.Arrays.stream(this.values).forEach(x -> {
      x.value += right.value;
    });
  }

  public void useClosures2(BasicType right) {
    System.out.println(String.format("Increment %s", this));
    java.util.Arrays.stream(this.values).forEach(new Consumer<BasicType>() {
      @Override
      public void accept(BasicType x) {
        x.value += right.value;
      }
    });
  }

  public void useClosures3(BasicType right) {
    System.out.println(String.format("Increment %s", this));
    java.util.Arrays.stream(this.values).forEach(new RefAwareConsumer<BasicType>() {
      @Override
      public void accept(BasicType x) {
        x.value += right.value;
      }
    });
  }
}