package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import com.simiacryptus.lang.ref.wrappers.RefHashMap;
import java.util.Map;
import java.util.function.Consumer;

public @com.simiacryptus.lang.ref.RefAware class MapValuesContainer extends ReferenceCountingBase {
  public com.simiacryptus.lang.ref.wrappers.RefHashMap<Integer, BasicType> values = new com.simiacryptus.lang.ref.wrappers.RefHashMap<>();

  public MapValuesContainer(BasicType... basicTypes) {
    for (int i = 0; i < basicTypes.length; i++) {
      com.simiacryptus.lang.ref.RefUtil.freeRef(values.put(i, basicTypes[i].addRef()));
    }
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(basicTypes);
  }

  public static void testBasicOperations(com.simiacryptus.lang.ref.wrappers.RefHashMap<Integer, BasicType> valuesMap) {
    final com.simiacryptus.lang.ref.wrappers.RefHashMap<Integer, BasicType> copyMap = new com.simiacryptus.lang.ref.wrappers.RefHashMap<>();
    copyMap.putAll(valuesMap.addRef());
    copyMap.freeRef();
    valuesMap.clear();
    assert valuesMap.isEmpty();
    com.simiacryptus.lang.ref.RefUtil.freeRef(valuesMap.put(12, new BasicType()));
    com.simiacryptus.lang.ref.RefUtil.freeRef(valuesMap.put(12, new BasicType()));
    com.simiacryptus.lang.ref.RefUtil.freeRef(valuesMap.put(32, new BasicType()));
    if (2 != valuesMap.size()) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    if (!valuesMap.containsKey(12)) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    com.simiacryptus.lang.ref.wrappers.RefSet<java.lang.Integer> temp1487 = valuesMap.keySet();
    if (!temp1487.contains(12)) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    temp1487.freeRef();
    com.simiacryptus.lang.ref.RefUtil.freeRef(valuesMap.remove(12));
    if (valuesMap.containsKey(12)) {
      valuesMap.freeRef();
      throw new AssertionError();
    }
    if (!valuesMap.containsValue(valuesMap.get(32)))
      throw new AssertionError();
    valuesMap.freeRef();
  }

  public static void testStreamOperations(com.simiacryptus.lang.ref.wrappers.RefHashMap<Integer, BasicType> values) {
    com.simiacryptus.lang.ref.wrappers.RefHashSet<com.simiacryptus.refcount.test.BasicType> temp1551 = values.values();
    com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp5002 = temp1551.stream();
    temp5002.forEach(x -> {
      x.value++;
      x.freeRef();
    });
    com.simiacryptus.lang.ref.RefUtil.freeRef(temp5002);
    temp1551.freeRef();
    values.freeRef();
  }

  public @Override void _free() {
    if (null != values)
      values.freeRef();
    super._free();
  }

  private void test(Consumer<com.simiacryptus.lang.ref.wrappers.RefHashMap<Integer, BasicType>> fn) {
    final com.simiacryptus.lang.ref.wrappers.RefHashMap<Integer, BasicType> hashMap = new com.simiacryptus.lang.ref.wrappers.RefHashMap<>();
    fn.accept(hashMap);
    com.simiacryptus.lang.ref.RefUtil.freeRef(fn);
  }

  private void testEntries() {
    test(values -> {
      com.simiacryptus.lang.ref.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.lang.ref.RefUtil.freeRef(values.put(2, new BasicType()));
      com.simiacryptus.lang.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.refcount.test.BasicType>> temp8820 = values
          .entrySet();
      temp8820.forEach(fooEntry -> {
        com.simiacryptus.refcount.test.BasicType temp3005 = fooEntry.getValue();
        assert null != temp3005;
        temp3005.freeRef();
        assert null != fooEntry.getKey();
        com.simiacryptus.lang.ref.RefUtil.freeRef(fooEntry.setValue(new BasicType()));
        com.simiacryptus.lang.ref.RefUtil.freeRef(fooEntry);
      });
      temp8820.freeRef();
      values.freeRef();
    });
    test(values -> {
      com.simiacryptus.lang.ref.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.lang.ref.RefUtil.freeRef(values.put(2, new BasicType()));
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = fooEntry2 -> {
        if (1 == ((int) fooEntry2.getKey())) {
          com.simiacryptus.refcount.test.BasicType temp8596 = fooEntry2.getValue();
          if (null == temp8596) {
            com.simiacryptus.lang.ref.RefUtil.freeRef(fooEntry2);
            throw new AssertionError();
          }
          temp8596.freeRef();
        } else {
          com.simiacryptus.refcount.test.BasicType temp6307 = fooEntry2.getValue();
          if (null == temp6307) {
            com.simiacryptus.lang.ref.RefUtil.freeRef(fooEntry2);
            return;
          }
          temp6307.freeRef();
        }
        com.simiacryptus.lang.ref.RefUtil.freeRef(fooEntry2.setValue(new BasicType()));
        com.simiacryptus.lang.ref.RefUtil.freeRef(fooEntry2);
      };
      com.simiacryptus.lang.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.refcount.test.BasicType>> temp8244 = values
          .entrySet();
      temp8244.forEach(com.simiacryptus.lang.ref.RefUtil.addRef(entryConsumer));
      temp8244.freeRef();
      com.simiacryptus.lang.ref.RefUtil.freeRef(entryConsumer);
      values.freeRef();
    });
    test((com.simiacryptus.lang.ref.wrappers.RefHashMap<Integer, BasicType> values) -> {
      com.simiacryptus.lang.ref.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.lang.ref.RefUtil.freeRef(values.put(2, new BasicType()));
      final com.simiacryptus.lang.ref.wrappers.RefHashMap<Integer, BasicType> targetMap = new com.simiacryptus.lang.ref.wrappers.RefHashMap<>();
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = entry -> {
        if (1 == ((int) entry.getKey())) {
          com.simiacryptus.refcount.test.BasicType temp4774 = entry.getValue();
          if (null == temp4774) {
            com.simiacryptus.lang.ref.RefUtil.freeRef(entry);
            throw new AssertionError();
          }
          temp4774.freeRef();
        } else {
          com.simiacryptus.refcount.test.BasicType temp3214 = entry.getValue();
          if (null == temp3214) {
            com.simiacryptus.lang.ref.RefUtil.freeRef(entry);
            return;
          }
          temp3214.freeRef();
        }
        com.simiacryptus.lang.ref.RefUtil.freeRef(targetMap.put(entry.getKey(), entry.getValue()));
        com.simiacryptus.lang.ref.RefUtil.freeRef(entry);
      };
      com.simiacryptus.lang.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.refcount.test.BasicType>> temp6038 = values
          .entrySet();
      temp6038.forEach(com.simiacryptus.lang.ref.RefUtil.addRef(entryConsumer));
      temp6038.freeRef();
      com.simiacryptus.lang.ref.RefUtil.freeRef(entryConsumer);
      assert targetMap.size() == values.size();
      targetMap.freeRef();
      values.freeRef();
    });
    test((com.simiacryptus.lang.ref.wrappers.RefHashMap<Integer, BasicType> values) -> {
      com.simiacryptus.lang.ref.RefUtil.freeRef(values.put(1, new BasicType()));
      com.simiacryptus.lang.ref.RefUtil.freeRef(values.put(2, new BasicType()));
      final com.simiacryptus.lang.ref.wrappers.RefHashMap<Integer, BasicType> targetMap = new com.simiacryptus.lang.ref.wrappers.RefHashMap<>();
      final Consumer<Map.Entry<Integer, BasicType>> entryConsumer = new Consumer<Map.Entry<Integer, BasicType>>() {
        @Override
        public void accept(Map.Entry<Integer, BasicType> entry) {
          if (1 == ((int) entry.getKey())) {
            com.simiacryptus.refcount.test.BasicType temp6276 = entry.getValue();
            if (null == temp6276) {
              com.simiacryptus.lang.ref.RefUtil.freeRef(entry);
              throw new AssertionError();
            }
            temp6276.freeRef();
          } else {
            com.simiacryptus.refcount.test.BasicType temp9920 = entry.getValue();
            if (null == temp9920) {
              com.simiacryptus.lang.ref.RefUtil.freeRef(entry);
              return;
            }
            temp9920.freeRef();
          }
          com.simiacryptus.lang.ref.RefUtil.freeRef(targetMap.put(entry.getKey(), entry.getValue()));
          com.simiacryptus.lang.ref.RefUtil.freeRef(entry);
        }
      };
      com.simiacryptus.lang.ref.wrappers.RefHashSet<java.util.Map.Entry<java.lang.Integer, com.simiacryptus.refcount.test.BasicType>> temp5889 = values
          .entrySet();
      temp5889.forEach(com.simiacryptus.lang.ref.RefUtil.addRef(entryConsumer));
      temp5889.freeRef();
      com.simiacryptus.lang.ref.RefUtil.freeRef(entryConsumer);
      assert targetMap.size() == values.size();
      targetMap.freeRef();
      values.freeRef();
    });
  }

  @Override
  public String toString() {
    return "MapValuesContainer{" + "values=" + values + '}';
  }

  public void use() {
    System.out.println(String.format("Increment %s", this));
    for (int i = 0; i < TestOperations.count; i++) {
      testEntries();
      testBasicOperations(new com.simiacryptus.lang.ref.wrappers.RefHashMap<>(this.values.addRef()));
      testStreamOperations(new com.simiacryptus.lang.ref.wrappers.RefHashMap<>(this.values.addRef()));
    }
  }

  public @Override MapValuesContainer addRef() {
    return (MapValuesContainer) super.addRef();
  }

  public static MapValuesContainer[] addRefs(MapValuesContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(MapValuesContainer::addRef)
        .toArray((x) -> new MapValuesContainer[x]);
  }
}
