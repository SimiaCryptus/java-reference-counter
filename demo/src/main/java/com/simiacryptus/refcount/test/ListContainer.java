package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import com.simiacryptus.lang.ref.wrappers.RefStream;
import org.jetbrains.annotations.NotNull;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class ListContainer extends ReferenceCountingBase {
  public com.simiacryptus.lang.ref.wrappers.RefArrayList<BasicType> values = new com.simiacryptus.lang.ref.wrappers.RefArrayList<>();

  public ListContainer(BasicType... values) {
    for (BasicType value : values) {
      this.values.add(value.addRef());
    }
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(values);
  }

  public void test() {
    System.out.println(String.format("Increment %s", this));
    com.simiacryptus.lang.ref.wrappers.RefArrayList<com.simiacryptus.refcount.test.BasicType> temp6502 = new com.simiacryptus.lang.ref.wrappers.RefArrayList<>(
        this.values.addRef());
    if (this.values.size() != temp6502.size())
      throw new RuntimeException();
    temp6502.freeRef();
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
    });
    testOperations(values -> {
      final ListIterator<BasicType> iterator = values.listIterator();
      values.freeRef();
      assert 0 == iterator.nextIndex();
      if (iterator.hasNext()) {
        com.simiacryptus.lang.ref.RefUtil.freeRef(iterator.next());
        iterator.remove();
      }
      iterator.add(new BasicType());
      if (iterator.hasNext()) {
        com.simiacryptus.lang.ref.RefUtil.freeRef(iterator.next());
      }
      assert 1 == iterator.previousIndex() : 1 + " != " + iterator.previousIndex();
      if (iterator.hasPrevious()) {
        com.simiacryptus.lang.ref.RefUtil.freeRef(iterator.previous());
        iterator.set(new BasicType());
      }
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
      while (spliterator.tryAdvance(x -> {
        assert null != x;
        x.freeRef();
      })) {
      }
    });
  }

  private void testStreamOperations() {
    testOperations(values -> {
      com.simiacryptus.refcount.test.BasicType[] temp8251 = values.stream().map(x -> {
        x.value++;
        return x;
      }).sorted().toArray(i -> new BasicType[i]);
      assert values.size() == temp8251.length;
      com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp8251);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.refcount.test.BasicType[] temp3614 = values.stream().flatMap(x -> {
        x.value++;
        RefStream<BasicType> temp9066 = com.simiacryptus.lang.ref.wrappers.RefStream.of(x.addRef());
        x.freeRef();
        return temp9066;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp3614.length;
      com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp3614);
      values.freeRef();
    });
    testOperations(values -> {
      values.stream().flatMap(x -> {
        x.value++;
        RefStream<BasicType> temp4960 = com.simiacryptus.lang.ref.wrappers.RefStream.of(x.addRef(), new BasicType());
        x.freeRef();
        return temp4960;
      }).forEach(x -> {
        assert x != null;
        x.freeRef();
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().allMatch(x -> {
        boolean temp1135 = x != null;
        x.freeRef();
        return temp1135;
      });
      values.freeRef();
    });
    testOperations(values -> {
      values.stream().peek(x -> {
        assert x != null;
        x.freeRef();
      });
      values.freeRef();
    });
    if (false)
      testOperations(values -> {
        assert values.size() == values.stream().flatMap(x -> {
          x.freeRef();
          return values.stream();
        }).distinct().count();
        values.freeRef();
      });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.refcount.test.BasicType> temp7211 = values.stream()
          .reduce((reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp7211.isPresent();
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp7211);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.refcount.test.BasicType temp5986 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp5986 != null;
      temp5986.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.refcount.test.BasicType temp6307 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          }, (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp6307 != null;
      temp6307.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefList<com.simiacryptus.refcount.test.BasicType> temp8769 = values.stream()
          .collect(com.simiacryptus.lang.ref.wrappers.RefCollectors.toList());
      assert values.size() == temp8769.size();
      temp8769.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.lang.ref.wrappers.RefSet<com.simiacryptus.refcount.test.BasicType> temp2063 = values.stream()
          .collect(com.simiacryptus.lang.ref.wrappers.RefCollectors.toSet());
      assert values.size() == temp2063.size();
      temp2063.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().anyMatch(x -> {
        boolean temp6739 = x != null;
        x.freeRef();
        return temp6739;
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().noneMatch(x -> {
        boolean temp1704 = x == null;
        x.freeRef();
        return temp1704;
      });
      values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.refcount.test.BasicType> temp4232 = values.stream().filter(x -> {
        boolean temp3799 = x == null;
        x.freeRef();
        return temp3799;
      }).findAny();
      assert !temp4232.isPresent();
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp4232);
      values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.refcount.test.BasicType> temp8075 = values.stream().filter(x -> {
        boolean temp7241 = x != null;
        x.freeRef();
        return temp7241;
      }).findFirst();
      assert temp8075.isPresent();
      com.simiacryptus.lang.ref.RefUtil.freeRef(temp8075);
      values.freeRef();
    });
    testOperations(values -> {
      Iterator<BasicType> iter = values.stream().iterator();
      values.freeRef();
      while (iter.hasNext()) {
        com.simiacryptus.refcount.test.BasicType temp3368 = iter.next();
        assert temp3368 != null;
        temp3368.freeRef();
      }
    });
    testOperations(values -> {
      @NotNull
      Spliterator<BasicType> iter = values.stream().spliterator();
      values.freeRef();
      while (iter.tryAdvance(x -> {
        assert x != null;
        x.freeRef();
      })) {
      }
    });
    testOperations(values -> {
      @NotNull
      ListIterator<BasicType> iter = values.listIterator();
      values.freeRef();
      while (iter.hasNext()) {
        com.simiacryptus.refcount.test.BasicType temp6176 = iter.next();
        assert temp6176 != null;
        temp6176.freeRef();
      }
    });
    testOperations(values -> {
      com.simiacryptus.refcount.test.BasicType[] temp1845 = values.stream()
          .sorted(com.simiacryptus.lang.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp1845.length;
      com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp1845);
      values.freeRef();
    });
    if (false)
      testOperations(values -> {
        values.freeRef();
        if (true) {
          com.simiacryptus.refcount.test.BasicType[] temp4582 = values.stream().toArray(i -> new BasicType[i]);
          assert values.size() == temp4582.length;
          com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp4582);
        }
      });
    testOperations(values -> {
      com.simiacryptus.refcount.test.BasicType[] temp4083 = values.stream()
          .sorted(com.simiacryptus.lang.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp4083.length;
      com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp4083);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.refcount.test.BasicType[] temp5056 = values.stream().map(x -> {
        x.value++;
        return x;
      }).sorted(com.simiacryptus.lang.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp5056.length;
      com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp5056);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.refcount.test.BasicType temp4549 = values.stream()
          .max(com.simiacryptus.lang.ref.wrappers.RefComparator.comparing(x -> {
            int temp6268 = x.value;
            x.freeRef();
            return temp6268;
          })).get();
      assert null != temp4549;
      temp4549.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.refcount.test.BasicType temp3887 = values.stream()
          .min(com.simiacryptus.lang.ref.wrappers.RefComparator.comparing(x -> {
            int temp5403 = x.value;
            x.freeRef();
            return temp5403;
          })).get();
      assert null != temp3887;
      temp3887.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      if (values.size() > 4 && values.stream().skip(1).limit(5).count() != 4)
        throw new AssertionError();
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
      if (!values.stream().parallel().isParallel())
        throw new AssertionError();
      values.freeRef();
    });
    testOperations(values -> {
      values.stream().forEachOrdered(x -> {
        x.value++;
        x.freeRef();
      });
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.refcount.test.BasicType[] temp6980 = values.stream().sequential().unordered().map(x -> {
        x.value++;
        return x;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp6980.length;
      com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp6980);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp1624 = foobar1.value;
        foobar1.freeRef();
        return temp1624;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(x -> {
        int temp2391 = x.value;
        x.freeRef();
        return temp2391;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(x -> {
        int temp8806 = x.value;
        x.freeRef();
        return temp8806;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToInt(foobar1 -> {
        IntStream temp5028 = IntStream.of(foobar1.value);
        foobar1.freeRef();
        return temp5028;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToDouble(x -> {
        DoubleStream temp3275 = DoubleStream.of(x.value);
        x.freeRef();
        return temp3275;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToLong(x -> {
        LongStream temp3787 = LongStream.of(x.value);
        x.freeRef();
        return temp3787;
      }).toArray().length;
      values.freeRef();
    });
  }

  private static void testOperations(Consumer<com.simiacryptus.lang.ref.wrappers.RefArrayList<BasicType>> fn) {
    com.simiacryptus.lang.ref.wrappers.RefArrayList<BasicType> values = new com.simiacryptus.lang.ref.wrappers.RefArrayList<>();
    for (int i = 0; i < 5; i++)
      values.add(new BasicType());
    fn.accept(values.addRef());
    values.freeRef();
  }

  private static void testArrayOperations(com.simiacryptus.lang.ref.wrappers.RefArrayList<BasicType> values) {
    if (0 == values.size())
      throw new RuntimeException();
    if (false) {
      if (values.size() != values.toArray().length)
        throw new RuntimeException();
    }
    com.simiacryptus.refcount.test.BasicType[] temp6891 = values.toArray(new BasicType[] {});
    if (values.size() != temp6891.length)
      throw new RuntimeException();
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(temp6891);
    values.freeRef();
  }

  private static void testElementOperations(com.simiacryptus.lang.ref.wrappers.RefArrayList<BasicType> values) {
    if (!values.isEmpty())
      throw new RuntimeException();
    final BasicType basicType1 = new BasicType();
    if (!values.add(basicType1.addRef()))
      throw new RuntimeException();
    if (!values.contains(basicType1.addRef()))
      throw new RuntimeException();
    if (!values.add(values.iterator().next()))
      throw new RuntimeException();
    if (values.size() != 2)
      throw new RuntimeException();
    if (!values.remove(basicType1.addRef()))
      throw new RuntimeException();
    if (values.size() != 1)
      throw new RuntimeException();
    if (!values.add(basicType1.addRef()))
      throw new RuntimeException();
    com.simiacryptus.lang.ref.RefUtil.freeRef(values.remove(0));
    if (values.size() != 1)
      throw new RuntimeException();
    com.simiacryptus.lang.ref.RefUtil.freeRef(values.set(0, new BasicType()));
    if (values.size() != 1)
      throw new RuntimeException();
    values.clear();
    if (!values.isEmpty())
      throw new RuntimeException();
    final BasicType[] basicTypeN = new BasicType[] { new BasicType(), new BasicType(), new BasicType() };
    values.addAll(com.simiacryptus.lang.ref.wrappers.RefArrays
        .asList(com.simiacryptus.refcount.test.BasicType.addRefs(basicTypeN)));
    values.add(1, basicType1.addRef());
    values.addAll(1, com.simiacryptus.lang.ref.wrappers.RefArrays
        .asList(com.simiacryptus.refcount.test.BasicType.addRefs(basicTypeN)));
    if (values.indexOf(basicType1.addRef()) != 4)
      throw new RuntimeException();
    basicType1.freeRef();
    if (values.indexOf(basicTypeN[0].addRef()) != 0)
      throw new RuntimeException();
    if (values.lastIndexOf(basicTypeN[0].addRef()) != 1)
      throw new RuntimeException();
    com.simiacryptus.lang.ref.ReferenceCounting.freeRefs(basicTypeN);
    com.simiacryptus.lang.ref.wrappers.RefArrayList<com.simiacryptus.refcount.test.BasicType> temp5025 = values
        .subList(1, 3);
    if (temp5025.size() != 2)
      throw new RuntimeException();
    temp5025.freeRef();
    values.clear();
    if (!values.isEmpty())
      throw new RuntimeException();
    values.freeRef();
  }

  private static void testCollectionOperations(com.simiacryptus.lang.ref.wrappers.RefArrayList<BasicType> values) {
    values.add(new BasicType());
    final BasicType basicType = new BasicType();
    final com.simiacryptus.lang.ref.wrappers.RefList<BasicType> list = com.simiacryptus.lang.ref.wrappers.RefArrays
        .asList(basicType.addRef());
    basicType.freeRef();
    if (!values.addAll(list.addRef()))
      throw new RuntimeException();
    if (!values.containsAll(list.addRef()))
      throw new RuntimeException();
    if (!values.retainAll(list.addRef()))
      throw new RuntimeException();
    testArrayOperations(values.addRef());
    values.removeAll(list.addRef());
    list.freeRef();
    values.clear();
    if (!values.isEmpty())
      throw new RuntimeException();
    values.freeRef();
  }

  @Override
  public String toString() {
    return "ListContainer{" + "values=" + values + '}';
  }

  public @Override void _free() {
    if (null != values)
      values.freeRef();
    super._free();
  }

  public @Override ListContainer addRef() {
    return (ListContainer) super.addRef();
  }

  public static ListContainer[] addRefs(ListContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ListContainer::addRef)
        .toArray((x) -> new ListContainer[x]);
  }
}
