package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefConsumer;
import org.jetbrains.annotations.NotNull;

import java.util.Spliterator;

public @com.simiacryptus.ref.lang.RefAware
class ListContainer extends ReferenceCountingBase {
  public com.simiacryptus.ref.wrappers.RefArrayList<BasicType> values = new com.simiacryptus.ref.wrappers.RefArrayList<>();

  public ListContainer(BasicType... values) {
    for (BasicType value : values) {
      this.values.add(value.addRef());
    }
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(values);
  }

  private static void testOperations(RefConsumer<com.simiacryptus.ref.wrappers.RefArrayList<BasicType>> fn) {
    com.simiacryptus.ref.wrappers.RefArrayList<BasicType> values = new com.simiacryptus.ref.wrappers.RefArrayList<>();
    for (int i = 0; i < 5; i++) {
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
    com.simiacryptus.demo.refcount.BasicType[] temp2295 = values.toArray(new BasicType[]{});
    if (values.size() != temp2295.length)
      throw new RuntimeException();
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp2295);
    values.freeRef();
  }

  private static void testElementOperations(com.simiacryptus.ref.wrappers.RefArrayList<BasicType> values) {
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
    if (!values.add(values.iterator().next())) {
      values.freeRef();
      basicType1.freeRef();
      throw new RuntimeException();
    }
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
    final BasicType[] basicTypeN = new BasicType[]{new BasicType(), new BasicType(), new BasicType()};
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
    com.simiacryptus.ref.wrappers.RefArrayList<com.simiacryptus.demo.refcount.BasicType> temp2710 = values.subList(1,
        3);
    if (temp2710.size() != 2) {
      values.freeRef();
      throw new RuntimeException();
    }
    temp2710.freeRef();
    values.clear();
    if (!values.isEmpty())
      throw new RuntimeException();
    values.freeRef();
  }

  private static void testCollectionOperations(com.simiacryptus.ref.wrappers.RefArrayList<BasicType> values) {
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
  }

  public static ListContainer[] addRefs(ListContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ListContainer::addRef)
        .toArray((x) -> new ListContainer[x]);
  }

  public static ListContainer[][] addRefs(ListContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ListContainer::addRefs)
        .toArray((x) -> new ListContainer[x][]);
  }

  public @Override
  void _free() {
    if (null != values)
      values.freeRef();
    super._free();
  }

  public @Override
  ListContainer addRef() {
    return (ListContainer) super.addRef();
  }

  public void test() {
    System.out.println(String.format("Increment %s", this));
    com.simiacryptus.ref.wrappers.RefArrayList<com.simiacryptus.demo.refcount.BasicType> temp3702 = new com.simiacryptus.ref.wrappers.RefArrayList<>(
        this.values.addRef());
    if (this.values.size() != temp3702.size())
      throw new RuntimeException();
    temp3702.freeRef();
    for (int i = 0; i < TestOperations.count; i++) {
      testCollectionOperations(new com.simiacryptus.ref.wrappers.RefArrayList<>(this.values.addRef()));
      testElementOperations(new com.simiacryptus.ref.wrappers.RefArrayList<>());
      testStreamOperations();
      testIteratorOperations();
    }
  }

  private void testIteratorOperations() {
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      final com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iterator = values
          .iterator();
      values.freeRef();
      while (iterator.hasNext()) {
        com.simiacryptus.ref.lang.RefUtil.freeRef(iterator.next());
        iterator.remove();
      }
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
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
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      final com.simiacryptus.ref.wrappers.RefSpliterator<com.simiacryptus.demo.refcount.BasicType> spliterator = values
          .spliterator();
      values.freeRef();
      final Spliterator<BasicType> split = spliterator.trySplit();
      if (null != split)
        while (split.tryAdvance(x -> {
          assert null != x;
          x.freeRef();
        })) {
        }
      com.simiacryptus.ref.lang.RefUtil.freeRef(split);
      while (spliterator.tryAdvance(x -> {
        assert null != x;
        x.freeRef();
      })) {
      }
    }));
  }

  private void testStreamOperations() {
    if (false)
      testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
        assert values.size() == values.stream().flatMap(com.simiacryptus.ref.lang.RefUtil.wrapInterface(
            (java.util.function.Function<? super com.simiacryptus.demo.refcount.BasicType, ? extends java.util.stream.Stream<? extends com.simiacryptus.demo.refcount.BasicType>>) x -> {
              x.freeRef();
              return values.stream();
            }, values)).distinct().count();
      }));
    if (false)
      testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values_conditionalBlock -> {
        if (true) {
          com.simiacryptus.demo.refcount.BasicType[] temp2641 = values_conditionalBlock.stream()
              .toArray(i -> new BasicType[i]);
          assert values_conditionalBlock.size() == temp2641.length;
          com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp2641);
        }
        values_conditionalBlock.freeRef();
      }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp8036 = values.stream().map(x -> {
        x.value++;
        return x;
      }).sorted().toArray(i -> new BasicType[i]);
      assert values.size() == temp8036.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp8036);
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp9406 = values.stream().flatMap(x -> {
        x.value++;
        com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp2807 = com.simiacryptus.ref.wrappers.RefStream
            .of(x);
        return temp2807;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp9406.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp9406);
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      values.stream().flatMap(x -> {
        x.value++;
        com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp5637 = com.simiacryptus.ref.wrappers.RefStream
            .of(x, new BasicType());
        return temp5637;
      }).forEach(x -> {
        assert x != null;
        x.freeRef();
      });
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      assert values.stream().allMatch(x -> {
        boolean temp7587 = x != null;
        x.freeRef();
        return temp7587;
      });
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      values.stream().peek(x -> {
        assert x != null;
        x.freeRef();
      });
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp9172 = values.stream()
          .reduce((reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp9172.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp9172);
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      com.simiacryptus.demo.refcount.BasicType temp9028 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp9028 != null;
      temp9028.freeRef();
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      com.simiacryptus.demo.refcount.BasicType temp1290 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          }, (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp1290 != null;
      temp1290.freeRef();
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp5505 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values.size() == temp5505.size();
      temp5505.freeRef();
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      com.simiacryptus.ref.wrappers.RefSet<com.simiacryptus.demo.refcount.BasicType> temp3682 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toSet());
      assert values.size() == temp3682.size();
      temp3682.freeRef();
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      assert values.stream().anyMatch(x -> {
        boolean temp5036 = x != null;
        x.freeRef();
        return temp5036;
      });
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      assert values.stream().noneMatch(x -> {
        boolean temp8827 = x == null;
        x.freeRef();
        return temp8827;
      });
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp2767 = values.stream().filter(x -> {
        boolean temp7912 = x == null;
        x.freeRef();
        return temp7912;
      }).findAny();
      assert !temp2767.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp2767);
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp5921 = values.stream().filter(x -> {
        boolean temp2106 = x != null;
        x.freeRef();
        return temp2106;
      }).findFirst();
      assert temp5921.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp5921);
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iter = values.stream()
          .iterator();
      values.freeRef();
      while (iter.hasNext()) {
        com.simiacryptus.demo.refcount.BasicType temp3000 = iter.next();
        assert temp3000 != null;
        temp3000.freeRef();
      }
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      @NotNull
      com.simiacryptus.ref.wrappers.RefSpliterator<com.simiacryptus.demo.refcount.BasicType> iter = values.stream()
          .spliterator();
      values.freeRef();
      while (iter.tryAdvance(x -> {
        assert x != null;
        x.freeRef();
      })) {
      }
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      @NotNull
      com.simiacryptus.ref.wrappers.RefListIterator<com.simiacryptus.demo.refcount.BasicType> iter = values
          .listIterator();
      values.freeRef();
      while (iter.hasNext()) {
        com.simiacryptus.demo.refcount.BasicType temp4800 = iter.next();
        assert temp4800 != null;
        temp4800.freeRef();
      }
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp5030 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp5030.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp5030);
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp7240 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp7240.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp7240);
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp7977 = values.stream().map(x -> {
        x.value++;
        return x;
      }).sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp7977.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp7977);
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      com.simiacryptus.demo.refcount.BasicType temp2562 = values.stream()
          .max(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
            int temp8441 = x.value;
            x.freeRef();
            return temp8441;
          })).get();
      assert null != temp2562;
      temp2562.freeRef();
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      com.simiacryptus.demo.refcount.BasicType temp6161 = values.stream()
          .min(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
            int temp7043 = x.value;
            x.freeRef();
            return temp7043;
          })).get();
      assert null != temp6161;
      temp6161.freeRef();
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      if (values.size() > 4 && values.stream().skip(1).limit(5).count() != 4) {
        values.freeRef();
        throw new AssertionError();
      }
      values.stream().forEach(x -> {
        x.value++;
        x.freeRef();
      });
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      values.stream().parallel().forEach(x -> {
        x.value++;
        x.freeRef();
      });
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      if (!values.stream().parallel().isParallel())
        throw new AssertionError();
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      values.stream().forEachOrdered(x -> {
        x.value++;
        x.freeRef();
      });
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp3067 = values.stream().sequential().unordered().map(x -> {
        x.value++;
        return x;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp3067.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp3067);
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp6553 = foobar1.value;
        foobar1.freeRef();
        return temp6553;
      }).toArray().length;
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      assert values.size() == values.stream().mapToDouble(x -> {
        int temp3224 = x.value;
        x.freeRef();
        return temp3224;
      }).toArray().length;
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      assert values.size() == values.stream().mapToLong(x -> {
        int temp7902 = x.value;
        x.freeRef();
        return temp7902;
      }).toArray().length;
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      assert values.size() == values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp7636 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp7636;
      }).toArray().length;
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      assert values.size() == values.stream().flatMapToDouble(x -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp5027 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(x.value);
        x.freeRef();
        return temp5027;
      }).toArray().length;
      values.freeRef();
    }));
    testOperations(com.simiacryptus.ref.lang.RefUtil.addRef(values -> {
      assert values.size() == values.stream().flatMapToLong(x -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp6884 = com.simiacryptus.ref.wrappers.RefLongStream.of(x.value);
        x.freeRef();
        return temp6884;
      }).toArray().length;
      values.freeRef();
    }));
  }

  @Override
  public String toString() {
    return "ListContainer{" + "values=" + values + '}';
  }
}
