package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import java.util.HashMap;

public class MapValuesContainer extends ReferenceCountingBase {
  public java.util.HashMap<Integer, BasicType> values = new java.util.HashMap<>();

  public MapValuesContainer(BasicType... values) {
    for (BasicType value : values) {
      if (null != this.values.put(this.values.size(), value))
        this.values.put(this.values.size(), value);
    }
  }

  @Override
  public String toString() {
    return "MapValuesContainer{" + "values=" + values + '}';
  }
}
