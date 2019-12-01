package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public @com.simiacryptus.lang.ref.RefAware class ListContainer extends ReferenceCountingBase {
  public com.simiacryptus.lang.ref.wrappers.RefArrayList<BasicType> values = new com.simiacryptus.lang.ref.wrappers.RefArrayList<>();

  public ListContainer(BasicType... values) {
    for (BasicType value : values) {
      this.values.add(value.addRef());
    }
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(values);
  }

  private static void testOperations(Consumer<com.simiacryptus.lang.ref.wrappers.RefArrayList<BasicType>> fn) {
    com.simiacryptus.lang.ref.wrappers.RefArrayList<BasicType> values = new com.simiacryptus.lang.ref.wrappers.RefArrayList<>();
    for (int i = 0; i < 5; i++)
      values.add(new BasicType());
    fn.accept(values);
    com.simiacryptus.lang.ref.RefUtil.freeRef(fn);
  }

  private static void testArrayOperations(com.simiacryptus.lang.ref.wrappers.RefArrayList<BasicType> values) {
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
    com.simiacryptus.refcount.test.BasicType[] temp5767 = values.toArray(new BasicType[] {});
    if (values.size() != temp5767.length)
      throw new RuntimeException();
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp5767);
    values.freeRef();
  }

  private static void testElementOperations(com.simiacryptus.lang.ref.wrappers.RefArrayList<BasicType> values) {
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
    java.util.Iterator<com.simiacryptus.refcount.test.BasicType> temp2669 = values.iterator();
    if (!values.add(temp2669.next())) {
      values.freeRef();
      basicType1.freeRef();
      throw new RuntimeException();
    }
    com.simiacryptus.lang.ref.RefUtil.freeRef(temp2669);
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
    com.simiacryptus.lang.ref.RefUtil.freeRef(values.remove(0));
    if (values.size() != 1) {
      values.freeRef();
      basicType1.freeRef();
      throw new RuntimeException();
    }
    com.simiacryptus.lang.ref.RefUtil.freeRef(values.set(0, new BasicType()));
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
    values.addAll(com.simiacryptus.lang.ref.wrappers.RefArrays
        .asList(com.simiacryptus.refcount.test.BasicType.addRefs(basicTypeN)));
    values.add(1, basicType1.addRef());
    values.addAll(1, com.simiacryptus.lang.ref.wrappers.RefArrays
        .asList(com.simiacryptus.refcount.test.BasicType.addRefs(basicTypeN)));
    if (values.indexOf(basicType1) != 4) {
      values.freeRef();
      com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(basicTypeN);
      throw new RuntimeException();
    }
    if (values.indexOf(basicTypeN[0].addRef()) != 0) {
      values.freeRef();
      com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(basicTypeN);
      throw new RuntimeException();
    }
    if (values.lastIndexOf(basicTypeN[0].addRef()) != 1) {
      values.freeRef();
      throw new RuntimeException();
    }
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(basicTypeN);
    com.simiacryptus.lang.ref.wrappers.RefArrayList<com.simiacryptus.refcount.test.BasicType> temp6400 = values
        .subList(1, 3);
    if (temp6400.size() != 2) {
      values.freeRef();
      throw new RuntimeException();
    }
    temp6400.freeRef();
    values.clear();
    if (!values.isEmpty())
      throw new RuntimeException();
    values.freeRef();
  }

  private static void testCollectionOperations(com.simiacryptus.lang.ref.wrappers.RefArrayList<BasicType> values) {
    values.add(new BasicType());
    final BasicType basicType = new BasicType();
    final com.simiacryptus.lang.ref.wrappers.RefList<BasicType> list = com.simiacryptus.lang.ref.wrappers.RefArrays
        .asList(basicType);
    if (!values.addAll(com.simiacryptus.lang.ref.RefUtil.addRef(list))) {
      values.freeRef();
      list.freeRef();
      throw new RuntimeException();
    }
    if (!values.containsAll(com.simiacryptus.lang.ref.RefUtil.addRef(list))) {
      values.freeRef();
      list.freeRef();
      throw new RuntimeException();
    }
    if (!values.retainAll(com.simiacryptus.lang.ref.RefUtil.addRef(list))) {
      values.freeRef();
      list.freeRef();
      throw new RuntimeException();
    }
    testArrayOperations(values.addRef());
    values.removeAll(com.simiacryptus.lang.ref.RefUtil.addRef(list));
    list.freeRef();
    values.clear();
    if (!values.isEmpty())
      throw new RuntimeException();
    values.freeRef();
  }

  public @Override void _free() {
    if (null != values)
      values.freeRef();
    super._free();
  }

  public void test() {
    System.out.println(String.format("Increment %s", this));
    com.simiacryptus.lang.ref.wrappers.RefArrayList<com.simiacryptus.refcount.test.BasicType> temp3468 = new com.simiacryptus.lang.ref.wrappers.RefArrayList<>(
        this.values.addRef());
    if (this.values.size() != temp3468.size())
      throw new RuntimeException();
    temp3468.freeRef();
    for (int i = 0; i < TestOperations.count; i++) {
      testCollectionOperations(new com.simiacryptus.lang.ref.wrappers.RefArrayList<>(this.values.addRef()));
      testElementOperations(new com.simiacryptus.lang.ref.wrappers.RefArrayList<>());
      testStreamOperations();
      testIteratorOperations();
    }
  }

  private void testIteratorOperations() {
    testOperations(values -> {
      final Iterator<BasicType> iterator = values.iterator();
      values.freeRef();
      while (iterator.hasNext()) {
        com.simiacryptus.lang.ref.RefUtil.freeRef(iterator.next());
        iterator.remove();
      }
      com.simiacryptus.lang.ref.RefUtil.freeRef(iterator);
    });
    testOperations(values -> {
      final ListIterator<BasicType> iterator = values.listIterator();
      values.freeRef();
      assert 0 == iterator.nextIndex();
      if (iterator.hasNext()) {
        com.simiacryptus.lang.ref.RefUtil.freeRef(iterator.next());
        iterator.remove();
      }
      if (iterator.hasNext()) {
        com.simiacryptus.lang.ref.RefUtil.freeRef(iterator.next());
        iterator.add(new BasicType());
      }
      if (iterator.hasNext()) {
        com.simiacryptus.lang.ref.RefUtil.freeRef(iterator.next());
      }
      if (iterator.hasPrevious()) {
        assert 1 == iterator.previousIndex() : 1 + " != " + iterator.previousIndex();
        com.simiacryptus.lang.ref.RefUtil.freeRef(iterator.previous());
        iterator.set(new BasicType());
      }
      com.simiacryptus.lang.ref.RefUtil.freeRef(iterator);
    });
    testOperations(values -> {
      final Spliterator<BasicType> spliterator = values.spliterator();
      values.freeRef();
      final Spliterator<BasicType> split = spliterator.trySplit();
      if (null != split)
        while (split.tryAdvance(x -> {
          assert null != x;
          x.freeRef();
        })) {
        }
      com.simiacryptus.lang.ref.RefUtil.freeRef(split);
      while (spliterator.tryAdvance(x -> {
        assert null != x;
        x.freeRef();
      })) {
      }
      com.simiacryptus.lang.ref.RefUtil.freeRef(spliterator);
    });
  }

  private void testStreamOperations() {
    if (false)
      testOperations(values -> {
        com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp3089 = values
            .stream();
        com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp8723 = temp3089
            .flatMap(x -> {
              x.freeRef();
              return values.stream();
            });
        com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp9076 = temp8723
            .distinct();
        assert values.size() == temp9076.count();
        com.simiacryptus.lang.ref.RefUtil.freeRef(temp9076);
        com.simiacryptus.lang.ref.RefUtil.freeRef(temp8723);
        com.simiacryptus.lang.ref.RefUtil.freeRef(temp3089);
        values.freeRef();
      });
    if (false)
      testOperations(values_conditionalBlock -> {
        if (true) {
          com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp9045 = values_conditionalBlock
              .stream();
          com.simiacryptus.refcount.test.BasicType[] temp1535 = temp9045.toArray(i -> new BasicType[i]);
          assert values_conditionalBlock.size() == temp1535.length;
          com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp1535);
          com.simiacryptus.lang.ref.RefUtil.freeRef(temp9045);
        }
        values_conditionalBlock.freeRef();
      });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp5348 = values.stream();
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp4680 = temp5348
          .map(x -> {
            x.value++;
            return x;
          });
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp5852 = temp4680
          .sorted();
      com.simiacryptus.refcount.test.BasicType[] temp4980 = temp5852.toArray(i -> new BasicType[i]);
      assert values.size() == temp4980.length;
      com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp4980);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp5852);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp4680);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp5348);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp3132 = values.stream();
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp4755 = temp3132
          .flatMap(x -> {
            x.value++;
            com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp2710 = com.simiacryptus.lang.ref.wrappers.RefStream
                .of(x);
            return temp2710;
          });
      com.simiacryptus.refcount.test.BasicType[] temp6550 = temp4755.toArray(i -> new BasicType[i]);
      assert values.size() == temp6550.length;
      com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp6550);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp4755);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp3132);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp1502 = values.stream();
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp2200 = temp1502
          .flatMap(x -> {
            x.value++;
            com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp6915 = com.simiacryptus.lang.ref.wrappers.RefStream
                .of(x, new BasicType());
            return temp6915;
          });
      temp2200.forEach(x -> {
        assert x != null;
        x.freeRef();
      });
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp2200);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp1502);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp5215 = values.stream();
      assert temp5215.allMatch(x -> {
        boolean temp8710 = x != null;
        x.freeRef();
        return temp8710;
      });
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp5215);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp2710 = values.stream();
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp2710.peek(x -> {
        assert x != null;
        x.freeRef();
      }));
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp2710);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp7724 = values.stream();
      java.util.Optional<com.simiacryptus.refcount.test.BasicType> temp6200 = temp7724
          .reduce((reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp6200.isPresent();
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp6200);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp7724);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp7640 = values.stream();
      com.simiacryptus.refcount.test.BasicType temp9627 = temp7640.reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp9627 != null;
      temp9627.freeRef();
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp7640);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp2893 = values.stream();
      com.simiacryptus.refcount.test.BasicType temp3121 = temp2893.reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          }, (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp3121 != null;
      temp3121.freeRef();
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp2893);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp2762 = values.stream();
      com.simiacryptus.lang.ref.wrappers.RefList<com.simiacryptus.refcount.test.BasicType> temp7184 = temp2762
          .collect(com.simiacryptus.lang.ref.wrappers.RefCollectors.toList());
      assert values.size() == temp7184.size();
      temp7184.freeRef();
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp2762);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp9409 = values.stream();
      com.simiacryptus.lang.ref.wrappers.RefSet<com.simiacryptus.refcount.test.BasicType> temp6742 = temp9409
          .collect(com.simiacryptus.lang.ref.wrappers.RefCollectors.toSet());
      assert values.size() == temp6742.size();
      temp6742.freeRef();
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp9409);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp8307 = values.stream();
      assert temp8307.anyMatch(x -> {
        boolean temp8366 = x != null;
        x.freeRef();
        return temp8366;
      });
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp8307);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp4863 = values.stream();
      assert temp4863.noneMatch(x -> {
        boolean temp4094 = x == null;
        x.freeRef();
        return temp4094;
      });
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp4863);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp9634 = values.stream();
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp8167 = temp9634
          .filter(x -> {
            boolean temp4100 = x == null;
            x.freeRef();
            return temp4100;
          });
      java.util.Optional<com.simiacryptus.refcount.test.BasicType> temp7293 = temp8167.findAny();
      assert !temp7293.isPresent();
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp7293);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp8167);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp9634);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp5564 = values.stream();
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp7059 = temp5564
          .filter(x -> {
            boolean temp4418 = x != null;
            x.freeRef();
            return temp4418;
          });
      java.util.Optional<com.simiacryptus.refcount.test.BasicType> temp5253 = temp7059.findFirst();
      assert temp5253.isPresent();
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp5253);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp7059);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp5564);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp1215 = values.stream();
      Iterator<BasicType> iter = temp1215.iterator();
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp1215);
      values.freeRef();
      while (iter.hasNext()) {
        com.simiacryptus.refcount.test.BasicType temp8856 = iter.next();
        assert temp8856 != null;
        temp8856.freeRef();
      }
      com.simiacryptus.lang.ref.RefUtil.freeRef(iter);
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp4871 = values.stream();
      @NotNull
      Spliterator<BasicType> iter = temp4871.spliterator();
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp4871);
      values.freeRef();
      while (iter.tryAdvance(x -> {
        assert x != null;
        x.freeRef();
      })) {
      }
      com.simiacryptus.lang.ref.RefUtil.freeRef(iter);
    });
    testOperations(values -> {
      @NotNull
      ListIterator<BasicType> iter = values.listIterator();
      values.freeRef();
      while (iter.hasNext()) {
        com.simiacryptus.refcount.test.BasicType temp1172 = iter.next();
        assert temp1172 != null;
        temp1172.freeRef();
      }
      com.simiacryptus.lang.ref.RefUtil.freeRef(iter);
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp7376 = values.stream();
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp5017 = temp7376
          .sorted(com.simiacryptus.lang.ref.wrappers.RefComparator.naturalOrder());
      com.simiacryptus.refcount.test.BasicType[] temp4116 = temp5017.toArray(i -> new BasicType[i]);
      assert values.size() == temp4116.length;
      com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp4116);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp5017);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp7376);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp8989 = values.stream();
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp2069 = temp8989
          .sorted(com.simiacryptus.lang.ref.wrappers.RefComparator.naturalOrder());
      com.simiacryptus.refcount.test.BasicType[] temp6582 = temp2069.toArray(i -> new BasicType[i]);
      assert values.size() == temp6582.length;
      com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp6582);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp2069);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp8989);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp1798 = values.stream();
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp7406 = temp1798
          .map(x -> {
            x.value++;
            return x;
          });
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp4787 = temp7406
          .sorted(com.simiacryptus.lang.ref.wrappers.RefComparator.naturalOrder());
      com.simiacryptus.refcount.test.BasicType[] temp2156 = temp4787.toArray(i -> new BasicType[i]);
      assert values.size() == temp2156.length;
      com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp2156);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp4787);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp7406);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp1798);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp1268 = values.stream();
      com.simiacryptus.refcount.test.BasicType temp3595 = temp1268
          .max(com.simiacryptus.lang.ref.wrappers.RefComparator.comparing(x -> {
            int temp6344 = x.value;
            x.freeRef();
            return temp6344;
          })).get();
      assert null != temp3595;
      temp3595.freeRef();
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp1268);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp7151 = values.stream();
      com.simiacryptus.refcount.test.BasicType temp4780 = temp7151
          .min(com.simiacryptus.lang.ref.wrappers.RefComparator.comparing(x -> {
            int temp4685 = x.value;
            x.freeRef();
            return temp4685;
          })).get();
      assert null != temp4780;
      temp4780.freeRef();
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp7151);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp7295 = values.stream();
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp4959 = temp7295
          .skip(1);
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp6465 = temp4959
          .limit(5);
      if (values.size() > 4 && temp6465.count() != 4) {
        values.freeRef();
        throw new AssertionError();
      }
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp6465);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp4959);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp7295);
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp8312 = values.stream();
      temp8312.forEach(x -> {
        x.value++;
        x.freeRef();
      });
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp8312);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp8469 = values.stream();
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp7391 = temp8469
          .parallel();
      temp7391.forEach(x -> {
        x.value++;
        x.freeRef();
      });
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp7391);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp8469);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp1651 = values.stream();
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp3892 = temp1651
          .parallel();
      if (!temp3892.isParallel())
        throw new AssertionError();
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp3892);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp1651);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp6173 = values.stream();
      temp6173.forEachOrdered(x -> {
        x.value++;
        x.freeRef();
      });
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp6173);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp3461 = values.stream();
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp7009 = temp3461
          .sequential();
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp1977 = temp7009
          .unordered();
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp6369 = temp1977
          .map(x -> {
            x.value++;
            return x;
          });
      com.simiacryptus.refcount.test.BasicType[] temp2365 = temp6369.toArray(i -> new BasicType[i]);
      assert values.size() == temp2365.length;
      com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp2365);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp6369);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp1977);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp7009);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp3461);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp8054 = values.stream();
      java.util.stream.IntStream temp2245 = temp8054.mapToInt(foobar1 -> {
        int temp5356 = foobar1.value;
        foobar1.freeRef();
        return temp5356;
      });
      assert values.size() == temp2245.toArray().length;
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp2245);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp8054);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp8078 = values.stream();
      java.util.stream.DoubleStream temp6523 = temp8078.mapToDouble(x -> {
        int temp8691 = x.value;
        x.freeRef();
        return temp8691;
      });
      assert values.size() == temp6523.toArray().length;
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp6523);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp8078);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp8517 = values.stream();
      java.util.stream.LongStream temp7630 = temp8517.mapToLong(x -> {
        int temp6989 = x.value;
        x.freeRef();
        return temp6989;
      });
      assert values.size() == temp7630.toArray().length;
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp7630);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp8517);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp2173 = values.stream();
      java.util.stream.IntStream temp8970 = temp2173.flatMapToInt(foobar1 -> {
        java.util.stream.IntStream temp5832 = IntStream.of(foobar1.value);
        foobar1.freeRef();
        return temp5832;
      });
      assert values.size() == temp8970.toArray().length;
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp8970);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp2173);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp4994 = values.stream();
      java.util.stream.DoubleStream temp2785 = temp4994.flatMapToDouble(x -> {
        java.util.stream.DoubleStream temp5504 = DoubleStream.of(x.value);
        x.freeRef();
        return temp5504;
      });
      assert values.size() == temp2785.toArray().length;
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp2785);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp4994);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefStream<com.simiacryptus.refcount.test.BasicType> temp6559 = values.stream();
      java.util.stream.LongStream temp3276 = temp6559.flatMapToLong(x -> {
        java.util.stream.LongStream temp6667 = LongStream.of(x.value);
        x.freeRef();
        return temp6667;
      });
      assert values.size() == temp3276.toArray().length;
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp3276);
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp6559);
      values.freeRef();
    });
  }

  @Override
  public String toString() {
    return "ListContainer{" + "values=" + values + '}';
  }

  public @Override ListContainer addRef() {
    return (ListContainer) super.addRef();
  }

  public static ListContainer[] addRefs(ListContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ListContainer::addRef)
        .toArray((x) -> new ListContainer[x]);
  }
}
