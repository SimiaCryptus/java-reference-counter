package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;

public class SimpleContainer extends ReferenceCountingBase {
  public BasicType value;

  public SimpleContainer() {
    value = new BasicType();
  }

  public SimpleContainer(BasicType value) {
    if (null != this.value)
      this.value.freeRef();
    this.value = value.addRef();
    value.freeRef();
  }

  public void use() {
    System.out.println(String.format("Increment %s", this));
    this.value.value++;
  }

  @Override
  public String toString() {
    return "SimpleContainer{" + "values=" + value + '}';
  }

  public @Override void _free() {
    if (null != value)
      value.freeRef();
    super._free();
  }

  public @Override SimpleContainer addRef() {
    return (SimpleContainer) super.addRef();
  }

  public static SimpleContainer[] addRefs(SimpleContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(SimpleContainer::addRef)
        .toArray((x) -> new SimpleContainer[x]);
  }
}
