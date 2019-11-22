package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;

public class BasicType extends ReferenceCountingBase {
  public int value;

  public BasicType() {
    value = 1;
  }

  @Override
  public String toString() {
    return "BasicType{" + "values=" + value + '}';
  }
}
