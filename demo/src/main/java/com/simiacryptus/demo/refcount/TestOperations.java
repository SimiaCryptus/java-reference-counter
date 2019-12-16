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
public class TestOperations extends ReferenceCountingBase {
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
  }

  public @Override void _free() {
    super._free();
  }

  private void run() {
    BasicType basicType = testBasicType();
    testSimpleContainer();
    test(basicType);
  }

  private void test(BasicType datum1) {
    testList();
    testArray(new BasicType[] { datum1 });
    testSet();
    testDeque();
    testMap();
  }

  private void testArray(BasicType[] values) {
    {
      {
		this.arrayContainer = new ArrayContainer(
		    values);
	}
      for (int i = 0; i < count; i++) {
        arrayContainer.test();
      }
      arrayContainer.useClosures1(testBasicType());
      arrayContainer.useClosures2(testBasicType());
      arrayContainer.useClosures3(testBasicType());
    }
  }

  @NotNull
  private BasicType testBasicType() {
    BasicType datum1 = new BasicType();
    testBasicType(datum1);
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
      {
		this.deququeContainer = new DeququeContainer();
	}
      for (int i = 0; i < count; i++) {
        deququeContainer.test();
      }
    }
  }

  private void testList() {
    {
      {
		this.arrayListContainer = new ArrayListContainer();
	}
      for (int i = 0; i < count; i++) {
        arrayListContainer.test();
      }
    }
    {
      {
		this.linkedListContainer = new LinkedListContainer();
	}
      for (int i = 0; i < count; i++) {
        linkedListContainer.test();
      }
    }
  }

  private void testMap() {
    {
      {
		this.hashMapValuesContainer = new HashMapValuesContainer();
	}
      for (int i = 0; i < count; i++) {
        hashMapValuesContainer.test();
      }
    }
    {
      {
		this.linkedHashMapValuesContainer = new LinkedHashMapValuesContainer();
	}
      for (int i = 0; i < count; i++) {
        linkedHashMapValuesContainer.test();
      }
    }
    {
      {
		this.concurrentHashMapValuesContainer = new ConcurrentHashMapValuesContainer();
	}
      for (int i = 0; i < count; i++) {
        concurrentHashMapValuesContainer.test();
      }
    }
  }

  private void testSet() {
    {
      {
		this.hashSetContainer = new HashSetContainer();
	}
      for (int i = 0; i < count; i++) {
        hashSetContainer.test();
      }
    }
    {
      {
		this.treeSetContainer = new TreeSetContainer();
	}
      for (int i = 0; i < count; i++) {
        treeSetContainer.test();
      }
    }
  }

  private void testSimpleContainer() {
    {
		this.simpleContainer = new SimpleContainer();
	}
    for (int i = 0; i < count; i++) {
      simpleContainer.test();
    }
  }
}
