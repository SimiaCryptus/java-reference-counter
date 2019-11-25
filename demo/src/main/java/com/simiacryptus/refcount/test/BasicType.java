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

  public @Override void _free() {
    super._free();
  }

  public @Override BasicType addRef() {
    return (BasicType) super.addRef();
  }

  public static BasicType[] addRefs(BasicType[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(BasicType::addRef)
        .toArray((x) -> new BasicType[x]);
  }
}
