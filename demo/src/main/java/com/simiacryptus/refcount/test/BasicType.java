package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;

public class BasicType extends ReferenceCountingBase {
  public int value;

  public BasicType() {
    value = 1;
  }

  public void use() {
    System.out.println(String.format("Increment %s", this));
    this.value++;
  }

  @Override
  public String toString() {
    return "BasicType{" + "values=" + value + '}';
  }
}
