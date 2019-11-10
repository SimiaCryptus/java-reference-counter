package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;

public class DataType2 extends ReferenceCountingBase {
  public DataType1 value;

  public DataType2() {
    this(new DataType1());
  }

  public DataType2(DataType1 value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "DataType2{" + "values=" + value + '}';
  }
}
