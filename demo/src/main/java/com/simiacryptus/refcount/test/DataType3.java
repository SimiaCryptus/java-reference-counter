package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import java.util.Arrays;

public class DataType3 extends ReferenceCountingBase {
  public DataType1[] values;

  public DataType3(DataType1... values) {
    this.values = values;
  }

  @Override
  public String toString() {
    return "DataType3{" + "values=" + Arrays.toString(values) + '}';
  }
}
