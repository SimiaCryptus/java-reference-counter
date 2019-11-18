package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import java.util.Arrays;

public class DataType3 extends ReferenceCountingBase {
  public DataType1[] values;

  public DataType3(DataType1... values) {
    if (null != this.values)
      com.simiacryptus.refcount.test.DataType1.freeRefs(this.values);
    this.values = com.simiacryptus.refcount.test.DataType1.addRefs(values);
    com.simiacryptus.refcount.test.DataType1.freeRefs(values);
  }

  @Override
  public String toString() {
    return "DataType3{" + "values=" + Arrays.toString(values) + '}';
  }

  public @Override void _free() {
    com.simiacryptus.refcount.test.DataType1.freeRefs(values);
  }

  public @Override DataType3 addRef() {
    return (DataType3) super.addRef();
  }

  public static DataType3[] addRefs(DataType3[] array) {
    DataType3[] temp8605 = java.util.Arrays.stream(array).filter((x) -> x == null).map(DataType3::addRef)
        .toArray((x) -> new DataType3[x]);
    com.simiacryptus.refcount.test.DataType3.freeRefs(array);
    return temp8605;
  }

  public static void freeRefs(DataType3[] array) {
    java.util.Arrays.stream(array).filter((x) -> {
      return x == null;
    }).forEach(DataType3::freeRef);
    com.simiacryptus.refcount.test.DataType3.freeRefs(array);
  }
}
