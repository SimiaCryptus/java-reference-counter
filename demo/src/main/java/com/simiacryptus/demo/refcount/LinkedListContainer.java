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

import com.google.common.util.concurrent.AtomicDouble;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;

import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import com.simiacryptus.ref.wrappers.RefPrimitiveIterator;
import com.simiacryptus.ref.wrappers.RefSpliterator;

/**
 * The type Linked list container.
 */
public @com.simiacryptus.ref.lang.RefAware class LinkedListContainer extends ReferenceCountingBase {
  private static void testOperations(
      com.simiacryptus.ref.wrappers.RefConsumer<com.simiacryptus.ref.wrappers.RefLinkedList<BasicType>> fn) {
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
    com.simiacryptus.demo.refcount.BasicType[] temp8496 = values.toArray(new BasicType[] {});
    if (values.size() != temp8496.length) {
      values.freeRef();
      throw new RuntimeException();
    }
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp8496);
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
    com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> temp6895 = values.iterator();
    if (!values.add(temp6895.next())) {
      values.freeRef();
      basicType1.freeRef();
      throw new RuntimeException();
    }
    temp6895.freeRef();
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
      basicType1.freeRef();
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
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(basicTypeN);
      throw new RuntimeException();
    }
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(basicTypeN);
    com.simiacryptus.ref.wrappers.RefArrayList<com.simiacryptus.demo.refcount.BasicType> temp6716 = values.subList(1,
        3);
    if (temp6716.size() != 2) {
      values.freeRef();
      throw new RuntimeException();
    }
    temp6716.freeRef();
    values.clear();
    if (!values.isEmpty()) {
      values.freeRef();
      throw new RuntimeException();
    }
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
      testCollectionOperations(new com.simiacryptus.ref.wrappers.RefLinkedList<>());
      testElementOperations(new com.simiacryptus.ref.wrappers.RefLinkedList<>());
      testStreamOperations();
      testIteratorOperations();
    }
  }

  private static void testDoubleStream() {
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefDoubleStream intStream = values.stream().mapToDouble(foobar1 -> {
        int temp4840 = foobar1.getValue();
        foobar1.freeRef();
        return temp4840;
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
        int temp6551 = foobar1.getValue();
        foobar1.freeRef();
        return temp6551;
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
        int temp5357 = foobar1.getValue();
        foobar1.freeRef();
        return temp5357;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp3786 = foobar1.getValue();
        foobar1.freeRef();
        return temp3786;
      }).allMatch(i -> i > 0);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp5678 = foobar1.getValue();
        foobar1.freeRef();
        return temp5678;
      }).anyMatch(i -> i > 0);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp4359 = foobar1.getValue();
        foobar1.freeRef();
        return temp4359;
      }).average().getAsDouble() > 0;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp4077 = foobar1.getValue();
        foobar1.freeRef();
        return temp4077;
      }).boxed().allMatch(x -> x != null);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp2681 = foobar1.getValue();
        foobar1.freeRef();
        return temp2681;
      }).noneMatch(x -> x < 0);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp8255 = foobar1.getValue();
        foobar1.freeRef();
        return temp8255;
      }).filter(i -> i > 0).parallel().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp2515 = foobar1.getValue();
        foobar1.freeRef();
        return temp2515;
      }).filter(i -> i > 0).mapToInt(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp4284 = foobar1.getValue();
        foobar1.freeRef();
        return temp4284;
      }).filter(i -> i > 0).mapToObj(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp8975 = foobar1.getValue();
        foobar1.freeRef();
        return temp8975;
      }).filter(i -> i > 0).map(i -> Math.random()).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp2538 = foobar1.getValue();
        foobar1.freeRef();
        return temp2538;
      }).filter(i -> i > 0).mapToLong(i -> (long) (Math.random() * Long.MAX_VALUE)).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp4091 = foobar1.getValue();
        foobar1.freeRef();
        return temp4091;
      }).filter(i -> i > 0).findFirst().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp5458 = foobar1.getValue();
        foobar1.freeRef();
        return temp5458;
      }).filter(i -> i > 0).unordered().findAny().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp4555 = foobar1.getValue();
        foobar1.freeRef();
        return temp4555;
      }).flatMap(i -> com.simiacryptus.ref.wrappers.RefDoubleStream.of(i, i, i)).skip(values.size())
          .limit(values.size()).count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp7383 = foobar1.getValue();
        foobar1.freeRef();
        return temp7383;
      }).sorted().summaryStatistics().getCount();
      values.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefDoubleStream parallel = values.stream().mapToDouble(foobar1 -> {
        int temp6441 = foobar1.getValue();
        foobar1.freeRef();
        return temp6441;
      }).parallel();
      values.freeRef();
      assert parallel.isParallel();
      parallel.close();
    });
    testOperations(values -> {
      values.stream().mapToDouble(foobar1 -> {
        int temp3472 = foobar1.getValue();
        foobar1.freeRef();
        return temp3472;
      }).parallel().sequential().forEachOrdered(i -> {
        assert i > 0;
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().flatMapToDouble(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp3086 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp3086;
      }).max().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().flatMapToDouble(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp2325 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp2325;
      }).min().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToDouble(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp2756 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp2756;
      }).peek(i -> {
        assert 0 < i;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToDouble(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp3466 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp3466;
      }).reduce((a, b) -> a + b).getAsDouble();
      values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToDouble(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp3044 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp3044;
      }).reduce(0, (a, b) -> a + b);
      values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToDouble(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp7566 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp7566;
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

  private static void testIntStream() {
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefIntStream intStream = values.stream().mapToInt(foobar1 -> {
        int temp6341 = foobar1.getValue();
        foobar1.freeRef();
        return temp6341;
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
        int temp6368 = foobar1.getValue();
        foobar1.freeRef();
        return temp6368;
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
        int temp6170 = foobar1.getValue();
        foobar1.freeRef();
        return temp6170;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp2066 = foobar1.getValue();
        foobar1.freeRef();
        return temp2066;
      }).allMatch(i -> i > 0);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp5508 = foobar1.getValue();
        foobar1.freeRef();
        return temp5508;
      }).anyMatch(i -> i > 0);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp8197 = foobar1.getValue();
        foobar1.freeRef();
        return temp8197;
      }).asDoubleStream().toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp2240 = foobar1.getValue();
        foobar1.freeRef();
        return temp2240;
      }).asLongStream().toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp5046 = foobar1.getValue();
        foobar1.freeRef();
        return temp5046;
      }).average().getAsDouble() > 0;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp1910 = foobar1.getValue();
        foobar1.freeRef();
        return temp1910;
      }).boxed().allMatch(x -> x != null);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp8745 = foobar1.getValue();
        foobar1.freeRef();
        return temp8745;
      }).noneMatch(x -> x < 0);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp5278 = foobar1.getValue();
        foobar1.freeRef();
        return temp5278;
      }).filter(i -> i > 0).parallel().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp3500 = foobar1.getValue();
        foobar1.freeRef();
        return temp3500;
      }).filter(i -> i > 0).map(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp4987 = foobar1.getValue();
        foobar1.freeRef();
        return temp4987;
      }).filter(i -> i > 0).mapToObj(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp2254 = foobar1.getValue();
        foobar1.freeRef();
        return temp2254;
      }).filter(i -> i > 0).mapToDouble(i -> Math.random()).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp1582 = foobar1.getValue();
        foobar1.freeRef();
        return temp1582;
      }).filter(i -> i > 0).mapToLong(i -> (long) (Math.random() * Long.MAX_VALUE)).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp2014 = foobar1.getValue();
        foobar1.freeRef();
        return temp2014;
      }).filter(i -> i > 0).findFirst().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp4104 = foobar1.getValue();
        foobar1.freeRef();
        return temp4104;
      }).filter(i -> i > 0).unordered().findAny().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp8878 = foobar1.getValue();
        foobar1.freeRef();
        return temp8878;
      }).flatMap(i -> com.simiacryptus.ref.wrappers.RefIntStream.of(i, i, i)).skip(values.size()).limit(values.size())
          .count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp2524 = foobar1.getValue();
        foobar1.freeRef();
        return temp2524;
      }).sorted().summaryStatistics().getCount();
      values.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefIntStream parallel = values.stream().mapToInt(foobar1 -> {
        int temp1007 = foobar1.getValue();
        foobar1.freeRef();
        return temp1007;
      }).parallel();
      values.freeRef();
      assert parallel.isParallel();
      parallel.close();
    });
    testOperations(values -> {
      values.stream().mapToInt(foobar1 -> {
        int temp1472 = foobar1.getValue();
        foobar1.freeRef();
        return temp1472;
      }).parallel().sequential().forEachOrdered(i -> {
        assert i > 0;
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp5728 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp5728;
      }).max().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp4794 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp4794;
      }).min().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp2173 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp2173;
      }).peek(i -> {
        assert 0 < i;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp9167 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp9167;
      }).reduce((a, b) -> a + b).getAsInt();
      values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp7532 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp7532;
      }).reduce(0, (a, b) -> a + b);
      values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp7389 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp7389;
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

  private static void testIteratorOperations() {
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
      final com.simiacryptus.ref.wrappers.RefSpliterator<com.simiacryptus.demo.refcount.BasicType> spliterator647 = values
          .spliterator();
      values.freeRef();
      while (spliterator647.tryAdvance(x -> {
        assert null != x;
        x.freeRef();
      })) {
      }
      spliterator647.freeRef();
    });
  }

  private static void testLongStream() {
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefLongStream intStream = values.stream().mapToLong(foobar1 -> {
        int temp7143 = foobar1.getValue();
        foobar1.freeRef();
        return temp7143;
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
        int temp4253 = foobar1.getValue();
        foobar1.freeRef();
        return temp4253;
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
        int temp4601 = foobar1.getValue();
        foobar1.freeRef();
        return temp4601;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp2707 = foobar1.getValue();
        foobar1.freeRef();
        return temp2707;
      }).allMatch(i -> i > 0);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp8096 = foobar1.getValue();
        foobar1.freeRef();
        return temp8096;
      }).anyMatch(i -> i > 0);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp4879 = foobar1.getValue();
        foobar1.freeRef();
        return temp4879;
      }).asDoubleStream().toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp7949 = foobar1.getValue();
        foobar1.freeRef();
        return temp7949;
      }).average().getAsDouble() > 0;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp2223 = foobar1.getValue();
        foobar1.freeRef();
        return temp2223;
      }).boxed().allMatch(x -> x != null);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp7827 = foobar1.getValue();
        foobar1.freeRef();
        return temp7827;
      }).noneMatch(x -> x < 0);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp3751 = foobar1.getValue();
        foobar1.freeRef();
        return temp3751;
      }).filter(i -> i > 0).parallel().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp5711 = foobar1.getValue();
        foobar1.freeRef();
        return temp5711;
      }).filter(i -> i > 0).mapToInt(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp3611 = foobar1.getValue();
        foobar1.freeRef();
        return temp3611;
      }).filter(i -> i > 0).mapToObj(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp5096 = foobar1.getValue();
        foobar1.freeRef();
        return temp5096;
      }).filter(i -> i > 0).mapToDouble(i -> Math.random()).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp1138 = foobar1.getValue();
        foobar1.freeRef();
        return temp1138;
      }).filter(i -> i > 0).map(i -> (long) (Math.random() * Long.MAX_VALUE)).distinct().count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp3578 = foobar1.getValue();
        foobar1.freeRef();
        return temp3578;
      }).filter(i -> i > 0).findFirst().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp4784 = foobar1.getValue();
        foobar1.freeRef();
        return temp4784;
      }).filter(i -> i > 0).unordered().findAny().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp1880 = foobar1.getValue();
        foobar1.freeRef();
        return temp1880;
      }).flatMap(i -> com.simiacryptus.ref.wrappers.RefLongStream.of(i, i, i)).skip(values.size()).limit(values.size())
          .count();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp5604 = foobar1.getValue();
        foobar1.freeRef();
        return temp5604;
      }).sorted().summaryStatistics().getCount();
      values.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefLongStream parallel = values.stream().mapToLong(foobar1 -> {
        int temp1713 = foobar1.getValue();
        foobar1.freeRef();
        return temp1713;
      }).parallel();
      values.freeRef();
      assert parallel.isParallel();
      parallel.close();
    });
    testOperations(values -> {
      values.stream().mapToLong(foobar1 -> {
        int temp5413 = foobar1.getValue();
        foobar1.freeRef();
        return temp5413;
      }).parallel().sequential().forEachOrdered(i -> {
        assert i > 0;
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().flatMapToLong(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp6679 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp6679;
      }).max().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().flatMapToLong(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp7848 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp7848;
      }).min().isPresent();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToLong(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp6204 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp6204;
      }).peek(i -> {
        assert 0 < i;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToLong(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp2390 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp2390;
      }).reduce((a, b) -> a + b).getAsLong();
      values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToLong(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp1446 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp1446;
      }).reduce(0, (a, b) -> a + b);
      values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToLong(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp9006 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp9006;
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

  private static void testObjStream() {
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
          com.simiacryptus.demo.refcount.BasicType[] temp3115 = values_conditionalBlock.stream()
              .toArray(i -> new BasicType[i]);
          assert values_conditionalBlock.size() == temp3115.length;
          com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp3115);
        }
        values_conditionalBlock.freeRef();
      });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp5497 = values.stream().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).sorted().toArray(i -> new BasicType[i]);
      assert values.size() == temp5497.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp5497);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp1898 = values.stream().flatMap(x -> {
        x.setValue(x.getValue() + 1);
        com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp3717 = com.simiacryptus.ref.wrappers.RefStream
            .of(x);
        return temp3717;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp1898.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp1898);
      values.freeRef();
    });
    testOperations(values -> {
      values.stream().flatMap(x -> {
        x.setValue(x.getValue() + 1);
        com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp4068 = com.simiacryptus.ref.wrappers.RefStream
            .of(x, new BasicType());
        return temp4068;
      }).forEach(x -> {
        assert x != null;
        x.freeRef();
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().allMatch(x -> {
        boolean temp4393 = x != null;
        x.freeRef();
        return temp4393;
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
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp3323 = values.stream()
          .reduce((reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp3323.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp3323);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp8703 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp8703 != null;
      temp8703.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp1119 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          }, (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp1119 != null;
      temp1119.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp1095 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values.size() == temp1095.size();
      temp1095.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefSet<com.simiacryptus.demo.refcount.BasicType> temp3116 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toSet());
      assert values.size() == temp3116.size();
      temp3116.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefMap<com.simiacryptus.demo.refcount.BasicType, java.lang.Integer> temp5636 = values
          .stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.toMap(x -> {
            return x;
          }, x -> {
            int temp2420 = x.getValue();
            x.freeRef();
            return temp2420;
          }));
      assert values.size() == temp5636.size();
      temp5636.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefMap<com.simiacryptus.demo.refcount.BasicType, com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType>> temp1385 = values
          .stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.groupingBy(x -> {
            return x;
          }));
      assert values.size() == temp1385.size();
      temp1385.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().anyMatch(x -> {
        boolean temp3909 = x != null;
        x.freeRef();
        return temp3909;
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().noneMatch(x -> {
        boolean temp4833 = x == null;
        x.freeRef();
        return temp4833;
      });
      values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp1569 = values.stream().filter(x -> {
        boolean temp2919 = x == null;
        x.freeRef();
        return temp2919;
      }).findAny();
      assert !temp1569.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp1569);
      values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp1845 = values.stream().filter(x -> {
        boolean temp3007 = x != null;
        x.freeRef();
        return temp3007;
      }).findFirst();
      assert temp1845.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp1845);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iter = values.stream()
          .iterator();
      values.freeRef();
      while (iter.hasNext()) {
        com.simiacryptus.demo.refcount.BasicType temp1601 = iter.next();
        assert temp1601 != null;
        temp1601.freeRef();
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
      com.simiacryptus.ref.wrappers.RefListIterator<com.simiacryptus.demo.refcount.BasicType> iter1056 = values
          .listIterator();
      values.freeRef();
      while (iter1056.hasNext()) {
        com.simiacryptus.demo.refcount.BasicType temp3595 = iter1056.next();
        assert temp3595 != null;
        temp3595.freeRef();
      }
      iter1056.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp6874 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp6874.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp6874);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp8641 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp8641.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp8641);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp7825 = values.stream().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp7825.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp7825);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp6004 = values.stream()
          .max(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
            int temp2171 = x.getValue();
            x.freeRef();
            return temp2171;
          })).get();
      assert null != temp6004;
      temp6004.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp4231 = values.stream()
          .min(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
            int temp7959 = x.getValue();
            x.freeRef();
            return temp7959;
          })).get();
      assert null != temp4231;
      temp4231.freeRef();
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
      final com.simiacryptus.ref.wrappers.RefStream<BasicType> stream = values.stream().parallel();
      values.freeRef();
      if (!stream.isParallel())
        throw new AssertionError();
      stream.close();
    });
    testOperations(values -> {
      values.stream().forEachOrdered(x -> {
        x.setValue(x.getValue() + 1);
        x.freeRef();
      });
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp8255 = values.stream().sequential().unordered().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp8255.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp8255);
      values.freeRef();
    });
  }

  private static void testStreamOperations() {
    testObjStream();
    testIntStream();
    testDoubleStream();
    testLongStream();
  }

  public @Override void _free() {
    super._free();
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
