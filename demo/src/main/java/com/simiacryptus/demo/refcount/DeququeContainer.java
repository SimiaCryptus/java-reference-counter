/*
 * Copyright (c) 2019 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.demo.refcount;

import com.simiacryptus.ref.lang.ReferenceCountingBase;

/**
 * The type Dequque container.
 */
public @com.simiacryptus.ref.lang.RefAware class DeququeContainer extends ReferenceCountingBase {
  private static void testOperations(
      com.simiacryptus.ref.wrappers.RefConsumer<com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<BasicType>> fn) {
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
    com.simiacryptus.demo.refcount.BasicType[] temp8850 = values.toArray(new BasicType[] {});
    if (values.size() != temp8850.length) {
      values.freeRef();
      throw new RuntimeException();
    }
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp8850);
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
      basicType2.freeRef();
      throw new RuntimeException();
    }
    if (values.size() != 2) {
      values.freeRef();
      basicType1.freeRef();
      throw new RuntimeException();
    }
    com.simiacryptus.demo.refcount.BasicType temp2731 = values.poll();
    if (!temp2731.equals(basicType1)) {
      values.freeRef();
      basicType1.freeRef();
      throw new RuntimeException();
    }
    temp2731.freeRef();
    basicType1.freeRef();
    if (values.size() != 1) {
      values.freeRef();
      throw new RuntimeException();
    }
    values.clear();
    if (!values.isEmpty()) {
      values.freeRef();
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
    if (!values.isEmpty()) {
      values.freeRef();
      throw new RuntimeException();
    }
    values.freeRef();
  }

  /**
   * Test.
   */
  public static void test() {
    for (int i = 0; i < TestOperations.count; i++) {
      testCollectionOperations(new com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<>());
      testElementOperations(new com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<>());
      testDequeOperations();
      testStreamOperations();
      testIteratorOperations();
    }
  }

  private static void testDequeOperations() {
    testOperations(values -> {
      final int initSize = values.size();
      final BasicType basicType1 = new BasicType();
      final BasicType basicType2 = new BasicType();
      values.addFirst(basicType1.addRef());
      values.addLast(basicType2.addRef());
      assert values.size() == initSize + 2;
      com.simiacryptus.demo.refcount.BasicType temp8712 = values.element();
      assert temp8712 != null;
      temp8712.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp5507 = values.getFirst();
      assert temp5507 == basicType1;
      temp5507.freeRef();
      basicType1.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp3428 = values.getLast();
      assert temp3428 == basicType2;
      temp3428.freeRef();
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
      com.simiacryptus.demo.refcount.BasicType temp1834 = values.peek();
      assert temp1834 != null;
      temp1834.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp2729 = values.peekFirst();
      assert temp2729 == basicType1;
      temp2729.freeRef();
      basicType1.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp3563 = values.peekLast();
      assert temp3563 == basicType2;
      temp3563.freeRef();
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
      com.simiacryptus.demo.refcount.BasicType temp9144 = values.pollFirst();
      assert temp9144 == basicType1;
      temp9144.freeRef();
      basicType1.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp2254 = values.pollLast();
      assert temp2254 == basicType2;
      temp2254.freeRef();
      basicType2.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp1892 = values.poll();
      assert temp1892 != null;
      temp1892.freeRef();
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
      com.simiacryptus.demo.refcount.BasicType temp6864 = values.removeFirst();
      assert temp6864 == basicType1;
      temp6864.freeRef();
      basicType1.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp5144 = values.removeLast();
      assert temp5144 == basicType2;
      temp5144.freeRef();
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
      com.simiacryptus.demo.refcount.BasicType temp6407 = values.remove();
      assert temp6407 != null;
      temp6407.freeRef();
      assert values.size() == initSize;
      values.freeRef();
    });
    testOperations(values -> {
      values.clear();
      final int initSize = values.size();
      final BasicType basicType1 = new BasicType();
      values.push(basicType1.addRef());
      assert values.size() == initSize + 1;
      com.simiacryptus.demo.refcount.BasicType temp6867 = values.pop();
      assert temp6867 == basicType1;
      temp6867.freeRef();
      basicType1.freeRef();
      assert values.size() == initSize;
      values.freeRef();
    });
  }

  private static void testIteratorOperations() {
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

  private static void testStreamOperations() {
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
        com.simiacryptus.demo.refcount.BasicType[] temp4469 = values_conditionalBlock.stream()
            .toArray(i -> new BasicType[i]);
        assert values_conditionalBlock.size() == temp4469.length;
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp4469);
      }
      values_conditionalBlock.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp4153 = values.stream().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).sorted().toArray(i -> new BasicType[i]);
      assert values.size() == temp4153.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp4153);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp1863 = values.stream().flatMap(x -> {
        x.setValue(x.getValue() + 1);
        com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp4371 = com.simiacryptus.ref.wrappers.RefStream
            .of(x);
        return temp4371;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp1863.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp1863);
      values.freeRef();
    });
    testOperations(values -> {
      values.stream().flatMap(x -> {
        x.setValue(x.getValue() + 1);
        com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp6539 = com.simiacryptus.ref.wrappers.RefStream
            .of(x, new BasicType());
        return temp6539;
      }).forEach(x -> {
        assert x != null;
        x.freeRef();
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().allMatch(x -> {
        boolean temp6667 = x != null;
        x.freeRef();
        return temp6667;
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
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp3699 = values.stream()
          .reduce((reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp3699.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp3699);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp7177 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp7177 != null;
      temp7177.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp1486 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          }, (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp1486 != null;
      temp1486.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp7279 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values.size() == temp7279.size();
      temp7279.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefSet<com.simiacryptus.demo.refcount.BasicType> temp4120 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toSet());
      assert values.size() == temp4120.size();
      temp4120.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().anyMatch(x -> {
        boolean temp7193 = x != null;
        x.freeRef();
        return temp7193;
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().noneMatch(x -> {
        boolean temp9169 = x == null;
        x.freeRef();
        return temp9169;
      });
      values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp8067 = values.stream().filter(x -> {
        boolean temp8086 = x == null;
        x.freeRef();
        return temp8086;
      }).findAny();
      assert !temp8067.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp8067);
      values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp7656 = values.stream().filter(x -> {
        boolean temp4309 = x != null;
        x.freeRef();
        return temp4309;
      }).findFirst();
      assert temp7656.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp7656);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iter = values.stream()
          .iterator();
      values.freeRef();
      while (iter.hasNext()) {
        com.simiacryptus.demo.refcount.BasicType temp3127 = iter.next();
        assert temp3127 != null;
        temp3127.freeRef();
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
      com.simiacryptus.demo.refcount.BasicType[] temp3658 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp3658.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp3658);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp6751 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp6751.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp6751);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp2037 = values.stream().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp2037.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp2037);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp3227 = values.stream()
          .max(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
            int temp7495 = x.getValue();
            x.freeRef();
            return temp7495;
          })).get();
      assert null != temp3227;
      temp3227.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp5903 = values.stream()
          .min(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
            int temp7327 = x.getValue();
            x.freeRef();
            return temp7327;
          })).get();
      assert null != temp5903;
      temp5903.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      if (values.size() > 4 && values.stream().skip(1).limit(5).count() != 4) {
        values.freeRef();
        throw new AssertionError();
      }
      values.stream().forEach(x -> {
        x.setValue(x.getValue() + 1);
        x.freeRef();
      });
      values.freeRef();
    });
    testOperations(values -> {
      values.stream().parallel().forEach(x -> {
        x.setValue(x.getValue() + 1);
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
        x.setValue(x.getValue() + 1);
        x.freeRef();
      });
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp1550 = values.stream().sequential().unordered().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp1550.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp1550);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp5980 = foobar1.getValue();
        foobar1.freeRef();
        return temp5980;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(x -> {
        int temp6586 = x.getValue();
        x.freeRef();
        return temp6586;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(x -> {
        int temp3552 = x.getValue();
        x.freeRef();
        return temp3552;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp2266 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp2266;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToDouble(x -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp4098 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(x.getValue());
        x.freeRef();
        return temp4098;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      final long[] longs = values.stream().flatMapToLong(x -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp4011 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(x.getValue());
        x.freeRef();
        return temp4011;
      }).toArray();
      assert values.size() == longs.length;
      values.freeRef();
    });
  }

  public @Override void _free() {
    super._free();
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
