package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefConsumer;
import java.util.Map;
import java.util.function.Consumer;

public @com.simiacryptus.ref.lang.RefAware class HashMapValuesContainer extends ReferenceCountingBase {
  public com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> values = new com.simiacryptus.ref.wrappers.RefHashMap<>();

  public HashMapValuesContainer(BasicType... basicTypes) {
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
    com.simiacryptus.ref.wrappers.RefSet<java.lang.Integer> temp5735 = valuesMap.keySet();
    if (!temp5735.contains(12)) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    temp5735.freeRef();
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
    com.simiacryptus.ref.wrappers.RefHashSet<com.simiacryptus.demo.refcount.BasicType> temp2277 = values.values();
    temp2277.stream().forEach(x -> {
      x.value++;
      x.freeRef();
    });
    temp2277.freeRef();
    values.freeRef();
  }

  public @Override void _free() {
    if (null != values)
      values.freeRef();
    super._free();
  }

  private void test(RefConsumer<com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType>> fn) {
    final com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> hashMap = new com.simiacryptus.ref.wrappers.RefHashMap<>();
    fn.accept(com.simiacryptus.ref.lang.RefUtil.addRef(hashMap));
    hashMap.freeRef();
    fn.freeRef();
  }

  private void testEntries() {
    test(values -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp2418 = values
          .entrySet();
      temp2418.forEach(fooEntry -> {
        com.simiacryptus.demo.refcount.BasicType temp6748 = fooEntry.getValue();
        assert null != temp6748;
        temp6748.freeRef();
        assert null != fooEntry.getKey();
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry.setValue(new BasicType()));
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry);
      });
      temp2418.freeRef();
      values.freeRef();
    });
    test(values -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = fooEntry2 -> {
        if (1 == ((int) fooEntry2.getKey())) {
          com.simiacryptus.demo.refcount.BasicType temp8957 = fooEntry2.getValue();
          if (null == temp8957) {
            com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
            throw new AssertionError();
          }
          temp8957.freeRef();
        } else {
          com.simiacryptus.demo.refcount.BasicType temp5141 = fooEntry2.getValue();
          if (null == temp5141) {
            com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
            return;
          }
          temp5141.freeRef();
        }
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2.setValue(new BasicType()));
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
      };
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp1748 = values
          .entrySet();
      temp1748.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      temp1748.freeRef();
      com.simiacryptus.ref.lang.RefUtil.freeRef(entryConsumer);
      values.freeRef();
    });
    test((com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> values) -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> closureMap = new com.simiacryptus.ref.wrappers.RefHashMap<>();
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = (java.util.function.Consumer<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>>) com.simiacryptus.ref.lang.RefUtil
          .wrapInterface(
              (java.util.function.Consumer<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>>) lambdaParameter -> {
                if (1 == ((int) lambdaParameter.getKey())) {
                  com.simiacryptus.demo.refcount.BasicType temp5562 = lambdaParameter.getValue();
                  if (null == temp5562) {
                    com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
                    throw new AssertionError();
                  }
                  temp5562.freeRef();
                } else {
                  com.simiacryptus.demo.refcount.BasicType temp7371 = lambdaParameter.getValue();
                  if (null == temp7371) {
                    com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
                    return;
                  }
                  temp7371.freeRef();
                }
                com.simiacryptus.ref.lang.RefUtil
                    .freeRef(closureMap.put(lambdaParameter.getKey(), lambdaParameter.getValue()));
                com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
              }, com.simiacryptus.ref.lang.RefUtil.addRef(closureMap));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp6479 = values
          .entrySet();
      temp6479.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      temp6479.freeRef();
      com.simiacryptus.ref.lang.RefUtil.freeRef(entryConsumer);
      assert closureMap.size() == values.size();
      closureMap.freeRef();
      values.freeRef();
    });
    test((com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> values) -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final com.simiacryptus.ref.wrappers.RefHashMap<Integer, BasicType> closureMap = new com.simiacryptus.ref.wrappers.RefHashMap<>();
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = com.simiacryptus.ref.lang.RefUtil
          .wrapInterface(new Consumer<Map.Entry<Integer, BasicType>>() {
            @Override
            public void accept(Map.Entry<Integer, BasicType> anonymousParameter) {
              if (1 == ((int) anonymousParameter.getKey())) {
                com.simiacryptus.demo.refcount.BasicType temp3511 = anonymousParameter.getValue();
                if (null == temp3511) {
                  com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
                  throw new AssertionError();
                }
                temp3511.freeRef();
              } else {
                com.simiacryptus.demo.refcount.BasicType temp3370 = anonymousParameter.getValue();
                if (null == temp3370) {
                  com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
                  return;
                }
                temp3370.freeRef();
              }
              com.simiacryptus.ref.lang.RefUtil
                  .freeRef(closureMap.put(anonymousParameter.getKey(), anonymousParameter.getValue()));
              com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
            }
          }, com.simiacryptus.ref.lang.RefUtil.addRef(closureMap));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp1802 = values
          .entrySet();
      temp1802.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      temp1802.freeRef();
      com.simiacryptus.ref.lang.RefUtil.freeRef(entryConsumer);
      assert closureMap.size() == values.size();
      closureMap.freeRef();
      values.freeRef();
    });
  }

  @Override
  public String toString() {
    return "HashMapValuesContainer{" + "values=" + values + '}';
  }

  public void use() {
    for (int i = 0; i < TestOperations.count; i++) {
      testEntries();
      testBasicOperations(
          new com.simiacryptus.ref.wrappers.RefHashMap<>(com.simiacryptus.ref.lang.RefUtil.addRef(this.values)));
      testStreamOperations(
          new com.simiacryptus.ref.wrappers.RefHashMap<>(com.simiacryptus.ref.lang.RefUtil.addRef(this.values)));
    }
  }

  public @Override HashMapValuesContainer addRef() {
    return (HashMapValuesContainer) super.addRef();
  }

  public static HashMapValuesContainer[] addRefs(HashMapValuesContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(HashMapValuesContainer::addRef)
        .toArray((x) -> new HashMapValuesContainer[x]);
  }

  public static HashMapValuesContainer[][] addRefs(HashMapValuesContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(HashMapValuesContainer::addRefs)
        .toArray((x) -> new HashMapValuesContainer[x][]);
  }
}
