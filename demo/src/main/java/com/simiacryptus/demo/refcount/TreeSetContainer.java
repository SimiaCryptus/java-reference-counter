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
public @com.simiacryptus.ref.lang.RefAware class TreeSetContainer extends ReferenceCountingBase {
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

  private static void testStreamOperations() {
    testOperations(values -> {
      values.stream().forEach(x -> {
        x.setValue(x.getValue() + 1);
        if (null != x)
          x.freeRef();
      });
      if (null != values)
        values.freeRef();
    });
  }

  private static void testArrayOperations() {
    testOperations(values -> {
      values.add(new BasicType());
      if (0 == values.size()) {
        if (null != values)
          values.freeRef();
        throw new RuntimeException();
      }
      if (false) {
        if (values.size() != values.toArray().length) {
          if (null != values)
            values.freeRef();
          throw new RuntimeException();
        }
      }
      com.simiacryptus.demo.refcount.BasicType[] temp_12_0001 = values.toArray(new BasicType[] {});
      if (values.size() != temp_12_0001.length) {
        if (null != values)
          values.freeRef();
        throw new RuntimeException();
      }
      if (null != temp_12_0001)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_12_0001);
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(values.toArray(new BasicType[] {}));
      if (null != values)
        values.freeRef();
    });
  }

  private static void testElementOperations() {
    testOperations(values -> {
      if (!values.isEmpty()) {
        if (null != values)
          values.freeRef();
        throw new RuntimeException();
      }
      final BasicType basicType1 = new BasicType();
      if (!values.add(basicType1 == null ? null : basicType1.addRef())) {
        if (null != values)
          values.freeRef();
        if (null != basicType1)
          basicType1.freeRef();
        throw new RuntimeException();
      }
      if (!values.contains(basicType1 == null ? null : basicType1.addRef())) {
        if (null != values)
          values.freeRef();
        if (null != basicType1)
          basicType1.freeRef();
        throw new RuntimeException();
      }
      com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> temp_12_0002 = values
          .iterator();
      if (values.add(temp_12_0002.next())) {
        if (null != values)
          values.freeRef();
        if (null != basicType1)
          basicType1.freeRef();
        throw new RuntimeException();
      }
      if (null != temp_12_0002)
        temp_12_0002.freeRef();
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.iterator());
      if (!values.contains(basicType1 == null ? null : basicType1.addRef())) {
        if (null != values)
          values.freeRef();
        if (null != basicType1)
          basicType1.freeRef();
        throw new RuntimeException();
      }
      if (!values.remove(basicType1 == null ? null : basicType1.addRef())) {
        if (null != values)
          values.freeRef();
        if (null != basicType1)
          basicType1.freeRef();
        throw new RuntimeException();
      }
      if (values.remove(basicType1 == null ? null : basicType1.addRef())) {
        if (null != values)
          values.freeRef();
        if (null != basicType1)
          basicType1.freeRef();
        throw new RuntimeException();
      }
      if (!values.add(basicType1 == null ? null : basicType1.addRef())) {
        if (null != values)
          values.freeRef();
        if (null != basicType1)
          basicType1.freeRef();
        throw new RuntimeException();
      }
      if (null != basicType1)
        basicType1.freeRef();
      values.clear();
      if (!values.isEmpty()) {
        if (null != values)
          values.freeRef();
        throw new RuntimeException();
      }
      if (null != values)
        values.freeRef();
    });
  }

  private static void testOperations(
      com.simiacryptus.ref.wrappers.RefConsumer<com.simiacryptus.ref.wrappers.RefTreeSet<BasicType>> setRefConsumer) {
    com.simiacryptus.ref.wrappers.RefTreeSet<BasicType> values = new com.simiacryptus.ref.wrappers.RefTreeSet<>();
    setRefConsumer.accept(values == null ? null : values.addRef());
    if (null != setRefConsumer)
      setRefConsumer.freeRef();
    if (null != values)
      values.freeRef();
  }

  private static void testCollectionOperations() {
    testOperations(setValues -> {
      setValues.add(new BasicType());
      final BasicType basicType = new BasicType();
      final com.simiacryptus.ref.wrappers.RefList<BasicType> list = com.simiacryptus.ref.wrappers.RefArrays
          .asList(basicType == null ? null : basicType.addRef());
      if (null != basicType)
        basicType.freeRef();
      if (!setValues.addAll(list == null ? null : list.addRef())) {
        if (null != setValues)
          setValues.freeRef();
        if (null != list)
          list.freeRef();
        throw new RuntimeException();
      }
      if (!setValues.containsAll(list == null ? null : list.addRef())) {
        if (null != setValues)
          setValues.freeRef();
        if (null != list)
          list.freeRef();
        throw new RuntimeException();
      }
      if (!setValues.retainAll(list == null ? null : list.addRef())) {
        if (null != setValues)
          setValues.freeRef();
        if (null != list)
          list.freeRef();
        throw new RuntimeException();
      }
      setValues.removeAll(list == null ? null : list.addRef());
      if (null != list)
        list.freeRef();
      if (!setValues.isEmpty()) {
        if (null != setValues)
          setValues.freeRef();
        throw new RuntimeException();
      }
      if (null != setValues)
        setValues.freeRef();
    });
  }

  public @Override void _free() {
    super._free();
  }

  public @Override @SuppressWarnings("unused") TreeSetContainer addRef() {
    return (TreeSetContainer) super.addRef();
  }

  public static @SuppressWarnings("unused") TreeSetContainer[] addRefs(TreeSetContainer[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(TreeSetContainer::addRef)
        .toArray((x) -> new TreeSetContainer[x]);
  }

  public static @SuppressWarnings("unused") TreeSetContainer[][] addRefs(TreeSetContainer[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(TreeSetContainer::addRefs)
        .toArray((x) -> new TreeSetContainer[x][]);
  }
}
