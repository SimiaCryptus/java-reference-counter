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
import com.simiacryptus.ref.wrappers.RefMap;

/**
 * The type Hash map values container.
 */
public @com.simiacryptus.ref.lang.RefAware class HashMapValuesContainer extends ReferenceCountingBase {
  /**
   * Test basic operations.
   *
   * @param valuesMap the values map
   */
  public static void testBasicOperations(com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> valuesMap) {
    final com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> copyMap = new com.simiacryptus.ref.wrappers.RefHashMap<>();
    copyMap.putAll(com.simiacryptus.ref.lang.RefUtil.addRef(valuesMap));
    copyMap.freeRef();
    valuesMap.clear();
    assert valuesMap.isEmpty();
    com.simiacryptus.ref.lang.RefUtil.freeRef(valuesMap.put(12, new BasicType()));
    com.simiacryptus.ref.lang.RefUtil.freeRef(valuesMap.put(12, new BasicType()));
    com.simiacryptus.ref.lang.RefUtil.freeRef(valuesMap.put(32, new BasicType()));
    if (2 != valuesMap.size()) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    if (!valuesMap.containsKey(12)) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    com.simiacryptus.ref.wrappers.RefSet<java.lang.Integer> temp4217 = valuesMap.keySet();
    if (!temp4217.contains(12)) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    temp4217.freeRef();
    com.simiacryptus.ref.lang.RefUtil.freeRef(valuesMap.remove(12));
    if (valuesMap.containsKey(12)) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    if (!valuesMap.containsValue(valuesMap.get(32))) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    valuesMap.freeRef();
  }

  /**
   * Test stream operations.
   *
   * @param values the values
   */
  public static void testStreamOperations(com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> values) {
    com.simiacryptus.ref.wrappers.RefHashSet<com.simiacryptus.demo.refcount.BasicType> temp7473 = values.values();
    temp7473.stream().forEach(x -> {
      x.setValue(x.getValue() + 1);
      x.freeRef();
    });
    temp7473.freeRef();
    values.freeRef();
  }

  private static void test(
      com.simiacryptus.ref.wrappers.RefConsumer<com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType>> fn) {
    final com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> hashMap = new com.simiacryptus.ref.wrappers.RefHashMap<>();
    fn.accept(com.simiacryptus.ref.lang.RefUtil.addRef(hashMap));
    hashMap.freeRef();
    fn.freeRef();
  }

  private static void testEntries() {
    test(values -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp8982 = values
          .entrySet();
      temp8982.forEach(fooEntry -> {
        com.simiacryptus.demo.refcount.BasicType temp6932 = fooEntry.getValue();
        assert null != temp6932;
        temp6932.freeRef();
        assert null != fooEntry.getKey();
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry.setValue(new BasicType()));
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry);
      });
      temp8982.freeRef();
      values.freeRef();
    });
    test(values -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>> entryConsumer = fooEntry2 -> {
        if (1 == ((int) fooEntry2.getKey())) {
          com.simiacryptus.demo.refcount.BasicType temp8463 = fooEntry2.getValue();
          if (null == temp8463) {
            com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
            throw new AssertionError();
          }
          temp8463.freeRef();
        } else {
          com.simiacryptus.demo.refcount.BasicType temp1823 = fooEntry2.getValue();
          if (null == temp1823) {
            com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
            return;
          }
          temp1823.freeRef();
        }
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2.setValue(new BasicType()));
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
      };
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp7902 = values
          .entrySet();
      temp7902.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      temp7902.freeRef();
      entryConsumer.freeRef();
      values.freeRef();
    });
    test((com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> values) -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> closureMap = new com.simiacryptus.ref.wrappers.RefHashMap<>();
      final com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>> entryConsumer = (com.simiacryptus.ref.wrappers.RefConsumer<com.simiacryptus.ref.wrappers.RefMap.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>>) com.simiacryptus.ref.lang.RefUtil
          .wrapInterface(
              (com.simiacryptus.ref.wrappers.RefConsumer<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>>) lambdaParameter -> {
                if (1 == ((int) lambdaParameter.getKey())) {
                  com.simiacryptus.demo.refcount.BasicType temp6133 = lambdaParameter.getValue();
                  if (null == temp6133) {
                    com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
                    throw new AssertionError();
                  }
                  temp6133.freeRef();
                } else {
                  com.simiacryptus.demo.refcount.BasicType temp2124 = lambdaParameter.getValue();
                  if (null == temp2124) {
                    com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
                    return;
                  }
                  temp2124.freeRef();
                }
                com.simiacryptus.ref.lang.RefUtil
                    .freeRef(closureMap.put(lambdaParameter.getKey(), lambdaParameter.getValue()));
                com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
              }, com.simiacryptus.ref.lang.RefUtil.addRef(closureMap));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp6710 = values
          .entrySet();
      temp6710.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      temp6710.freeRef();
      entryConsumer.freeRef();
      assert closureMap.size() == values.size();
      closureMap.freeRef();
      values.freeRef();
    });
    test((com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> values) -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> closureMap = new com.simiacryptus.ref.wrappers.RefHashMap<>();
      final com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>> entryConsumer = com.simiacryptus.ref.lang.RefUtil
          .wrapInterface(new com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>>() {
            @Override
            public void accept(Map.Entry<Integer, BasicType> anonymousParameter) {
              if (1 == ((int) anonymousParameter.getKey())) {
                com.simiacryptus.demo.refcount.BasicType temp7431 = anonymousParameter.getValue();
                if (null == temp7431) {
                  com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
                  throw new AssertionError();
                }
                temp7431.freeRef();
              } else {
                com.simiacryptus.demo.refcount.BasicType temp2196 = anonymousParameter.getValue();
                if (null == temp2196) {
                  com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
                  return;
                }
                temp2196.freeRef();
              }
              com.simiacryptus.ref.lang.RefUtil
                  .freeRef(closureMap.put(anonymousParameter.getKey(), anonymousParameter.getValue()));
              com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
            }

            public void _free() {
            }
          }, com.simiacryptus.ref.lang.RefUtil.addRef(closureMap));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp2407 = values
          .entrySet();
      temp2407.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      temp2407.freeRef();
      entryConsumer.freeRef();
      assert closureMap.size() == values.size();
      closureMap.freeRef();
      values.freeRef();
    });
  }

  /**
   * Test.
   */
  public static void test() {
    for (int i = 0; i < TestOperations.count; i++) {
      testEntries();
      testBasicOperations(new com.simiacryptus.ref.wrappers.RefHashMap<>());
      testStreamOperations(new com.simiacryptus.ref.wrappers.RefHashMap<>());
    }
  }

  public @Override void _free() {
    super._free();
  }

  public @Override HashMapValuesContainer addRef() {
    return (HashMapValuesContainer) super.addRef();
  }

  public static HashMapValuesContainer[] addRefs(HashMapValuesContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(HashMapValuesContainer::addRef)
        .toArray((x) -> new HashMapValuesContainer[x]);
  }

  public static HashMapValuesContainer[][] addRefs(HashMapValuesContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(HashMapValuesContainer::addRefs)
        .toArray((x) -> new HashMapValuesContainer[x][]);
  }
}
