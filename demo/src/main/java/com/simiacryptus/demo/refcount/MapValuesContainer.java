package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefConsumer;

import java.util.Map;
import java.util.function.Consumer;

public @com.simiacryptus.ref.lang.RefAware
class MapValuesContainer extends ReferenceCountingBase {
  public com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> values = new com.simiacryptus.ref.wrappers.RefHashMap<>();

  public MapValuesContainer(BasicType... basicTypes) {
    for (int i = 0; i < basicTypes.length; i++) {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(i, basicTypes[i].addRef()));
    }
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(basicTypes);
  }

  public static void testBasicOperations(com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> valuesMap) {
    final com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> copyMap = new com.simiacryptus.ref.wrappers.RefHashMap<>();
    copyMap.putAll(com.simiacryptus.ref.lang.RefUtil.addRef(valuesMap));
    copyMap.freeRef();
    valuesMap.clear();
    assert valuesMap.isEmpty();
    com.simiacryptus.ref.lang.RefUtil.freeRef(valuesMap.put(12, new BasicType()));
    com.simiacryptus.ref.lang.RefUtil.freeRef(valuesMap.put(12, new BasicType()));
    com.simiacryptus.ref.lang.RefUtil.freeRef(valuesMap.put(32, new BasicType()));
    if (2 != valuesMap.size()) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    if (!valuesMap.containsKey(12)) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    com.simiacryptus.ref.wrappers.RefSet<java.lang.Integer> temp7635 = valuesMap.keySet();
    if (!temp7635.contains(12)) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    temp7635.freeRef();
    com.simiacryptus.ref.lang.RefUtil.freeRef(valuesMap.remove(12));
    if (valuesMap.containsKey(12)) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    if (!valuesMap.containsValue(valuesMap.get(32)))
      throw new AssertionError();
    valuesMap.freeRef();
  }

  public static void testStreamOperations(com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> values) {
    com.simiacryptus.ref.wrappers.RefHashSet<com.simiacryptus.demo.refcount.BasicType> temp2029 = values.values();
    temp2029.stream().forEach(x -> {
      x.value++;
      x.freeRef();
    });
    temp2029.freeRef();
    values.freeRef();
  }

  public static MapValuesContainer[] addRefs(MapValuesContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(MapValuesContainer::addRef)
        .toArray((x) -> new MapValuesContainer[x]);
  }

  public static MapValuesContainer[][] addRefs(MapValuesContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(MapValuesContainer::addRefs)
        .toArray((x) -> new MapValuesContainer[x][]);
  }

  public @Override
  void _free() {
    if (null != values)
      values.freeRef();
    super._free();
  }

  public @Override
  MapValuesContainer addRef() {
    return (MapValuesContainer) super.addRef();
  }

  private void test(RefConsumer<com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType>> fn) {
    final com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> hashMap = new com.simiacryptus.ref.wrappers.RefHashMap<>();
    fn.accept(com.simiacryptus.ref.lang.RefUtil.addRef(hashMap));
    hashMap.freeRef();
    fn.freeRef();
  }

  private void testEntries() {
    test(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp3439 = values
          .entrySet();
      temp3439.forEach(fooEntry -> {
        com.simiacryptus.demo.refcount.BasicType temp1068 = fooEntry.getValue();
        assert null != temp1068;
        temp1068.freeRef();
        assert null != fooEntry.getKey();
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry.setValue(new BasicType()));
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry);
      });
      temp3439.freeRef();
      values.freeRef();
    }));
    test(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = fooEntry2 -> {
        if (1 == ((int) fooEntry2.getKey())) {
          com.simiacryptus.demo.refcount.BasicType temp6447 = fooEntry2.getValue();
          if (null == temp6447) {
            com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
            throw new AssertionError();
          }
          temp6447.freeRef();
        } else {
          com.simiacryptus.demo.refcount.BasicType temp1362 = fooEntry2.getValue();
          if (null == temp1362) {
            com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
            return;
          }
          temp1362.freeRef();
        }
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2.setValue(new BasicType()));
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
      };
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp8406 = values
          .entrySet();
      temp8406.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      temp8406.freeRef();
      com.simiacryptus.ref.lang.RefUtil.freeRef(entryConsumer);
      values.freeRef();
    }));
    test(com.simiacryptus.ref.lang.RefUtil
        .addRef((com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> values) -> {
          com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
          com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
          final com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> closureMap = new com.simiacryptus.ref.wrappers.RefHashMap<>();
          final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = com.simiacryptus.ref.lang.RefUtil.wrapInterface(
              (java.util.function.Consumer<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>>) lambdaParameter -> {
                if (1 == ((int) lambdaParameter.getKey())) {
                  com.simiacryptus.demo.refcount.BasicType temp7783 = lambdaParameter.getValue();
                  if (null == temp7783) {
                    com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
                    throw new AssertionError();
                  }
                  temp7783.freeRef();
                } else {
                  com.simiacryptus.demo.refcount.BasicType temp1332 = lambdaParameter.getValue();
                  if (null == temp1332) {
                    com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
                    return;
                  }
                  temp1332.freeRef();
                }
                com.simiacryptus.ref.lang.RefUtil
                    .freeRef(closureMap.put(lambdaParameter.getKey(), lambdaParameter.getValue()));
                com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
              }, closureMap.addRef());
          com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp4770 = values
              .entrySet();
          temp4770.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
          temp4770.freeRef();
          com.simiacryptus.ref.lang.RefUtil.freeRef(entryConsumer);
          assert closureMap.size() == values.size();
          closureMap.freeRef();
          values.freeRef();
        }));
    test(com.simiacryptus.ref.lang.RefUtil
        .addRef((com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> values) -> {
          com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
          com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
          final com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> closureMap = new com.simiacryptus.ref.wrappers.RefHashMap<>();
          final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = com.simiacryptus.ref.lang.RefUtil
              .wrapInterface(new Consumer<Map.Entry<Integer, BasicType>>() {
                @Override
                public void accept(Map.Entry<Integer, BasicType> anonymousParameter) {
                  if (1 == ((int) anonymousParameter.getKey())) {
                    com.simiacryptus.demo.refcount.BasicType temp8531 = anonymousParameter.getValue();
                    if (null == temp8531) {
                      com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
                      throw new AssertionError();
                    }
                    temp8531.freeRef();
                  } else {
                    com.simiacryptus.demo.refcount.BasicType temp4302 = anonymousParameter.getValue();
                    if (null == temp4302) {
                      com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
                      return;
                    }
                    temp4302.freeRef();
                  }
                  com.simiacryptus.ref.lang.RefUtil
                      .freeRef(closureMap.put(anonymousParameter.getKey(), anonymousParameter.getValue()));
                  com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
                }
              }, closureMap.addRef());
          com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp2683 = values
              .entrySet();
          temp2683.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
          temp2683.freeRef();
          com.simiacryptus.ref.lang.RefUtil.freeRef(entryConsumer);
          assert closureMap.size() == values.size();
          closureMap.freeRef();
          values.freeRef();
        }));
  }

  @Override
  public String toString() {
    return "MapValuesContainer{" + "values=" + values + '}';
  }

  public void use() {
    System.out.println(String.format("Increment %s", this));
    for (int i = 0; i < TestOperations.count; i++) {
      testEntries();
      testBasicOperations(
          new com.simiacryptus.ref.wrappers.RefHashMap<>(com.simiacryptus.ref.lang.RefUtil.addRef(this.values)));
      testStreamOperations(
          new com.simiacryptus.ref.wrappers.RefHashMap<>(com.simiacryptus.ref.lang.RefUtil.addRef(this.values)));
    }
  }
}
