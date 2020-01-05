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

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class TreeSetContainer extends ReferenceCountingBase {
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
      @NotNull Consumer<TreeSet<BasicType>> setRefConsumer) {
    TreeSet<BasicType> values = new TreeSet<>();
    setRefConsumer.accept(values);
  }

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

  public @Override
  void _free() {
    super._free();
  }
}
