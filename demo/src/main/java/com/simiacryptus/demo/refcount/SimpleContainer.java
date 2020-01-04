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
public class SimpleContainer extends ReferenceCountingBase {
  public BasicType value;

  public SimpleContainer() {
    {
      value = new BasicType();
    }
  }

  public SimpleContainer(BasicType value) {
    {
      this.value = value;
    }
  }

  public @Override
  void _free() {
    value = null;
    super._free();
  }

  public void test() {
    this.value.setValue(this.value.getValue() + 1);
  }

  @NotNull
  @Override
  public String toString() {
    return "SimpleContainer{" + "values=" + value + '}';
  }
}
