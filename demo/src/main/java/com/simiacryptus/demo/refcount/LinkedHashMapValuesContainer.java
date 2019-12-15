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
 * The type Linked hash map values container.
 */
public @com.simiacryptus.ref.lang.RefAware class LinkedHashMapValuesContainer extends ReferenceCountingBase {
  /**
   * Test basic operations.
   *
   * @param valuesMap the values map
   */
  public static void testBasicOperations(com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> valuesMap) {
    final com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> copyMap = new com.simiacryptus.ref.wrappers.RefLinkedHashMap<>();
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
    com.simiacryptus.ref.wrappers.RefSet<java.lang.Integer> temp7820 = valuesMap.keySet();
    if (!temp7820.contains(12)) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    temp7820.freeRef();
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
  public static void testStreamOperations(com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> values) {
    com.simiacryptus.ref.wrappers.RefHashSet<com.simiacryptus.demo.refcount.BasicType> temp9742 = values.values();
    temp9742.stream().forEach(x -> {
      x.setValue(x.getValue() + 1);
      x.freeRef();
    });
    temp9742.freeRef();
    values.freeRef();
  }

  private static void test(
      com.simiacryptus.ref.wrappers.RefConsumer<com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType>> fn) {
    final com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> hashMap = new com.simiacryptus.ref.wrappers.RefLinkedHashMap<>();
    fn.accept(com.simiacryptus.ref.lang.RefUtil.addRef(hashMap));
    hashMap.freeRef();
    fn.freeRef();
  }

  private static void testEntries() {
    test(values -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp4613 = values
          .entrySet();
      temp4613.forEach(fooEntry -> {
        com.simiacryptus.demo.refcount.BasicType temp5670 = fooEntry.getValue();
        assert null != temp5670;
        temp5670.freeRef();
        assert null != fooEntry.getKey();
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry.setValue(new BasicType()));
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry);
      });
      temp4613.freeRef();
      values.freeRef();
    });
    test(values -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>> entryConsumer = fooEntry81 -> {
        if (1 == ((int) fooEntry81.getKey())) {
          com.simiacryptus.demo.refcount.BasicType temp3850 = fooEntry81.getValue();
          if (null == temp3850) {
            com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry81);
            throw new AssertionError();
          }
          temp3850.freeRef();
        } else {
          com.simiacryptus.demo.refcount.BasicType temp4372 = fooEntry81.getValue();
          if (null == temp4372) {
            com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry81);
            return;
          }
          temp4372.freeRef();
        }
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry81.setValue(new BasicType()));
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry81);
      };
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp5736 = values
          .entrySet();
      temp5736.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      temp5736.freeRef();
      entryConsumer.freeRef();
      values.freeRef();
    });
    test((com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> values) -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> closureMap = new com.simiacryptus.ref.wrappers.RefLinkedHashMap<>();
      final com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>> entryConsumer = (com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>>) com.simiacryptus.ref.lang.RefUtil
          .wrapInterface(
              (com.simiacryptus.ref.wrappers.RefConsumer<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>>) lambdaParameter -> {
                if (1 == ((int) lambdaParameter.getKey())) {
                  com.simiacryptus.demo.refcount.BasicType temp9134 = lambdaParameter.getValue();
                  if (null == temp9134) {
                    com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
                    throw new AssertionError();
                  }
                  temp9134.freeRef();
                } else {
                  com.simiacryptus.demo.refcount.BasicType temp5760 = lambdaParameter.getValue();
                  if (null == temp5760) {
                    com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
                    return;
                  }
                  temp5760.freeRef();
                }
                com.simiacryptus.ref.lang.RefUtil
                    .freeRef(closureMap.put(lambdaParameter.getKey(), lambdaParameter.getValue()));
                com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
              }, com.simiacryptus.ref.lang.RefUtil.addRef(closureMap));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp4095 = values
          .entrySet();
      temp4095.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      temp4095.freeRef();
      entryConsumer.freeRef();
      assert closureMap.size() == values.size();
      closureMap.freeRef();
      values.freeRef();
    });
    test((com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> values) -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> closureMap = new com.simiacryptus.ref.wrappers.RefLinkedHashMap<>();
      final com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>> entryConsumer = com.simiacryptus.ref.lang.RefUtil
          .wrapInterface(new com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>>() {
            @Override
            public void accept(Map.Entry<Integer, BasicType> anonymousParameter) {
              if (1 == ((int) anonymousParameter.getKey())) {
                com.simiacryptus.demo.refcount.BasicType temp2851 = anonymousParameter.getValue();
                if (null == temp2851) {
                  com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
                  throw new AssertionError();
                }
                temp2851.freeRef();
              } else {
                com.simiacryptus.demo.refcount.BasicType temp2105 = anonymousParameter.getValue();
                if (null == temp2105) {
                  com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
                  return;
                }
                temp2105.freeRef();
              }
              com.simiacryptus.ref.lang.RefUtil
                  .freeRef(closureMap.put(anonymousParameter.getKey(), anonymousParameter.getValue()));
              com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
            }

            public void _free() {
            }
          }, com.simiacryptus.ref.lang.RefUtil.addRef(closureMap));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp6516 = values
          .entrySet();
      temp6516.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      temp6516.freeRef();
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
      testBasicOperations(new com.simiacryptus.ref.wrappers.RefLinkedHashMap<>());
      testStreamOperations(new com.simiacryptus.ref.wrappers.RefLinkedHashMap<>());
    }
  }

  public @Override void _free() {
    super._free();
  }

  public @Override LinkedHashMapValuesContainer addRef() {
    return (LinkedHashMapValuesContainer) super.addRef();
  }

  public static LinkedHashMapValuesContainer[] addRefs(LinkedHashMapValuesContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(LinkedHashMapValuesContainer::addRef)
        .toArray((x) -> new LinkedHashMapValuesContainer[x]);
  }

  public static LinkedHashMapValuesContainer[][] addRefs(LinkedHashMapValuesContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(LinkedHashMapValuesContainer::addRefs)
        .toArray((x) -> new LinkedHashMapValuesContainer[x][]);
  }
}
