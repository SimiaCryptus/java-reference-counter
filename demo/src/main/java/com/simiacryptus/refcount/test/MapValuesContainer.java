package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import com.simiacryptus.lang.ref.wrappers.RefHashMapV;

public class MapValuesContainer extends ReferenceCountingBase {
  public RefHashMapV<Integer, BasicType> values = new RefHashMapV<>();

  public MapValuesContainer(BasicType... values) {
    for (int i = 0; i < values.length; i++) {
      this.values.put(i, values[i]);
    }
  }

  @Override
  public String toString() {
    return "MapValuesContainer{" + "values=" + values + '}';
  }
}
