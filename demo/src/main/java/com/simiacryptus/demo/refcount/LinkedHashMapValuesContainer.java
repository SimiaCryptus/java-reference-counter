package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefConsumer;
import java.util.Map;
import java.util.function.Consumer;

public @com.simiacryptus.ref.lang.RefAware class LinkedHashMapValuesContainer extends ReferenceCountingBase {
  public com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> values = new com.simiacryptus.ref.wrappers.RefLinkedHashMap<>();

  public LinkedHashMapValuesContainer(BasicType... basicTypes) {
    for (int i = 0; i < basicTypes.length; i++) {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(i, basicTypes[i].addRef()));
    }
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(basicTypes);
  }

  public static void testBasicOperations(com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> valuesMap) {
    final com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> copyMap = new com.simiacryptus.ref.wrappers.RefLinkedHashMap<>();
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
    com.simiacryptus.ref.wrappers.RefSet<java.lang.Integer> temp4643 = valuesMap.keySet();
    if (!temp4643.contains(12)) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    temp4643.freeRef();
    com.simiacryptus.ref.lang.RefUtil.freeRef(valuesMap.remove(12));
    if (valuesMap.containsKey(12)) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    if (!valuesMap.containsValue(valuesMap.get(32)))
      throw new AssertionError();
    valuesMap.freeRef();
  }

  public static void testStreamOperations(com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> values) {
    com.simiacryptus.ref.wrappers.RefHashSet<com.simiacryptus.demo.refcount.BasicType> temp4079 = values.values();
    temp4079.stream().forEach(x -> {
      x.value++;
      x.freeRef();
    });
    temp4079.freeRef();
    values.freeRef();
  }

  public @Override void _free() {
    if (null != values)
      values.freeRef();
    super._free();
  }

  private void test(RefConsumer<com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType>> fn) {
    final com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> hashMap = new com.simiacryptus.ref.wrappers.RefLinkedHashMap<>();
    fn.accept(com.simiacryptus.ref.lang.RefUtil.addRef(hashMap));
    hashMap.freeRef();
    fn.freeRef();
  }

  private void testEntries() {
    test(values -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp1962 = values
          .entrySet();
      temp1962.forEach(fooEntry -> {
        com.simiacryptus.demo.refcount.BasicType temp4864 = fooEntry.getValue();
        assert null != temp4864;
        temp4864.freeRef();
        assert null != fooEntry.getKey();
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry.setValue(new BasicType()));
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry);
      });
      temp1962.freeRef();
      values.freeRef();
    });
    test(values -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = fooEntry2 -> {
        if (1 == ((int) fooEntry2.getKey())) {
          com.simiacryptus.demo.refcount.BasicType temp3402 = fooEntry2.getValue();
          if (null == temp3402) {
            com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
            throw new AssertionError();
          }
          temp3402.freeRef();
        } else {
          com.simiacryptus.demo.refcount.BasicType temp6578 = fooEntry2.getValue();
          if (null == temp6578) {
            com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
            return;
          }
          temp6578.freeRef();
        }
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2.setValue(new BasicType()));
        com.simiacryptus.ref.lang.RefUtil.freeRef(fooEntry2);
      };
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp9216 = values
          .entrySet();
      temp9216.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      temp9216.freeRef();
      com.simiacryptus.ref.lang.RefUtil.freeRef(entryConsumer);
      values.freeRef();
    });
    test((com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> values) -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> closureMap = new com.simiacryptus.ref.wrappers.RefLinkedHashMap<>();
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = (Consumer<Map.Entry<Integer, BasicType>>) com.simiacryptus.ref.lang.RefUtil
          .wrapInterface(
              (java.util.function.Consumer<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>>) lambdaParameter -> {
                if (1 == ((int) lambdaParameter.getKey())) {
                  com.simiacryptus.demo.refcount.BasicType temp2558 = lambdaParameter.getValue();
                  if (null == temp2558) {
                    com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
                    throw new AssertionError();
                  }
                  temp2558.freeRef();
                } else {
                  com.simiacryptus.demo.refcount.BasicType temp8259 = lambdaParameter.getValue();
                  if (null == temp8259) {
                    com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
                    return;
                  }
                  temp8259.freeRef();
                }
                com.simiacryptus.ref.lang.RefUtil
                    .freeRef(closureMap.put(lambdaParameter.getKey(), lambdaParameter.getValue()));
                com.simiacryptus.ref.lang.RefUtil.freeRef(lambdaParameter);
              }, com.simiacryptus.ref.lang.RefUtil.addRef(closureMap));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp3714 = values
          .entrySet();
      temp3714.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      temp3714.freeRef();
      com.simiacryptus.ref.lang.RefUtil.freeRef(entryConsumer);
      assert closureMap.size() == values.size();
      closureMap.freeRef();
      values.freeRef();
    });
    test((com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> values) -> {
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.put(2, new BasicType()));
      final com.simiacryptus.ref.wrappers.RefLinkedHashMap<Integer, BasicType> closureMap = new com.simiacryptus.ref.wrappers.RefLinkedHashMap<>();
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = com.simiacryptus.ref.lang.RefUtil
          .wrapInterface(new Consumer<Map.Entry<Integer, BasicType>>() {
            @Override
            public void accept(Map.Entry<Integer, BasicType> anonymousParameter) {
              if (1 == ((int) anonymousParameter.getKey())) {
                com.simiacryptus.demo.refcount.BasicType temp2465 = anonymousParameter.getValue();
                if (null == temp2465) {
                  com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
                  throw new AssertionError();
                }
                temp2465.freeRef();
              } else {
                com.simiacryptus.demo.refcount.BasicType temp3154 = anonymousParameter.getValue();
                if (null == temp3154) {
                  com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
                  return;
                }
                temp3154.freeRef();
              }
              com.simiacryptus.ref.lang.RefUtil
                  .freeRef(closureMap.put(anonymousParameter.getKey(), anonymousParameter.getValue()));
              com.simiacryptus.ref.lang.RefUtil.freeRef(anonymousParameter);
            }
          }, com.simiacryptus.ref.lang.RefUtil.addRef(closureMap));
      com.simiacryptus.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.demo.refcount.BasicType>> temp2308 = values
          .entrySet();
      temp2308.forEach(com.simiacryptus.ref.lang.RefUtil.addRef(entryConsumer));
      temp2308.freeRef();
      com.simiacryptus.ref.lang.RefUtil.freeRef(entryConsumer);
      assert closureMap.size() == values.size();
      closureMap.freeRef();
      values.freeRef();
    });
  }

  @Override
  public String toString() {
    return "LinkedHashMapValuesContainer{" + "values=" + values + '}';
  }

  public void use() {
    for (int i = 0; i < TestOperations.count; i++) {
      testEntries();
      testBasicOperations(
          new com.simiacryptus.ref.wrappers.RefLinkedHashMap<>(com.simiacryptus.ref.lang.RefUtil.addRef(this.values)));
      testStreamOperations(
          new com.simiacryptus.ref.wrappers.RefLinkedHashMap<>(com.simiacryptus.ref.lang.RefUtil.addRef(this.values)));
    }
  }

  public @Override LinkedHashMapValuesContainer addRef() {
    return (LinkedHashMapValuesContainer) super.addRef();
  }

  public static LinkedHashMapValuesContainer[] addRefs(LinkedHashMapValuesContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(LinkedHashMapValuesContainer::addRef)
        .toArray((x) -> new LinkedHashMapValuesContainer[x]);
  }

  public static LinkedHashMapValuesContainer[][] addRefs(LinkedHashMapValuesContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(LinkedHashMapValuesContainer::addRefs)
        .toArray((x) -> new LinkedHashMapValuesContainer[x][]);
  }
}
