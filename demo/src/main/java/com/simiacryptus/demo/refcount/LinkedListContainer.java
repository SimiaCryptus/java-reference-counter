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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The type Linked list container.
 */
public @com.simiacryptus.ref.lang.RefAware class LinkedListContainer extends ReferenceCountingBase {
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

  private static void testOperations(
      com.simiacryptus.ref.wrappers.RefConsumer<com.simiacryptus.ref.wrappers.RefLinkedList<BasicType>> fn) {
    com.simiacryptus.ref.wrappers.RefLinkedList<BasicType> values = new com.simiacryptus.ref.wrappers.RefLinkedList<>();
    for (int i = 0; i < TestOperations.count; i++) {
      values.add(new BasicType());
    }
    fn.accept(values == null ? null : values.addRef());
    if (null != fn)
      fn.freeRef();
    if (null != values)
      values.freeRef();
  }

  private static void testArrayOperations(com.simiacryptus.ref.wrappers.RefLinkedList<BasicType> values) {
    if (0 == values.size()) {
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (false) {
      if (values.size() != values.toArray().length) {
        if (null != values)
          values.freeRef();
        throw new RuntimeException();
      }
    }
    com.simiacryptus.demo.refcount.BasicType[] temp_06_0089 = values.toArray(new BasicType[] {});
    if (values.size() != temp_06_0089.length) {
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (null != temp_06_0089)
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_06_0089);
    if (null != values)
      values.freeRef();
  }

  private static void testElementOperations(com.simiacryptus.ref.wrappers.RefLinkedList<BasicType> values) {
    if (!values.isEmpty()) {
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    final BasicType basicType1 = new BasicType();
    if (!values.add(basicType1 == null ? null : basicType1.addRef())) {
      if (null != basicType1)
        basicType1.freeRef();
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (!values.contains(basicType1 == null ? null : basicType1.addRef())) {
      if (null != basicType1)
        basicType1.freeRef();
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> temp_06_0090 = values
        .iterator();
    if (!values.add(temp_06_0090.next())) {
      if (null != basicType1)
        basicType1.freeRef();
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (null != temp_06_0090)
      temp_06_0090.freeRef();
    if (values.size() != 2) {
      if (null != basicType1)
        basicType1.freeRef();
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (!values.remove(basicType1 == null ? null : basicType1.addRef())) {
      if (null != basicType1)
        basicType1.freeRef();
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (values.size() != 1) {
      if (null != basicType1)
        basicType1.freeRef();
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (!values.add(basicType1 == null ? null : basicType1.addRef())) {
      if (null != basicType1)
        basicType1.freeRef();
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    com.simiacryptus.ref.lang.RefUtil.freeRef(values.remove(0));
    if (values.size() != 1) {
      if (null != basicType1)
        basicType1.freeRef();
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    com.simiacryptus.ref.lang.RefUtil.freeRef(values.set(0, new BasicType()));
    if (values.size() != 1) {
      if (null != basicType1)
        basicType1.freeRef();
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    values.clear();
    if (!values.isEmpty()) {
      if (null != basicType1)
        basicType1.freeRef();
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    final BasicType[] basicTypeN = new BasicType[] { new BasicType(), new BasicType(), new BasicType() };
    values.addAll(
        com.simiacryptus.ref.wrappers.RefArrays.asList(com.simiacryptus.demo.refcount.BasicType.addRefs(basicTypeN)));
    values.add(1, basicType1 == null ? null : basicType1.addRef());
    values.addAll(1,
        com.simiacryptus.ref.wrappers.RefArrays.asList(com.simiacryptus.demo.refcount.BasicType.addRefs(basicTypeN)));
    if (values.indexOf(basicType1 == null ? null : basicType1.addRef()) != 4) {
      if (null != basicType1)
        basicType1.freeRef();
      if (null != basicTypeN)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(basicTypeN);
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (null != basicType1)
      basicType1.freeRef();
    if (values.indexOf(basicTypeN[0].addRef()) != 0) {
      if (null != basicTypeN)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(basicTypeN);
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (values.lastIndexOf(basicTypeN[0].addRef()) != 1) {
      if (null != basicTypeN)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(basicTypeN);
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (null != basicTypeN)
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(basicTypeN);
    com.simiacryptus.ref.wrappers.RefArrayList<com.simiacryptus.demo.refcount.BasicType> temp_06_0091 = values
        .subList(1, 3);
    if (temp_06_0091.size() != 2) {
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (null != temp_06_0091)
      temp_06_0091.freeRef();
    values.clear();
    if (!values.isEmpty()) {
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (null != values)
      values.freeRef();
  }

  private static void testCollectionOperations(com.simiacryptus.ref.wrappers.RefLinkedList<BasicType> values) {
    values.add(new BasicType());
    final BasicType basicType = new BasicType();
    final com.simiacryptus.ref.wrappers.RefList<BasicType> list = com.simiacryptus.ref.wrappers.RefArrays
        .asList(basicType == null ? null : basicType.addRef());
    if (null != basicType)
      basicType.freeRef();
    if (!values.addAll(list == null ? null : list.addRef())) {
      if (null != list)
        list.freeRef();
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (!values.containsAll(list == null ? null : list.addRef())) {
      if (null != list)
        list.freeRef();
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (!values.retainAll(list == null ? null : list.addRef())) {
      if (null != list)
        list.freeRef();
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    testArrayOperations(values == null ? null : values.addRef());
    values.removeAll(list == null ? null : list.addRef());
    if (null != list)
      list.freeRef();
    values.clear();
    if (!values.isEmpty()) {
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (null != values)
      values.freeRef();
  }

  private static void testDoubleStream() {
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefDoubleStream intStream = values.stream().mapToDouble(foobar1 -> {
        int temp_06_0001 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0001;
      });
      if (null != values)
        values.freeRef();
      final com.simiacryptus.ref.wrappers.RefPrimitiveIterator.OfDouble iterator = intStream.iterator();
      while (iterator.hasNext()) {
        assert null != iterator.next();
      }
      if (null != iterator)
        iterator.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefDoubleStream intStream = values.stream().mapToDouble(foobar1 -> {
        int temp_06_0002 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0002;
      });
      if (null != values)
        values.freeRef();
      final com.simiacryptus.ref.wrappers.RefSpliterator.OfDouble iterator = intStream.spliterator();
      iterator.forEachRemaining((double i) -> {
        assert i > 0;
      });
      if (null != iterator)
        iterator.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp_06_0003 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0003;
      }).toArray().length;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp_06_0004 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0004;
      }).allMatch(i -> i > 0);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp_06_0005 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0005;
      }).anyMatch(i -> i > 0);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp_06_0006 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0006;
      }).average().getAsDouble() > 0;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp_06_0007 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0007;
      }).boxed().allMatch(x -> x != null);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp_06_0008 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0008;
      }).noneMatch(x -> x < 0);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp_06_0009 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0009;
      }).filter(i -> i > 0).parallel().count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp_06_0010 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0010;
      }).filter(i -> i > 0).mapToInt(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp_06_0011 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0011;
      }).filter(i -> i > 0).mapToObj(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp_06_0012 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0012;
      }).filter(i -> i > 0).map(i -> Math.random()).distinct().count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp_06_0013 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0013;
      }).filter(i -> i > 0).mapToLong(i -> (long) (Math.random() * Long.MAX_VALUE)).distinct().count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp_06_0014 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0014;
      }).filter(i -> i > 0).findFirst().isPresent();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        int temp_06_0015 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0015;
      }).filter(i -> i > 0).unordered().findAny().isPresent();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp_06_0016 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0016;
      }).flatMap(i -> com.simiacryptus.ref.wrappers.RefDoubleStream.of(i, i, i)).skip(values.size())
          .limit(values.size()).count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        int temp_06_0017 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0017;
      }).sorted().summaryStatistics().getCount();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefDoubleStream parallel = values.stream().mapToDouble(foobar1 -> {
        int temp_06_0018 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0018;
      }).parallel();
      if (null != values)
        values.freeRef();
      assert parallel.isParallel();
      parallel.close();
    });
    testOperations(values -> {
      values.stream().mapToDouble(foobar1 -> {
        int temp_06_0019 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0019;
      }).parallel().sequential().forEachOrdered(i -> {
        assert i > 0;
      });
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().flatMapToDouble(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp_06_0020 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(foobar1.getValue());
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0020;
      }).max().isPresent();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().flatMapToDouble(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp_06_0021 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(foobar1.getValue());
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0021;
      }).min().isPresent();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToDouble(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp_06_0022 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(foobar1.getValue());
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0022;
      }).peek(i -> {
        assert 0 < i;
      }).toArray().length;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToDouble(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp_06_0023 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(foobar1.getValue());
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0023;
      }).reduce((a, b) -> a + b).getAsDouble();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToDouble(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp_06_0024 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(foobar1.getValue());
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0024;
      }).reduce(0, (a, b) -> a + b);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToDouble(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp_06_0025 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(foobar1.getValue());
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0025;
      }).collect(AtomicDouble::new, (a, x) -> a.addAndGet(x), (a, b) -> a.addAndGet(b.get())).get();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      if (null != values)
        values.freeRef();
      final double sum = com.simiacryptus.ref.wrappers.RefDoubleStream.iterate(1, x -> x + 1).limit(10).sum();
      assert 0 < sum : "0 >= " + sum;
    });
    testOperations(values -> {
      if (null != values)
        values.freeRef();
      assert 0 < com.simiacryptus.ref.wrappers.RefDoubleStream.generate(() -> (int) (Math.random() * 5)).limit(10)
          .sum();
    });
  }

  private static void testIntStream() {
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefIntStream intStream = values.stream().mapToInt(foobar1 -> {
        int temp_06_0026 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0026;
      });
      if (null != values)
        values.freeRef();
      final com.simiacryptus.ref.wrappers.RefPrimitiveIterator.OfInt iterator = intStream.iterator();
      while (iterator.hasNext()) {
        assert null != iterator.next();
      }
      if (null != iterator)
        iterator.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefIntStream intStream = values.stream().mapToInt(foobar1 -> {
        int temp_06_0027 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0027;
      });
      if (null != values)
        values.freeRef();
      final com.simiacryptus.ref.wrappers.RefSpliterator.OfInt iterator = intStream.spliterator();
      iterator.forEachRemaining((int i) -> {
        assert i > 0;
      });
      if (null != iterator)
        iterator.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp_06_0028 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0028;
      }).toArray().length;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp_06_0029 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0029;
      }).allMatch(i -> i > 0);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp_06_0030 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0030;
      }).anyMatch(i -> i > 0);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp_06_0031 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0031;
      }).asDoubleStream().toArray().length;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp_06_0032 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0032;
      }).asLongStream().toArray().length;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp_06_0033 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0033;
      }).average().getAsDouble() > 0;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp_06_0034 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0034;
      }).boxed().allMatch(x -> x != null);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp_06_0035 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0035;
      }).noneMatch(x -> x < 0);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp_06_0036 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0036;
      }).filter(i -> i > 0).parallel().count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp_06_0037 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0037;
      }).filter(i -> i > 0).map(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp_06_0038 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0038;
      }).filter(i -> i > 0).mapToObj(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp_06_0039 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0039;
      }).filter(i -> i > 0).mapToDouble(i -> Math.random()).distinct().count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp_06_0040 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0040;
      }).filter(i -> i > 0).mapToLong(i -> (long) (Math.random() * Long.MAX_VALUE)).distinct().count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp_06_0041 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0041;
      }).filter(i -> i > 0).findFirst().isPresent();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        int temp_06_0042 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0042;
      }).filter(i -> i > 0).unordered().findAny().isPresent();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp_06_0043 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0043;
      }).flatMap(i -> com.simiacryptus.ref.wrappers.RefIntStream.of(i, i, i)).skip(values.size()).limit(values.size())
          .count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp_06_0044 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0044;
      }).sorted().summaryStatistics().getCount();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefIntStream parallel = values.stream().mapToInt(foobar1 -> {
        int temp_06_0045 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0045;
      }).parallel();
      if (null != values)
        values.freeRef();
      assert parallel.isParallel();
      parallel.close();
    });
    testOperations(values -> {
      values.stream().mapToInt(foobar1 -> {
        int temp_06_0046 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0046;
      }).parallel().sequential().forEachOrdered(i -> {
        assert i > 0;
      });
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp_06_0047 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.getValue());
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0047;
      }).max().isPresent();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp_06_0048 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.getValue());
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0048;
      }).min().isPresent();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp_06_0049 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.getValue());
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0049;
      }).peek(i -> {
        assert 0 < i;
      }).toArray().length;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp_06_0050 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.getValue());
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0050;
      }).reduce((a, b) -> a + b).getAsInt();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp_06_0051 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.getValue());
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0051;
      }).reduce(0, (a, b) -> a + b);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp_06_0052 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.getValue());
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0052;
      }).collect(AtomicInteger::new, (a, x) -> a.addAndGet(x), (a, b) -> a.addAndGet(b.get())).get();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      if (null != values)
        values.freeRef();
      final int sum = com.simiacryptus.ref.wrappers.RefIntStream.range(1, 5).sum();
      assert 0 < sum : "0 >= " + sum;
    });
    testOperations(values -> {
      if (null != values)
        values.freeRef();
      assert 0 < com.simiacryptus.ref.wrappers.RefIntStream.generate(() -> (int) (Math.random() * 5)).limit(10).sum();
    });
  }

  private static void testIteratorOperations() {
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iterator = values
          .iterator();
      if (null != values)
        values.freeRef();
      while (iterator.hasNext()) {
        com.simiacryptus.ref.lang.RefUtil.freeRef(iterator.next());
        iterator.remove();
      }
      if (null != iterator)
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
        if (null != iterator)
          iterator.freeRef();
      }
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefSpliterator<com.simiacryptus.demo.refcount.BasicType> spliterator647 = values
          .spliterator();
      if (null != values)
        values.freeRef();
      while (spliterator647.tryAdvance(x -> {
        assert null != x;
        if (null != x)
          x.freeRef();
      })) {
      }
      if (null != spliterator647)
        spliterator647.freeRef();
    });
  }

  private static void testLongStream() {
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefLongStream intStream = values.stream().mapToLong(foobar1 -> {
        int temp_06_0053 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0053;
      });
      if (null != values)
        values.freeRef();
      final com.simiacryptus.ref.wrappers.RefPrimitiveIterator.OfLong iterator = intStream.iterator();
      while (iterator.hasNext()) {
        assert null != iterator.next();
      }
      if (null != iterator)
        iterator.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefLongStream intStream = values.stream().mapToLong(foobar1 -> {
        int temp_06_0054 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0054;
      });
      if (null != values)
        values.freeRef();
      final com.simiacryptus.ref.wrappers.RefSpliterator.OfLong iterator = intStream.spliterator();
      iterator.forEachRemaining((long i) -> {
        assert i > 0;
      });
      if (null != iterator)
        iterator.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp_06_0055 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0055;
      }).toArray().length;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp_06_0056 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0056;
      }).allMatch(i -> i > 0);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp_06_0057 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0057;
      }).anyMatch(i -> i > 0);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp_06_0058 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0058;
      }).asDoubleStream().toArray().length;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp_06_0059 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0059;
      }).average().getAsDouble() > 0;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp_06_0060 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0060;
      }).boxed().allMatch(x -> x != null);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp_06_0061 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0061;
      }).noneMatch(x -> x < 0);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp_06_0062 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0062;
      }).filter(i -> i > 0).parallel().count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp_06_0063 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0063;
      }).filter(i -> i > 0).mapToInt(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp_06_0064 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0064;
      }).filter(i -> i > 0).mapToObj(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp_06_0065 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0065;
      }).filter(i -> i > 0).mapToDouble(i -> Math.random()).distinct().count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp_06_0066 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0066;
      }).filter(i -> i > 0).map(i -> (long) (Math.random() * Long.MAX_VALUE)).distinct().count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp_06_0067 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0067;
      }).filter(i -> i > 0).findFirst().isPresent();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        int temp_06_0068 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0068;
      }).filter(i -> i > 0).unordered().findAny().isPresent();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp_06_0069 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0069;
      }).flatMap(i -> com.simiacryptus.ref.wrappers.RefLongStream.of(i, i, i)).skip(values.size()).limit(values.size())
          .count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        int temp_06_0070 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0070;
      }).sorted().summaryStatistics().getCount();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefLongStream parallel = values.stream().mapToLong(foobar1 -> {
        int temp_06_0071 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0071;
      }).parallel();
      if (null != values)
        values.freeRef();
      assert parallel.isParallel();
      parallel.close();
    });
    testOperations(values -> {
      values.stream().mapToLong(foobar1 -> {
        int temp_06_0072 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0072;
      }).parallel().sequential().forEachOrdered(i -> {
        assert i > 0;
      });
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().flatMapToLong(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp_06_0073 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(foobar1.getValue());
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0073;
      }).max().isPresent();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().flatMapToLong(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp_06_0074 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(foobar1.getValue());
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0074;
      }).min().isPresent();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToLong(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp_06_0075 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(foobar1.getValue());
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0075;
      }).peek(i -> {
        assert 0 < i;
      }).toArray().length;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToLong(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp_06_0076 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(foobar1.getValue());
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0076;
      }).reduce((a, b) -> a + b).getAsLong();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToLong(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp_06_0077 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(foobar1.getValue());
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0077;
      }).reduce(0, (a, b) -> a + b);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToLong(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp_06_0078 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(foobar1.getValue());
        if (null != foobar1)
          foobar1.freeRef();
        return temp_06_0078;
      }).collect(AtomicLong::new, (a, x) -> a.addAndGet(x), (a, b) -> a.addAndGet(b.get())).get();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      if (null != values)
        values.freeRef();
      final long sum = com.simiacryptus.ref.wrappers.RefLongStream.range(1, 5).sum();
      assert 0 < sum : "0 >= " + sum;
    });
    testOperations(values -> {
      if (null != values)
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
                      if (null != x)
                        x.freeRef();
                      return values.stream();
                    }, values == null ? null : values.addRef()))
            .distinct().count();
        if (null != values)
          values.freeRef();
      });
    if (false)
      testOperations(values_conditionalBlock -> {
        if (true) {
          com.simiacryptus.demo.refcount.BasicType[] temp_06_0092 = values_conditionalBlock.stream()
              .toArray(i -> new BasicType[i]);
          assert values_conditionalBlock.size() == temp_06_0092.length;
          if (null != temp_06_0092)
            com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_06_0092);
        }
        if (null != values_conditionalBlock)
          values_conditionalBlock.freeRef();
      });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp_06_0093 = values.stream().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).sorted().toArray(i -> new BasicType[i]);
      assert values.size() == temp_06_0093.length;
      if (null != temp_06_0093)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_06_0093);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp_06_0094 = values.stream().flatMap(x -> {
        x.setValue(x.getValue() + 1);
        com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp_06_0079 = com.simiacryptus.ref.wrappers.RefStream
            .of(x == null ? null : x.addRef());
        if (null != x)
          x.freeRef();
        return temp_06_0079;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp_06_0094.length;
      if (null != temp_06_0094)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_06_0094);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      values.stream().flatMap(x -> {
        x.setValue(x.getValue() + 1);
        com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp_06_0080 = com.simiacryptus.ref.wrappers.RefStream
            .of(x == null ? null : x.addRef(), new BasicType());
        if (null != x)
          x.freeRef();
        return temp_06_0080;
      }).forEach(x -> {
        assert x != null;
        if (null != x)
          x.freeRef();
      });
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().allMatch(x -> {
        boolean temp_06_0081 = x != null;
        if (null != x)
          x.freeRef();
        return temp_06_0081;
      });
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().peek(x -> {
        assert x != null;
        if (null != x)
          x.freeRef();
      }).count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp_06_0095 = values.stream()
          .reduce((reduceOpA, reduceOpB) -> {
            if (null != reduceOpB)
              reduceOpB.freeRef();
            if (null != reduceOpA)
              reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp_06_0095.isPresent();
      if (null != temp_06_0095)
        com.simiacryptus.ref.lang.RefUtil.freeRef(temp_06_0095);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp_06_0096 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            if (null != reduceOpB)
              reduceOpB.freeRef();
            if (null != reduceOpA)
              reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp_06_0096 != null;
      if (null != temp_06_0096)
        temp_06_0096.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp_06_0097 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            if (null != reduceOpB)
              reduceOpB.freeRef();
            if (null != reduceOpA)
              reduceOpA.freeRef();
            return new BasicType("reduceOp");
          }, (reduceOpA, reduceOpB) -> {
            if (null != reduceOpB)
              reduceOpB.freeRef();
            if (null != reduceOpA)
              reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp_06_0097 != null;
      if (null != temp_06_0097)
        temp_06_0097.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp_06_0098 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values.size() == temp_06_0098.size();
      if (null != temp_06_0098)
        temp_06_0098.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefSet<com.simiacryptus.demo.refcount.BasicType> temp_06_0099 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toSet());
      assert values.size() == temp_06_0099.size();
      if (null != temp_06_0099)
        temp_06_0099.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefMap<com.simiacryptus.demo.refcount.BasicType, java.lang.Integer> temp_06_0100 = values
          .stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.toMap(x -> {
            return x;
          }, x -> {
            int temp_06_0082 = x.getValue();
            if (null != x)
              x.freeRef();
            return temp_06_0082;
          }));
      assert values.size() == temp_06_0100.size();
      if (null != temp_06_0100)
        temp_06_0100.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefMap<com.simiacryptus.demo.refcount.BasicType, com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType>> temp_06_0101 = values
          .stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.groupingBy(x -> {
            return x;
          }));
      assert values.size() == temp_06_0101.size();
      if (null != temp_06_0101)
        temp_06_0101.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().anyMatch(x -> {
        boolean temp_06_0083 = x != null;
        if (null != x)
          x.freeRef();
        return temp_06_0083;
      });
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().noneMatch(x -> {
        boolean temp_06_0084 = x == null;
        if (null != x)
          x.freeRef();
        return temp_06_0084;
      });
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp_06_0102 = values.stream().filter(x -> {
        boolean temp_06_0085 = x == null;
        if (null != x)
          x.freeRef();
        return temp_06_0085;
      }).findAny();
      assert !temp_06_0102.isPresent();
      if (null != temp_06_0102)
        com.simiacryptus.ref.lang.RefUtil.freeRef(temp_06_0102);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp_06_0103 = values.stream().filter(x -> {
        boolean temp_06_0086 = x != null;
        if (null != x)
          x.freeRef();
        return temp_06_0086;
      }).findFirst();
      assert temp_06_0103.isPresent();
      if (null != temp_06_0103)
        com.simiacryptus.ref.lang.RefUtil.freeRef(temp_06_0103);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iter = values.stream()
          .iterator();
      if (null != values)
        values.freeRef();
      while (iter.hasNext()) {
        com.simiacryptus.demo.refcount.BasicType temp_06_0104 = iter.next();
        assert temp_06_0104 != null;
        if (null != temp_06_0104)
          temp_06_0104.freeRef();
      }
      if (null != iter)
        iter.freeRef();
    });
    testOperations(values -> {
      @NotNull
      com.simiacryptus.ref.wrappers.RefSpliterator<com.simiacryptus.demo.refcount.BasicType> iter = values.stream()
          .spliterator();
      if (null != values)
        values.freeRef();
      while (iter.tryAdvance(x -> {
        assert x != null;
        if (null != x)
          x.freeRef();
      })) {
      }
      if (null != iter)
        iter.freeRef();
    });
    testOperations(values -> {
      @NotNull
      com.simiacryptus.ref.wrappers.RefListIterator<com.simiacryptus.demo.refcount.BasicType> iter1056 = values
          .listIterator();
      if (null != values)
        values.freeRef();
      while (iter1056.hasNext()) {
        com.simiacryptus.demo.refcount.BasicType temp_06_0105 = iter1056.next();
        assert temp_06_0105 != null;
        if (null != temp_06_0105)
          temp_06_0105.freeRef();
      }
      if (null != iter1056)
        iter1056.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp_06_0106 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp_06_0106.length;
      if (null != temp_06_0106)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_06_0106);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp_06_0107 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp_06_0107.length;
      if (null != temp_06_0107)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_06_0107);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp_06_0108 = values.stream().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp_06_0108.length;
      if (null != temp_06_0108)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_06_0108);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp_06_0109 = values.stream()
          .max(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
            int temp_06_0087 = x.getValue();
            if (null != x)
              x.freeRef();
            return temp_06_0087;
          })).get();
      assert null != temp_06_0109;
      if (null != temp_06_0109)
        temp_06_0109.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp_06_0110 = values.stream()
          .min(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
            int temp_06_0088 = x.getValue();
            if (null != x)
              x.freeRef();
            return temp_06_0088;
          })).get();
      assert null != temp_06_0110;
      if (null != temp_06_0110)
        temp_06_0110.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      if (values.size() > 4 && values.stream().skip(1).limit(5).count() != 4) {
        if (null != values)
          values.freeRef();
        throw new AssertionError();
      }
      values.stream().forEach(x -> {
        x.setValue(x.getValue() + 1);
        if (null != x)
          x.freeRef();
      });
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      values.stream().parallel().forEach(x -> {
        x.setValue(x.getValue() + 1);
        if (null != x)
          x.freeRef();
      });
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefStream<BasicType> stream = values.stream().parallel();
      if (null != values)
        values.freeRef();
      if (!stream.isParallel())
        throw new AssertionError();
      stream.close();
    });
    testOperations(values -> {
      values.stream().forEachOrdered(x -> {
        x.setValue(x.getValue() + 1);
        if (null != x)
          x.freeRef();
      });
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp_06_0111 = values.stream().sequential().unordered().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp_06_0111.length;
      if (null != temp_06_0111)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_06_0111);
      if (null != values)
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

  public @Override @SuppressWarnings("unused") LinkedListContainer addRef() {
    return (LinkedListContainer) super.addRef();
  }

  public static @SuppressWarnings("unused") LinkedListContainer[] addRefs(LinkedListContainer[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(LinkedListContainer::addRef)
        .toArray((x) -> new LinkedListContainer[x]);
  }

  public static @SuppressWarnings("unused") LinkedListContainer[][] addRefs(LinkedListContainer[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(LinkedListContainer::addRefs)
        .toArray((x) -> new LinkedListContainer[x][]);
  }
}
