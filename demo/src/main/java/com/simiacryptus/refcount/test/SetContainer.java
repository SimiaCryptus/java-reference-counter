package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import com.simiacryptus.lang.ref.wrappers.RefHashSet;
import java.util.function.Consumer;

public class SetContainer extends ReferenceCountingBase {
  public com.simiacryptus.lang.ref.wrappers.RefHashSet<BasicType> values = new com.simiacryptus.lang.ref.wrappers.RefHashSet<>();

  public SetContainer(BasicType... values) {
    for (BasicType value : values) {
      this.values.add(value.addRef());
    }
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(values);
  }

  public void test() {
    System.out.println(String.format("Increment %s", this));
    com.simiacryptus.lang.ref.wrappers.RefHashSet<com.simiacryptus.refcount.test.BasicType> temp2634 = new com.simiacryptus.lang.ref.wrappers.RefHashSet<>(
        this.values.addRef());
    if (this.values.size() != temp2634.size())
      throw new RuntimeException();
    temp2634.freeRef();
    for (int i = 0; i < TestOperations.count; i++) {
      testCollectionOperations();
      testElementOperations();
      testStreamOperations();
      testArrayOperations();
    }
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
      if (0 == values.size())
        throw new RuntimeException();
      if (false) {
        if (values.size() != values.toArray().length)
          throw new RuntimeException();
      }
      com.simiacryptus.refcount.test.BasicType[] temp5651 = values.toArray(new BasicType[] {});
      if (values.size() != temp5651.length)
        throw new RuntimeException();
      com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp5651);
      values.freeRef();
    });
  }

  private static void testElementOperations() {
    testOperations(values -> {
      if (!values.isEmpty())
        throw new RuntimeException();
      final BasicType basicType1 = new BasicType();
      if (!values.add(basicType1.addRef()))
        throw new RuntimeException();
      if (!values.contains(basicType1.addRef()))
        throw new RuntimeException();
      if (values.add(values.iterator().next()))
        throw new RuntimeException();
      if (!values.contains(basicType1.addRef()))
        throw new RuntimeException();
      if (!values.remove(basicType1.addRef()))
        throw new RuntimeException();
      if (values.remove(basicType1.addRef()))
        throw new RuntimeException();
      if (!values.add(basicType1.addRef()))
        throw new RuntimeException();
      basicType1.freeRef();
      values.clear();
      if (!values.isEmpty())
        throw new RuntimeException();
      values.freeRef();
    });
  }

  private void testCollectionOperations() {
    testOperations(values -> {
      values.add(new BasicType());
      final BasicType basicType = new BasicType();
      final com.simiacryptus.lang.ref.wrappers.RefList<BasicType> list = com.simiacryptus.lang.ref.wrappers.RefArrays
          .asList(basicType.addRef());
      basicType.freeRef();
      if (!values.addAll(list.addRef()))
        throw new RuntimeException();
      if (!values.containsAll(list.addRef()))
        throw new RuntimeException();
      if (!values.retainAll(list.addRef()))
        throw new RuntimeException();
      values.removeAll(list.addRef());
      list.freeRef();
      if (!values.isEmpty())
        throw new RuntimeException();
      values.freeRef();
    });
  }

  private static void testOperations(Consumer<com.simiacryptus.lang.ref.wrappers.RefHashSet<BasicType>> fn) {
    com.simiacryptus.lang.ref.wrappers.RefHashSet<BasicType> values = new com.simiacryptus.lang.ref.wrappers.RefHashSet<>();
    fn.accept(values.addRef());
    values.freeRef();
  }

  @Override
  public String toString() {
    return "SetContainer{" + "values=" + values + '}';
  }

  public @Override void _free() {
    if (null != values)
      values.freeRef();
    super._free();
  }

  public @Override SetContainer addRef() {
    return (SetContainer) super.addRef();
  }

  public static SetContainer[] addRefs(SetContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(SetContainer::addRef)
        .toArray((x) -> new SetContainer[x]);
  }
}
