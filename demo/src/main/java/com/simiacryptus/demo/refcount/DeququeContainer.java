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

  private static void testOperations(
      com.simiacryptus.ref.wrappers.RefConsumer<com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<BasicType>> fn) {
    com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<BasicType> values = new com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<>();
    for (int i = 0; i < TestOperations.count; i++) {
      values.add(new BasicType());
    }
    fn.accept(values == null ? null : values.addRef());
    if (null != fn)
      fn.freeRef();
    if (null != values)
      values.freeRef();
  }

  private static void testArrayOperations(com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<BasicType> values) {
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
    com.simiacryptus.demo.refcount.BasicType[] temp_05_0016 = values.toArray(new BasicType[] {});
    if (values.size() != temp_05_0016.length) {
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (null != temp_05_0016)
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_05_0016);
    if (null != values)
      values.freeRef();
  }

  private static void testElementOperations(com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<BasicType> values) {
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
    final BasicType basicType2 = new BasicType();
    if (!values.add(basicType2 == null ? null : basicType2.addRef())) {
      if (null != basicType1)
        basicType1.freeRef();
      if (null != basicType2)
        basicType2.freeRef();
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (null != basicType2)
      basicType2.freeRef();
    if (values.size() != 2) {
      if (null != basicType1)
        basicType1.freeRef();
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    com.simiacryptus.demo.refcount.BasicType temp_05_0017 = values.poll();
    if (!temp_05_0017.equals(basicType1)) {
      if (null != basicType1)
        basicType1.freeRef();
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (null != temp_05_0017)
      temp_05_0017.freeRef();
    if (null != basicType1)
      basicType1.freeRef();
    if (values.size() != 1) {
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    values.clear();
    if (!values.isEmpty()) {
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (null != values)
      values.freeRef();
  }

  private static void testCollectionOperations(
      com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<BasicType> values) {
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

  private static void testDequeOperations() {
    testOperations(values -> {
      final int initSize = values.size();
      final BasicType basicType1 = new BasicType();
      final BasicType basicType2 = new BasicType();
      values.addFirst(basicType1 == null ? null : basicType1.addRef());
      values.addLast(basicType2 == null ? null : basicType2.addRef());
      assert values.size() == initSize + 2;
      com.simiacryptus.demo.refcount.BasicType temp_05_0018 = values.element();
      assert temp_05_0018 != null;
      if (null != temp_05_0018)
        temp_05_0018.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp_05_0019 = values.getFirst();
      assert temp_05_0019 == basicType1;
      if (null != temp_05_0019)
        temp_05_0019.freeRef();
      if (null != basicType1)
        basicType1.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp_05_0020 = values.getLast();
      assert temp_05_0020 == basicType2;
      if (null != temp_05_0020)
        temp_05_0020.freeRef();
      if (null != basicType2)
        basicType2.freeRef();
      assert values.size() == initSize + 2;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      final int initSize = values.size();
      final BasicType basicType1 = new BasicType();
      final BasicType basicType2 = new BasicType();
      values.addFirst(basicType1 == null ? null : basicType1.addRef());
      values.addLast(basicType2 == null ? null : basicType2.addRef());
      assert values.size() == initSize + 2;
      com.simiacryptus.demo.refcount.BasicType temp_05_0021 = values.peek();
      assert temp_05_0021 != null;
      if (null != temp_05_0021)
        temp_05_0021.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp_05_0022 = values.peekFirst();
      assert temp_05_0022 == basicType1;
      if (null != temp_05_0022)
        temp_05_0022.freeRef();
      if (null != basicType1)
        basicType1.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp_05_0023 = values.peekLast();
      assert temp_05_0023 == basicType2;
      if (null != temp_05_0023)
        temp_05_0023.freeRef();
      if (null != basicType2)
        basicType2.freeRef();
      assert values.size() == initSize + 2;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      final int initSize = values.size();
      final BasicType basicType1 = new BasicType();
      final BasicType basicType2 = new BasicType();
      values.addFirst(basicType1 == null ? null : basicType1.addRef());
      values.addLast(basicType2 == null ? null : basicType2.addRef());
      assert values.size() == initSize + 2;
      com.simiacryptus.demo.refcount.BasicType temp_05_0024 = values.pollFirst();
      assert temp_05_0024 == basicType1;
      if (null != temp_05_0024)
        temp_05_0024.freeRef();
      if (null != basicType1)
        basicType1.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp_05_0025 = values.pollLast();
      assert temp_05_0025 == basicType2;
      if (null != temp_05_0025)
        temp_05_0025.freeRef();
      if (null != basicType2)
        basicType2.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp_05_0026 = values.poll();
      assert temp_05_0026 != null;
      if (null != temp_05_0026)
        temp_05_0026.freeRef();
      assert values.size() == initSize - 1;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      final int initSize = values.size();
      final BasicType basicType0 = new BasicType();
      final BasicType basicType1 = new BasicType();
      final BasicType basicType2 = new BasicType();
      assert values.offer(basicType0 == null ? null : basicType0.addRef());
      assert values.offerFirst(basicType1 == null ? null : basicType1.addRef());
      assert values.offerLast(basicType2 == null ? null : basicType2.addRef());
      assert values.size() == initSize + 3;
      com.simiacryptus.demo.refcount.BasicType temp_05_0027 = values.removeFirst();
      assert temp_05_0027 == basicType1;
      if (null != temp_05_0027)
        temp_05_0027.freeRef();
      if (null != basicType1)
        basicType1.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp_05_0028 = values.removeLast();
      assert temp_05_0028 == basicType2;
      if (null != temp_05_0028)
        temp_05_0028.freeRef();
      if (null != basicType2)
        basicType2.freeRef();
      assert values.remove(basicType0 == null ? null : basicType0.addRef());
      if (null != basicType0)
        basicType0.freeRef();
      assert values.size() == initSize;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      final int initSize = values.size();
      final BasicType basicType0 = new BasicType();
      final BasicType basicType1 = new BasicType();
      final BasicType basicType2 = new BasicType();
      assert values.offer(basicType0 == null ? null : basicType0.addRef());
      if (null != basicType0)
        basicType0.freeRef();
      assert values.offerFirst(basicType1 == null ? null : basicType1.addRef());
      assert values.offerLast(basicType2 == null ? null : basicType2.addRef());
      assert values.size() == initSize + 3;
      assert values.removeFirstOccurrence(basicType1 == null ? null : basicType1.addRef());
      if (null != basicType1)
        basicType1.freeRef();
      assert values.removeLastOccurrence(basicType2 == null ? null : basicType2.addRef());
      if (null != basicType2)
        basicType2.freeRef();
      com.simiacryptus.demo.refcount.BasicType temp_05_0029 = values.remove();
      assert temp_05_0029 != null;
      if (null != temp_05_0029)
        temp_05_0029.freeRef();
      assert values.size() == initSize;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      values.clear();
      final int initSize = values.size();
      final BasicType basicType1 = new BasicType();
      values.push(basicType1 == null ? null : basicType1.addRef());
      assert values.size() == initSize + 1;
      com.simiacryptus.demo.refcount.BasicType temp_05_0030 = values.pop();
      assert temp_05_0030 == basicType1;
      if (null != temp_05_0030)
        temp_05_0030.freeRef();
      if (null != basicType1)
        basicType1.freeRef();
      assert values.size() == initSize;
      if (null != values)
        values.freeRef();
    });
  }

  private static void testIteratorOperations() {
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iterator48093 = values
          .iterator();
      if (null != values)
        values.freeRef();
      while (iterator48093.hasNext()) {
        com.simiacryptus.ref.lang.RefUtil.freeRef(iterator48093.next());
        iterator48093.remove();
      }
      if (null != iterator48093)
        iterator48093.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iterator48093 = values
          .descendingIterator();
      if (null != values)
        values.freeRef();
      while (iterator48093.hasNext()) {
        com.simiacryptus.ref.lang.RefUtil.freeRef(iterator48093.next());
        iterator48093.remove();
      }
      if (null != iterator48093)
        iterator48093.freeRef();
    });
    if (false) {
      testOperations(values -> {
        final com.simiacryptus.ref.wrappers.RefSpliterator<com.simiacryptus.demo.refcount.BasicType> spliterator = values
            .spliterator();
        if (null != values)
          values.freeRef();
        while (spliterator.tryAdvance(x -> {
          assert null != x;
          if (null != x)
            x.freeRef();
        })) {
        }
        if (null != spliterator)
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
                      if (null != x)
                        x.freeRef();
                      return values.stream();
                    }, values == null ? null : values.addRef()))
            .distinct().count();
        if (null != values)
          values.freeRef();
      });
    testOperations(values_conditionalBlock -> {
      if (true) {
        com.simiacryptus.demo.refcount.BasicType[] temp_05_0031 = values_conditionalBlock.stream()
            .toArray(i -> new BasicType[i]);
        assert values_conditionalBlock.size() == temp_05_0031.length;
        if (null != temp_05_0031)
          com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_05_0031);
      }
      if (null != values_conditionalBlock)
        values_conditionalBlock.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp_05_0032 = values.stream().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).sorted().toArray(i -> new BasicType[i]);
      assert values.size() == temp_05_0032.length;
      if (null != temp_05_0032)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_05_0032);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp_05_0033 = values.stream().flatMap(x -> {
        x.setValue(x.getValue() + 1);
        com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp_05_0001 = com.simiacryptus.ref.wrappers.RefStream
            .of(x == null ? null : x.addRef());
        if (null != x)
          x.freeRef();
        return temp_05_0001;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp_05_0033.length;
      if (null != temp_05_0033)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_05_0033);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      values.stream().flatMap(x -> {
        x.setValue(x.getValue() + 1);
        com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp_05_0002 = com.simiacryptus.ref.wrappers.RefStream
            .of(x == null ? null : x.addRef(), new BasicType());
        if (null != x)
          x.freeRef();
        return temp_05_0002;
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
        boolean temp_05_0003 = x != null;
        if (null != x)
          x.freeRef();
        return temp_05_0003;
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
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp_05_0034 = values.stream()
          .reduce((reduceOpA, reduceOpB) -> {
            if (null != reduceOpB)
              reduceOpB.freeRef();
            if (null != reduceOpA)
              reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp_05_0034.isPresent();
      if (null != temp_05_0034)
        com.simiacryptus.ref.lang.RefUtil.freeRef(temp_05_0034);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp_05_0035 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            if (null != reduceOpB)
              reduceOpB.freeRef();
            if (null != reduceOpA)
              reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp_05_0035 != null;
      if (null != temp_05_0035)
        temp_05_0035.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp_05_0036 = values.stream().reduce(new BasicType("reduceInit"),
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
      assert temp_05_0036 != null;
      if (null != temp_05_0036)
        temp_05_0036.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp_05_0037 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values.size() == temp_05_0037.size();
      if (null != temp_05_0037)
        temp_05_0037.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefSet<com.simiacryptus.demo.refcount.BasicType> temp_05_0038 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toSet());
      assert values.size() == temp_05_0038.size();
      if (null != temp_05_0038)
        temp_05_0038.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().anyMatch(x -> {
        boolean temp_05_0004 = x != null;
        if (null != x)
          x.freeRef();
        return temp_05_0004;
      });
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().noneMatch(x -> {
        boolean temp_05_0005 = x == null;
        if (null != x)
          x.freeRef();
        return temp_05_0005;
      });
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp_05_0039 = values.stream().filter(x -> {
        boolean temp_05_0006 = x == null;
        if (null != x)
          x.freeRef();
        return temp_05_0006;
      }).findAny();
      assert !temp_05_0039.isPresent();
      if (null != temp_05_0039)
        com.simiacryptus.ref.lang.RefUtil.freeRef(temp_05_0039);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp_05_0040 = values.stream().filter(x -> {
        boolean temp_05_0007 = x != null;
        if (null != x)
          x.freeRef();
        return temp_05_0007;
      }).findFirst();
      assert temp_05_0040.isPresent();
      if (null != temp_05_0040)
        com.simiacryptus.ref.lang.RefUtil.freeRef(temp_05_0040);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iter = values.stream()
          .iterator();
      if (null != values)
        values.freeRef();
      while (iter.hasNext()) {
        com.simiacryptus.demo.refcount.BasicType temp_05_0041 = iter.next();
        assert temp_05_0041 != null;
        if (null != temp_05_0041)
          temp_05_0041.freeRef();
      }
      if (null != iter)
        iter.freeRef();
    });
    testOperations(values -> {
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
      com.simiacryptus.demo.refcount.BasicType[] temp_05_0042 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp_05_0042.length;
      if (null != temp_05_0042)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_05_0042);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp_05_0043 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp_05_0043.length;
      if (null != temp_05_0043)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_05_0043);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp_05_0044 = values.stream().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp_05_0044.length;
      if (null != temp_05_0044)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_05_0044);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp_05_0045 = values.stream()
          .max(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
            int temp_05_0008 = x.getValue();
            if (null != x)
              x.freeRef();
            return temp_05_0008;
          })).get();
      assert null != temp_05_0045;
      if (null != temp_05_0045)
        temp_05_0045.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp_05_0046 = values.stream()
          .min(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
            int temp_05_0009 = x.getValue();
            if (null != x)
              x.freeRef();
            return temp_05_0009;
          })).get();
      assert null != temp_05_0046;
      if (null != temp_05_0046)
        temp_05_0046.freeRef();
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
      final com.simiacryptus.ref.wrappers.RefStream<BasicType> parallel = values.stream().parallel();
      if (null != values)
        values.freeRef();
      if (!parallel.isParallel())
        throw new AssertionError();
      parallel.close();
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
      com.simiacryptus.demo.refcount.BasicType[] temp_05_0047 = values.stream().sequential().unordered().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp_05_0047.length;
      if (null != temp_05_0047)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_05_0047);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp_05_0010 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_05_0010;
      }).toArray().length;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(x -> {
        int temp_05_0011 = x.getValue();
        if (null != x)
          x.freeRef();
        return temp_05_0011;
      }).toArray().length;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(x -> {
        int temp_05_0012 = x.getValue();
        if (null != x)
          x.freeRef();
        return temp_05_0012;
      }).toArray().length;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp_05_0013 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.getValue());
        if (null != foobar1)
          foobar1.freeRef();
        return temp_05_0013;
      }).toArray().length;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToDouble(x -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp_05_0014 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(x.getValue());
        if (null != x)
          x.freeRef();
        return temp_05_0014;
      }).toArray().length;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      final long[] longs = values.stream().flatMapToLong(x -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp_05_0015 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(x.getValue());
        if (null != x)
          x.freeRef();
        return temp_05_0015;
      }).toArray();
      assert values.size() == longs.length;
      if (null != values)
        values.freeRef();
    });
  }

  public @Override void _free() {
    super._free();
  }

  public @Override @SuppressWarnings("unused") DeququeContainer addRef() {
    return (DeququeContainer) super.addRef();
  }

  public static @SuppressWarnings("unused") DeququeContainer[] addRefs(DeququeContainer[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(DeququeContainer::addRef)
        .toArray((x) -> new DeququeContainer[x]);
  }

  public static @SuppressWarnings("unused") DeququeContainer[][] addRefs(DeququeContainer[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(DeququeContainer::addRefs)
        .toArray((x) -> new DeququeContainer[x][]);
  }
}
