/*
 * Copyright (c) 2020 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.*;

@SuppressWarnings("unused")
public class LinkedListContainer extends ReferenceCountingBase {
  public static void test() {
    for (int i = 0; i < TestOperations.count; i++) {
      testCollectionOperations(new LinkedList<>());
      testElementOperations(new LinkedList<>());
      testStreamOperations();
      testIteratorOperations();
    }
  }

  private static void testOperations(@Nonnull Consumer<LinkedList<BasicType>> fn) {
    LinkedList<BasicType> values = new LinkedList<>();
    for (int i = 0; i < TestOperations.count; i++) {
      values.add(new BasicType());
    }
    fn.accept(values);
  }

  private static void testArrayOperations(@Nonnull LinkedList<BasicType> values) {
    if (0 == values.size()) {
      throw new RuntimeException();
    }
    if (values.size() != values.toArray(new BasicType[]{}).length) {
      throw new RuntimeException();
    }
  }

  private static void testElementOperations(@Nonnull LinkedList<BasicType> values) {
    if (!values.isEmpty()) {
      throw new RuntimeException();
    }
    final BasicType basicType1 = new BasicType();
    if (!values.add(basicType1)) {
      throw new RuntimeException();
    }
    if (!values.contains(basicType1)) {
      throw new RuntimeException();
    }
    if (!values.add(values.iterator().next())) {
      throw new RuntimeException();
    }
    if (values.size() != 2) {
      throw new RuntimeException();
    }
    if (!values.remove(basicType1)) {
      throw new RuntimeException();
    }
    if (values.size() != 1) {
      throw new RuntimeException();
    }
    if (!values.add(basicType1)) {
      throw new RuntimeException();
    }
    values.remove(0);
    if (values.size() != 1) {
      throw new RuntimeException();
    }
    values.set(0, new BasicType());
    if (values.size() != 1) {
      throw new RuntimeException();
    }
    values.clear();
    final BasicType[] basicTypeN = new BasicType[]{new BasicType(), new BasicType(), new BasicType()};
    values.addAll(Arrays.asList(basicTypeN));
    values.add(1, basicType1);
    values.addAll(1, Arrays.asList(basicTypeN));
    if (values.indexOf(basicType1) != 4) {
      throw new RuntimeException();
    }
    if (values.indexOf(basicTypeN[0]) != 0) {
      throw new RuntimeException();
    }
    if (values.lastIndexOf(basicTypeN[0]) != 1) {
      throw new RuntimeException();
    }
    if (values.subList(1, 3).size() != 2) {
      throw new RuntimeException();
    }
    values.clear();
  }

  private static void testCollectionOperations(@Nonnull LinkedList<BasicType> values) {
    values.add(new BasicType());
    final BasicType basicType = new BasicType();
    final List<BasicType> list = Arrays.asList(basicType);
    if (!values.addAll(list)) {
      throw new RuntimeException();
    }
    if (!values.containsAll(list)) {
      throw new RuntimeException();
    }
    if (!values.retainAll(list)) {
      throw new RuntimeException();
    }
    testArrayOperations(values);
    values.removeAll(list);
    values.clear();
  }

  private static void testDoubleStream() {
    testOperations(values -> {
      final DoubleStream doubleStream = values.stream().mapToDouble(foobar1 -> {
        return foobar1.getValue();
      });
      final PrimitiveIterator.OfDouble iterator = doubleStream.iterator();
      while (iterator.hasNext()) {
        assert null != iterator.next();
      }
    });
    testOperations(values -> {
      final DoubleStream intStream = values.stream().mapToDouble(foobar1 -> {
        return foobar1.getValue();
      });
      final Spliterator.OfDouble iterator = intStream.spliterator();
      iterator.forEachRemaining((double i) -> {
        assert i > 0;
      });
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        return foobar1.getValue();
      }).toArray().length;
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        return foobar1.getValue();
      }).allMatch(i -> i > 0);
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        return foobar1.getValue();
      }).anyMatch(i -> i > 0);
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        return foobar1.getValue();
      }).average().getAsDouble() > 0;
    });
    testOperations(values -> {
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        return foobar1.getValue();
      }).noneMatch(x -> x < 0);
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).parallel().count();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).mapToInt(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).mapToObj(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).map(i -> Math.random()).distinct().count();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).mapToLong(i -> (long) (Math.random() * Long.MAX_VALUE)).distinct().count();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).findFirst().isPresent();
    });
    testOperations(values -> {
      assert values.stream().mapToDouble(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).unordered().findAny().isPresent();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        return foobar1.getValue();
      }).flatMap(i -> DoubleStream.of(i, i, i)).skip(values.size()).limit(values.size()).count();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(foobar1 -> {
        return foobar1.getValue();
      }).sorted().summaryStatistics().getCount();
    });
    testOperations(values -> {
      final DoubleStream parallel = values.stream().mapToDouble(foobar1 -> {
        return foobar1.getValue();
      }).parallel();
      assert parallel.isParallel();
      parallel.close();
    });
    testOperations(values -> {
      values.stream().mapToDouble(foobar1 -> {
        return foobar1.getValue();
      }).parallel().sequential().forEachOrdered(i -> {
        assert i > 0;
      });
    });
    testOperations(values -> {
      assert values.stream().flatMapToDouble(foobar1 -> {
        return DoubleStream.of(foobar1.getValue());
      }).max().isPresent();
    });
    testOperations(values -> {
      assert values.stream().flatMapToDouble(foobar1 -> {
        return DoubleStream.of(foobar1.getValue());
      }).min().isPresent();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToDouble(foobar1 -> {
        return DoubleStream.of(foobar1.getValue());
      }).peek(i -> {
        assert 0 < i;
      }).toArray().length;
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToDouble(foobar1 -> {
        return DoubleStream.of(foobar1.getValue());
      }).reduce((a, b) -> a + b).getAsDouble();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToDouble(foobar1 -> {
        return DoubleStream.of(foobar1.getValue());
      }).reduce(0, (a, b) -> a + b);
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToDouble(foobar1 -> {
        return DoubleStream.of(foobar1.getValue());
      }).collect(() -> new AtomicDouble(), (a, x) -> a.addAndGet(x), (a, b) -> a.addAndGet(b.get())).get();
    });
    testOperations(values -> {
      final double sum = DoubleStream.iterate(1, x -> x + 1).limit(10).sum();
      assert 0 < sum : "0 >= " + sum;
    });
    testOperations(values -> {
      assert 0 < DoubleStream.generate(() -> (int) (Math.random() * 5)).limit(10).sum();
    });
  }

  private static void testIntStream() {
    testOperations(values -> {
      final IntStream intStream = values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      });
      final PrimitiveIterator.OfInt iterator = intStream.iterator();
      while (iterator.hasNext()) {
        assert null != iterator.next();
      }
    });
    testOperations(values -> {
      final IntStream intStream = values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      });
      final Spliterator.OfInt iterator = intStream.spliterator();
      iterator.forEachRemaining((int i) -> {
        assert i > 0;
      });
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      }).toArray().length;
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      }).allMatch(i -> i > 0);
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      }).anyMatch(i -> i > 0);
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      }).asDoubleStream().toArray().length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      }).asLongStream().toArray().length;
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      }).average().getAsDouble() > 0;
    });
    testOperations(values -> {
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      }).noneMatch(x -> x < 0);
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).parallel().count();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).map(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).mapToObj(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).mapToDouble(i -> Math.random()).distinct().count();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).mapToLong(i -> (long) (Math.random() * Long.MAX_VALUE)).distinct().count();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).findFirst().isPresent();
    });
    testOperations(values -> {
      assert values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).unordered().findAny().isPresent();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      }).flatMap(i -> IntStream.of(i, i, i)).skip(values.size()).limit(values.size()).count();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      }).sorted().summaryStatistics().getCount();
    });
    testOperations(values -> {
      final IntStream parallel = values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      }).parallel();
      assert parallel.isParallel();
      parallel.close();
    });
    testOperations(values -> {
      values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      }).parallel().sequential().forEachOrdered(i -> {
        assert i > 0;
      });
    });
    testOperations(values -> {
      assert values.stream().flatMapToInt(foobar1 -> {
        return IntStream.of(foobar1.getValue());
      }).max().isPresent();
    });
    testOperations(values -> {
      assert values.stream().flatMapToInt(foobar1 -> {
        return IntStream.of(foobar1.getValue());
      }).min().isPresent();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToInt(foobar1 -> {
        return IntStream.of(foobar1.getValue());
      }).peek(i -> {
        assert 0 < i;
      }).toArray().length;
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToInt(foobar1 -> {
        return IntStream.of(foobar1.getValue());
      }).reduce((a, b) -> a + b).getAsInt();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToInt(foobar1 -> {
        return IntStream.of(foobar1.getValue());
      }).reduce(0, (a, b) -> a + b);
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToInt(foobar1 -> {
        return IntStream.of(foobar1.getValue());
      }).collect(() -> new AtomicInteger(), (a, x) -> a.addAndGet(x), (a, b) -> a.addAndGet(b.get())).get();
    });
    testOperations(values -> {
      final int sum = IntStream.range(1, 5).sum();
      assert 0 < sum : "0 >= " + sum;
    });
    testOperations(values -> {
      assert 0 < IntStream.generate(() -> (int) (Math.random() * 5)).limit(10).sum();
    });
  }

  private static void testIteratorOperations() {
    testOperations(values -> {
      final Iterator<BasicType> iterator = values.iterator();
      while (iterator.hasNext()) {
        iterator.next();
        iterator.remove();
      }
    });
    testOperations(values -> {
      if (values.size() > 3) {
        final ListIterator<BasicType> iterator = values.listIterator();
        assert 0 == iterator.nextIndex();
        if (iterator.hasNext()) {
          iterator.next();
          iterator.remove();
        }
        if (iterator.hasNext()) {
          iterator.next();
          iterator.add(new BasicType());
        }
        if (iterator.hasNext()) {
          iterator.next();
        }
        if (iterator.hasPrevious()) {
          assert 2 == iterator.previousIndex() : 2 + " != " + iterator.previousIndex();
          iterator.previous();
          iterator.set(new BasicType());
        }
      }
    });
    testOperations(values -> {
      final Spliterator<BasicType> spliterator647 = values.spliterator();
      while (spliterator647.tryAdvance(x -> {
        assert null != x;
      })) {
      }
    });
  }

  private static void testLongStream() {
    testOperations(values -> {
      final LongStream intStream = values.stream().mapToLong(foobar1 -> {
        return foobar1.getValue();
      });
      final PrimitiveIterator.OfLong iterator = intStream.iterator();
      while (iterator.hasNext()) {
        assert null != iterator.next();
      }
    });
    testOperations(values -> {
      final LongStream intStream = values.stream().mapToLong(foobar1 -> {
        return foobar1.getValue();
      });
      final Spliterator.OfLong iterator = intStream.spliterator();
      iterator.forEachRemaining((long i) -> {
        assert i > 0;
      });
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        return foobar1.getValue();
      }).toArray().length;
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        return foobar1.getValue();
      }).allMatch(i -> i > 0);
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        return foobar1.getValue();
      }).anyMatch(i -> i > 0);
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        return foobar1.getValue();
      }).asDoubleStream().toArray().length;
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        return foobar1.getValue();
      }).average().getAsDouble() > 0;
    });
    testOperations(values -> {
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        return foobar1.getValue();
      }).noneMatch(x -> x < 0);
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).parallel().count();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).mapToInt(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).mapToObj(i -> (int) (Math.random() * Integer.MAX_VALUE)).distinct().count();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).mapToDouble(i -> Math.random()).distinct().count();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).map(i -> (long) (Math.random() * Long.MAX_VALUE)).distinct().count();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).findFirst().isPresent();
    });
    testOperations(values -> {
      assert values.stream().mapToLong(foobar1 -> {
        return foobar1.getValue();
      }).filter(i -> i > 0).unordered().findAny().isPresent();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        return foobar1.getValue();
      }).flatMap(i -> LongStream.of(i, i, i)).skip(values.size()).limit(values.size()).count();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(foobar1 -> {
        return foobar1.getValue();
      }).sorted().summaryStatistics().getCount();
    });
    testOperations(values -> {
      final LongStream parallel = values.stream().mapToLong(foobar1 -> {
        return foobar1.getValue();
      }).parallel();
      assert parallel.isParallel();
      parallel.close();
    });
    testOperations(values -> {
      values.stream().mapToLong(foobar1 -> {
        return foobar1.getValue();
      }).parallel().sequential().forEachOrdered(i -> {
        assert i > 0;
      });
    });
    testOperations(values -> {
      assert values.stream().flatMapToLong(foobar1 -> {
        return LongStream.of(foobar1.getValue());
      }).max().isPresent();
    });
    testOperations(values -> {
      assert values.stream().flatMapToLong(foobar1 -> {
        return LongStream.of(foobar1.getValue());
      }).min().isPresent();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToLong(foobar1 -> {
        return LongStream.of(foobar1.getValue());
      }).peek(i -> {
        assert 0 < i;
      }).toArray().length;
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToLong(foobar1 -> {
        return LongStream.of(foobar1.getValue());
      }).reduce((a, b) -> a + b).getAsLong();
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToLong(foobar1 -> {
        return LongStream.of(foobar1.getValue());
      }).reduce(0, (a, b) -> a + b);
    });
    testOperations(values -> {
      assert 0 < values.stream().flatMapToLong(foobar1 -> {
        return LongStream.of(foobar1.getValue());
      }).collect(() -> new AtomicLong(), (a, x) -> a.addAndGet(x), (a, b) -> a.addAndGet(b.get())).get();
    });
    testOperations(values -> {
      final long sum = LongStream.range(1, 5).sum();
      assert 0 < sum : "0 >= " + sum;
    });
    testOperations(values -> {
      assert 0 < LongStream.generate(() -> (int) (Math.random() * 5)).limit(10).sum();
    });
  }

  private static void testObjStream() {
    testOperations(values -> {
      assert values.size() == values.stream().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).sorted().toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMap(x -> {
        x.setValue(x.getValue() + 1);
        return Stream.of(x);
      }).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      values.stream().flatMap(x -> {
        x.setValue(x.getValue() + 1);
        return Stream.of(x, new BasicType());
      }).forEach(x -> {
        assert x != null;
      });
    });
    testOperations(values -> {
      assert values.stream().allMatch(x -> {
        return x != null;
      });
    });
    testOperations(values -> {
      assert values.size() == values.stream().peek(x -> {
        assert x != null;
      }).count();
    });
    testOperations(values -> {
      assert values.stream().reduce((reduceOpA, reduceOpB) -> {
        return new BasicType("reduceOp");
      }).isPresent();
    });
    testOperations(values -> {
      assert values.stream().reduce(new BasicType("reduceInit"), (reduceOpA, reduceOpB) -> {
        return new BasicType("reduceOp");
      }) != null;
    });
    testOperations(values -> {
      assert values.stream().reduce(new BasicType("reduceInit"), (reduceOpA, reduceOpB) -> {
        return new BasicType("reduceOp");
      }, (reduceOpA, reduceOpB) -> {
        return new BasicType("reduceOp");
      }) != null;
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(Collectors.toList()).size();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(Collectors.toSet()).size();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(Collectors.toMap(x -> {
        return x;
      }, x -> {
        return x.getValue();
      })).size();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(Collectors.groupingBy(x -> {
        return x;
      })).size();
    });
    testOperations(values -> {
      assert values.stream().anyMatch(x -> {
        return x != null;
      });
    });
    testOperations(values -> {
      assert values.stream().noneMatch(x -> {
        return x == null;
      });
    });
    testOperations(values -> {
      assert !values.stream().filter(x -> {
        return x == null;
      }).findAny().isPresent();
    });
    testOperations(values -> {
      assert values.stream().filter(x -> {
        return x != null;
      }).findFirst().isPresent();
    });
    testOperations(values -> {
      Iterator<BasicType> iter = values.stream().iterator();
      while (iter.hasNext()) {
        assert iter.next() != null;
      }
    });
    testOperations(values -> {
      @Nonnull
      Spliterator<BasicType> iter = values.stream().spliterator();
      while (iter.tryAdvance(x -> {
        assert x != null;
      })) {
      }
    });
    testOperations(values -> {
      @Nonnull
      ListIterator<BasicType> iter1056 = values.listIterator();
      while (iter1056.hasNext()) {
        assert iter1056.next() != null;
      }
    });
    testOperations(values -> {
      assert values
          .size() == values.stream().sorted(Comparator.naturalOrder()).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert values
          .size() == values.stream().sorted(Comparator.naturalOrder()).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).sorted(Comparator.naturalOrder()).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
    });
    testOperations(values -> {
    });
    testOperations(values -> {
      if (values.size() > 4 && values.stream().skip(1).limit(5).count() != 4) {
        throw new AssertionError();
      }
      values.stream().forEach(x -> {
        x.setValue(x.getValue() + 1);
      });
    });
    testOperations(values -> {
      values.stream().parallel().forEach(x -> {
        x.setValue(x.getValue() + 1);
      });
    });
    testOperations(values -> {
      final Stream<BasicType> stream = values.stream().parallel();
      if (!stream.isParallel())
        throw new AssertionError();
      stream.close();
    });
    testOperations(values -> {
      values.stream().forEachOrdered(x -> {
        x.setValue(x.getValue() + 1);
      });
    });
    testOperations(values -> {
      assert values.size() == values.stream().sequential().unordered().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).toArray(i -> new BasicType[i]).length;
    });
  }

  private static void testStreamOperations() {
    testObjStream();
    testIntStream();
    testDoubleStream();
    testLongStream();
  }

  public @Override
  void _free() {
    super._free();
  }
}
