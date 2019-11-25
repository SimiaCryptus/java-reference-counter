package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import com.simiacryptus.lang.ref.wrappers.RefMap;

public class MapValuesContainer extends ReferenceCountingBase {
  public com.simiacryptus.lang.ref.wrappers.RefMap<Integer, BasicType> values = new com.simiacryptus.lang.ref.wrappers.RefMap<>();

  public MapValuesContainer(BasicType... basicTypes) {
    for (BasicType value : basicTypes) {
      if (null != this.values.put(this.values.size(), value.addRef()))
        this.values.put(this.values.size(), value.addRef());
    }
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(basicTypes);
  }

  public void use() {
    System.out.println(String.format("Increment %s", this));
    for (int i = 0; i < TestOperations.count; i++) {
      com.simiacryptus.lang.ref.wrappers.RefSet<com.simiacryptus.refcount.test.BasicType> temp2757 = this.values
          .values();
      temp2757.stream().forEach(x -> {
        x.value++;
        x.freeRef();
      });
      temp2757.freeRef();
    }
  }

  @Override
  public String toString() {
    return "MapValuesContainer{" + "values=" + values + '}';
  }

  public @Override void _free() {
    if (null != values)
      values.freeRef();
    super._free();
  }

  public @Override MapValuesContainer addRef() {
    return (MapValuesContainer) super.addRef();
  }

  public static MapValuesContainer[] addRefs(MapValuesContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(MapValuesContainer::addRef)
        .toArray((x) -> new MapValuesContainer[x]);
  }
}
