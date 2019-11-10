package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;

public class DataType1 extends ReferenceCountingBase {
  public int value;

  public DataType1() {
    value = 1;
  }

  @Override
  public String toString() {
    return "DataType1{" + "values=" + value + '}';
  }
}
