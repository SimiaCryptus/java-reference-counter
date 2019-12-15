package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;

public class SimpleContainer extends ReferenceCountingBase {
  public BasicType value;

  public SimpleContainer() {
    value = new BasicType();
  }

  public SimpleContainer(BasicType value) {
    this.value = value;
  }

  public @Override
  void _free() {
    super._free();
  }

  public void test() {
    this.value.setValue(this.value.getValue() + 1);
  }

  @Override
  public String toString() {
    return "SimpleContainer{" + "values=" + value + '}';
  }
}
