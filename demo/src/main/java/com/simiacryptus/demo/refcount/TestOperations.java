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

public class TestOperations extends ReferenceCountingBase {
  public static final int count = 1;
  public static final Random random = new Random(143892);
  private SimpleContainer simpleContainer;
  private ArrayContainer arrayContainer;
  private ListContainer listContainer;
  private MapValuesContainer mapValuesContainer;
  private SetContainer setContainer;

  public static void main(String... args) {
    final TestOperations self = new TestOperations();
    self.run();
  }

  public @Override
  void _free() {
    super._free();
  }

  private void run() {
    BasicType basicType = testBasicType();
    testSimpleContainer();
    test(basicType);
  }

  private void test(BasicType datum1) {
    testArray(new BasicType[]{datum1});
    testSet();
    testList();
    testMap();
  }

  private void testArray(BasicType[] values) {
    this.arrayContainer = new ArrayContainer(values);
    System.out.println(String.format("Instantiated %s", arrayContainer));
    for (int i = 0; i < count; i++) {
      arrayContainer.use();
    }
    arrayContainer.useClosures1(testBasicType());
    arrayContainer.useClosures2(testBasicType());
    arrayContainer.useClosures3(testBasicType());
  }

  @NotNull
  private BasicType testBasicType() {
    BasicType datum1 = new BasicType();
    testBasicType(datum1);
    return datum1;
  }

  private BasicType testBasicType(BasicType datum1) {
    System.out.println(String.format("Instantiated %s", datum1));
    for (int i = 0; i < count; i++) {
      datum1.use();
    }
    return datum1;
  }

  private void testList() {
    this.listContainer = new ListContainer();
    System.out.println(String.format("Instantiated %s", listContainer));
    for (int i = 0; i < count; i++) {
      listContainer.test();
    }
  }

  private void testMap() {
    this.mapValuesContainer = new MapValuesContainer();
    System.out.println(String.format("Instantiated %s", mapValuesContainer));
    for (int i = 0; i < count; i++) {
      mapValuesContainer.use();
    }
  }

  private void testSet() {
    this.setContainer = new SetContainer();
    System.out.println(String.format("Instantiated %s", setContainer));
    for (int i = 0; i < count; i++) {
      setContainer.test();
    }
  }

  private void testSimpleContainer() {
    this.simpleContainer = new SimpleContainer();
    System.out.println(String.format("Instantiated %s", simpleContainer));
    for (int i = 0; i < count; i++) {
      simpleContainer.use();
    }
  }
}