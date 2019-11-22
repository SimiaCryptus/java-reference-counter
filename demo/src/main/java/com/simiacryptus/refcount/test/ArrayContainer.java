package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;

public class ArrayContainer extends ReferenceCountingBase {
  public BasicType[] values;

  public ArrayContainer(BasicType... values) {
    this.values = values;
  }

  @Override
  public String toString() {
    return "ArrayContainer{" + "values=" + java.util.Arrays.toString(values) + '}';
  }
}
