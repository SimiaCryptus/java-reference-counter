package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefConsumer;
import java.util.Map;
import java.util.function.Consumer;

public @com.simiacryptus.ref.lang.RefAware class ConcurrentHashMapValuesContainer extends ReferenceCountingBase {
  public com.simiacryptus.ref.wrappers.RefConcurrentHashMap<Integer, BasicType> values = new com.simiacryptus.ref.wrappers.RefConcurrentHashMap<>();

  public ConcurrentHashMapValuesContainer(BasicType... basicTypes) {
    for (int i = 0; i < basicTypes.length; i++) {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(i, basicTypes[i].addRef()));
    }
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(basicTypes);
  }

  public static void testBasicOperations(
      com.simiacryptus.ref.wrappers.RefConcurrentHashMap<Integer, BasicType> valuesMap) {
    final com.simiacryptus.ref.wrappers.RefConcurrentHashMap<Integer, BasicType> copyMap = new com.simiacryptus.ref.wrappers.RefConcurrentHashMap<>();
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
    com.simiacryptus.ref.wrappers.RefSet<java.lang.Integer> temp6562 = valuesMap.keySet();
    if (!temp6562.contains(12)) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    temp6562.freeRef();
    com.simiacryptus.ref.lang.RefUtil.freeRef(valuesMap.remove(12));
    if (valuesMap.containsKey(12)) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    if (!valuesMap.containsValue(valuesMap.get(32)))
      throw new AssertionError();
    valuesMap.freeRef();
  }

  public static void testStreamOperations(
      com.simiacryptus.ref.wrappers.RefConcurrentHashMap<Integer, BasicType> values) {
    com.simiacryptus.ref.wrappers.RefHashSet<com.simiacryptus.demo.refcount.BasicType> temp6503 = values.values();
    temp6503.stream().forEach(x -> {
      x.value++;
      x.freeRef();
    });
    temp6503.freeRef();
    values.freeRef();
  }

  public @Override void _free() {
    if (null != values)
      values.freeRef();
    super._free();
  }

  private void test(RefConsumer<com.simiacryptus.ref.wrappers.RefConcurrentHashMap<Integer, BasicType>> fn) {
    final com.simiacryptus.ref.wrappers.RefConcurrentHashMap<Integer, BasicType> hashMap = new com.simiacryptus.ref.wrappers.RefConcurrentHashMap<>();
    fn.accept(com.simiacryptus.ref.lang.RefUtil.addRef(hashMap));
    hashMap.freeRef();
    fn.freeRef();
  }

  private void testEntries() {
    test(values -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp7278 = values
          .entrySet();
      temp7278.forEach(fooEntry -> {
        com.simiacryptus.demo.refcount.BasicType temp9140 = fooEntry.getValue();
        assert null != temp9140;
        temp9140.freeRef();
        assert null != fooEntry.getKey();
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry.setValue(new BasicType()));
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry);
      });
      temp7278.freeRef();
      values.freeRef();
    });
    test(values -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = fooEntry2 -> {
        if (1 == ((int) fooEntry2.getKey())) {
          com.simiacryptus.demo.refcount.BasicType temp6019 = fooEntry2.getValue();
          if (null == temp6019) {
            com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
            throw new AssertionError();
          }
          temp6019.freeRef();
        } else {
          com.simiacryptus.demo.refcount.BasicType temp1120 = fooEntry2.getValue();
          if (null == temp1120) {
            com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
            return;
          }
          temp1120.freeRef();
        }
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2.setValue(new BasicType()));
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
      };
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp6361 = values
          .entrySet();
      temp6361.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      temp6361.freeRef();
      com.simiacryptus.ref.lang.RefUtil.freeRef(entryConsumer);
      values.freeRef();
    });
    test((com.simiacryptus.ref.wrappers.RefConcurrentHashMap<Integer, BasicType> values) -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final com.simiacryptus.ref.wrappers.RefConcurrentHashMap<Integer, BasicType> closureMap = new com.simiacryptus.ref.wrappers.RefConcurrentHashMap<>();
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = (Consumer<Map.Entry<Integer, BasicType>>) com.simiacryptus.ref.lang.RefUtil
          .wrapInterface(
              (java.util.function.Consumer<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>>) lambdaParameter -> {
                if (1 == ((int) lambdaParameter.getKey())) {
                  com.simiacryptus.demo.refcount.BasicType temp1010 = lambdaParameter.getValue();
                  if (null == temp1010) {
                    com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
                    throw new AssertionError();
                  }
                  temp1010.freeRef();
                } else {
                  com.simiacryptus.demo.refcount.BasicType temp3206 = lambdaParameter.getValue();
                  if (null == temp3206) {
                    com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
                    return;
                  }
                  temp3206.freeRef();
                }
                com.simiacryptus.ref.lang.RefUtil
                    .freeRef(closureMap.put(lambdaParameter.getKey(), lambdaParameter.getValue()));
                com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
              }, com.simiacryptus.ref.lang.RefUtil.addRef(closureMap));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp5955 = values
          .entrySet();
      temp5955.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      temp5955.freeRef();
      com.simiacryptus.ref.lang.RefUtil.freeRef(entryConsumer);
      assert closureMap.size() == values.size();
      closureMap.freeRef();
      values.freeRef();
    });
    test((com.simiacryptus.ref.wrappers.RefConcurrentHashMap<Integer, BasicType> values) -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final com.simiacryptus.ref.wrappers.RefConcurrentHashMap<Integer, BasicType> closureMap = new com.simiacryptus.ref.wrappers.RefConcurrentHashMap<>();
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = com.simiacryptus.ref.lang.RefUtil
          .wrapInterface(new Consumer<Map.Entry<Integer, BasicType>>() {
            @Override
            public void accept(Map.Entry<Integer, BasicType> anonymousParameter) {
              if (1 == ((int) anonymousParameter.getKey())) {
                com.simiacryptus.demo.refcount.BasicType temp2946 = anonymousParameter.getValue();
                if (null == temp2946) {
                  com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
                  throw new AssertionError();
                }
                temp2946.freeRef();
              } else {
                com.simiacryptus.demo.refcount.BasicType temp6209 = anonymousParameter.getValue();
                if (null == temp6209) {
                  com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
                  return;
                }
                temp6209.freeRef();
              }
              com.simiacryptus.ref.lang.RefUtil
                  .freeRef(closureMap.put(anonymousParameter.getKey(), anonymousParameter.getValue()));
              com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
            }
          }, com.simiacryptus.ref.lang.RefUtil.addRef(closureMap));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp7177 = values
          .entrySet();
      temp7177.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      temp7177.freeRef();
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
      testBasicOperations(new com.simiacryptus.ref.wrappers.RefConcurrentHashMap<>(
          com.simiacryptus.ref.lang.RefUtil.addRef(this.values)));
      testStreamOperations(new com.simiacryptus.ref.wrappers.RefConcurrentHashMap<>(
          com.simiacryptus.ref.lang.RefUtil.addRef(this.values)));
    }
  }

  public @Override ConcurrentHashMapValuesContainer addRef() {
    return (ConcurrentHashMapValuesContainer) super.addRef();
  }

  public static ConcurrentHashMapValuesContainer[] addRefs(ConcurrentHashMapValuesContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ConcurrentHashMapValuesContainer::addRef)
        .toArray((x) -> new ConcurrentHashMapValuesContainer[x]);
  }

  public static ConcurrentHashMapValuesContainer[][] addRefs(ConcurrentHashMapValuesContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ConcurrentHashMapValuesContainer::addRefs)
        .toArray((x) -> new ConcurrentHashMapValuesContainer[x][]);
  }
}
