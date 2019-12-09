package com.simiacryptus.demo.refcount;

import com.google.common.util.concurrent.AtomicDouble;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefConsumer;
import org.jetbrains.annotations.NotNull;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public @com.simiacryptus.ref.lang.RefAware class LinkedListContainer extends ReferenceCountingBase {
  public com.simiacryptus.ref.wrappers.RefLinkedList<BasicType> values = new com.simiacryptus.ref.wrappers.RefLinkedList<>();

  public LinkedListContainer(BasicType... values) {
    for (BasicType value : values) {
      this.values.add(value.addRef());
    }
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(values);
  }

  private static void testOperations(RefConsumer<com.simiacryptus.ref.wrappers.RefLinkedList<BasicType>> fn) {
    com.simiacryptus.ref.wrappers.RefLinkedList<BasicType> values = new com.simiacryptus.ref.wrappers.RefLinkedList<>();
    for (int i = 0; i < TestOperations.count; i++) {
      values.add(new BasicType());
    }
    fn.accept(values);
    fn.freeRef();
  }

  private static void testArrayOperations(com.simiacryptus.ref.wrappers.RefLinkedList<BasicType> values) {
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
    com.simiacryptus.demo.refcount.BasicType[] temp5964 = values.toArray(new BasicType[] {});
    if (values.size() != temp5964.length)
      throw new RuntimeException();
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp5964);
    values.freeRef();
  }

  private static void testElementOperations(com.simiacryptus.ref.wrappers.RefLinkedList<BasicType> values) {
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
    com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> temp8565 = values.iterator();
    if (!values.add(temp8565.next())) {
      values.freeRef();
      basicType1.freeRef();
      throw new RuntimeException();
    }
    temp8565.freeRef();
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
    com.simiacryptus.ref.wrappers.RefArrayList<com.simiacryptus.demo.refcount.BasicType> temp8987 = values.subList(1,
        3);
    if (temp8987.size() != 2) {
      values.freeRef();
      throw new RuntimeException();
    }
    temp8987.freeRef();
    values.clear();
    if (!values.isEmpty())
      throw new RuntimeException();
    values.freeRef();
  }

  private static void testCollectionOperations(com.simiacryptus.ref.wrappers.RefLinkedList<BasicType> values) {
    values.add(new BasicType());
    final BasicType basicType = new BasicType();
    final com.simiacryptus.ref.wrappers.RefList<BasicType> list = com.simiacryptus.ref.wrappers.RefArrays
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

  public @Override void _free() {
    if (null != values)
      values.freeRef();
    super._free();
  }

  public void test() {
    com.simiacryptus.ref.wrappers.RefLinkedList<com.simiacryptus.demo.refcount.BasicType> temp6523 = new com.simiacryptus.ref.wrappers.RefLinkedList<>(
        this.values.addRef());
    if (this.values.size() != temp6523.size())
      throw new RuntimeException();
    temp6523.freeRef();
    for (int i = 0; i < TestOperations.count; i++) {
      testCollectionOperations(new com.simiacryptus.ref.wrappers.RefLinkedList<>(this.values.addRef()));
      testElementOperations(new com.simiacryptus.ref.wrappers.RefLinkedList<>());
      testStreamOperations();
      testIteratorOperations();
    }
  }

  private void testDoubleStream() {
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefDoubleStream intStream = values.stream().mapToDouble(foobar1 -> {
        int temp6496 = foobar1.value;
        foobar1.freeRef();
        return temp6496;
      });
      values.freeRef();
      final PrimitiveIterator.OfDouble iterator = intStream.iterator();
      while (iterator.hasNext()) {
        assert null != iterator.next();
      }
      com.simiacryptus.ref.lang.RefUtil.freeRef(iterator);
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefDoubleStream intStream = values.stream().mapToDouble(foobar1 -> {
        int temp9819 = foobar1.value;
        foobar1.freeRef();
        return temp9819;
      });
      values.freeRef();
      final Spliterator.OfDouble iterator = intStream.spliterator();
      iterator.forEachRemaining((double i) -> {
        assert i > 0;
      });
      com.simiacryptus.ref.lang.RefUtil.freeRef(iterator);
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp5939 = foobar1.value;
        foobar1.freeRef();
        return temp5939;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp6998 = foobar1.value;
        foobar1.freeRef();
        return temp6998;
      }).allMatch(i -> i > 0);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp1244 = foobar1.value;
        foobar1.freeRef();
        return temp1244;
      }).anyMatch(i -> i > 0);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp4178 = foobar1.value;
        foobar1.freeRef();
        return temp4178;
      }).average().getAsDouble() > 0;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp1194 = foobar1.value;
        foobar1.freeRef();
        return temp1194;
      }).boxed().allMatch(x -> x != null);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp1633 = foobar1.value;
        foobar1.freeRef();
        return temp1633;
      }).noneMatch(x -> x < 0);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp5332 = foobar1.value;
        foobar1.freeRef();
        return temp5332;
      }).filter(i -> i > 0).parallel().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp7720 = foobar1.value;
        foobar1.freeRef();
        return temp7720;
      }).filter(i -> i > 0).mapToInt(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp5757 = foobar1.value;
        foobar1.freeRef();
        return temp5757;
      }).filter(i -> i > 0).mapToObj(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp1659 = foobar1.value;
        foobar1.freeRef();
        return temp1659;
      }).filter(i -> i > 0).map(i -> Math.random()).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp6196 = foobar1.value;
        foobar1.freeRef();
        return temp6196;
      }).filter(i -> i > 0).mapToLong(i -> (long) (Math.random() * Long.MAX_VALUE)).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp2261 = foobar1.value;
        foobar1.freeRef();
        return temp2261;
      }).filter(i -> i > 0).findFirst().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp1865 = foobar1.value;
        foobar1.freeRef();
        return temp1865;
      }).filter(i -> i > 0).unordered().findAny().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp5237 = foobar1.value;
        foobar1.freeRef();
        return temp5237;
      }).flatMap(i -> com.simiacryptus.ref.wrappers.RefDoubleStream.of(i, i, i)).skip(values.size())
          .limit(values.size()).count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp7239 = foobar1.value;
        foobar1.freeRef();
        return temp7239;
      }).sorted().summaryStatistics().getCount();
      values.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefDoubleStream parallel = values.stream().mapToDouble(foobar1 -> {
        int temp4230 = foobar1.value;
        foobar1.freeRef();
        return temp4230;
      }).parallel();
      values.freeRef();
      assert parallel.isParallel();
      parallel.close();
    });
    testOperations(values -> {
      values.stream().mapToDouble(foobar1 -> {
        int temp3952 = foobar1.value;
        foobar1.freeRef();
        return temp3952;
      }).parallel().sequential().forEachOrdered(i -> {
        assert i > 0;
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().flatMapToDouble(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp4682 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp4682;
      }).max().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().flatMapToDouble(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp3100 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp3100;
      }).min().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToDouble(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp7083 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp7083;
      }).peek(i -> {
        assert 0 < i;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToDouble(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp5575 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp5575;
      }).reduce((a, b) -> a + b).getAsDouble();
      values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToDouble(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp7117 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp7117;
      }).reduce(0, (a, b) -> a + b);
      values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToDouble(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp4537 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp4537;
      }).collect(AtomicDouble::new, (a, x) -> a.addAndGet(x), (a, b) -> a.addAndGet(b.get())).get();
      values.freeRef();
    });
    testOperations(values -> {
      values.freeRef();
      final double sum = com.simiacryptus.ref.wrappers.RefDoubleStream.iterate(1, x -> x + 1).limit(10).sum();
      assert 0 < sum : "0 >= " + sum;
    });
    testOperations(values -> {
      values.freeRef();
      assert 0 < com.simiacryptus.ref.wrappers.RefDoubleStream.generate(() -> (int) (Math.random() * 5)).limit(10)
          .sum();
    });
  }

  private void testIntStream() {
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefIntStream intStream = values.stream().mapToInt(foobar1 -> {
        int temp1120 = foobar1.value;
        foobar1.freeRef();
        return temp1120;
      });
      values.freeRef();
      final PrimitiveIterator.OfInt iterator = intStream.iterator();
      while (iterator.hasNext()) {
        assert null != iterator.next();
      }
      com.simiacryptus.ref.lang.RefUtil.freeRef(iterator);
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefIntStream intStream = values.stream().mapToInt(foobar1 -> {
        int temp6716 = foobar1.value;
        foobar1.freeRef();
        return temp6716;
      });
      values.freeRef();
      final Spliterator.OfInt iterator = intStream.spliterator();
      iterator.forEachRemaining((int i) -> {
        assert i > 0;
      });
      com.simiacryptus.ref.lang.RefUtil.freeRef(iterator);
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp4846 = foobar1.value;
        foobar1.freeRef();
        return temp4846;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp5293 = foobar1.value;
        foobar1.freeRef();
        return temp5293;
      }).allMatch(i -> i > 0);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp5462 = foobar1.value;
        foobar1.freeRef();
        return temp5462;
      }).anyMatch(i -> i > 0);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp5964 = foobar1.value;
        foobar1.freeRef();
        return temp5964;
      }).asDoubleStream().toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp8347 = foobar1.value;
        foobar1.freeRef();
        return temp8347;
      }).asLongStream().toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp7975 = foobar1.value;
        foobar1.freeRef();
        return temp7975;
      }).average().getAsDouble() > 0;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp9040 = foobar1.value;
        foobar1.freeRef();
        return temp9040;
      }).boxed().allMatch(x -> x != null);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp6739 = foobar1.value;
        foobar1.freeRef();
        return temp6739;
      }).noneMatch(x -> x < 0);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp9049 = foobar1.value;
        foobar1.freeRef();
        return temp9049;
      }).filter(i -> i > 0).parallel().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp8304 = foobar1.value;
        foobar1.freeRef();
        return temp8304;
      }).filter(i -> i > 0).map(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp1347 = foobar1.value;
        foobar1.freeRef();
        return temp1347;
      }).filter(i -> i > 0).mapToObj(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp2225 = foobar1.value;
        foobar1.freeRef();
        return temp2225;
      }).filter(i -> i > 0).mapToDouble(i -> Math.random()).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp6115 = foobar1.value;
        foobar1.freeRef();
        return temp6115;
      }).filter(i -> i > 0).mapToLong(i -> (long) (Math.random() * Long.MAX_VALUE)).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp4536 = foobar1.value;
        foobar1.freeRef();
        return temp4536;
      }).filter(i -> i > 0).findFirst().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp5991 = foobar1.value;
        foobar1.freeRef();
        return temp5991;
      }).filter(i -> i > 0).unordered().findAny().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp7926 = foobar1.value;
        foobar1.freeRef();
        return temp7926;
      }).flatMap(i -> com.simiacryptus.ref.wrappers.RefIntStream.of(i, i, i)).skip(values.size()).limit(values.size())
          .count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp6164 = foobar1.value;
        foobar1.freeRef();
        return temp6164;
      }).sorted().summaryStatistics().getCount();
      values.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefIntStream parallel = values.stream().mapToInt(foobar1 -> {
        int temp5118 = foobar1.value;
        foobar1.freeRef();
        return temp5118;
      }).parallel();
      values.freeRef();
      assert parallel.isParallel();
      parallel.close();
    });
    testOperations(values -> {
      values.stream().mapToInt(foobar1 -> {
        int temp7989 = foobar1.value;
        foobar1.freeRef();
        return temp7989;
      }).parallel().sequential().forEachOrdered(i -> {
        assert i > 0;
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp1862 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp1862;
      }).max().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp3591 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp3591;
      }).min().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp1032 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp1032;
      }).peek(i -> {
        assert 0 < i;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp7119 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp7119;
      }).reduce((a, b) -> a + b).getAsInt();
      values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp4264 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp4264;
      }).reduce(0, (a, b) -> a + b);
      values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp9406 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp9406;
      }).collect(AtomicInteger::new, (a, x) -> a.addAndGet(x), (a, b) -> a.addAndGet(b.get())).get();
      values.freeRef();
    });
    testOperations(values -> {
      values.freeRef();
      final int sum = com.simiacryptus.ref.wrappers.RefIntStream.range(1, 5).sum();
      assert 0 < sum : "0 >= " + sum;
    });
    testOperations(values -> {
      values.freeRef();
      assert 0 < com.simiacryptus.ref.wrappers.RefIntStream.generate(() -> (int) (Math.random() * 5)).limit(10).sum();
    });
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
      if (values.size() > 3) {
        final com.simiacryptus.ref.wrappers.RefListIterator<com.simiacryptus.demo.refcount.BasicType> iterator = values
            .listIterator();
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
      }
      values.freeRef();
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

  private void testLongStream() {
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefLongStream intStream = values.stream().mapToLong(foobar1 -> {
        int temp1975 = foobar1.value;
        foobar1.freeRef();
        return temp1975;
      });
      values.freeRef();
      final PrimitiveIterator.OfLong iterator = intStream.iterator();
      while (iterator.hasNext()) {
        assert null != iterator.next();
      }
      com.simiacryptus.ref.lang.RefUtil.freeRef(iterator);
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefLongStream intStream = values.stream().mapToLong(foobar1 -> {
        int temp2103 = foobar1.value;
        foobar1.freeRef();
        return temp2103;
      });
      values.freeRef();
      final Spliterator.OfLong iterator = intStream.spliterator();
      iterator.forEachRemaining((long i) -> {
        assert i > 0;
      });
      com.simiacryptus.ref.lang.RefUtil.freeRef(iterator);
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp4447 = foobar1.value;
        foobar1.freeRef();
        return temp4447;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp8662 = foobar1.value;
        foobar1.freeRef();
        return temp8662;
      }).allMatch(i -> i > 0);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp1267 = foobar1.value;
        foobar1.freeRef();
        return temp1267;
      }).anyMatch(i -> i > 0);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp6004 = foobar1.value;
        foobar1.freeRef();
        return temp6004;
      }).asDoubleStream().toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp3746 = foobar1.value;
        foobar1.freeRef();
        return temp3746;
      }).average().getAsDouble() > 0;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp7126 = foobar1.value;
        foobar1.freeRef();
        return temp7126;
      }).boxed().allMatch(x -> x != null);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp3875 = foobar1.value;
        foobar1.freeRef();
        return temp3875;
      }).noneMatch(x -> x < 0);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp4761 = foobar1.value;
        foobar1.freeRef();
        return temp4761;
      }).filter(i -> i > 0).parallel().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp2268 = foobar1.value;
        foobar1.freeRef();
        return temp2268;
      }).filter(i -> i > 0).mapToInt(i -> (int) (Math.random() * 10000)).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp1444 = foobar1.value;
        foobar1.freeRef();
        return temp1444;
      }).filter(i -> i > 0).mapToObj(i -> (int) (Math.random() * 10000)).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp8565 = foobar1.value;
        foobar1.freeRef();
        return temp8565;
      }).filter(i -> i > 0).mapToDouble(i -> (Math.random() * 10000)).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp3976 = foobar1.value;
        foobar1.freeRef();
        return temp3976;
      }).filter(i -> i > 0).map(i -> (long) (Math.random() * 10000)).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp1406 = foobar1.value;
        foobar1.freeRef();
        return temp1406;
      }).filter(i -> i > 0).findFirst().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp6186 = foobar1.value;
        foobar1.freeRef();
        return temp6186;
      }).filter(i -> i > 0).unordered().findAny().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp4155 = foobar1.value;
        foobar1.freeRef();
        return temp4155;
      }).flatMap(i -> com.simiacryptus.ref.wrappers.RefLongStream.of(i, i, i)).skip(values.size()).limit(values.size())
          .count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp7330 = foobar1.value;
        foobar1.freeRef();
        return temp7330;
      }).sorted().summaryStatistics().getCount();
      values.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefLongStream parallel = values.stream().mapToLong(foobar1 -> {
        int temp1290 = foobar1.value;
        foobar1.freeRef();
        return temp1290;
      }).parallel();
      values.freeRef();
      assert parallel.isParallel();
      parallel.close();
    });
    testOperations(values -> {
      values.stream().mapToLong(foobar1 -> {
        int temp8197 = foobar1.value;
        foobar1.freeRef();
        return temp8197;
      }).parallel().sequential().forEachOrdered(i -> {
        assert i > 0;
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().flatMapToLong(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp2382 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp2382;
      }).max().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().flatMapToLong(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp7683 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp7683;
      }).min().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToLong(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp9087 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp9087;
      }).peek(i -> {
        assert 0 < i;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToLong(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp4958 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp4958;
      }).reduce((a, b) -> a + b).getAsLong();
      values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToLong(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp7789 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp7789;
      }).reduce(0, (a, b) -> a + b);
      values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToLong(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp6070 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp6070;
      }).collect(AtomicLong::new, (a, x) -> a.addAndGet(x), (a, b) -> a.addAndGet(b.get())).get();
      values.freeRef();
    });
    testOperations(values -> {
      values.freeRef();
      final long sum = com.simiacryptus.ref.wrappers.RefLongStream.range(1, 5).sum();
      assert 0 < sum : "0 >= " + sum;
    });
    testOperations(values -> {
      values.freeRef();
      assert 0 < com.simiacryptus.ref.wrappers.RefLongStream.generate(() -> (int) (Math.random() * 5)).limit(10).sum();
    });
  }

  private void testObjStream() {
    if (false)
      testOperations(values -> {
        assert values.size() == values.stream().flatMap(
            (java.util.function.Function<? super BasicType, ? extends com.simiacryptus.ref.wrappers.RefStream<? extends BasicType>>) com.simiacryptus.ref.lang.RefUtil
                .wrapInterface(
                    (java.util.function.Function<? super com.simiacryptus.demo.refcount.BasicType, ? extends com.simiacryptus.ref.wrappers.RefStream<? extends com.simiacryptus.demo.refcount.BasicType>>) x -> {
                      x.freeRef();
                      return values.stream();
                    }, values))
            .distinct().count();
      });
    if (false)
      testOperations(values_conditionalBlock -> {
        if (true) {
          com.simiacryptus.demo.refcount.BasicType[] temp4859 = values_conditionalBlock.stream()
              .toArray(i -> new BasicType[i]);
          assert values_conditionalBlock.size() == temp4859.length;
          com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp4859);
        }
        values_conditionalBlock.freeRef();
      });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp7373 = values.stream().map(x -> {
        x.value++;
        return x;
      }).sorted().toArray(i -> new BasicType[i]);
      assert values.size() == temp7373.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp7373);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp2556 = values.stream().flatMap(x -> {
        x.value++;
        com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp1067 = com.simiacryptus.ref.wrappers.RefStream
            .of(x);
        return temp1067;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp2556.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp2556);
      values.freeRef();
    });
    testOperations(values -> {
      values.stream().flatMap(x -> {
        x.value++;
        com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp7077 = com.simiacryptus.ref.wrappers.RefStream
            .of(x, new BasicType());
        return temp7077;
      }).forEach(x -> {
        assert x != null;
        x.freeRef();
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().allMatch(x -> {
        boolean temp7021 = x != null;
        x.freeRef();
        return temp7021;
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
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp8782 = values.stream()
          .reduce((reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp8782.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp8782);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp4025 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp4025 != null;
      temp4025.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp8996 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          }, (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp8996 != null;
      temp8996.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp1876 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values.size() == temp1876.size();
      temp1876.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefSet<com.simiacryptus.demo.refcount.BasicType> temp2415 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toSet());
      assert values.size() == temp2415.size();
      temp2415.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefMap<com.simiacryptus.demo.refcount.BasicType, java.lang.Integer> temp2861 = values
          .stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.toMap(x -> {
            return x;
          }, x -> {
            int temp5136 = x.value;
            x.freeRef();
            return temp5136;
          }));
      assert values.size() == temp2861.size();
      temp2861.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefMap<com.simiacryptus.demo.refcount.BasicType, com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType>> temp3197 = values
          .stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.groupingBy(x -> {
            return x;
          }));
      assert values.size() == temp3197.size();
      temp3197.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().anyMatch(x -> {
        boolean temp1503 = x != null;
        x.freeRef();
        return temp1503;
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().noneMatch(x -> {
        boolean temp3178 = x == null;
        x.freeRef();
        return temp3178;
      });
      values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp2353 = values.stream().filter(x -> {
        boolean temp5500 = x == null;
        x.freeRef();
        return temp5500;
      }).findAny();
      assert !temp2353.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp2353);
      values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp3821 = values.stream().filter(x -> {
        boolean temp1099 = x != null;
        x.freeRef();
        return temp1099;
      }).findFirst();
      assert temp3821.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp3821);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iter = values.stream()
          .iterator();
      values.freeRef();
      while (iter.hasNext()) {
        com.simiacryptus.demo.refcount.BasicType temp2267 = iter.next();
        assert temp2267 != null;
        temp2267.freeRef();
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
        com.simiacryptus.demo.refcount.BasicType temp5488 = iter.next();
        assert temp5488 != null;
        temp5488.freeRef();
      }
      iter.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp6747 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp6747.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp6747);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp1551 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp1551.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp1551);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp5013 = values.stream().map(x -> {
        x.value++;
        return x;
      }).sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp5013.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp5013);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp4337 = values.stream()
          .max(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
            int temp8737 = x.value;
            x.freeRef();
            return temp8737;
          })).get();
      assert null != temp4337;
      temp4337.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp7678 = values.stream()
          .min(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
            int temp8609 = x.value;
            x.freeRef();
            return temp8609;
          })).get();
      assert null != temp7678;
      temp7678.freeRef();
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
      final com.simiacryptus.ref.wrappers.RefStream<BasicType> stream = values.stream().parallel();
      values.freeRef();
      if (!stream.isParallel())
        throw new AssertionError();
      stream.close();
    });
    testOperations(values -> {
      values.stream().forEachOrdered(x -> {
        x.value++;
        x.freeRef();
      });
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp1957 = values.stream().sequential().unordered().map(x -> {
        x.value++;
        return x;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp1957.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp1957);
      values.freeRef();
    });
  }

  private void testStreamOperations() {
    testObjStream();
    testIntStream();
    testDoubleStream();
    testLongStream();
  }

  @Override
  public String toString() {
    return "ArrayListContainer{" + "values=" + values + '}';
  }

  public @Override LinkedListContainer addRef() {
    return (LinkedListContainer) super.addRef();
  }

  public static LinkedListContainer[] addRefs(LinkedListContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(LinkedListContainer::addRef)
        .toArray((x) -> new LinkedListContainer[x]);
  }

  public static LinkedListContainer[][] addRefs(LinkedListContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(LinkedListContainer::addRefs)
        .toArray((x) -> new LinkedListContainer[x][]);
  }
}
