package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefConsumer;

public @com.simiacryptus.ref.lang.RefAware class TreeSetContainer extends ReferenceCountingBase {
  public com.simiacryptus.ref.wrappers.RefTreeSet<BasicType> values = new com.simiacryptus.ref.wrappers.RefTreeSet<>();

  public TreeSetContainer(BasicType... values) {
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
      com.simiacryptus.demo.refcount.BasicType[] temp4687 = values.toArray(new BasicType[] {});
      if (values.size() != temp4687.length) {
        values.freeRef();
        throw new RuntimeException();
      }
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp4687);
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
      com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> temp5014 = values.iterator();
      if (values.add(temp5014.next())) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      temp5014.freeRef();
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

  private static void testOperations(RefConsumer<com.simiacryptus.ref.wrappers.RefTreeSet<BasicType>> setRefConsumer) {
    com.simiacryptus.ref.wrappers.RefTreeSet<BasicType> values = new com.simiacryptus.ref.wrappers.RefTreeSet<>();
    setRefConsumer.accept(values);
    setRefConsumer.freeRef();
  }

  public @Override void _free() {
    if (null != values)
      values.freeRef();
    super._free();
  }

  public void test() {
    com.simiacryptus.ref.wrappers.RefTreeSet<com.simiacryptus.demo.refcount.BasicType> temp5708 = new com.simiacryptus.ref.wrappers.RefTreeSet<>(
        this.values.addRef());
    if (this.values.size() != temp5708.size())
      throw new RuntimeException();
    temp5708.freeRef();
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
      final com.simiacryptus.ref.wrappers.RefList<BasicType> list = com.simiacryptus.ref.wrappers.RefArrays
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

  public @Override TreeSetContainer addRef() {
    return (TreeSetContainer) super.addRef();
  }

  public static TreeSetContainer[] addRefs(TreeSetContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(TreeSetContainer::addRef)
        .toArray((x) -> new TreeSetContainer[x]);
  }

  public static TreeSetContainer[][] addRefs(TreeSetContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(TreeSetContainer::addRefs)
        .toArray((x) -> new TreeSetContainer[x][]);
  }
}
