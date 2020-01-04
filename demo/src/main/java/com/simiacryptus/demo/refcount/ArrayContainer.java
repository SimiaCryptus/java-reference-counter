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
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ArrayContainer extends ReferenceCountingBase {
  public BasicType[] values;

  public ArrayContainer(BasicType... values) {
    {
      this.values = values;
    }
  }

  public @Override
  void _free() {
    values = null;
    super._free();
  }

  public void test() {
    java.util.Arrays.stream(this.values).forEach(x -> {
      x.setValue(x.getValue() + 1);
    });
  }

  @NotNull
  @Override
  public String toString() {
    return "ArrayContainer{" + "values=" + java.util.Arrays.toString(values) + '}';
  }

  public void useClosures1(@NotNull BasicType right) {
    java.util.Arrays.stream(this.values)
        .forEach((java.util.function.Consumer<? super com.simiacryptus.demo.refcount.BasicType>) x -> {
          x.setValue(x.getValue() + right.getValue());
        });
  }

  public void useClosures2(@NotNull BasicType right) {
    java.util.Arrays.stream(this.values).forEach(new java.util.function.Consumer<BasicType>() {
      @Override
      public void accept(@NotNull BasicType x) {
        x.setValue(x.getValue() + right.getValue());
      }

      public void _free() {
      }

    });
  }

  public void useClosures3(@NotNull BasicType right) {
    java.util.Arrays.stream(this.values).forEach(new RefAwareConsumer<BasicType>() {
      @Override
      public void accept(@NotNull BasicType x) {
        x.setValue(x.getValue() + right.getValue());
      }

      public @Override
      void _free() {
        super._free();
      }
    });
  }
}
