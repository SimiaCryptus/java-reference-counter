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
import java.util.Random;

/**
 * This class tests the operations of the other classes.
 *
 * @author Java
 * @docgenVersion 9
 */
@SuppressWarnings("unused")
public class TestOperations extends ReferenceCountingBase {
  public static final int count = 1;
  public static final Random random = new Random(143892);
  @Nullable
  private SimpleContainer simpleContainer;
  @Nullable
  private ArrayContainer arrayContainer;
  @Nullable
  private ArrayListContainer arrayListContainer;
  @Nullable
  private LinkedListContainer linkedListContainer;
  @Nullable
  private DeququeContainer deququeContainer;
  @Nullable
  private HashMapValuesContainer hashMapValuesContainer;
  @Nullable
  private LinkedHashMapValuesContainer linkedHashMapValuesContainer;
  @Nullable
  private ConcurrentHashMapValuesContainer concurrentHashMapValuesContainer;
  @Nullable
  private HashSetContainer hashSetContainer;
  @Nullable
  private TreeSetContainer treeSetContainer;

  /**
   * Main method for the TestOperations class.
   *
   * @param args Command line arguments.
   * @docgenVersion 9
   */
  public static void main(String... args) {
    final TestOperations testOperations2424 = new TestOperations();
    testOperations2424.run();
  }

  /**
   * This method frees the object.
   *
   * @docgenVersion 9
   */
  public @Override
  void _free() {
    treeSetContainer = null;
    hashSetContainer = null;
    concurrentHashMapValuesContainer = null;
    linkedHashMapValuesContainer = null;
    hashMapValuesContainer = null;
    deququeContainer = null;
    linkedListContainer = null;
    arrayListContainer = null;
    arrayContainer = null;
    simpleContainer = null;
    super._free();
  }

  /**
   * This method runs the tests for the BasicType and SimpleContainer classes.
   *
   * @param basicType an instance of the BasicType class
   * @docgenVersion 9
   */
  private void run() {
    BasicType basicType = testBasicType();
    testSimpleContainer();
    test(basicType);
  }

  /**
   * Tests the functionality of the data structures.
   *
   * @param datum1 the first piece of data to test with
   * @docgenVersion 9
   */
  private void test(BasicType datum1) {
    testList();
    testArray(new BasicType[]{datum1});
    testSet();
    testDeque();
    testMap();
  }

  /**
   * Tests the given array of BasicType values.
   *
   * @param values the array of BasicType values to test
   * @docgenVersion 9
   */
  private void testArray(BasicType[] values) {
    this.arrayContainer = new ArrayContainer(values);
    for (int i = 0; i < count; i++) {
      arrayContainer.test();
    }
    arrayContainer.useClosures1(testBasicType());
    arrayContainer.useClosures2(testBasicType());
    arrayContainer.useClosures3(testBasicType());
  }

  /**
   * @return BasicType
   * @docgenVersion 9
   */
  @Nonnull
  private BasicType testBasicType() {
    BasicType datum1 = new BasicType();
    testBasicType(datum1);
    return datum1;
  }

  /**
   * @param datum1 the BasicType to use
   * @return the BasicType that was used
   * @throws NullPointerException if datum1 is null
   * @docgenVersion 9
   */
  @Nonnull
  private BasicType testBasicType(@Nonnull BasicType datum1) {
    for (int i = 0; i < count; i++) {
      datum1.use();
    }
    return datum1;
  }

  /**
   * Tests the DequeContainer class.
   *
   * @docgenVersion 9
   */
  private void testDeque() {
    this.deququeContainer = new DeququeContainer();
    for (int i = 0; i < count; i++) {
      DeququeContainer.test();
    }
  }

  /**
   * Tests the list to see if it is empty.
   *
   * @docgenVersion 9
   */
  private void testList() {
    this.arrayListContainer = new ArrayListContainer();
    for (int i = 0; i < count; i++) {
      ArrayListContainer.test();
    }
    this.linkedListContainer = new LinkedListContainer();
    for (int i = 0; i < count; i++) {
      LinkedListContainer.test();
    }
  }

  /**
   * Tests the map.
   *
   * @docgenVersion 9
   */
  private void testMap() {
    this.hashMapValuesContainer = new HashMapValuesContainer();
    for (int i = 0; i < count; i++) {
      HashMapValuesContainer.test();
    }
    this.linkedHashMapValuesContainer = new LinkedHashMapValuesContainer();
    for (int i = 0; i < count; i++) {
      LinkedHashMapValuesContainer.test();
    }
    this.concurrentHashMapValuesContainer = new ConcurrentHashMapValuesContainer();
    for (int i = 0; i < count; i++) {
      concurrentHashMapValuesContainer.test();
    }
  }

  /**
   * @testSet private void testSet();
   * @docgenVersion 9
   */
  private void testSet() {
    this.hashSetContainer = new HashSetContainer();
    for (int i = 0; i < count; i++) {
      HashSetContainer.test();
    }
    this.treeSetContainer = new TreeSetContainer();
    for (int i = 0; i < count; i++) {
      TreeSetContainer.test();
    }
  }

  /**
   * Tests the simple container.
   *
   * @docgenVersion 9
   */
  private void testSimpleContainer() {
    this.simpleContainer = new SimpleContainer();
    for (int i = 0; i < count; i++) {
      simpleContainer.test();
    }
  }
}
