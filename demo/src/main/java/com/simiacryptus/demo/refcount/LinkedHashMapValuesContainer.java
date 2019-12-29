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

import java.util.Map;

/**
 * The type Linked hash map values container.
 */
public @com.simiacryptus.ref.lang.RefAware
class LinkedHashMapValuesContainer extends ReferenceCountingBase {
  /**
   * Test basic operations.
   *
   * @param valuesMap the values map
   */
  public static void testBasicOperations(com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> valuesMap) {
    final com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> copyMap = new com.simiacryptus.ref.wrappers.RefLinkedHashMap<>();
    copyMap.putAll(valuesMap);
    valuesMap.clear();
    assert valuesMap.isEmpty();
    valuesMap.put(12, new BasicType());
    valuesMap.put(12, new BasicType());
    valuesMap.put(32, new BasicType());
    if (2 != valuesMap.size()) {
      throw new AssertionError();
    }
    if (!valuesMap.containsKey(12)) {
      throw new AssertionError();
    }
    if (!valuesMap.keySet().contains(12)) {
      throw new AssertionError();
    }
    valuesMap.remove(12);
    if (valuesMap.containsKey(12)) {
      throw new AssertionError();
    }
    if (!valuesMap.containsValue(valuesMap.get(32))) {
      throw new AssertionError();
    }
  }

  /**
   * Test stream operations.
   *
   * @param values the values
   */
  public static void testStreamOperations(com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> values) {
    values.values().stream().forEach(x -> {
      x.setValue(x.getValue() + 1);
    });
  }

  /**
   * Test.
   */
  public static void test() {
    for (int i = 0; i < TestOperations.count; i++) {
      testEntries();
      testBasicOperations(new com.simiacryptus.ref.wrappers.RefLinkedHashMap<>());
      testStreamOperations(new com.simiacryptus.ref.wrappers.RefLinkedHashMap<>());
    }
  }

  public static LinkedHashMapValuesContainer[] addRefs(LinkedHashMapValuesContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(LinkedHashMapValuesContainer::addRef)
        .toArray((x) -> new LinkedHashMapValuesContainer[x]);
  }

  public static LinkedHashMapValuesContainer[][] addRefs(LinkedHashMapValuesContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(LinkedHashMapValuesContainer::addRefs)
        .toArray((x) -> new LinkedHashMapValuesContainer[x][]);
  }

  private static void test(
      com.simiacryptus.ref.wrappers.RefConsumer<com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType>> fn) {
    final com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> hashMap = new com.simiacryptus.ref.wrappers.RefLinkedHashMap<>();
    fn.accept(hashMap);
  }

  private static void testEntries() {
    test(values -> {
      values.put(1, new BasicType());
      values.put(2, new BasicType());
      values.entrySet().forEach(fooEntry -> {
        assert null != fooEntry.getValue();
        assert null != fooEntry.getKey();
        fooEntry.setValue(new BasicType());
      });
    });
    test(values -> {
      values.put(1, new BasicType());
      values.put(2, new BasicType());
      final com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>> entryConsumer = fooEntry81 -> {
        if (1 == ((int) fooEntry81.getKey())) {
          if (null == fooEntry81.getValue()) {
            throw new AssertionError();
          }
        } else {
          if (null == fooEntry81.getValue()) {
            return;
          }
        }
        fooEntry81.setValue(new BasicType());
      };
      values.entrySet().forEach(entryConsumer);
    });
    test((com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> values) -> {
      values.put(1, new BasicType());
      values.put(2, new BasicType());
      final com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> closureMap = new com.simiacryptus.ref.wrappers.RefLinkedHashMap<>();
      final com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>> entryConsumer = (com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>>) lambdaParameter -> {
        if (1 == ((int) lambdaParameter.getKey())) {
          if (null == lambdaParameter.getValue()) {
            throw new AssertionError();
          }
        } else {
          if (null == lambdaParameter.getValue()) {
            return;
          }
        }
        closureMap.put(lambdaParameter.getKey(), lambdaParameter.getValue());
      };
      values.entrySet().forEach(entryConsumer);
      assert closureMap.size() == values.size();
    });
    test((com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> values) -> {
      values.put(1, new BasicType());
      values.put(2, new BasicType());
      final com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> closureMap = new com.simiacryptus.ref.wrappers.RefLinkedHashMap<>();
      final com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>> entryConsumer = new com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>>() {
        @Override
        public void accept(Map.Entry<Integer, BasicType> anonymousParameter) {
          if (1 == ((int) anonymousParameter.getKey())) {
            if (null == anonymousParameter.getValue()) {
              throw new AssertionError();
            }
          } else {
            if (null == anonymousParameter.getValue()) {
              return;
            }
          }
          closureMap.put(anonymousParameter.getKey(), anonymousParameter.getValue());
        }

        public void _free() {
        }

      };
      values.entrySet().forEach(entryConsumer);
      assert closureMap.size() == values.size();
    });
  }

  public @Override
  void _free() {
    super._free();
  }

  public @Override
  LinkedHashMapValuesContainer addRef() {
    return (LinkedHashMapValuesContainer) super.addRef();
  }
}
