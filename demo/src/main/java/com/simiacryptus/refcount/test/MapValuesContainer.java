package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import com.simiacryptus.lang.ref.wrappers.RefHashSet;

public class MapValuesContainer extends ReferenceCountingBase {
  public com.simiacryptus.lang.ref.wrappers.RefHashMap<Integer, BasicType> values = new com.simiacryptus.lang.ref.wrappers.RefHashMap<>();

  public MapValuesContainer(BasicType... basicTypes) {
    for (int i = 0; i < basicTypes.length; i++) {
      com.simiacryptus.lang.ref.RefUtil.freeRef(values.put(i, basicTypes[i].addRef()));
    }
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(basicTypes);
  }

  public void use() {
    System.out.println(String.format("Increment %s", this));
    for (int i = 0; i < TestOperations.count; i++) {
      testBasicOperations(new com.simiacryptus.lang.ref.wrappers.RefHashMap<>(this.values.addRef()));
      testStreamOperations(new com.simiacryptus.lang.ref.wrappers.RefHashMap<>(this.values.addRef()));
    }
  }

  public static void testBasicOperations(com.simiacryptus.lang.ref.wrappers.RefHashMap<Integer, BasicType> values) {
    final com.simiacryptus.lang.ref.wrappers.RefHashMap<Integer, BasicType> copyMap = new com.simiacryptus.lang.ref.wrappers.RefHashMap<>();
    copyMap.putAll(values.addRef());
    copyMap.freeRef();
    values.clear();
    assert values.isEmpty();
    com.simiacryptus.lang.ref.RefUtil.freeRef(values.put(12, new BasicType()));
    com.simiacryptus.lang.ref.RefUtil.freeRef(values.put(12, new BasicType()));
    com.simiacryptus.lang.ref.RefUtil.freeRef(values.put(32, new BasicType()));
    if (2 != values.size())
      throw new AssertionError();
    if (!values.containsKey(12))
      throw new AssertionError();
    com.simiacryptus.lang.ref.wrappers.RefSet<java.lang.Integer> temp4348 = values.keySet();
    if (!temp4348.contains(12))
      throw new AssertionError();
    temp4348.freeRef();
    com.simiacryptus.lang.ref.RefUtil.freeRef(values.remove(12));
    if (values.containsKey(12))
      throw new AssertionError();
    if (!values.containsValue(values.get(32)))
      throw new AssertionError();
    values.freeRef();
  }

  public static void testStreamOperations(com.simiacryptus.lang.ref.wrappers.RefHashMap<Integer, BasicType> values) {
    com.simiacryptus.lang.ref.wrappers.RefHashSet<com.simiacryptus.refcount.test.BasicType> temp6819 = values.values();
    temp6819.stream().forEach(x -> {
      x.value++;
      x.freeRef();
    });
    temp6819.freeRef();
    values.freeRef();
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
