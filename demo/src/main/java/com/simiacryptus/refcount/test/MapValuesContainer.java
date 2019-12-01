package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import java.util.Map;
import java.util.function.Consumer;

public class MapValuesContainer extends ReferenceCountingBase {
  public java.util.HashMap<Integer, BasicType> values = new java.util.HashMap<>();

  public MapValuesContainer(BasicType... basicTypes) {
    for (int i = 0; i < basicTypes.length; i++) {
      values.put(i, basicTypes[i]);
    }
  }

  public static void testBasicOperations(java.util.HashMap<Integer, BasicType> valuesMap) {
    final java.util.HashMap<Integer, BasicType> copyMap = new java.util.HashMap<>();
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

  public static void testStreamOperations(java.util.HashMap<Integer, BasicType> values) {
    values.values().stream().forEach(x -> {
      x.value++;
    });
  }

  public @Override void _free() {
    super._free();
  }

  private void test(Consumer<java.util.HashMap<Integer, BasicType>> fn) {
    final java.util.HashMap<Integer, BasicType> hashMap = new java.util.HashMap<>();
    fn.accept(hashMap);
  }

  private void testEntries() {
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
    test((java.util.HashMap<Integer, BasicType> values) -> {
      values.put(1, new BasicType());
      values.put(2, new BasicType());
      final java.util.HashMap<Integer, BasicType> targetMap = new java.util.HashMap<>();
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = entry -> {
        if (1 == ((int) entry.getKey())) {
          if (null == entry.getValue()) {
            throw new AssertionError();
          }
        } else {
          if (null == entry.getValue()) {
            return;
          }
        }
        targetMap.put(entry.getKey(), entry.getValue());
      };
      values.entrySet().forEach(entryConsumer);
      assert targetMap.size() == values.size();
    });
    test((java.util.HashMap<Integer, BasicType> values) -> {
      values.put(1, new BasicType());
      values.put(2, new BasicType());
      final java.util.HashMap<Integer, BasicType> targetMap = new java.util.HashMap<>();
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = new Consumer<Map.Entry<Integer, BasicType>>() {
        @Override
        public void accept(Map.Entry<Integer, BasicType> entry) {
          if (1 == ((int) entry.getKey())) {
            if (null == entry.getValue()) {
              throw new AssertionError();
            }
          } else {
            if (null == entry.getValue()) {
              return;
            }
          }
          targetMap.put(entry.getKey(), entry.getValue());
        }
      };
      values.entrySet().forEach(entryConsumer);
      assert targetMap.size() == values.size();
    });
  }

  @Override
  public String toString() {
    return "MapValuesContainer{" + "values=" + values + '}';
  }

  public void use() {
    System.out.println(String.format("Increment %s", this));
    for (int i = 0; i < TestOperations.count; i++) {
      testEntries();
      testBasicOperations(new java.util.HashMap<>(this.values));
      testStreamOperations(new java.util.HashMap<>(this.values));
    }
  }
}
