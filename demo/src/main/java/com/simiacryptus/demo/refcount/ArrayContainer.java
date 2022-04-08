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
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * This class contains an array of BasicType objects.
 * The array may be null.
 *
 * @docgenVersion 9
 */
@SuppressWarnings("unused")
public class ArrayContainer extends ReferenceCountingBase {
  @Nullable
  public BasicType[] values;

  public ArrayContainer(BasicType... values) {
    this.values = values;
  }

  /**
   * Frees this object from memory.
   *
   * @docgenVersion 9
   */
  public @Override
  void _free() {
    values = null;
    super._free();
  }

  /**
   * Tests that the values are not null and increments each value by 1.
   *
   * @docgenVersion 9
   */
  public void test() {
    assert this.values != null;
    Arrays.stream(this.values).forEach(x -> {
      x.setValue(x.getValue() + 1);
    });
  }

  /**
   * Returns a string representation of this ArrayContainer.
   *
   * @return a string representation of this ArrayContainer
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public String toString() {
    return "ArrayContainer{" + "values=" + Arrays.toString(values) + '}';
  }

  /**
   * Use closures to add the value of the right BasicType to each value in the values array.
   *
   * @param right the BasicType whose value will be added to each value in the values array
   * @docgenVersion 9
   */
  public void useClosures1(@Nonnull BasicType right) {
    assert this.values != null;
    Arrays.stream(this.values)
        .forEach((Consumer<? super BasicType>) x -> {
          x.setValue(x.getValue() + right.getValue());
        });
  }

  /**
   * @param right the right BasicType to use
   * @throws NullPointerException if right is null
   * @docgenVersion 9
   */
  public void useClosures2(@Nonnull BasicType right) {
    assert this.values != null;
    Arrays.stream(this.values).forEach(new Consumer<BasicType>() {
      /**
       * @Override
       * public void accept(@Nonnull BasicType x) {
       *     x.setValue(x.getValue() + right.getValue());
       * }
       *
       *   @docgenVersion 9
       */
      @Override
      public void accept(@Nonnull BasicType x) {
        x.setValue(x.getValue() + right.getValue());
      }

      /**
       * Frees this object from memory.
       *
       *   @docgenVersion 9
       */
      public void _free() {
      }
    });
  }

  /**
   * @param right the right BasicType to use
   * @docgenVersion 9
   */
  public void useClosures3(@Nonnull BasicType right) {
    assert this.values != null;
    Arrays.stream(this.values).forEach(new RefAwareConsumer<BasicType>() {
      /**
       * @Override
       * public void accept(@Nonnull BasicType x) {
       *     x.setValue(x.getValue() + right.getValue());
       * }
       *
       *   @docgenVersion 9
       */
      @Override
      public void accept(@Nonnull BasicType x) {
        x.setValue(x.getValue() + right.getValue());
      }

      /**
       * This method frees the object.
       *
       *   @docgenVersion 9
       */
      public @Override
      void _free() {
        super._free();
      }
    });
  }
}
