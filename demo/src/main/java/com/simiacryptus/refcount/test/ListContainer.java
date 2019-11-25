package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import java.util.ArrayList;

public class ListContainer extends ReferenceCountingBase {
  public java.util.ArrayList<BasicType> values = new java.util.ArrayList<>();

  public ListContainer(BasicType... values) {
    for (BasicType value : values) {
      this.values.add(value);
    }
  }

  @Override
  public String toString() {
    return "ListContainer{" + "values=" + values + '}';
  }
}
