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
 * The type Concurrent hash map values container.
 */
public @com.simiacryptus.ref.lang.RefAware class ConcurrentHashMapValuesContainer extends ReferenceCountingBase {
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
  public static void testBasicOperations(
      com.simiacryptus.ref.wrappers.RefConcurrentHashMap<Integer, BasicType> valuesMap) {
    final com.simiacryptus.ref.wrappers.RefConcurrentHashMap<Integer, BasicType> copyMap = new com.simiacryptus.ref.wrappers.RefConcurrentHashMap<>();
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
    com.simiacryptus.ref.wrappers.RefSet<java.lang.Integer> temp5902 = valuesMap.keySet();
    if (!temp5902.contains(12)) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    temp5902.freeRef();
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
  public static void testStreamOperations(
      com.simiacryptus.ref.wrappers.RefConcurrentHashMap<Integer, BasicType> values) {
    com.simiacryptus.ref.wrappers.RefHashSet<com.simiacryptus.demo.refcount.BasicType> temp6309 = values.values();
    temp6309.stream().forEach(x -> {
      x.setValue(x.getValue() + 1);
      x.freeRef();
    });
    temp6309.freeRef();
    values.freeRef();
  }

  private static void test(
      com.simiacryptus.ref.wrappers.RefConsumer<com.simiacryptus.ref.wrappers.RefConcurrentHashMap<Integer, BasicType>> fn) {
    final com.simiacryptus.ref.wrappers.RefConcurrentHashMap<Integer, BasicType> hashMap = new com.simiacryptus.ref.wrappers.RefConcurrentHashMap<>();
    fn.accept(com.simiacryptus.ref.lang.RefUtil.addRef(hashMap));
    hashMap.freeRef();
    fn.freeRef();
  }

  private static void testEntries() {
    test(values -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp3860 = values
          .entrySet();
      temp3860.forEach(fooEntry -> {
        com.simiacryptus.demo.refcount.BasicType temp7998 = fooEntry.getValue();
        assert null != temp7998;
        temp7998.freeRef();
        assert null != fooEntry.getKey();
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry.setValue(new BasicType()));
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry);
      });
      temp3860.freeRef();
      values.freeRef();
    });
    test(values -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>> entryConsumer = fooEntry2 -> {
        if (1 == ((int) fooEntry2.getKey())) {
          com.simiacryptus.demo.refcount.BasicType temp4379 = fooEntry2.getValue();
          if (null == temp4379) {
            com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
            throw new AssertionError();
          }
          temp4379.freeRef();
        } else {
          com.simiacryptus.demo.refcount.BasicType temp4970 = fooEntry2.getValue();
          if (null == temp4970) {
            com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
            return;
          }
          temp4970.freeRef();
        }
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2.setValue(new BasicType()));
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
      };
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp1620 = values
          .entrySet();
      temp1620.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      temp1620.freeRef();
      entryConsumer.freeRef();
      values.freeRef();
    });
    test((com.simiacryptus.ref.wrappers.RefConcurrentHashMap<Integer, BasicType> values) -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final com.simiacryptus.ref.wrappers.RefConcurrentHashMap<Integer, BasicType> closureMap = new com.simiacryptus.ref.wrappers.RefConcurrentHashMap<>();
      final com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>> entryConsumer = (com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>>) com.simiacryptus.ref.lang.RefUtil
          .wrapInterface(
              (com.simiacryptus.ref.wrappers.RefConsumer<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>>) lambdaParameter -> {
                if (1 == ((int) lambdaParameter.getKey())) {
                  com.simiacryptus.demo.refcount.BasicType temp8492 = lambdaParameter.getValue();
                  if (null == temp8492) {
                    com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
                    throw new AssertionError();
                  }
                  temp8492.freeRef();
                } else {
                  com.simiacryptus.demo.refcount.BasicType temp7582 = lambdaParameter.getValue();
                  if (null == temp7582) {
                    com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
                    return;
                  }
                  temp7582.freeRef();
                }
                com.simiacryptus.ref.lang.RefUtil
                    .freeRef(closureMap.put(lambdaParameter.getKey(), lambdaParameter.getValue()));
                com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
              }, com.simiacryptus.ref.lang.RefUtil.addRef(closureMap));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp4249 = values
          .entrySet();
      temp4249.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      temp4249.freeRef();
      entryConsumer.freeRef();
      assert closureMap.size() == values.size();
      closureMap.freeRef();
      values.freeRef();
    });
    test((com.simiacryptus.ref.wrappers.RefConcurrentHashMap<Integer, BasicType> values) -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final com.simiacryptus.ref.wrappers.RefConcurrentHashMap<Integer, BasicType> closureMap = new com.simiacryptus.ref.wrappers.RefConcurrentHashMap<>();
      final com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>> entryConsumer = com.simiacryptus.ref.lang.RefUtil
          .wrapInterface(new com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>>() {
            @Override
            public void accept(Map.Entry<Integer, BasicType> anonymousParameter) {
              if (1 == ((int) anonymousParameter.getKey())) {
                com.simiacryptus.demo.refcount.BasicType temp5219 = anonymousParameter.getValue();
                if (null == temp5219) {
                  com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
                  throw new AssertionError();
                }
                temp5219.freeRef();
              } else {
                com.simiacryptus.demo.refcount.BasicType temp5068 = anonymousParameter.getValue();
                if (null == temp5068) {
                  com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
                  return;
                }
                temp5068.freeRef();
              }
              com.simiacryptus.ref.lang.RefUtil
                  .freeRef(closureMap.put(anonymousParameter.getKey(), anonymousParameter.getValue()));
              com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
            }

            public void _free() {
            }
          }, com.simiacryptus.ref.lang.RefUtil.addRef(closureMap));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp8058 = values
          .entrySet();
      temp8058.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      temp8058.freeRef();
      entryConsumer.freeRef();
      assert closureMap.size() == values.size();
      closureMap.freeRef();
      values.freeRef();
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
      testBasicOperations(new com.simiacryptus.ref.wrappers.RefConcurrentHashMap<>());
      testStreamOperations(new com.simiacryptus.ref.wrappers.RefConcurrentHashMap<>());
    }
  }

  public @Override ConcurrentHashMapValuesContainer addRef() {
    return (ConcurrentHashMapValuesContainer) super.addRef();
  }

  public static ConcurrentHashMapValuesContainer[] addRefs(ConcurrentHashMapValuesContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ConcurrentHashMapValuesContainer::addRef)
        .toArray((x) -> new ConcurrentHashMapValuesContainer[x]);
  }

  public static ConcurrentHashMapValuesContainer[][] addRefs(ConcurrentHashMapValuesContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ConcurrentHashMapValuesContainer::addRefs)
        .toArray((x) -> new ConcurrentHashMapValuesContainer[x][]);
  }
}
