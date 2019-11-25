package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import java.util.HashSet;

public class SetContainer extends ReferenceCountingBase {
  public java.util.HashSet<BasicType> values = new java.util.HashSet<>();

  public SetContainer(BasicType... values) {
    for (BasicType value : values) {
      this.values.add(value);
    }
  }

  @Override
  public String toString() {
    return "SetContainer{" + "values=" + values + '}';
  }
}
