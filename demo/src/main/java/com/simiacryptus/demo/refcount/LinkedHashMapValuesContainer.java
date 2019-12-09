package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefConsumer;
import java.util.Map;
import java.util.function.Consumer;

public class LinkedHashMapValuesContainer extends ReferenceCountingBase {
  public static void testBasicOperations(java.util.LinkedHashMap<Integer, BasicType> valuesMap) {
    final java.util.LinkedHashMap<Integer, BasicType> copyMap = new java.util.LinkedHashMap<>();
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
    if (!valuesMap.containsValue(valuesMap.get(32)))
      throw new AssertionError();
  }

  public static void testStreamOperations(java.util.LinkedHashMap<Integer, BasicType> values) {
    values.values().stream().forEach(x -> {
      x.setValue(x.getValue() + 1);
    });
  }

  private static void test(RefConsumer<java.util.LinkedHashMap<Integer, BasicType>> fn) {
    final java.util.LinkedHashMap<Integer, BasicType> hashMap = new java.util.LinkedHashMap<>();
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
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = fooEntry2 -> {
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
    test((java.util.LinkedHashMap<Integer, BasicType> values) -> {
      values.put(1, new BasicType());
      values.put(2, new BasicType());
      final java.util.LinkedHashMap<Integer, BasicType> closureMap = new java.util.LinkedHashMap<>();
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = (Consumer<Map.Entry<Integer, BasicType>>) lambdaParameter -> {
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
    test((java.util.LinkedHashMap<Integer, BasicType> values) -> {
      values.put(1, new BasicType());
      values.put(2, new BasicType());
      final java.util.LinkedHashMap<Integer, BasicType> closureMap = new java.util.LinkedHashMap<>();
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = new Consumer<Map.Entry<Integer, BasicType>>() {
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
      };
      values.entrySet().forEach(entryConsumer);
      assert closureMap.size() == values.size();
    });
  }

  public static void test() {
    for (int i = 0; i < TestOperations.count; i++) {
      testEntries();
      testBasicOperations(new java.util.LinkedHashMap<>());
      testStreamOperations(new java.util.LinkedHashMap<>());
    }
  }

  public @Override void _free() {
    super._free();
  }
}
