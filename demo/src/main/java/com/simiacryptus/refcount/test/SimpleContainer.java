package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;

public class SimpleContainer extends ReferenceCountingBase {
  public BasicType value;

  public SimpleContainer() {
    this(new BasicType());
  }

  public SimpleContainer(BasicType value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "SimpleContainer{" + "values=" + value + '}';
  }
}
