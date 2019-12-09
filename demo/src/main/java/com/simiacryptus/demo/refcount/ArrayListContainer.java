package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefConsumer;
import com.simiacryptus.ref.wrappers.RefHashSet;
import com.simiacryptus.ref.wrappers.RefList;
import com.simiacryptus.ref.wrappers.RefCollectors;
import com.simiacryptus.ref.wrappers.RefLists;
import org.jetbrains.annotations.NotNull;

public @com.simiacryptus.ref.lang.RefAware class ArrayListContainer extends ReferenceCountingBase {
  public com.simiacryptus.ref.wrappers.RefArrayList<BasicType> values = new com.simiacryptus.ref.wrappers.RefArrayList<>();

  public ArrayListContainer(BasicType... values) {
    for (BasicType value : values) {
      this.values.add(value.addRef());
    }
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(values);
  }

  private static void testOperations(RefConsumer<com.simiacryptus.ref.wrappers.RefArrayList<BasicType>> fn) {
    com.simiacryptus.ref.wrappers.RefArrayList<BasicType> values = new com.simiacryptus.ref.wrappers.RefArrayList<>();
    for (int i = 0; i < TestOperations.count; i++) {
      values.add(new BasicType());
    }
    fn.accept(values);
    fn.freeRef();
  }

  private static void testArrayOperations(com.simiacryptus.ref.wrappers.RefArrayList<BasicType> values) {
    if (0 == values.size()) {
      values.freeRef();
      throw new RuntimeException();
    }
    if (false) {
      if (values.size() != values.toArray().length) {
        values.freeRef();
        throw new RuntimeException();
      }
    }
    com.simiacryptus.demo.refcount.BasicType[] temp7531 = values.toArray(new BasicType[] {});
    if (values.size() != temp7531.length)
      throw new RuntimeException();
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp7531);
    values.freeRef();
  }

  private static void testElementOperations() {
    testOperations(values -> {
      values.clear();
      if (!values.isEmpty()) {
        values.freeRef();
        throw new RuntimeException();
      }
      final BasicType basicType1 = new BasicType();
      if (!values.add(basicType1.addRef())) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      if (!values.contains(basicType1.addRef())) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> temp7402 = values.iterator();
      if (!values.add(temp7402.next())) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      temp7402.freeRef();
      if (values.size() != 2) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      if (!values.remove(basicType1.addRef())) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      if (values.size() != 1) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      if (!values.add(basicType1.addRef())) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.remove(0));
      if (values.size() != 1) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.set(0, new BasicType()));
      if (values.size() != 1) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      values.clear();
      if (!values.isEmpty()) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      final BasicType[] basicTypeN = new BasicType[] { new BasicType(), new BasicType(), new BasicType() };
      values.addAll(
          com.simiacryptus.ref.wrappers.RefArrays.asList(com.simiacryptus.demo.refcount.BasicType.addRefs(basicTypeN)));
      values.add(1, basicType1.addRef());
      values.addAll(1,
          com.simiacryptus.ref.wrappers.RefArrays.asList(com.simiacryptus.demo.refcount.BasicType.addRefs(basicTypeN)));
      if (values.indexOf(basicType1) != 4) {
        values.freeRef();
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(basicTypeN);
        throw new RuntimeException();
      }
      if (values.indexOf(basicTypeN[0].addRef()) != 0) {
        values.freeRef();
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(basicTypeN);
        throw new RuntimeException();
      }
      if (values.lastIndexOf(basicTypeN[0].addRef()) != 1) {
        values.freeRef();
        throw new RuntimeException();
      }
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(basicTypeN);
      com.simiacryptus.ref.wrappers.RefArrayList<com.simiacryptus.demo.refcount.BasicType> temp6405 = values.subList(1,
          3);
      if (temp6405.size() != 2) {
        values.freeRef();
        throw new RuntimeException();
      }
      temp6405.freeRef();
      values.clear();
      if (!values.isEmpty())
        throw new RuntimeException();
      values.freeRef();
    });
  }

  private static void testCollectionOperations() {
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefList<java.lang.Integer> temp5958 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.mapping(x -> {
            x.freeRef();
            return x.value;
          }, com.simiacryptus.ref.wrappers.RefCollectors.toList()));
      assert values.size() == temp5958.size();
      temp5958.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefHashSet temp7771 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors
              .collectingAndThen(com.simiacryptus.ref.wrappers.RefCollectors.toList(), x -> {
                return new com.simiacryptus.ref.wrappers.RefHashSet(x);
              }));
      assert values.size() == temp7771.size();
      temp7771.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      assert values
          .size() == (int) values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.reducing(0, x -> {
            x.freeRef();
            return 1;
          }, (a, b) -> a + b));
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().map(x -> {
        final int value = x.value;
        RefUtil.freeRef(x);
        return value;
      }).collect(com.simiacryptus.ref.wrappers.RefCollectors.reducing((a, b) -> a + b)).get();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.mapping(x -> {
        x.freeRef();
        return 1;
      }, com.simiacryptus.ref.wrappers.RefCollectors.reducing((a, b) -> a + b))).get();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.mapping(x -> {
        x.freeRef();
        return 1;
      }, com.simiacryptus.ref.wrappers.RefCollectors.counting()));
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.counting());
      values.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.ref.wrappers.RefList<BasicType>> partitions = com.simiacryptus.ref.wrappers.RefLists
          .partition(values, 2);
      partitions.forEach(x -> {
        assert x != null;
        x.freeRef();
      });
      partitions.freeRef();
    });
    testOperations(values -> {
      values.add(new BasicType());
      final BasicType basicType = new BasicType();
      final com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> list = com.simiacryptus.ref.wrappers.RefArrays
          .asList(basicType);
      if (!values.addAll(list.addRef())) {
        values.freeRef();
        list.freeRef();
        throw new RuntimeException();
      }
      if (!values.containsAll(list.addRef())) {
        values.freeRef();
        list.freeRef();
        throw new RuntimeException();
      }
      if (!values.retainAll(list.addRef())) {
        values.freeRef();
        list.freeRef();
        throw new RuntimeException();
      }
      testArrayOperations(values.addRef());
      values.removeAll(list);
      values.clear();
      if (!values.isEmpty())
        throw new RuntimeException();
      values.freeRef();
    });
  }

  public @Override void _free() {
    if (null != values)
      values.freeRef();
    super._free();
  }

  public void test() {
    com.simiacryptus.ref.wrappers.RefArrayList<com.simiacryptus.demo.refcount.BasicType> temp4136 = new com.simiacryptus.ref.wrappers.RefArrayList<>(
        this.values.addRef());
    if (this.values.size() != temp4136.size())
      throw new RuntimeException();
    temp4136.freeRef();
    for (int i = 0; i < TestOperations.count; i++) {
      testCollectionOperations();
      testElementOperations();
      testIteratorOperations();
      testStreamOperations();
    }
  }

  private void testIteratorOperations() {
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iterator = values
          .iterator();
      values.freeRef();
      while (iterator.hasNext()) {
        com.simiacryptus.ref.lang.RefUtil.freeRef(iterator.next());
        iterator.remove();
      }
      iterator.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefListIterator<com.simiacryptus.demo.refcount.BasicType> iterator = values
          .listIterator();
      values.freeRef();
      assert 0 == iterator.nextIndex();
      if (iterator.hasNext()) {
        com.simiacryptus.ref.lang.RefUtil.freeRef(iterator.next());
        iterator.remove();
      }
      if (iterator.hasNext()) {
        com.simiacryptus.ref.lang.RefUtil.freeRef(iterator.next());
        iterator.add(new BasicType());
      }
      if (iterator.hasNext()) {
        com.simiacryptus.ref.lang.RefUtil.freeRef(iterator.next());
      }
      if (iterator.hasPrevious()) {
        assert 2 == iterator.previousIndex() : 2 + " != " + iterator.previousIndex();
        com.simiacryptus.ref.lang.RefUtil.freeRef(iterator.previous());
        iterator.set(new BasicType());
      }
      iterator.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefSpliterator<com.simiacryptus.demo.refcount.BasicType> spliterator = values
          .spliterator();
      values.freeRef();
      while (spliterator.tryAdvance(x -> {
        assert null != x;
        x.freeRef();
      })) {
      }
      spliterator.freeRef();
    });
  }

  private void testStreamOperations() {
    if (false)
      testOperations(values -> {
        assert values.size() == values.stream().flatMap(
            (java.util.function.Function<? super com.simiacryptus.demo.refcount.BasicType, ? extends com.simiacryptus.ref.wrappers.RefStream<? extends com.simiacryptus.demo.refcount.BasicType>>) com.simiacryptus.ref.lang.RefUtil
                .wrapInterface(
                    (java.util.function.Function<? super com.simiacryptus.demo.refcount.BasicType, ? extends com.simiacryptus.ref.wrappers.RefStream<? extends com.simiacryptus.demo.refcount.BasicType>>) x -> {
                      x.freeRef();
                      return values.stream();
                    }, values))
            .distinct().count();
      });
    testOperations(values_conditionalBlock -> {
      if (true) {
        com.simiacryptus.demo.refcount.BasicType[] temp5148 = values_conditionalBlock.stream()
            .toArray(i -> new BasicType[i]);
        assert values_conditionalBlock.size() == temp5148.length;
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp5148);
      }
      values_conditionalBlock.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp1462 = values.stream().map(x -> {
        x.value++;
        return x;
      }).sorted().toArray(i -> new BasicType[i]);
      assert values.size() == temp1462.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp1462);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp9669 = values.stream().flatMap(x -> {
        x.value++;
        com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp6739 = com.simiacryptus.ref.wrappers.RefStream
            .of(x);
        return temp6739;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp9669.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp9669);
      values.freeRef();
    });
    testOperations(values -> {
      values.stream().flatMap(x -> {
        x.value++;
        com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp9412 = com.simiacryptus.ref.wrappers.RefStream
            .of(x, new BasicType());
        return temp9412;
      }).forEach(x -> {
        assert x != null;
        x.freeRef();
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().allMatch(x -> {
        boolean temp4246 = x != null;
        x.freeRef();
        return temp4246;
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().peek(x -> {
        assert x != null;
        x.freeRef();
      }).count();
      values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp5214 = values.stream()
          .reduce((reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp5214.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp5214);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp4368 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp4368 != null;
      temp4368.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp1891 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          }, (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp1891 != null;
      temp1891.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp5664 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp2784 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values.size() == temp5664.size() : values.size() + " != " + temp2784.size();
      temp2784.freeRef();
      temp5664.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefSet<com.simiacryptus.demo.refcount.BasicType> temp6898 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toSet());
      assert values.size() == temp6898.size();
      temp6898.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().anyMatch(x -> {
        boolean temp3113 = x != null;
        x.freeRef();
        return temp3113;
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().noneMatch(x -> {
        boolean temp1797 = x == null;
        x.freeRef();
        return temp1797;
      });
      values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp7014 = values.stream().filter(x -> {
        boolean temp8415 = x == null;
        x.freeRef();
        return temp8415;
      }).findAny();
      assert !temp7014.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp7014);
      values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp4539 = values.stream().filter(x -> {
        boolean temp1936 = x != null;
        x.freeRef();
        return temp1936;
      }).findFirst();
      assert temp4539.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp4539);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iter = values.stream()
          .iterator();
      values.freeRef();
      while (iter.hasNext()) {
        com.simiacryptus.demo.refcount.BasicType temp4133 = iter.next();
        assert temp4133 != null;
        temp4133.freeRef();
      }
      iter.freeRef();
    });
    testOperations(values -> {
      @NotNull
      com.simiacryptus.ref.wrappers.RefSpliterator<com.simiacryptus.demo.refcount.BasicType> iter = values.stream()
          .spliterator();
      values.freeRef();
      while (iter.tryAdvance(x -> {
        assert x != null;
        x.freeRef();
      })) {
      }
      iter.freeRef();
    });
    testOperations(values -> {
      @NotNull
      com.simiacryptus.ref.wrappers.RefListIterator<com.simiacryptus.demo.refcount.BasicType> iter = values
          .listIterator();
      values.freeRef();
      while (iter.hasNext()) {
        com.simiacryptus.demo.refcount.BasicType temp4339 = iter.next();
        assert temp4339 != null;
        temp4339.freeRef();
      }
      iter.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp2615 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp2615.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp2615);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp9090 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp9090.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp9090);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp9138 = values.stream().map(x -> {
        x.value++;
        return x;
      }).sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp9138.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp9138);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp2615 = values.stream()
          .max(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
            int temp2453 = x.value;
            x.freeRef();
            return temp2453;
          })).get();
      assert null != temp2615;
      temp2615.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp4261 = values.stream()
          .min(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
            int temp5040 = x.value;
            x.freeRef();
            return temp5040;
          })).get();
      assert null != temp4261;
      temp4261.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      if (values.size() > 4 && values.stream().skip(1).limit(5).count() != 4) {
        values.freeRef();
        throw new AssertionError();
      }
      values.stream().forEach(x -> {
        x.value++;
        x.freeRef();
      });
      values.freeRef();
    });
    testOperations(values -> {
      values.stream().parallel().forEach(x -> {
        x.value++;
        x.freeRef();
      });
      values.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefStream<BasicType> parallel = values.stream().parallel();
      values.freeRef();
      if (!parallel.isParallel())
        throw new AssertionError();
      parallel.close();
    });
    testOperations(values -> {
      values.stream().forEachOrdered(x -> {
        x.value++;
        x.freeRef();
      });
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp6600 = values.stream().sequential().unordered().map(x -> {
        x.value++;
        return x;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp6600.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp6600);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp3403 = foobar1.value;
        foobar1.freeRef();
        return temp3403;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(x -> {
        int temp8891 = x.value;
        x.freeRef();
        return temp8891;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(x -> {
        int temp4919 = x.value;
        x.freeRef();
        return temp4919;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp7843 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp7843;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToDouble(x -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp1800 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(x.value);
        x.freeRef();
        return temp1800;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      final long[] longs = values.stream().flatMapToLong(x -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp8746 = com.simiacryptus.ref.wrappers.RefLongStream.of(x.value);
        x.freeRef();
        return temp8746;
      }).toArray();
      assert values.size() == longs.length;
      values.freeRef();
    });
  }

  @Override
  public String toString() {
    return "ArrayListContainer{" + "values=" + values + '}';
  }

  public @Override ArrayListContainer addRef() {
    return (ArrayListContainer) super.addRef();
  }

  public static ArrayListContainer[] addRefs(ArrayListContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ArrayListContainer::addRef)
        .toArray((x) -> new ArrayListContainer[x]);
  }

  public static ArrayListContainer[][] addRefs(ArrayListContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ArrayListContainer::addRefs)
        .toArray((x) -> new ArrayListContainer[x][]);
  }
}
