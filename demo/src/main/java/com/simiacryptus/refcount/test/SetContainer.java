package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import java.util.function.Consumer;

public @com.simiacryptus.lang.ref.RefAware class SetContainer extends ReferenceCountingBase {
  public com.simiacryptus.lang.ref.wrappers.RefHashSet<BasicType> values = new com.simiacryptus.lang.ref.wrappers.RefHashSet<>();

  public SetContainer(BasicType... values) {
    for (BasicType value : values) {
      this.values.add(value.addRef());
    }
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(values);
  }

  private static void testStreamOperations() {
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp4728 = values.stream();
      temp4728.forEach(x -> {
        x.value++;
        x.freeRef();
      });
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp4728);
      values.freeRef();
    });
  }

  private static void testArrayOperations() {
    testOperations(values -> {
      values.add(new BasicType());
      if (0 == values.size()) {
        values.freeRef();
        throw new RuntimeException();
      }
      if (false) {
        if (values.size() != values.toArray().length) {
          values.freeRef();
          throw new RuntimeException();
        }
      }
      com.simiacryptus.refcount.test.BasicType[] temp2511 = values.toArray(new BasicType[] {});
      if (values.size() != temp2511.length)
        throw new RuntimeException();
      com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp2511);
      values.freeRef();
    });
  }

  private static void testElementOperations() {
    testOperations(values -> {
      if (!values.isEmpty()) {
        values.freeRef();
        throw new RuntimeException();
      }
      final BasicType basicType1 = new BasicType();
      if (!values.add(basicType1.addRef())) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      if (!values.contains(basicType1.addRef())) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      java.util.Iterator<com.simiacryptus.refcount.test.BasicType> temp2021 = values.iterator();
      if (values.add(temp2021.next())) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp2021);
      if (!values.contains(basicType1.addRef())) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      if (!values.remove(basicType1.addRef())) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      if (values.remove(basicType1.addRef())) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      if (!values.add(basicType1)) {
        values.freeRef();
        throw new RuntimeException();
      }
      values.clear();
      if (!values.isEmpty())
        throw new RuntimeException();
      values.freeRef();
    });
  }

  private static void testOperations(Consumer<com.simiacryptus.lang.ref.wrappers.RefHashSet<BasicType>> fn) {
    com.simiacryptus.lang.ref.wrappers.RefHashSet<BasicType> values = new com.simiacryptus.lang.ref.wrappers.RefHashSet<>();
    fn.accept(values);
    com.simiacryptus.lang.ref.RefUtil.freeRef(fn);
  }

  public @Override void _free() {
    if (null != values)
      values.freeRef();
    super._free();
  }

  public void test() {
    System.out.println(String.format("Increment %s", this));
    com.simiacryptus.lang.ref.wrappers.RefHashSet<com.simiacryptus.refcount.test.BasicType> temp6711 = new com.simiacryptus.lang.ref.wrappers.RefHashSet<>(
        this.values.addRef());
    if (this.values.size() != temp6711.size())
      throw new RuntimeException();
    temp6711.freeRef();
    for (int i = 0; i < TestOperations.count; i++) {
      testCollectionOperations();
      testElementOperations();
      testStreamOperations();
      testArrayOperations();
    }
  }

  private void testCollectionOperations() {
    testOperations(values -> {
      values.add(new BasicType());
      final BasicType basicType = new BasicType();
      final com.simiacryptus.lang.ref.wrappers.RefList<BasicType> list = com.simiacryptus.lang.ref.wrappers.RefArrays
          .asList(basicType);
      if (!values.addAll(com.simiacryptus.lang.ref.RefUtil.addRef(list))) {
        values.freeRef();
        list.freeRef();
        throw new RuntimeException();
      }
      if (!values.containsAll(com.simiacryptus.lang.ref.RefUtil.addRef(list))) {
        values.freeRef();
        list.freeRef();
        throw new RuntimeException();
      }
      if (!values.retainAll(com.simiacryptus.lang.ref.RefUtil.addRef(list))) {
        values.freeRef();
        list.freeRef();
        throw new RuntimeException();
      }
      values.removeAll(com.simiacryptus.lang.ref.RefUtil.addRef(list));
      list.freeRef();
      if (!values.isEmpty())
        throw new RuntimeException();
      values.freeRef();
    });
  }

  @Override
  public String toString() {
    return "SetContainer{" + "values=" + values + '}';
  }

  public @Override SetContainer addRef() {
    return (SetContainer) super.addRef();
  }

  public static SetContainer[] addRefs(SetContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(SetContainer::addRef)
        .toArray((x) -> new SetContainer[x]);
  }
}
