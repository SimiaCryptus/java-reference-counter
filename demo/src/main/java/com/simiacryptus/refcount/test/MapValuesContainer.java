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

  public static void testBasicOperations(java.util.HashMap<Integer, BasicType> values) {
    final java.util.HashMap<Integer, BasicType> copyMap = new java.util.HashMap<>();
    copyMap.putAll(values);
    values.clear();
    assert values.isEmpty();
    values.put(12, new BasicType());
    values.put(12, new BasicType());
    values.put(32, new BasicType());
    if (2 != values.size()) {
      throw new AssertionError();
    }
    if (!values.containsKey(12)) {
      throw new AssertionError();
    }
    if (!values.keySet().contains(12)) {
      throw new AssertionError();
    }
    values.remove(12);
    if (values.containsKey(12)) {
      throw new AssertionError();
    }
    if (!values.containsValue(values.get(32)))
      throw new AssertionError();
  }

  public static void testStreamOperations(java.util.HashMap<Integer, BasicType> values) {
    values.values().stream().forEach(x -> {
      x.value++;
    });
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

  public @Override void _free() {
    super._free();
  }
}
