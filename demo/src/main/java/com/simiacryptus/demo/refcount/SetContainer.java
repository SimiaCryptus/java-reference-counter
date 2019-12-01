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

public class SetContainer extends ReferenceCountingBase {
  public java.util.HashSet<BasicType> values = new java.util.HashSet<>();

  public SetContainer(BasicType... values) {
    for (BasicType value : values) {
      this.values.add(value);
    }
  }

  private static void testStreamOperations() {
    testOperations(values -> {
      values.stream().forEach(x -> {
        x.value++;
      });
    });
  }

  private static void testArrayOperations() {
    testOperations(values -> {
      values.add(new BasicType());
      if (0 == values.size()) {
        throw new RuntimeException();
      }
      if (false) {
        if (values.size() != values.toArray().length) {
          throw new RuntimeException();
        }
      }
      if (values.size() != values.toArray(new BasicType[]{}).length)
        throw new RuntimeException();
    });
  }

  private static void testElementOperations() {
    testOperations(values -> {
      if (!values.isEmpty()) {
        throw new RuntimeException();
      }
      final BasicType basicType1 = new BasicType();
      if (!values.add(basicType1)) {
        throw new RuntimeException();
      }
      if (!values.contains(basicType1)) {
        throw new RuntimeException();
      }
      if (values.add(values.iterator().next())) {
        throw new RuntimeException();
      }
      if (!values.contains(basicType1)) {
        throw new RuntimeException();
      }
      if (!values.remove(basicType1)) {
        throw new RuntimeException();
      }
      if (values.remove(basicType1)) {
        throw new RuntimeException();
      }
      if (!values.add(basicType1)) {
        throw new RuntimeException();
      }
      values.clear();
      if (!values.isEmpty())
        throw new RuntimeException();
    });
  }

  private static void testOperations(Consumer<java.util.HashSet<BasicType>> fn) {
    java.util.HashSet<BasicType> values = new java.util.HashSet<>();
    fn.accept(values);
  }

  public @Override
  void _free() {
    super._free();
  }

  public void test() {
    System.out.println(String.format("Increment %s", this));
    if (this.values.size() != new java.util.HashSet<>(this.values).size())
      throw new RuntimeException();
    for (int i = 0; i < TestOperations.count; i++) {
      testCollectionOperations();
      testElementOperations();
      testStreamOperations();
      testArrayOperations();
    }
  }

  private void testCollectionOperations() {
    testOperations(values -> {
      values.add(new BasicType());
      final BasicType basicType = new BasicType();
      final java.util.List<BasicType> list = java.util.Arrays.asList(basicType);
      if (!values.addAll(list)) {
        throw new RuntimeException();
      }
      if (!values.containsAll(list)) {
        throw new RuntimeException();
      }
      if (!values.retainAll(list)) {
        throw new RuntimeException();
      }
      values.removeAll(list);
      if (!values.isEmpty())
        throw new RuntimeException();
    });
  }

  @Override
  public String toString() {
    return "SetContainer{" + "values=" + values + '}';
  }
}
