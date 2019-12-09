package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;

public @com.simiacryptus.ref.lang.RefAware class SimpleContainer extends ReferenceCountingBase {
  public BasicType value;

  public SimpleContainer() {
    value = new BasicType();
  }

  public SimpleContainer(BasicType value) {
    if (null != this.value)
      this.value.freeRef();
    this.value = value;
  }

  public @Override void _free() {
    if (null != value)
      value.freeRef();
    super._free();
  }

  @Override
  public String toString() {
    return "SimpleContainer{" + "values=" + value + '}';
  }

  public void use() {
    this.value.value++;
  }

  public @Override SimpleContainer addRef() {
    return (SimpleContainer) super.addRef();
  }

  public static SimpleContainer[] addRefs(SimpleContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(SimpleContainer::addRef)
        .toArray((x) -> new SimpleContainer[x]);
  }

  public static SimpleContainer[][] addRefs(SimpleContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(SimpleContainer::addRefs)
        .toArray((x) -> new SimpleContainer[x][]);
  }
}
