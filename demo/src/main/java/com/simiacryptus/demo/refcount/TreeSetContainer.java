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
 * The type Tree set container.
 */
public @com.simiacryptus.ref.lang.RefAware
class TreeSetContainer extends ReferenceCountingBase {
  /**
   * Test.
   */
  public static void test() {
    for (int i = 0; i < TestOperations.count; i++) {
      testCollectionOperations();
      testElementOperations();
      testStreamOperations();
      testArrayOperations();
    }
  }

  public static TreeSetContainer[] addRefs(TreeSetContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(TreeSetContainer::addRef)
        .toArray((x) -> new TreeSetContainer[x]);
  }

  public static TreeSetContainer[][] addRefs(TreeSetContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(TreeSetContainer::addRefs)
        .toArray((x) -> new TreeSetContainer[x][]);
  }

  private static void testStreamOperations() {
    testOperations(values -> {
      values.stream().forEach(x -> {
        x.setValue(x.getValue() + 1);
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
      if (values.size() != values.toArray(new BasicType[]{}).length) {
        throw new RuntimeException();
      }
      values.toArray(new BasicType[]{});
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
      values.iterator();
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
      if (!values.isEmpty()) {
        throw new RuntimeException();
      }
    });
  }

  private static void testOperations(
      com.simiacryptus.ref.wrappers.RefConsumer<com.simiacryptus.ref.wrappers.RefTreeSet<BasicType>> setRefConsumer) {
    com.simiacryptus.ref.wrappers.RefTreeSet<BasicType> values = new com.simiacryptus.ref.wrappers.RefTreeSet<>();
    setRefConsumer.accept(values);
  }

  private static void testCollectionOperations() {
    testOperations(setValues -> {
      setValues.add(new BasicType());
      final BasicType basicType = new BasicType();
      final com.simiacryptus.ref.wrappers.RefList<BasicType> list = com.simiacryptus.ref.wrappers.RefArrays
          .asList(basicType);
      if (!setValues.addAll(list)) {
        throw new RuntimeException();
      }
      if (!setValues.containsAll(list)) {
        throw new RuntimeException();
      }
      if (!setValues.retainAll(list)) {
        throw new RuntimeException();
      }
      setValues.removeAll(list);
      if (!setValues.isEmpty()) {
        throw new RuntimeException();
      }
    });
  }

  public @Override
  void _free() {
    super._free();
  }

  public @Override
  TreeSetContainer addRef() {
    return (TreeSetContainer) super.addRef();
  }
}
