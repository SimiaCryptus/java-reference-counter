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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * This class represents a hash set container.
 *
 * @docgenVersion 9
 */
@SuppressWarnings("unused")
public class HashSetContainer extends ReferenceCountingBase {
  /**
   * This is the test method.
   * It will test the collection, element, stream, and array operations.
   *
   * @docgenVersion 9
   */
  public static void test() {
    for (int i = 0; i < TestOperations.count; i++) {
      testCollectionOperations();
      testElementOperations();
      testStreamOperations();
      testArrayOperations();
    }
  }

  /**
   * Tests stream operations on a {@link ValueContainer}.
   *
   * @param operation the operation to test
   * @docgenVersion 9
   */
  private static void testStreamOperations() {
    testOperations(values -> {
      values.stream().forEach(x -> {
        x.setValue(x.getValue() + 1);
      });
    });
  }

  /**
   * Tests the ArrayOperations class.
   *
   * @docgenVersion 9
   */
  private static void testArrayOperations() {
    testOperations(values -> {
      values.add(new BasicType());
      if (0 == values.size()) {
        throw new RuntimeException();
      }
      if (values.size() != values.toArray(new BasicType[]{}).length) {
        throw new RuntimeException();
      }
      values.toArray(new BasicType[]{});
    });
  }

  /**
   * Tests the element operations of the stack.
   *
   * @docgenVersion 9
   */
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
    });
  }

  /**
   * Tests the operations of a set.
   *
   * @param setRefConsumer the set to test
   * @docgenVersion 9
   */
  private static void testOperations(
      @Nonnull Consumer<HashSet<BasicType>> setRefConsumer) {
    HashSet<BasicType> values = new HashSet<>();
    setRefConsumer.accept(values);
  }

  /**
   * Tests the collection operations.
   *
   * @docgenVersion 9
   */
  private static void testCollectionOperations() {
    testOperations(setValues -> {
      setValues.add(new BasicType());
      final BasicType basicType = new BasicType();
      final List<BasicType> list = Arrays.asList(basicType);
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

  /**
   * This method frees the object.
   *
   * @docgenVersion 9
   */
  public @Override
  void _free() {
    super._free();
  }
}
