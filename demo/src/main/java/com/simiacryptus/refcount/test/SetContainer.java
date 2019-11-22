package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import com.simiacryptus.lang.ref.wrappers.RefSet;

public class SetContainer extends ReferenceCountingBase {
  public RefSet<BasicType> values = new RefSet<>();

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
