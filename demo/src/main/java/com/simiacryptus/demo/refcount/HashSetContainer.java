package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefConsumer;

public @com.simiacryptus.ref.lang.RefAware class HashSetContainer extends ReferenceCountingBase {
  public com.simiacryptus.ref.wrappers.RefHashSet<BasicType> values = new com.simiacryptus.ref.wrappers.RefHashSet<>();

  public HashSetContainer(BasicType... values) {
    for (BasicType value : values) {
      this.values.add(value.addRef());
    }
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(values);
  }

  private static void testStreamOperations() {
    testOperations(values -> {
      values.stream().forEach(x -> {
        x.value++;
        x.freeRef();
      });
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
      com.simiacryptus.demo.refcount.BasicType[] temp1214 = values.toArray(new BasicType[] {});
      if (values.size() != temp1214.length) {
        values.freeRef();
        throw new RuntimeException();
      }
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp1214);
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(values.toArray(new BasicType[] {}));
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
      com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> temp4833 = values.iterator();
      if (values.add(temp4833.next())) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      temp4833.freeRef();
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
    });
  }

  private static void testOperations(RefConsumer<com.simiacryptus.ref.wrappers.RefHashSet<BasicType>> setRefConsumer) {
    com.simiacryptus.ref.wrappers.RefHashSet<BasicType> values = new com.simiacryptus.ref.wrappers.RefHashSet<>();
    setRefConsumer.accept(values);
    setRefConsumer.freeRef();
  }

  public @Override void _free() {
    if (null != values)
      values.freeRef();
    super._free();
  }

  public void test() {
    com.simiacryptus.ref.wrappers.RefHashSet<com.simiacryptus.demo.refcount.BasicType> temp4225 = new com.simiacryptus.ref.wrappers.RefHashSet<>(
        this.values.addRef());
    if (this.values.size() != temp4225.size())
      throw new RuntimeException();
    temp4225.freeRef();
    for (int i = 0; i < TestOperations.count; i++) {
      testCollectionOperations();
      testElementOperations();
      testStreamOperations();
      testArrayOperations();
    }
  }

  private void testCollectionOperations() {
    testOperations(setValues -> {
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
    });
  }

  @Override
  public String toString() {
    return "HashSetContainer{" + "values=" + values + '}';
  }

  public @Override HashSetContainer addRef() {
    return (HashSetContainer) super.addRef();
  }

  public static HashSetContainer[] addRefs(HashSetContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(HashSetContainer::addRef)
        .toArray((x) -> new HashSetContainer[x]);
  }

  public static HashSetContainer[][] addRefs(HashSetContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(HashSetContainer::addRefs)
        .toArray((x) -> new HashSetContainer[x][]);
  }
}
