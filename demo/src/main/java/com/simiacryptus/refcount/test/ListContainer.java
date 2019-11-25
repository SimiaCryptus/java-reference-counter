package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import com.simiacryptus.lang.ref.wrappers.RefArrayList;

public class ListContainer extends ReferenceCountingBase {
  public com.simiacryptus.lang.ref.wrappers.RefArrayList<BasicType> values = new com.simiacryptus.lang.ref.wrappers.RefArrayList<>();

  public ListContainer(BasicType... values) {
    for (BasicType value : values) {
      this.values.add(value.addRef());
    }
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(values);
  }

  public void use() {
    System.out.println(String.format("Increment %s", this));
    com.simiacryptus.lang.ref.wrappers.RefSet<com.simiacryptus.refcount.test.BasicType> temp5446 = new com.simiacryptus.lang.ref.wrappers.RefSet<>(
        this.values.addRef());
    if (this.values.size() != temp5446.size())
      throw new RuntimeException();
    temp5446.freeRef();
    for (int i = 0; i < TestOperations.count; i++) {
      if (!this.values.add(this.values.iterator().next()))
        throw new RuntimeException();
      final com.simiacryptus.lang.ref.wrappers.RefList<BasicType> list = com.simiacryptus.lang.ref.wrappers.RefArrays
          .asList(new BasicType());
      this.values.addAll(list.addRef());
      if (!this.values.containsAll(list.addRef()))
        throw new RuntimeException();
      if (!this.values.retainAll(list.addRef()))
        throw new RuntimeException();
      if (!this.values.addAll(list.addRef()))
        throw new RuntimeException();
      this.values.removeAll(list.addRef());
      list.freeRef();
      if (false) {
        if (this.values.size() != this.values.toArray().length)
          throw new RuntimeException();
      }
      com.simiacryptus.refcount.test.BasicType[] temp1121 = this.values.toArray(new BasicType[] {});
      if (this.values.size() != temp1121.length)
        throw new RuntimeException();
      com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp1121);
      this.values.stream().forEach(x -> {
        x.value++;
        x.freeRef();
      });
    }
  }

  @Override
  public String toString() {
    return "ListContainer{" + "values=" + values + '}';
  }

  public @Override void _free() {
    if (null != values)
      values.freeRef();
    super._free();
  }

  public @Override ListContainer addRef() {
    return (ListContainer) super.addRef();
  }

  public static ListContainer[] addRefs(ListContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ListContainer::addRef)
        .toArray((x) -> new ListContainer[x]);
  }
}
