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

import java.util.Map;
import com.simiacryptus.ref.wrappers.RefMap;

/**
 * The type Concurrent hash map values container.
 */
public class ConcurrentHashMapValuesContainer extends ReferenceCountingBase {
  /**
   * Instantiates a new Concurrent hash map values container.
   */
  public ConcurrentHashMapValuesContainer() {
  }

  /**
   * Test basic operations.
   *
   * @param valuesMap the values map
   */
  public static void testBasicOperations(java.util.concurrent.ConcurrentHashMap<Integer, BasicType> valuesMap) {
    final java.util.concurrent.ConcurrentHashMap<Integer, BasicType> copyMap = new java.util.concurrent.ConcurrentHashMap<>();
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
  public static void testStreamOperations(java.util.concurrent.ConcurrentHashMap<Integer, BasicType> values) {
    values.values().stream().forEach(x -> {
      x.setValue(x.getValue() + 1);
    });
  }

  private static void test(java.util.function.Consumer<java.util.concurrent.ConcurrentHashMap<Integer, BasicType>> fn) {
    final java.util.concurrent.ConcurrentHashMap<Integer, BasicType> hashMap = new java.util.concurrent.ConcurrentHashMap<>();
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
      final java.util.function.Consumer<Map.Entry<Integer, BasicType>> entryConsumer = fooEntry2 -> {
        if (1 == ((int) fooEntry2.getKey())) {
          if (null == fooEntry2.getValue()) {
            throw new AssertionError();
          }
        } else {
          if (null == fooEntry2.getValue()) {
            return;
          }
        }
        fooEntry2.setValue(new BasicType());
      };
      values.entrySet().forEach(entryConsumer);
    });
    test((java.util.concurrent.ConcurrentHashMap<Integer, BasicType> values) -> {
      values.put(1, new BasicType());
      values.put(2, new BasicType());
      final java.util.concurrent.ConcurrentHashMap<Integer, BasicType> closureMap = new java.util.concurrent.ConcurrentHashMap<>();
      final java.util.function.Consumer<Map.Entry<Integer, BasicType>> entryConsumer = (java.util.function.Consumer<Map.Entry<Integer, BasicType>>) lambdaParameter -> {
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
    test((java.util.concurrent.ConcurrentHashMap<Integer, BasicType> values) -> {
      values.put(1, new BasicType());
      values.put(2, new BasicType());
      final java.util.concurrent.ConcurrentHashMap<Integer, BasicType> closureMap = new java.util.concurrent.ConcurrentHashMap<>();
      final java.util.function.Consumer<Map.Entry<Integer, BasicType>> entryConsumer = new java.util.function.Consumer<Map.Entry<Integer, BasicType>>() {
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

  public @Override void _free() {
    super._free();
  }

  /**
   * Test.
   */
  public void test() {
    for (int i = 0; i < TestOperations.count; i++) {
      testEntries();
      testBasicOperations(new java.util.concurrent.ConcurrentHashMap<>());
      testStreamOperations(new java.util.concurrent.ConcurrentHashMap<>());
    }
  }
}
