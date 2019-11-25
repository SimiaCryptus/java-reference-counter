package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;

public class SetContainer extends ReferenceCountingBase {
  public com.simiacryptus.lang.ref.wrappers.RefSet<BasicType> values = new com.simiacryptus.lang.ref.wrappers.RefSet<>();

  public SetContainer(BasicType... values) {
    for (BasicType value : values) {
      this.values.add(value.addRef());
    }
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(values);
  }

  public void test() {
    System.out.println(String.format("Increment %s", this));
    com.simiacryptus.lang.ref.wrappers.RefSet<com.simiacryptus.refcount.test.BasicType> temp8708 = new com.simiacryptus.lang.ref.wrappers.RefSet<>(
        this.values.addRef());
    if (this.values.size() != temp8708.size())
      throw new RuntimeException();
    temp8708.freeRef();
    for (int i = 0; i < TestOperations.count; i++) {
      testCollectionOperations();
      testElementOperations();
      this.values.stream().forEach(x -> {
        x.value++;
        x.freeRef();
      });
    }
  }

  private void testArrayOperations() {
    if (0 == this.values.size())
      throw new RuntimeException();
    if (false) {
      if (this.values.size() != this.values.toArray().length)
        throw new RuntimeException();
    }
    com.simiacryptus.refcount.test.BasicType[] temp4460 = this.values.toArray(new BasicType[] {});
    if (this.values.size() != temp4460.length)
      throw new RuntimeException();
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp4460);
  }

  private void testElementOperations() {
    if (!this.values.isEmpty())
      throw new RuntimeException();
    final BasicType basicType1 = new BasicType();
    if (!this.values.add(basicType1.addRef()))
      throw new RuntimeException();
    if (!this.values.contains(basicType1.addRef()))
      throw new RuntimeException();
    basicType1.freeRef();
    if (this.values.add(this.values.iterator().next()))
      throw new RuntimeException();
    if (false) {
      if (!this.values.remove(basicType1.addRef()))
        throw new RuntimeException();
    } else {
      this.values.clear();
    }
    if (!this.values.isEmpty())
      throw new RuntimeException();
  }

  private void testCollectionOperations() {
    final BasicType basicType = new BasicType();
    final com.simiacryptus.lang.ref.wrappers.RefList<BasicType> list = com.simiacryptus.lang.ref.wrappers.RefArrays
        .asList(basicType.addRef());
    basicType.freeRef();
    if (!this.values.addAll(list.addRef()))
      throw new RuntimeException();
    if (!this.values.containsAll(list.addRef()))
      throw new RuntimeException();
    if (!this.values.retainAll(list.addRef()))
      throw new RuntimeException();
    testArrayOperations();
    this.values.removeAll(list.addRef());
    list.freeRef();
    if (!this.values.isEmpty())
      throw new RuntimeException();
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
