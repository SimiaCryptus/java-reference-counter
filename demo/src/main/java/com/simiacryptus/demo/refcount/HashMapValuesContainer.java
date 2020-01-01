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
    if (null != copyMap)
      copyMap.freeRef();
    valuesMap.clear();
    assert valuesMap.isEmpty();
    com.simiacryptus.ref.lang.RefUtil.freeRef(valuesMap.put(12, new BasicType()));
    com.simiacryptus.ref.lang.RefUtil.freeRef(valuesMap.put(12, new BasicType()));
    com.simiacryptus.ref.lang.RefUtil.freeRef(valuesMap.put(32, new BasicType()));
    if (2 != valuesMap.size()) {
      if (null != valuesMap)
        valuesMap.freeRef();
      throw new AssertionError();
    }
    if (!valuesMap.containsKey(12)) {
      if (null != valuesMap)
        valuesMap.freeRef();
      throw new AssertionError();
    }
    com.simiacryptus.ref.wrappers.RefSet<java.lang.Integer> temp_09_0001 = valuesMap.keySet();
    if (!temp_09_0001.contains(12)) {
      if (null != valuesMap)
        valuesMap.freeRef();
      throw new AssertionError();
    }
    if (null != temp_09_0001)
      temp_09_0001.freeRef();
    com.simiacryptus.ref.lang.RefUtil.freeRef(valuesMap.remove(12));
    if (valuesMap.containsKey(12)) {
      if (null != valuesMap)
        valuesMap.freeRef();
      throw new AssertionError();
    }
    if (!valuesMap.containsValue(valuesMap.get(32))) {
      if (null != valuesMap)
        valuesMap.freeRef();
      throw new AssertionError();
    }
    if (null != valuesMap)
      valuesMap.freeRef();
  }

  /**
   * Test stream operations.
   *
   * @param values the values
   */
  public static void testStreamOperations(com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> values) {
    com.simiacryptus.ref.wrappers.RefHashSet<com.simiacryptus.demo.refcount.BasicType> temp_09_0002 = values.values();
    temp_09_0002.stream().forEach(x -> {
      x.setValue(x.getValue() + 1);
      if (null != x)
        x.freeRef();
    });
    if (null != temp_09_0002)
      temp_09_0002.freeRef();
    if (null != values)
      values.freeRef();
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

  private static void test(
      com.simiacryptus.ref.wrappers.RefConsumer<com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType>> fn) {
    final com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> hashMap = new com.simiacryptus.ref.wrappers.RefHashMap<>();
    fn.accept(com.simiacryptus.ref.lang.RefUtil.addRef(hashMap));
    if (null != fn)
      fn.freeRef();
    if (null != hashMap)
      hashMap.freeRef();
  }

  private static void testEntries() {
    test(values -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp_09_0003 = values
          .entrySet();
      temp_09_0003.forEach(fooEntry -> {
        com.simiacryptus.demo.refcount.BasicType temp_09_0004 = fooEntry.getValue();
        assert null != temp_09_0004;
        if (null != temp_09_0004)
          temp_09_0004.freeRef();
        assert null != fooEntry.getKey();
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry.setValue(new BasicType()));
        if (null != fooEntry)
          com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry);
      });
      if (null != temp_09_0003)
        temp_09_0003.freeRef();
      if (null != values)
        values.freeRef();
    });
    test(values -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final com.simiacryptus.ref.wrappers.RefConsumer<Map.Entry<Integer, BasicType>> entryConsumer = com.simiacryptus.ref.lang.RefUtil
          .addRef(fooEntry2 -> {
            if (1 == ((int) fooEntry2.getKey())) {
              com.simiacryptus.demo.refcount.BasicType temp_09_0005 = fooEntry2.getValue();
              if (null == temp_09_0005) {
                if (null != fooEntry2)
                  com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
                throw new AssertionError();
              }
              if (null != temp_09_0005)
                temp_09_0005.freeRef();
            } else {
              com.simiacryptus.demo.refcount.BasicType temp_09_0006 = fooEntry2.getValue();
              if (null == temp_09_0006) {
                if (null != fooEntry2)
                  com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
                return;
              }
              if (null != temp_09_0006)
                temp_09_0006.freeRef();
            }
            com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2.setValue(new BasicType()));
            if (null != fooEntry2)
              com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
          });
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp_09_0007 = values
          .entrySet();
      temp_09_0007.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      if (null != temp_09_0007)
        temp_09_0007.freeRef();
      if (null != entryConsumer)
        entryConsumer.freeRef();
      if (null != values)
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
                  com.simiacryptus.demo.refcount.BasicType temp_09_0008 = lambdaParameter.getValue();
                  if (null == temp_09_0008) {
                    if (null != lambdaParameter)
                      com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
                    throw new AssertionError();
                  }
                  if (null != temp_09_0008)
                    temp_09_0008.freeRef();
                } else {
                  com.simiacryptus.demo.refcount.BasicType temp_09_0009 = lambdaParameter.getValue();
                  if (null == temp_09_0009) {
                    if (null != lambdaParameter)
                      com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
                    return;
                  }
                  if (null != temp_09_0009)
                    temp_09_0009.freeRef();
                }
                com.simiacryptus.ref.lang.RefUtil
                    .freeRef(closureMap.put(lambdaParameter.getKey(), lambdaParameter.getValue()));
                if (null != lambdaParameter)
                  com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
              }, com.simiacryptus.ref.lang.RefUtil.addRef(closureMap));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp_09_0010 = values
          .entrySet();
      temp_09_0010.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      if (null != temp_09_0010)
        temp_09_0010.freeRef();
      if (null != entryConsumer)
        entryConsumer.freeRef();
      assert closureMap.size() == values.size();
      if (null != values)
        values.freeRef();
      if (null != closureMap)
        closureMap.freeRef();
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
                com.simiacryptus.demo.refcount.BasicType temp_09_0011 = anonymousParameter.getValue();
                if (null == temp_09_0011) {
                  if (null != anonymousParameter)
                    com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
                  throw new AssertionError();
                }
                if (null != temp_09_0011)
                  temp_09_0011.freeRef();
              } else {
                com.simiacryptus.demo.refcount.BasicType temp_09_0012 = anonymousParameter.getValue();
                if (null == temp_09_0012) {
                  if (null != anonymousParameter)
                    com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
                  return;
                }
                if (null != temp_09_0012)
                  temp_09_0012.freeRef();
              }
              com.simiacryptus.ref.lang.RefUtil
                  .freeRef(closureMap.put(anonymousParameter.getKey(), anonymousParameter.getValue()));
              if (null != anonymousParameter)
                com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
            }

            public void _free() {
            }

          }, com.simiacryptus.ref.lang.RefUtil.addRef(closureMap));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp_09_0013 = values
          .entrySet();
      temp_09_0013.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      if (null != temp_09_0013)
        temp_09_0013.freeRef();
      if (null != entryConsumer)
        entryConsumer.freeRef();
      assert closureMap.size() == values.size();
      if (null != values)
        values.freeRef();
      if (null != closureMap)
        closureMap.freeRef();
    });
  }

  public @Override void _free() {
    super._free();
  }

  public @Override @SuppressWarnings("unused") HashMapValuesContainer addRef() {
    return (HashMapValuesContainer) super.addRef();
  }

  public static @SuppressWarnings("unused") HashMapValuesContainer[] addRefs(HashMapValuesContainer[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(HashMapValuesContainer::addRef)
        .toArray((x) -> new HashMapValuesContainer[x]);
  }

  public static @SuppressWarnings("unused") HashMapValuesContainer[][] addRefs(HashMapValuesContainer[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(HashMapValuesContainer::addRefs)
        .toArray((x) -> new HashMapValuesContainer[x][]);
  }
}
