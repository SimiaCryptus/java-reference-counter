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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class LinkedHashMapValuesContainer extends ReferenceCountingBase {
  public static void testBasicOperations(@Nonnull LinkedHashMap<Integer, BasicType> valuesMap) {
    final LinkedHashMap<Integer, BasicType> copyMap = new LinkedHashMap<>();
    copyMap.putAll(valuesMap);
    valuesMap.clear();
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

  public static void testStreamOperations(@Nonnull LinkedHashMap<Integer, BasicType> values) {
    values.values().stream().forEach(x -> {
      x.setValue(x.getValue() + 1);
    });
  }

  public static void test() {
    for (int i = 0; i < TestOperations.count; i++) {
      testEntries();
      testBasicOperations(new LinkedHashMap<>());
      testStreamOperations(new LinkedHashMap<>());
    }
  }

  private static void test(@Nonnull Consumer<LinkedHashMap<Integer, BasicType>> fn) {
    final LinkedHashMap<Integer, BasicType> hashMap = new LinkedHashMap<>();
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
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = fooEntry81 -> {
        if (1 == fooEntry81.getKey()) {
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
    test((LinkedHashMap<Integer, BasicType> values) -> {
      values.put(1, new BasicType());
      values.put(2, new BasicType());
      final LinkedHashMap<Integer, BasicType> closureMap = new LinkedHashMap<>();
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = lambdaParameter -> {
        if (1 == lambdaParameter.getKey()) {
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
    test((LinkedHashMap<Integer, BasicType> values) -> {
      values.put(1, new BasicType());
      values.put(2, new BasicType());
      final LinkedHashMap<Integer, BasicType> closureMap = new LinkedHashMap<>();
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = new Consumer<Map.Entry<Integer, BasicType>>() {
        @Override
        public void accept(@Nonnull Map.Entry<Integer, BasicType> anonymousParameter) {
          if (1 == anonymousParameter.getKey()) {
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
}
