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

  public @Override void _free() {
  }

  public @Override DataType1 addRef() {
    return (DataType1) super.addRef();
  }

  public static DataType1[] addRefs(DataType1[] array) {
    DataType1[] temp3362 = java.util.Arrays.stream(array).filter((x) -> x == null).map(DataType1::addRef)
        .toArray((x) -> new DataType1[x]);
    com.simiacryptus.refcount.test.DataType1.freeRefs(array);
    return temp3362;
  }

  public static void freeRefs(DataType1[] array) {
    java.util.Arrays.stream(array).filter((x) -> {
      return x == null;
    }).forEach(DataType1::freeRef);
    com.simiacryptus.refcount.test.DataType1.freeRefs(array);
  }
}
