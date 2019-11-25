package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import java.util.HashMap;

public class MapValuesContainer extends ReferenceCountingBase {
  public java.util.HashMap<Integer, BasicType> values = new java.util.HashMap<>();

  public MapValuesContainer(BasicType... basicTypes) {
    for (BasicType value : basicTypes) {
      if (null != this.values.put(this.values.size(), value))
        this.values.put(this.values.size(), value);
    }
  }

  public void use() {
    System.out.println(String.format("Increment %s", this));
    for (int i = 0; i < TestOperations.count; i++) {
      this.values.values().stream().forEach(x -> {
        x.value++;
      });
    }
  }

  @Override
  public String toString() {
    return "MapValuesContainer{" + "values=" + values + '}';
  }
}
