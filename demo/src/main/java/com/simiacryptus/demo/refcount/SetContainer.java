package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefConsumer;

public @com.simiacryptus.ref.lang.RefAware
class SetContainer extends ReferenceCountingBase {
  public com.simiacryptus.ref.wrappers.RefHashSet<BasicType> values = new com.simiacryptus.ref.wrappers.RefHashSet<>();

  public SetContainer(BasicType... values) {
    for (BasicType value : values) {
      this.values.add(value.addRef());
    }
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(values);
  }

  private static void testStreamOperations() {
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      values.stream().forEach(x -> {
        x.value++;
        x.freeRef();
      });
      values.stream();
      values.freeRef();
    }));
  }

  private static void testArrayOperations() {
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
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
      com.simiacryptus.demo.refcount.BasicType[] temp5905 = values.toArray(new BasicType[]{});
      if (values.size() != temp5905.length) {
        values.freeRef();
        throw new RuntimeException();
      }
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp5905);
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(values.toArray(new BasicType[]{}));
      values.freeRef();
    }));
  }

  private static void testElementOperations() {
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
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
      java.util.Iterator<com.simiacryptus.demo.refcount.BasicType> temp6840 = values.iterator();
      if (values.add(temp6840.next())) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp6840);
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.iterator());
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
    }));
  }

  private static void testOperations(RefConsumer<com.simiacryptus.ref.wrappers.RefHashSet<BasicType>> setRefConsumer) {
    com.simiacryptus.ref.wrappers.RefHashSet<BasicType> values = new com.simiacryptus.ref.wrappers.RefHashSet<>();
    setRefConsumer.accept(values);
    setRefConsumer.freeRef();
  }

  public static SetContainer[] addRefs(SetContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(SetContainer::addRef)
        .toArray((x) -> new SetContainer[x]);
  }

  public static SetContainer[][] addRefs(SetContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(SetContainer::addRefs)
        .toArray((x) -> new SetContainer[x][]);
  }

  public @Override
  void _free() {
    if (null != values)
      values.freeRef();
    super._free();
  }

  public @Override
  SetContainer addRef() {
    return (SetContainer) super.addRef();
  }

  public void test() {
    System.out.println(String.format("Increment %s", this));
    com.simiacryptus.ref.wrappers.RefHashSet<com.simiacryptus.demo.refcount.BasicType> temp4262 = new com.simiacryptus.ref.wrappers.RefHashSet<>(
        this.values.addRef());
    if (this.values.size() != temp4262.size())
      throw new RuntimeException();
    temp4262.freeRef();
    for (int i = 0; i < TestOperations.count; i++) {
      testCollectionOperations();
      testElementOperations();
      testStreamOperations();
      testArrayOperations();
    }
  }

  private void testCollectionOperations() {
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(setValues -> {
      setValues.add(new BasicType());
      final BasicType basicType = new BasicType();
      final com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> list = com.simiacryptus.ref.wrappers.RefArrays
          .asList(basicType);
      if (!setValues.addAll(list.addRef())) {
        setValues.freeRef();
        list.freeRef();
        throw new RuntimeException();
      }
      if (!setValues.containsAll(list.addRef())) {
        setValues.freeRef();
        list.freeRef();
        throw new RuntimeException();
      }
      if (!setValues.retainAll(list.addRef())) {
        setValues.freeRef();
        list.freeRef();
        throw new RuntimeException();
      }
      setValues.removeAll(list);
      if (!setValues.isEmpty())
        throw new RuntimeException();
      setValues.freeRef();
    }));
  }

  @Override
  public String toString() {
    return "SetContainer{" + "values=" + values + '}';
  }
}
