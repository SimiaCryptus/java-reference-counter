package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;

public class SimpleContainer extends ReferenceCountingBase {
  public BasicType value;

  public SimpleContainer() {
    value = new BasicType();
  }

  public SimpleContainer(BasicType value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "SimpleContainer{" + "values=" + value + '}';
  }

  public void use() {
    System.out.println(String.format("Increment %s", this));
    this.value.value++;
  }

  public @Override void _free() {
    super._free();
  }
}
