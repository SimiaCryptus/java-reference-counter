package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefConsumer;

public @com.simiacryptus.ref.lang.RefAware class DeququeContainer extends ReferenceCountingBase {
  public com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<BasicType> values = new com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<>();

  public DeququeContainer(BasicType... values) {
    for (BasicType value : values) {
      this.values.add(value.addRef());
    }
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(values);
  }

  private static void testOperations(
      RefConsumer<com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<BasicType>> fn) {
    com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<BasicType> values = new com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<>();
    for (int i = 0; i < TestOperations.count; i++) {
      values.add(new BasicType());
    }
    fn.accept(values);
    fn.freeRef();
  }

  private static void testArrayOperations(com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<BasicType> values) {
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
    com.simiacryptus.demo.refcount.BasicType[] temp1977 = values.toArray(new BasicType[] {});
    if (values.size() != temp1977.length)
      throw new RuntimeException();
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp1977);
    values.freeRef();
  }

  private static void testElementOperations(com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<BasicType> values) {
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
    final BasicType basicType2 = new BasicType();
    if (!values.add(basicType2)) {
      values.freeRef();
      basicType1.freeRef();
      throw new RuntimeException();
    }
    if (values.size() != 2) {
      values.freeRef();
      basicType1.freeRef();
      throw new RuntimeException();
    }
    com.simiacryptus.demo.refcount.BasicType temp3439 = values.poll();
    if (!temp3439.equals(basicType1)) {
      values.freeRef();
      throw new RuntimeException();
    }
    temp3439.freeRef();
    basicType1.freeRef();
    if (values.size() != 1) {
      values.freeRef();
      throw new RuntimeException();
    }
    values.clear();
    if (!values.isEmpty()) {
      throw new RuntimeException();
    }
    values.freeRef();
  }

  private static void testCollectionOperations(
      com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<BasicType> values) {
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
    for (int i = 0; i < TestOperations.count; i++) {
      testCollectionOperations(new com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<>());
      testElementOperations(new com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<>());
      testDequeOperations();
      testStreamOperations();
      testIteratorOperations();
    }
  }

  private void testDequeOperations() {
    testOperations(values -> {
      final int initSize = values.size();
      final BasicType basicType1 = new BasicType();
      final BasicType basicType2 = new BasicType();
      values.addFirst(basicType1.addRef());
      values.addLast(basicType2.addRef());
      assert values.size() == initSize + 2;
      com.simiacryptus.demo.refcount.BasicType temp2729 = values.element();
      assert temp2729 != null;
      temp2729.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp7704 = values.getFirst();
      assert temp7704 == basicType1;
      temp7704.freeRef();
      basicType1.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp8821 = values.getLast();
      assert temp8821 == basicType2;
      temp8821.freeRef();
      basicType2.freeRef();
      assert values.size() == initSize + 2;
      values.freeRef();
    });
    testOperations(values -> {
      final int initSize = values.size();
      final BasicType basicType1 = new BasicType();
      final BasicType basicType2 = new BasicType();
      values.addFirst(basicType1.addRef());
      values.addLast(basicType2.addRef());
      assert values.size() == initSize + 2;
      com.simiacryptus.demo.refcount.BasicType temp8565 = values.peek();
      assert temp8565 != null;
      temp8565.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp7086 = values.peekFirst();
      assert temp7086 == basicType1;
      temp7086.freeRef();
      basicType1.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp8737 = values.peekLast();
      assert temp8737 == basicType2;
      temp8737.freeRef();
      basicType2.freeRef();
      assert values.size() == initSize + 2;
      values.freeRef();
    });
    testOperations(values -> {
      final int initSize = values.size();
      final BasicType basicType1 = new BasicType();
      final BasicType basicType2 = new BasicType();
      values.addFirst(basicType1.addRef());
      values.addLast(basicType2.addRef());
      assert values.size() == initSize + 2;
      com.simiacryptus.demo.refcount.BasicType temp8876 = values.pollFirst();
      assert temp8876 == basicType1;
      temp8876.freeRef();
      basicType1.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp2634 = values.pollLast();
      assert temp2634 == basicType2;
      temp2634.freeRef();
      basicType2.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp8797 = values.poll();
      assert temp8797 != null;
      temp8797.freeRef();
      assert values.size() == initSize - 1;
      values.freeRef();
    });
    testOperations(values -> {
      final int initSize = values.size();
      final BasicType basicType0 = new BasicType();
      final BasicType basicType1 = new BasicType();
      final BasicType basicType2 = new BasicType();
      assert values.offer(basicType0.addRef());
      assert values.offerFirst(basicType1.addRef());
      assert values.offerLast(basicType2.addRef());
      assert values.size() == initSize + 3;
      com.simiacryptus.demo.refcount.BasicType temp7144 = values.removeFirst();
      assert temp7144 == basicType1;
      temp7144.freeRef();
      basicType1.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp6834 = values.removeLast();
      assert temp6834 == basicType2;
      temp6834.freeRef();
      basicType2.freeRef();
      assert values.remove(basicType0);
      assert values.size() == initSize;
      values.freeRef();
    });
    testOperations(values -> {
      final int initSize = values.size();
      final BasicType basicType0 = new BasicType();
      final BasicType basicType1 = new BasicType();
      final BasicType basicType2 = new BasicType();
      assert values.offer(basicType0);
      assert values.offerFirst(basicType1.addRef());
      assert values.offerLast(basicType2.addRef());
      assert values.size() == initSize + 3;
      assert values.removeFirstOccurrence(basicType1);
      assert values.removeLastOccurrence(basicType2);
      com.simiacryptus.demo.refcount.BasicType temp3593 = values.remove();
      assert temp3593 != null;
      temp3593.freeRef();
      assert values.size() == initSize;
      values.freeRef();
    });
    testOperations(values -> {
      values.clear();
      final int initSize = values.size();
      final BasicType basicType1 = new BasicType();
      values.push(basicType1.addRef());
      assert values.size() == initSize + 1;
      com.simiacryptus.demo.refcount.BasicType temp1079 = values.pop();
      assert temp1079 == basicType1;
      temp1079.freeRef();
      basicType1.freeRef();
      assert values.size() == initSize;
      values.freeRef();
    });
  }

  private void testIteratorOperations() {
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iterator48093 = values
          .iterator();
      values.freeRef();
      while (iterator48093.hasNext()) {
        com.simiacryptus.ref.lang.RefUtil.freeRef(iterator48093.next());
        iterator48093.remove();
      }
      iterator48093.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iterator48093 = values
          .descendingIterator();
      values.freeRef();
      while (iterator48093.hasNext()) {
        com.simiacryptus.ref.lang.RefUtil.freeRef(iterator48093.next());
        iterator48093.remove();
      }
      iterator48093.freeRef();
    });
    if (false) {
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
  }

  private void testStreamOperations() {
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
    testOperations(values_conditionalBlock -> {
      if (true) {
        com.simiacryptus.demo.refcount.BasicType[] temp8900 = values_conditionalBlock.stream()
            .toArray(i -> new BasicType[i]);
        assert values_conditionalBlock.size() == temp8900.length;
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp8900);
      }
      values_conditionalBlock.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp6823 = values.stream().map(x -> {
        x.value++;
        return x;
      }).sorted().toArray(i -> new BasicType[i]);
      assert values.size() == temp6823.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp6823);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp4499 = values.stream().flatMap(x -> {
        x.value++;
        com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp2529 = com.simiacryptus.ref.wrappers.RefStream
            .of(x);
        return temp2529;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp4499.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp4499);
      values.freeRef();
    });
    testOperations(values -> {
      values.stream().flatMap(x -> {
        x.value++;
        com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp6566 = com.simiacryptus.ref.wrappers.RefStream
            .of(x, new BasicType());
        return temp6566;
      }).forEach(x -> {
        assert x != null;
        x.freeRef();
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().allMatch(x -> {
        boolean temp5577 = x != null;
        x.freeRef();
        return temp5577;
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
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp3187 = values.stream()
          .reduce((reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp3187.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp3187);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp5706 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp5706 != null;
      temp5706.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp1886 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          }, (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp1886 != null;
      temp1886.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp2289 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values.size() == temp2289.size();
      temp2289.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefSet<com.simiacryptus.demo.refcount.BasicType> temp3546 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toSet());
      assert values.size() == temp3546.size();
      temp3546.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().anyMatch(x -> {
        boolean temp7444 = x != null;
        x.freeRef();
        return temp7444;
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().noneMatch(x -> {
        boolean temp6974 = x == null;
        x.freeRef();
        return temp6974;
      });
      values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp4337 = values.stream().filter(x -> {
        boolean temp8531 = x == null;
        x.freeRef();
        return temp8531;
      }).findAny();
      assert !temp4337.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp4337);
      values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp6459 = values.stream().filter(x -> {
        boolean temp3314 = x != null;
        x.freeRef();
        return temp3314;
      }).findFirst();
      assert temp6459.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp6459);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iter = values.stream()
          .iterator();
      values.freeRef();
      while (iter.hasNext()) {
        com.simiacryptus.demo.refcount.BasicType temp8625 = iter.next();
        assert temp8625 != null;
        temp8625.freeRef();
      }
      iter.freeRef();
    });
    testOperations(values -> {
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
      com.simiacryptus.demo.refcount.BasicType[] temp4187 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp4187.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp4187);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp9506 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp9506.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp9506);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp4648 = values.stream().map(x -> {
        x.value++;
        return x;
      }).sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp4648.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp4648);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp8527 = values.stream()
          .max(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
            int temp8110 = x.value;
            x.freeRef();
            return temp8110;
          })).get();
      assert null != temp8527;
      temp8527.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp5497 = values.stream()
          .min(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
            int temp5841 = x.value;
            x.freeRef();
            return temp5841;
          })).get();
      assert null != temp5497;
      temp5497.freeRef();
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
      com.simiacryptus.demo.refcount.BasicType[] temp8623 = values.stream().sequential().unordered().map(x -> {
        x.value++;
        return x;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp8623.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp8623);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp9007 = foobar1.value;
        foobar1.freeRef();
        return temp9007;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(x -> {
        int temp6431 = x.value;
        x.freeRef();
        return temp6431;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(x -> {
        int temp8992 = x.value;
        x.freeRef();
        return temp8992;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp1639 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.value);
        foobar1.freeRef();
        return temp1639;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToDouble(x -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp2441 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(x.value);
        x.freeRef();
        return temp2441;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      final long[] longs = values.stream().flatMapToLong(x -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp5674 = com.simiacryptus.ref.wrappers.RefLongStream.of(x.value);
        x.freeRef();
        return temp5674;
      }).toArray();
      assert values.size() == longs.length;
      values.freeRef();
    });
  }

  @Override
  public String toString() {
    return "ArrayListContainer{" + "values=" + values + '}';
  }

  public @Override DeququeContainer addRef() {
    return (DeququeContainer) super.addRef();
  }

  public static DeququeContainer[] addRefs(DeququeContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(DeququeContainer::addRef)
        .toArray((x) -> new DeququeContainer[x]);
  }

  public static DeququeContainer[][] addRefs(DeququeContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(DeququeContainer::addRefs)
        .toArray((x) -> new DeququeContainer[x][]);
  }
}
