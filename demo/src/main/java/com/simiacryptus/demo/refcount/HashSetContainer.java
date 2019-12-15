package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;

public class HashSetContainer extends ReferenceCountingBase {
  private static void testStreamOperations() {
    testOperations(values -> {
      values.stream().forEach(x -> {
        x.setValue(x.getValue() + 1);
      });
    });
  }

  private static void testArrayOperations() {
    testOperations(values -> {
      values.add(new BasicType());
      if (0 == values.size()) {
        throw new RuntimeException();
      }
      if (false) {
        if (values.size() != values.toArray().length) {
          throw new RuntimeException();
        }
      }
      if (values.size() != values.toArray(new BasicType[]{}).length) {
        throw new RuntimeException();
      }
      values.toArray(new BasicType[]{});
    });
  }

  private static void testElementOperations() {
    testOperations(values -> {
      if (!values.isEmpty()) {
        throw new RuntimeException();
      }
      final BasicType basicType1 = new BasicType();
      if (!values.add(basicType1)) {
        throw new RuntimeException();
      }
      if (!values.contains(basicType1)) {
        throw new RuntimeException();
      }
      if (values.add(values.iterator().next())) {
        throw new RuntimeException();
      }
      values.iterator();
      if (!values.contains(basicType1)) {
        throw new RuntimeException();
      }
      if (!values.remove(basicType1)) {
        throw new RuntimeException();
      }
      if (values.remove(basicType1)) {
        throw new RuntimeException();
      }
      if (!values.add(basicType1)) {
        throw new RuntimeException();
      }
      values.clear();
      if (!values.isEmpty()) {
        throw new RuntimeException();
      }
    });
  }

  private static void testOperations(java.util.function.Consumer<java.util.HashSet<BasicType>> setRefConsumer) {
    java.util.HashSet<BasicType> values = new java.util.HashSet<>();
    setRefConsumer.accept(values);
  }

  public static void test() {
    for (int i = 0; i < TestOperations.count; i++) {
      testCollectionOperations();
      testElementOperations();
      testStreamOperations();
      testArrayOperations();
    }
  }

  private static void testCollectionOperations() {
    testOperations(setValues -> {
      setValues.add(new BasicType());
      final BasicType basicType = new BasicType();
      final java.util.List<com.simiacryptus.demo.refcount.BasicType> list = java.util.Arrays.asList(basicType);
      if (!setValues.addAll(list)) {
        throw new RuntimeException();
      }
      if (!setValues.containsAll(list)) {
        throw new RuntimeException();
      }
      if (!setValues.retainAll(list)) {
        throw new RuntimeException();
      }
      setValues.removeAll(list);
      if (!setValues.isEmpty()) {
        throw new RuntimeException();
      }
    });
  }

  public @Override
  void _free() {
    super._free();
  }
}
