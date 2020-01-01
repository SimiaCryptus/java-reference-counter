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

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefArrayList;
import com.simiacryptus.ref.wrappers.RefArrays;
import com.simiacryptus.ref.wrappers.RefCollectors;
import com.simiacryptus.ref.wrappers.RefComparator;
import com.simiacryptus.ref.wrappers.RefConsumer;
import com.simiacryptus.ref.wrappers.RefDoubleStream;
import com.simiacryptus.ref.wrappers.RefHashSet;
import com.simiacryptus.ref.wrappers.RefIntStream;
import com.simiacryptus.ref.wrappers.RefIterator;
import com.simiacryptus.ref.wrappers.RefIteratorBase;
import com.simiacryptus.ref.wrappers.RefList;
import com.simiacryptus.ref.wrappers.RefListIterator;
import com.simiacryptus.ref.wrappers.RefLists;
import com.simiacryptus.ref.wrappers.RefLongStream;
import com.simiacryptus.ref.wrappers.RefSpliterator;
import com.simiacryptus.ref.wrappers.RefSpliterators;
import com.simiacryptus.ref.wrappers.RefStream;
import com.simiacryptus.ref.wrappers.RefStreamSupport;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import com.google.common.collect.Lists;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

/**
 * The type Array list container.
 */
public @RefAware class ArrayListContainer extends ReferenceCountingBase {
  /**
   * Instantiates a new Array list container.
   */
  public ArrayListContainer() {
  }

  @NotNull
  private static Predicate<BasicType> getTest() {
    return ArrayListContainer::test1;
  }

  /**
   * Test.
   */
  public static void test() {
    for (int i = 0; i < TestOperations.count; i++) {
      testCodeGen();
      testCollectionOperations();
      testElementOperations();
      testIteratorOperations();
      testStreamOperations();
    }
  }

  private static void testOperations(
      com.simiacryptus.ref.wrappers.RefConsumer<com.simiacryptus.ref.wrappers.RefArrayList<BasicType>> fn) {
    com.simiacryptus.ref.wrappers.RefArrayList<BasicType> values = new com.simiacryptus.ref.wrappers.RefArrayList<>();
    for (int i = 0; i < TestOperations.count; i++) {
      values.add(new BasicType());
    }
    fn.accept(values == null ? null : values.addRef());
    if (null != fn)
      fn.freeRef();
    if (null != values)
      values.freeRef();
  }

  private static void testArrayOperations(com.simiacryptus.ref.wrappers.RefArrayList<BasicType> values) {
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
    com.simiacryptus.demo.refcount.BasicType[] temp_00_0037 = values.toArray(new BasicType[] {});
    if (values.size() != temp_00_0037.length) {
      if (null != values)
        values.freeRef();
      throw new RuntimeException();
    }
    if (null != temp_00_0037)
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_00_0037);
    if (null != values)
      values.freeRef();
  }

  private static void testElementOperations() {
    testOperations(values -> {
      // Test
      values.clear();
      // Test
      if (!values.isEmpty()) {
        if (null != values)
          values.freeRef();
        throw new RuntimeException();
      }
      final BasicType basicType1 = new BasicType();
      if (!values.add(basicType1 == null ? null : basicType1.addRef())) {
        if (null != values)
          values.freeRef();
        if (null != basicType1)
          basicType1.freeRef();
        throw new RuntimeException();
      }
      if (!values.contains(basicType1 == null ? null : basicType1.addRef())) {
        if (null != values)
          values.freeRef();
        if (null != basicType1)
          basicType1.freeRef();
        throw new RuntimeException();
      }
      com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> temp_00_0038 = values
          .iterator();
      if (!values.add(temp_00_0038.next())) {
        if (null != values)
          values.freeRef();
        if (null != basicType1)
          basicType1.freeRef();
        throw new RuntimeException();
      }
      if (null != temp_00_0038)
        temp_00_0038.freeRef();
      if (values.size() != 2) {
        if (null != values)
          values.freeRef();
        if (null != basicType1)
          basicType1.freeRef();
        throw new RuntimeException();
      }
      if (!values.remove(basicType1 == null ? null : basicType1.addRef())) {
        if (null != values)
          values.freeRef();
        if (null != basicType1)
          basicType1.freeRef();
        throw new RuntimeException();
      }
      if (values.size() != 1) {
        if (null != values)
          values.freeRef();
        if (null != basicType1)
          basicType1.freeRef();
        throw new RuntimeException();
      }
      if (!values.add(basicType1 == null ? null : basicType1.addRef())) {
        if (null != values)
          values.freeRef();
        if (null != basicType1)
          basicType1.freeRef();
        throw new RuntimeException();
      }
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.remove(0));
      if (values.size() != 1) {
        if (null != values)
          values.freeRef();
        if (null != basicType1)
          basicType1.freeRef();
        throw new RuntimeException();
      }
      com.simiacryptus.ref.lang.RefUtil.freeRef(values.set(0, new BasicType()));
      if (values.size() != 1) {
        if (null != values)
          values.freeRef();
        if (null != basicType1)
          basicType1.freeRef();
        throw new RuntimeException();
      }
      values.clear();
      if (!values.isEmpty()) {
        if (null != values)
          values.freeRef();
        if (null != basicType1)
          basicType1.freeRef();
        throw new RuntimeException();
      }
      final BasicType[] basicTypeN = new BasicType[] { new BasicType(), new BasicType(), new BasicType() };
      values.addAll(
          com.simiacryptus.ref.wrappers.RefArrays.asList(com.simiacryptus.demo.refcount.BasicType.addRefs(basicTypeN)));
      values.add(1, basicType1 == null ? null : basicType1.addRef());
      values.addAll(1,
          com.simiacryptus.ref.wrappers.RefArrays.asList(com.simiacryptus.demo.refcount.BasicType.addRefs(basicTypeN)));
      if (values.indexOf(basicType1 == null ? null : basicType1.addRef()) != 4) {
        if (null != values)
          values.freeRef();
        if (null != basicType1)
          basicType1.freeRef();
        if (null != basicTypeN)
          com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(basicTypeN);
        throw new RuntimeException();
      }
      if (null != basicType1)
        basicType1.freeRef();
      if (values.indexOf(basicTypeN[0].addRef()) != 0) {
        if (null != values)
          values.freeRef();
        if (null != basicTypeN)
          com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(basicTypeN);
        throw new RuntimeException();
      }
      if (values.lastIndexOf(basicTypeN[0].addRef()) != 1) {
        if (null != values)
          values.freeRef();
        if (null != basicTypeN)
          com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(basicTypeN);
        throw new RuntimeException();
      }
      if (null != basicTypeN)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(basicTypeN);
      com.simiacryptus.ref.wrappers.RefArrayList<com.simiacryptus.demo.refcount.BasicType> temp_00_0039 = values
          .subList(1, 3);
      if (temp_00_0039.size() != 2) {
        if (null != values)
          values.freeRef();
        throw new RuntimeException();
      }
      if (null != temp_00_0039)
        temp_00_0039.freeRef();
      values.clear();
      if (!values.isEmpty()) {
        if (null != values)
          values.freeRef();
        throw new RuntimeException();
      }
      if (null != values)
        values.freeRef();
    });
  }

  private static void testCodeGen() {
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp_00_0040 = values.stream()
          .map(x -> {
            return x;
          }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values.size() == temp_00_0040.size();
      if (null != temp_00_0040)
        temp_00_0040.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values101 -> {
      com.simiacryptus.ref.wrappers.RefList<java.lang.Integer> temp_00_0041 = values101.stream().map(x156 -> {
        if (1 == 1) {
          int temp_00_0002 = x156.getValue();
          if (null != x156)
            x156.freeRef();
          return temp_00_0002;
        } else {
          if (null != x156)
            x156.freeRef();
          throw new RuntimeException();
        }
      }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values101.size() == temp_00_0041.size();
      if (null != temp_00_0041)
        temp_00_0041.freeRef();
      if (null != values101)
        values101.freeRef();
    });
    testOperations(values110 -> {
      com.simiacryptus.ref.wrappers.RefList<java.lang.Integer> temp_00_0042 = values110.stream().map(x111 -> {
        try {
          int temp_00_0003 = x111.getValue();
          if (null != x111)
            x111.freeRef();
          return temp_00_0003;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values110.size() == temp_00_0042.size();
      if (null != temp_00_0042)
        temp_00_0042.freeRef();
      if (null != values110)
        values110.freeRef();
    });
    testOperations(values110 -> {
      com.simiacryptus.ref.wrappers.RefList<java.lang.Integer> temp_00_0044 = values110.stream().map(x111 -> {
        try {
          com.simiacryptus.demo.refcount.BasicType temp_00_0043 = x111.getSelf();
          int temp_00_0004 = temp_00_0043.getValue();
          if (null != temp_00_0043)
            temp_00_0043.freeRef();
          if (null != x111)
            x111.freeRef();
          return temp_00_0004;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values110.size() == temp_00_0044.size();
      if (null != temp_00_0044)
        temp_00_0044.freeRef();
      if (null != values110)
        values110.freeRef();
    });
    testOperations(values110 -> {
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp_00_0046 = values110.stream()
          .map(x111 -> {
            try {
              com.simiacryptus.demo.refcount.BasicType temp_00_0045 = x111.getSelf();
              com.simiacryptus.demo.refcount.WrapperType<com.simiacryptus.demo.refcount.BasicType> temp_00_0005 = temp_00_0045
                  .wrap();
              if (null != temp_00_0045)
                temp_00_0045.freeRef();
              if (null != x111)
                x111.freeRef();
              return temp_00_0005;
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }).map(x -> {
            com.simiacryptus.demo.refcount.BasicType temp_00_0006 = x.getInner();
            if (null != x)
              x.freeRef();
            return temp_00_0006;
          }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values110.size() == temp_00_0046.size();
      if (null != temp_00_0046)
        temp_00_0046.freeRef();
      if (null != values110)
        values110.freeRef();
    });
    testOperations(values110 -> {
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp_00_0048 = values110.stream()
          .filter(x111 -> {
            try {
              com.simiacryptus.demo.refcount.BasicType temp_00_0047 = x111.getSelf();
              boolean temp_00_0007 = temp_00_0047.value > 0;
              if (null != temp_00_0047)
                temp_00_0047.freeRef();
              if (null != x111)
                x111.freeRef();
              return temp_00_0007;
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values110.size() == temp_00_0048.size();
      if (null != temp_00_0048)
        temp_00_0048.freeRef();
      if (null != values110)
        values110.freeRef();
    });
    testOperations(values110 -> {
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp_00_0050 = values110.stream()
          .filter(x111 -> {
            try {
              com.simiacryptus.demo.refcount.BasicType temp_00_0049 = x111.getSelf();
              boolean temp_00_0008 = -1 != temp_00_0049.value;
              if (null != temp_00_0049)
                temp_00_0049.freeRef();
              if (null != x111)
                x111.freeRef();
              return temp_00_0008;
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values110.size() == temp_00_0050.size();
      if (null != temp_00_0050)
        temp_00_0050.freeRef();
      if (null != values110)
        values110.freeRef();
    });
    testOperations(values110 -> {
      assert values110.size() == getCount1(values110.iterator());
      if (null != values110)
        values110.freeRef();
    });
    testOperations(values110 -> {
      assert values110.size() == getCount2(values110.iterator());
      if (null != values110)
        values110.freeRef();
    });
  }

  private static long getCount1(com.simiacryptus.ref.wrappers.RefIterator<BasicType> iterator) {
    long temp_00_0034 = com.simiacryptus.ref.wrappers.RefStreamSupport
        .stream(com.simiacryptus.ref.wrappers.RefSpliterators.spliterator(new RefIteratorBase<BasicType>() {
          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          @Override
          public BasicType next() {
            return iterator.next();
          }

          public @Override void _free() {
            if (null != iterator)
              iterator.freeRef();
            super._free();
          }

          {
            iterator.addRef();
          }
        }, -1, 0), false).filter(x272 -> {
          boolean temp_00_0009 = test1(x272 == null ? null : x272.addRef());
          if (null != x272)
            x272.freeRef();
          return temp_00_0009;
        }).filter(ArrayListContainer::test2).filter((Predicate<? super BasicType>) x275 -> {
          boolean temp_00_0010 = x275.getValue() >= 0;
          if (null != x275)
            x275.freeRef();
          return temp_00_0010;
        }).filter((Predicate<? super BasicType>) x277 -> {
          boolean temp_00_0011 = x277 != null;
          if (null != x277)
            x277.freeRef();
          return temp_00_0011;
        }).count();
    if (null != iterator)
      iterator.freeRef();
    return temp_00_0034;
  }

  private static long getCount2(com.simiacryptus.ref.wrappers.RefIterator<BasicType> iterator) {
    com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp_00_0051 = com.simiacryptus.ref.wrappers.RefStreamSupport
        .stream(com.simiacryptus.ref.wrappers.RefSpliterators.spliterator(new RefIteratorBase<BasicType>() {
          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          @Override
          public BasicType next() {
            return iterator.next();
          }

          public @Override void _free() {
            if (null != iterator)
              iterator.freeRef();
            super._free();
          }

          {
            iterator.addRef();
          }
        }, -1, 0), false).filter((Predicate<? super BasicType>) x -> {
          boolean temp_00_0012 = test1(x == null ? null : x.addRef());
          if (null != x)
            x.freeRef();
          return temp_00_0012;
        }).filter(ArrayListContainer::test2).filter((Predicate<? super BasicType>) x -> {
          boolean temp_00_0013 = x.getValue() >= 0;
          if (null != x)
            x.freeRef();
          return temp_00_0013;
        }).filter((Predicate<? super BasicType>) x -> {
          boolean temp_00_0014 = x != null;
          if (null != x)
            x.freeRef();
          return temp_00_0014;
        }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
    int temp_00_0035 = temp_00_0051.size();
    if (null != temp_00_0051)
      temp_00_0051.freeRef();
    if (null != iterator)
      iterator.freeRef();
    return temp_00_0035;
  }

  private static boolean test1(BasicType x) {
    boolean temp_00_0036 = 0 < x.value;
    if (null != x)
      x.freeRef();
    return temp_00_0036;
  }

  private static boolean test2(BasicType x) {
    if (null != x)
      x.freeRef();
    return null != getTest();
  }

  private static void testCollectionOperations() {
    testOperations(values289 -> {
      com.simiacryptus.ref.wrappers.RefList<java.lang.Integer> temp_00_0052 = values289.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.mapping(x291 -> {
            int temp_00_0015 = x291.getValue();
            if (null != x291)
              x291.freeRef();
            return temp_00_0015;
          }, com.simiacryptus.ref.wrappers.RefCollectors.toList()));
      assert values289.size() == temp_00_0052.size();
      if (null != temp_00_0052)
        temp_00_0052.freeRef();
      if (null != values289)
        values289.freeRef();
    });
    testOperations(values296 -> {
      com.simiacryptus.ref.wrappers.RefHashSet temp_00_0053 = values296.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors
              .collectingAndThen(com.simiacryptus.ref.wrappers.RefCollectors.toList(), x191 -> {
                com.simiacryptus.ref.wrappers.RefHashSet temp_00_0016 = new com.simiacryptus.ref.wrappers.RefHashSet(
                    x191 == null ? null : x191.addRef());
                if (null != x191)
                  x191.freeRef();
                return temp_00_0016;
              }));
      assert values296.size() == temp_00_0053.size();
      if (null != temp_00_0053)
        temp_00_0053.freeRef();
      if (null != values296)
        values296.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.reducing(0,
          (Function<? super BasicType, ? extends Integer>) x -> {
            if (null != x)
              x.freeRef();
            return 1;
          }, (a, b) -> a + b));
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().map(x -> {
        int temp_00_0017 = x.getValue();
        if (null != x)
          x.freeRef();
        return temp_00_0017;
      }).collect(com.simiacryptus.ref.wrappers.RefCollectors.reducing((a, b) -> a + b)).get();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.mapping(x -> {
        if (null != x)
          x.freeRef();
        return 1;
      }, com.simiacryptus.ref.wrappers.RefCollectors.reducing((a, b) -> a + b))).get();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(
          com.simiacryptus.ref.wrappers.RefCollectors.mapping((Function<? super BasicType, ? extends Integer>) x -> {
            if (null != x)
              x.freeRef();
            return 1;
          }, com.simiacryptus.ref.wrappers.RefCollectors.counting()));
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.counting());
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.ref.wrappers.RefList<BasicType>> partitions = com.simiacryptus.ref.wrappers.RefLists
          .partition(values == null ? null : values.addRef(), 2);
      if (null != values)
        values.freeRef();
      partitions.forEach(
          (com.simiacryptus.ref.wrappers.RefConsumer<? super com.simiacryptus.ref.wrappers.RefList<BasicType>>) x -> {
            assert x != null;
            if (null != x)
              x.freeRef();
          });
      if (null != partitions)
        partitions.freeRef();
    });
    testOperations(values -> {
      values.add(new BasicType());
      final BasicType basicType = new BasicType();
      final com.simiacryptus.ref.wrappers.RefList<BasicType> list = com.simiacryptus.ref.wrappers.RefArrays
          .asList(basicType == null ? null : basicType.addRef());
      if (null != basicType)
        basicType.freeRef();
      if (!values.addAll(list == null ? null : list.addRef())) {
        if (null != values)
          values.freeRef();
        if (null != list)
          list.freeRef();
        throw new RuntimeException();
      }
      if (!values.containsAll(list == null ? null : list.addRef())) {
        if (null != values)
          values.freeRef();
        if (null != list)
          list.freeRef();
        throw new RuntimeException();
      }
      if (!values.retainAll(list == null ? null : list.addRef())) {
        if (null != values)
          values.freeRef();
        if (null != list)
          list.freeRef();
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
    });
  }

  private static void testIteratorOperations() {
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefIterator<BasicType> iterator = values.iterator();
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
      final com.simiacryptus.ref.wrappers.RefListIterator<BasicType> iterator = values.listIterator();
      if (null != values)
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
        com.simiacryptus.ref.lang.RefUtil.freeRef(iterator.previous());
        iterator.set(new BasicType());
      }
      if (null != iterator)
        iterator.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefSpliterator<BasicType> spliterator = values.spliterator();
      if (null != values)
        values.freeRef();
      while (spliterator.tryAdvance((com.simiacryptus.ref.wrappers.RefConsumer<? super BasicType>) x -> {
        assert null != x;
        if (null != x)
          x.freeRef();
      })) {
      }
      if (null != spliterator)
        spliterator.freeRef();
    });
  }

  private static void testStreamOperations() {
    testOperations(values -> {
      assert values.size() == values.stream().flatMap(
          (Function<? super BasicType, ? extends com.simiacryptus.ref.wrappers.RefStream<? extends BasicType>>) com.simiacryptus.ref.lang.RefUtil
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
    testOperations(values372 -> {

      final @NotNull BasicType[] array374 = values372.toArray(new BasicType[] {});
      if (null != values372)
        values372.freeRef();
      final int inputIndex = 0;
      @NotNull
      BasicType[] outputPrototype377 = com.simiacryptus.demo.refcount.BasicType.addRefs(array374);

      final BasicType inputTensor = array374[inputIndex].addRef();
      final int inputDims = inputTensor.value;
      if (null != inputTensor)
        inputTensor.freeRef();
      @Nonnull
      final BasicType result = new BasicType();
      result.freeRef();
      for (int j = 0; j < outputPrototype377.length; j++) {
        final int j_ = j;
        @Nonnull
        final WrapperType<BasicType> inputKey = new WrapperType<BasicType>(new BasicType());
        inputKey.freeRef();
        final WrapperType[] copyInput = com.simiacryptus.ref.wrappers.RefArrays
            .stream(com.simiacryptus.demo.refcount.BasicType.addRefs(array374))
            .map((Function<? super BasicType, ? extends WrapperType>) x -> {
              com.simiacryptus.demo.refcount.WrapperType temp_00_0018 = new WrapperType(x == null ? null : x.addRef());
              if (null != x)
                x.freeRef();
              return temp_00_0018;
            }).toArray(i -> new WrapperType[i]);
        {
          com.simiacryptus.demo.refcount.WrapperType temp_00_0001 = new WrapperType(new BasicType()) {
            @Override
            public ReferenceCounting getInner() {
              return super.getInner();
            }

            public @Override void _free() {
              super._free();
            }
          };
          if (null != copyInput[inputIndex])
            copyInput[inputIndex].freeRef();
          copyInput[inputIndex] = temp_00_0001 == null ? null : temp_00_0001.addRef();
          if (null != temp_00_0001)
            temp_00_0001.freeRef();
        }
        @Nullable
        final WrapperType eval;
        try {
          eval = new WrapperType(new BasicType());
        } finally {
          for (@Nonnull
          WrapperType nnResult : copyInput) {
            com.simiacryptus.ref.lang.ReferenceCounting temp_00_0054 = nnResult.getInner();
            temp_00_0054.assertAlive();
            if (null != temp_00_0054)
              temp_00_0054.freeRef();
          }
        }
        if (null != copyInput)
          com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(copyInput);
        try {
          eval.assertAlive();
        } finally {
          eval.assertAlive();
        }
        if (null != eval)
          eval.freeRef();
      }
      if (null != outputPrototype377)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(outputPrototype377);
      if (null != array374)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(array374);
      return;

    });
    testOperations(values -> {
      final int initialSize = values.size();
      for (int i = 0; i < 5; i++) {
        values.add(new BasicType());
        assert initialSize + i + 1 == values.size();
        assert initialSize + i + 1 == values.stream().count();
      }
      if (null != values)
        values.freeRef();
    });
    testOperations(values_conditionalBlock -> {
      com.simiacryptus.demo.refcount.BasicType[] temp_00_0055 = values_conditionalBlock.stream()
          .toArray(i -> new BasicType[i]);
      assert values_conditionalBlock.size() == temp_00_0055.length;
      if (null != temp_00_0055)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_00_0055);
      if (null != values_conditionalBlock)
        values_conditionalBlock.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp_00_0056 = values.stream()
          .map((Function<? super BasicType, ? extends BasicType>) x -> {
            x.setValue(x.getValue() + 1);
            return x;
          }).sorted().toArray(i -> new BasicType[i]);
      assert values.size() == temp_00_0056.length;
      if (null != temp_00_0056)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_00_0056);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp_00_0057 = values.stream().flatMap(
          (Function<? super BasicType, ? extends com.simiacryptus.ref.wrappers.RefStream<? extends BasicType>>) x -> {
            x.setValue(x.getValue() + 1);
            com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp_00_0019 = com.simiacryptus.ref.wrappers.RefStream
                .of(x == null ? null : x.addRef());
            if (null != x)
              x.freeRef();
            return temp_00_0019;
          }).toArray(i -> new BasicType[i]);
      assert values.size() == temp_00_0057.length;
      if (null != temp_00_0057)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_00_0057);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      values.stream().flatMap(
          (Function<? super BasicType, ? extends com.simiacryptus.ref.wrappers.RefStream<? extends BasicType>>) x -> {
            x.setValue(x.getValue() + 1);
            com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp_00_0020 = com.simiacryptus.ref.wrappers.RefStream
                .of(x == null ? null : x.addRef(), new BasicType());
            if (null != x)
              x.freeRef();
            return temp_00_0020;
          }).forEach((com.simiacryptus.ref.wrappers.RefConsumer<? super BasicType>) x -> {
            assert x != null;
            if (null != x)
              x.freeRef();
          });
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().allMatch((Predicate<? super BasicType>) x -> {
        boolean temp_00_0021 = x != null;
        if (null != x)
          x.freeRef();
        return temp_00_0021;
      });
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().peek((com.simiacryptus.ref.wrappers.RefConsumer<? super BasicType>) x -> {
        assert x != null;
        if (null != x)
          x.freeRef();
      }).count();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp_00_0058 = values.stream()
          .reduce((reduceOpA, reduceOpB) -> {
            if (null != reduceOpB)
              reduceOpB.freeRef();
            if (null != reduceOpA)
              reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp_00_0058.isPresent();
      if (null != temp_00_0058)
        com.simiacryptus.ref.lang.RefUtil.freeRef(temp_00_0058);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp_00_0059 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            if (null != reduceOpB)
              reduceOpB.freeRef();
            if (null != reduceOpA)
              reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp_00_0059 != null;
      if (null != temp_00_0059)
        temp_00_0059.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp_00_0060 = values.stream().reduce(new BasicType("reduceInit"),
          (BiFunction<BasicType, ? super BasicType, BasicType>) (reduceOpA, reduceOpB) -> {
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
      assert temp_00_0060 != null;
      if (null != temp_00_0060)
        temp_00_0060.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp_00_0061 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp_00_0062 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values.size() == temp_00_0061.size() : values.size() + " != " + temp_00_0062.size();
      if (null != temp_00_0062)
        temp_00_0062.freeRef();
      if (null != temp_00_0061)
        temp_00_0061.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefSet<com.simiacryptus.demo.refcount.BasicType> temp_00_0063 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toSet());
      assert values.size() == temp_00_0063.size();
      if (null != temp_00_0063)
        temp_00_0063.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().anyMatch((Predicate<? super BasicType>) x -> {
        boolean temp_00_0022 = x != null;
        if (null != x)
          x.freeRef();
        return temp_00_0022;
      });
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().noneMatch((Predicate<? super BasicType>) x -> {
        boolean temp_00_0023 = x == null;
        if (null != x)
          x.freeRef();
        return temp_00_0023;
      });
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp_00_0064 = values.stream()
          .filter((Predicate<? super BasicType>) x -> {
            boolean temp_00_0024 = x == null;
            if (null != x)
              x.freeRef();
            return temp_00_0024;
          }).findAny();
      assert !temp_00_0064.isPresent();
      if (null != temp_00_0064)
        com.simiacryptus.ref.lang.RefUtil.freeRef(temp_00_0064);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp_00_0065 = values.stream()
          .filter((Predicate<? super BasicType>) x -> {
            boolean temp_00_0025 = x != null;
            if (null != x)
              x.freeRef();
            return temp_00_0025;
          }).findFirst();
      assert temp_00_0065.isPresent();
      if (null != temp_00_0065)
        com.simiacryptus.ref.lang.RefUtil.freeRef(temp_00_0065);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefIterator<BasicType> iter = values.stream().iterator();
      if (null != values)
        values.freeRef();
      while (iter.hasNext()) {
        com.simiacryptus.demo.refcount.BasicType temp_00_0066 = iter.next();
        assert temp_00_0066 != null;
        if (null != temp_00_0066)
          temp_00_0066.freeRef();
      }
      if (null != iter)
        iter.freeRef();
    });
    testOperations(values -> {
      @NotNull
      com.simiacryptus.ref.wrappers.RefSpliterator<BasicType> iter = values.stream().spliterator();
      if (null != values)
        values.freeRef();
      while (iter.tryAdvance((com.simiacryptus.ref.wrappers.RefConsumer<? super BasicType>) x -> {
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
      com.simiacryptus.ref.wrappers.RefListIterator<BasicType> iter = values.listIterator();
      if (null != values)
        values.freeRef();
      while (iter.hasNext()) {
        com.simiacryptus.demo.refcount.BasicType temp_00_0067 = iter.next();
        assert temp_00_0067 != null;
        if (null != temp_00_0067)
          temp_00_0067.freeRef();
      }
      if (null != iter)
        iter.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp_00_0068 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp_00_0068.length;
      if (null != temp_00_0068)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_00_0068);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp_00_0069 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp_00_0069.length;
      if (null != temp_00_0069)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_00_0069);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp_00_0070 = values.stream()
          .map((Function<? super BasicType, ? extends BasicType>) x -> {
            x.setValue(x.getValue() + 1);
            return x;
          }).sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp_00_0070.length;
      if (null != temp_00_0070)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_00_0070);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp_00_0071 = values.stream().max(
          com.simiacryptus.ref.wrappers.RefComparator.comparing((Function<? super BasicType, ? extends Integer>) x -> {
            int temp_00_0026 = x.getValue();
            if (null != x)
              x.freeRef();
            return temp_00_0026;
          })).get();
      assert null != temp_00_0071;
      if (null != temp_00_0071)
        temp_00_0071.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp_00_0072 = values.stream().min(
          com.simiacryptus.ref.wrappers.RefComparator.comparing((Function<? super BasicType, ? extends Integer>) x -> {
            int temp_00_0027 = x.getValue();
            if (null != x)
              x.freeRef();
            return temp_00_0027;
          })).get();
      assert null != temp_00_0072;
      if (null != temp_00_0072)
        temp_00_0072.freeRef();
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      if (values.size() > 4 && values.stream().skip(1).limit(5).count() != 4) {
        if (null != values)
          values.freeRef();
        throw new AssertionError();
      }
      values.stream().forEach((com.simiacryptus.ref.wrappers.RefConsumer<? super BasicType>) x -> {
        x.setValue(x.getValue() + 1);
        if (null != x)
          x.freeRef();
      });
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      values.stream().parallel().forEach((com.simiacryptus.ref.wrappers.RefConsumer<? super BasicType>) x -> {
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
      values.stream().forEachOrdered((com.simiacryptus.ref.wrappers.RefConsumer<? super BasicType>) x -> {
        x.setValue(x.getValue() + 1);
        if (null != x)
          x.freeRef();
      });
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp_00_0073 = values.stream().sequential().unordered()
          .map((Function<? super BasicType, ? extends BasicType>) x -> {
            x.setValue(x.getValue() + 1);
            return x;
          }).toArray(i -> new BasicType[i]);
      assert values.size() == temp_00_0073.length;
      if (null != temp_00_0073)
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp_00_0073);
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt((ToIntFunction<? super BasicType>) foobar1 -> {
        int temp_00_0028 = foobar1.getValue();
        if (null != foobar1)
          foobar1.freeRef();
        return temp_00_0028;
      }).toArray().length;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble((ToDoubleFunction<? super BasicType>) x -> {
        int temp_00_0029 = x.getValue();
        if (null != x)
          x.freeRef();
        return temp_00_0029;
      }).toArray().length;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong((ToLongFunction<? super BasicType>) x -> {
        int temp_00_0030 = x.getValue();
        if (null != x)
          x.freeRef();
        return temp_00_0030;
      }).toArray().length;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream()
          .flatMapToInt((Function<? super BasicType, ? extends com.simiacryptus.ref.wrappers.RefIntStream>) foobar1 -> {
            com.simiacryptus.ref.wrappers.RefIntStream temp_00_0031 = com.simiacryptus.ref.wrappers.RefIntStream
                .of(foobar1.getValue());
            if (null != foobar1)
              foobar1.freeRef();
            return temp_00_0031;
          }).toArray().length;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream()
          .flatMapToDouble((Function<? super BasicType, ? extends com.simiacryptus.ref.wrappers.RefDoubleStream>) x -> {
            com.simiacryptus.ref.wrappers.RefDoubleStream temp_00_0032 = com.simiacryptus.ref.wrappers.RefDoubleStream
                .of(x.getValue());
            if (null != x)
              x.freeRef();
            return temp_00_0032;
          }).toArray().length;
      if (null != values)
        values.freeRef();
    });
    testOperations(values -> {
      final long[] longs = values.stream()
          .flatMapToLong((Function<? super BasicType, ? extends com.simiacryptus.ref.wrappers.RefLongStream>) x -> {
            com.simiacryptus.ref.wrappers.RefLongStream temp_00_0033 = com.simiacryptus.ref.wrappers.RefLongStream
                .of(x.getValue());
            if (null != x)
              x.freeRef();
            return temp_00_0033;
          }).toArray();
      assert values.size() == longs.length;
      if (null != values)
        values.freeRef();
    });
  }

  public @Override void _free() {
    super._free();
  }

  public @Override @SuppressWarnings("unused") ArrayListContainer addRef() {
    return (ArrayListContainer) super.addRef();
  }

  public static @SuppressWarnings("unused") ArrayListContainer[] addRefs(ArrayListContainer[] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ArrayListContainer::addRef)
        .toArray((x) -> new ArrayListContainer[x]);
  }

  public static @SuppressWarnings("unused") ArrayListContainer[][] addRefs(ArrayListContainer[][] array) {
    if (array == null)
      return null;
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ArrayListContainer::addRefs)
        .toArray((x) -> new ArrayListContainer[x][]);
  }
}
