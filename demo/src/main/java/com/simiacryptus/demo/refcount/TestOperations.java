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

import java.util.Random;

/**
 * The type Test operations.
 */
public @com.simiacryptus.ref.lang.RefAware class TestOperations extends ReferenceCountingBase {
  /**
   * The constant count.
   */
  public static final int count = 1;
  /**
   * The constant random.
   */
  public static final Random random = new Random(143892);
  private SimpleContainer simpleContainer;
  private ArrayContainer arrayContainer;
  private ArrayListContainer arrayListContainer;
  private LinkedListContainer linkedListContainer;
  private DeququeContainer deququeContainer;
  private HashMapValuesContainer hashMapValuesContainer;
  private LinkedHashMapValuesContainer linkedHashMapValuesContainer;
  private ConcurrentHashMapValuesContainer concurrentHashMapValuesContainer;
  private HashSetContainer hashSetContainer;
  private TreeSetContainer treeSetContainer;

  /**
   * The entry point of application.
   *
   * @param args the input arguments
   */
  public static void main(String... args) {
    final TestOperations testOperations2424 = new TestOperations();
    testOperations2424.run();
    if (null != testOperations2424)
      testOperations2424.freeRef();
  }

  public @Override void _free() {
    if (null != treeSetContainer)
      treeSetContainer.freeRef();
    if (null != hashSetContainer)
      hashSetContainer.freeRef();
    if (null != concurrentHashMapValuesContainer)
      concurrentHashMapValuesContainer.freeRef();
    if (null != linkedHashMapValuesContainer)
      linkedHashMapValuesContainer.freeRef();
    if (null != hashMapValuesContainer)
      hashMapValuesContainer.freeRef();
    if (null != deququeContainer)
      deququeContainer.freeRef();
    if (null != linkedListContainer)
      linkedListContainer.freeRef();
    if (null != arrayListContainer)
      arrayListContainer.freeRef();
    if (null != arrayContainer)
      arrayContainer.freeRef();
    if (null != simpleContainer)
      simpleContainer.freeRef();
    super._free();
  }

  private void run() {
    BasicType basicType = testBasicType();
    testSimpleContainer();
    test(basicType == null ? null : basicType.addRef());
    if (null != basicType)
      basicType.freeRef();
  }

  private void test(BasicType datum1) {
    testList();
    testArray(new BasicType[] { datum1 == null ? null : datum1.addRef() });
    if (null != datum1)
      datum1.freeRef();
    testSet();
    testDeque();
    testMap();
  }

  private void testArray(BasicType[] values) {
    {
      com.simiacryptus.demo.refcount.ArrayContainer temp_04_0001 = new ArrayContainer(
          com.simiacryptus.demo.refcount.BasicType.addRefs(values));
      if (null != this.arrayContainer)
        this.arrayContainer.freeRef();
      this.arrayContainer = temp_04_0001 == null ? null : temp_04_0001.addRef();
      if (null != temp_04_0001)
        temp_04_0001.freeRef();
    }
    if (null != values)
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(values);
    for (int i = 0; i < count; i++) {
      arrayContainer.test();
    }
    arrayContainer.useClosures1(testBasicType());
    arrayContainer.useClosures2(testBasicType());
    arrayContainer.useClosures3(testBasicType());
  }

  @NotNull
  private BasicType testBasicType() {
    BasicType datum1 = new BasicType();
    com.simiacryptus.ref.lang.RefUtil.freeRef(testBasicType(datum1 == null ? null : datum1.addRef()));
    return datum1;
  }

  private BasicType testBasicType(BasicType datum1) {
    for (int i = 0; i < count; i++) {
      datum1.use();
    }
    return datum1;
  }

  private void testDeque() {
    {
      com.simiacryptus.demo.refcount.DeququeContainer temp_04_0002 = new DeququeContainer();
      if (null != this.deququeContainer)
        this.deququeContainer.freeRef();
      this.deququeContainer = temp_04_0002 == null ? null : temp_04_0002.addRef();
      if (null != temp_04_0002)
        temp_04_0002.freeRef();
    }
    for (int i = 0; i < count; i++) {
      deququeContainer.test();
    }
  }

  private void testList() {
    {
      com.simiacryptus.demo.refcount.ArrayListContainer temp_04_0003 = new ArrayListContainer();
      if (null != this.arrayListContainer)
        this.arrayListContainer.freeRef();
      this.arrayListContainer = temp_04_0003 == null ? null : temp_04_0003.addRef();
      if (null != temp_04_0003)
        temp_04_0003.freeRef();
    }
    for (int i = 0; i < count; i++) {
      arrayListContainer.test();
    }
    {
      com.simiacryptus.demo.refcount.LinkedListContainer temp_04_0004 = new LinkedListContainer();
      if (null != this.linkedListContainer)
        this.linkedListContainer.freeRef();
      this.linkedListContainer = temp_04_0004 == null ? null : temp_04_0004.addRef();
      if (null != temp_04_0004)
        temp_04_0004.freeRef();
    }
    for (int i = 0; i < count; i++) {
      linkedListContainer.test();
    }
  }

  private void testMap() {
    {
      com.simiacryptus.demo.refcount.HashMapValuesContainer temp_04_0005 = new HashMapValuesContainer();
      if (null != this.hashMapValuesContainer)
        this.hashMapValuesContainer.freeRef();
      this.hashMapValuesContainer = temp_04_0005 == null ? null : temp_04_0005.addRef();
      if (null != temp_04_0005)
        temp_04_0005.freeRef();
    }
    for (int i = 0; i < count; i++) {
      hashMapValuesContainer.test();
    }
    {
      com.simiacryptus.demo.refcount.LinkedHashMapValuesContainer temp_04_0006 = new LinkedHashMapValuesContainer();
      if (null != this.linkedHashMapValuesContainer)
        this.linkedHashMapValuesContainer.freeRef();
      this.linkedHashMapValuesContainer = temp_04_0006 == null ? null : temp_04_0006.addRef();
      if (null != temp_04_0006)
        temp_04_0006.freeRef();
    }
    for (int i = 0; i < count; i++) {
      linkedHashMapValuesContainer.test();
    }
    {
      com.simiacryptus.demo.refcount.ConcurrentHashMapValuesContainer temp_04_0007 = new ConcurrentHashMapValuesContainer();
      if (null != this.concurrentHashMapValuesContainer)
        this.concurrentHashMapValuesContainer.freeRef();
      this.concurrentHashMapValuesContainer = temp_04_0007 == null ? null : temp_04_0007.addRef();
      if (null != temp_04_0007)
        temp_04_0007.freeRef();
    }
    for (int i = 0; i < count; i++) {
      concurrentHashMapValuesContainer.test();
    }
  }

  private void testSet() {
    {
      com.simiacryptus.demo.refcount.HashSetContainer temp_04_0008 = new HashSetContainer();
      if (null != this.hashSetContainer)
        this.hashSetContainer.freeRef();
      this.hashSetContainer = temp_04_0008 == null ? null : temp_04_0008.addRef();
      if (null != temp_04_0008)
        temp_04_0008.freeRef();
    }
    for (int i = 0; i < count; i++) {
      hashSetContainer.test();
    }
    {
      com.simiacryptus.demo.refcount.TreeSetContainer temp_04_0009 = new TreeSetContainer();
      if (null != this.treeSetContainer)
        this.treeSetContainer.freeRef();
      this.treeSetContainer = temp_04_0009 == null ? null : temp_04_0009.addRef();
      if (null != temp_04_0009)
        temp_04_0009.freeRef();
    }
    for (int i = 0; i < count; i++) {
      treeSetContainer.test();
    }
  }

  private void testSimpleContainer() {
    {
      com.simiacryptus.demo.refcount.SimpleContainer temp_04_0010 = new SimpleContainer();
      if (null != this.simpleContainer)
        this.simpleContainer.freeRef();
      this.simpleContainer = temp_04_0010 == null ? null : temp_04_0010.addRef();
      if (null != temp_04_0010)
        temp_04_0010.freeRef();
    }
    for (int i = 0; i < count; i++) {
      simpleContainer.test();
    }
  }

  public @Override @SuppressWarnings("unused") TestOperations addRef() {
    return (TestOperations) super.addRef();
  }

  public static @SuppressWarnings("unused") TestOperations[] addRefs(TestOperations[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(TestOperations::addRef)
        .toArray((x) -> new TestOperations[x]);
  }

  public static @SuppressWarnings("unused") TestOperations[][] addRefs(TestOperations[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(TestOperations::addRefs)
        .toArray((x) -> new TestOperations[x][]);
  }
}
