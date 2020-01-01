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

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefIteratorBase;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.*;

@SuppressWarnings("unused")
public @RefAware
class ArrayListContainer extends ReferenceCountingBase {
  public ArrayListContainer() {
  }

  @NotNull
  private static Predicate<BasicType> getTest() {
    return ArrayListContainer::test1;
  }

  public static void test() {
    for (int i = 0; i < TestOperations.count; i++) {
      testCodeGen();
      testCollectionOperations();
      testElementOperations();
      testIteratorOperations();
      testStreamOperations();
    }
  }

  private static void testOperations(@NotNull java.util.function.Consumer<java.util.ArrayList<BasicType>> fn) {
    java.util.ArrayList<BasicType> values = new java.util.ArrayList<>();
    for (int i = 0; i < TestOperations.count; i++) {
      values.add(new BasicType());
    }
    fn.accept(values);
  }

  private static void testArrayOperations(@NotNull java.util.ArrayList<BasicType> values) {
    if (0 == values.size()) {
      throw new RuntimeException();
    }
    if (false) {
      if (values.size() != values.toArray().length) {
        throw new RuntimeException();
      }
    }
    if (values.size() != values.toArray(new BasicType[]{}).length) {
      throw new RuntimeException();
    }
  }

  private static void testElementOperations() {
    testOperations(values -> {
      // Test
      values.clear();
      // Test
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
      if (!values.isEmpty()) {
        throw new RuntimeException();
      }
      final BasicType[] basicTypeN = new BasicType[]{new BasicType(), new BasicType(), new BasicType()};
      values.addAll(java.util.Arrays.asList(basicTypeN));
      values.add(1, basicType1);
      values.addAll(1, java.util.Arrays.asList(basicTypeN));
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
      if (!values.isEmpty()) {
        throw new RuntimeException();
      }
    });
  }

  private static void testCodeGen() {
    testOperations(values -> {
      assert values.size() == values.stream().map(x -> {
        return x;
      }).collect(java.util.stream.Collectors.toList()).size();
    });
    testOperations(values101 -> {
      assert values101.size() == values101.stream().map(x156 -> {
        if (1 == 1) {
          return x156.getValue();
        } else {
          throw new RuntimeException();
        }
      }).collect(java.util.stream.Collectors.toList()).size();
    });
    testOperations(values110 -> {
      assert values110.size() == values110.stream().map(x111 -> {
        try {
          return x111.getValue();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).collect(java.util.stream.Collectors.toList()).size();
    });
    testOperations(values110 -> {
      assert values110.size() == values110.stream().map(x111 -> {
        try {
          return x111.getSelf().getValue();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).collect(java.util.stream.Collectors.toList()).size();
    });
    testOperations(values110 -> {
      assert values110.size() == values110.stream().map(x111 -> {
        try {
          return x111.getSelf().wrap();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).map(x -> {
        return x.getInner();
      }).collect(java.util.stream.Collectors.toList()).size();
    });
    testOperations(values110 -> {
      assert values110.size() == values110.stream().filter(x111 -> {
        try {
          return x111.getSelf().value > 0;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).collect(java.util.stream.Collectors.toList()).size();
    });
    testOperations(values110 -> {
      assert values110.size() == values110.stream().filter(x111 -> {
        try {
          return -1 != x111.getSelf().value;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).collect(java.util.stream.Collectors.toList()).size();
    });
    testOperations(values110 -> {
      assert values110.size() == getCount1(values110.iterator());
    });
    testOperations(values110 -> {
      assert values110.size() == getCount2(values110.iterator());
    });
  }

  private static long getCount1(@NotNull java.util.Iterator<BasicType> iterator) {
    return java.util.stream.StreamSupport.stream(java.util.Spliterators.spliterator(new RefIteratorBase<BasicType>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public BasicType next() {
        return iterator.next();
      }

      public @Override
      void _free() {
        super._free();
      }
    }, -1, 0), false).filter(x272 -> {
      return test1(x272);
    }).filter(ArrayListContainer::test2).filter((Predicate<? super BasicType>) x275 -> {
      return x275.getValue() >= 0;
    }).filter((Predicate<? super BasicType>) x277 -> {
      return x277 != null;
    }).count();
  }

  private static long getCount2(@NotNull java.util.Iterator<BasicType> iterator) {
    return java.util.stream.StreamSupport.stream(java.util.Spliterators.spliterator(new RefIteratorBase<BasicType>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public BasicType next() {
        return iterator.next();
      }

      public @Override
      void _free() {
        super._free();
      }
    }, -1, 0), false).filter((Predicate<? super BasicType>) x -> {
      return test1(x);
    }).filter(ArrayListContainer::test2).filter((Predicate<? super BasicType>) x -> {
      return x.getValue() >= 0;
    }).filter((Predicate<? super BasicType>) x -> {
      return x != null;
    }).collect(java.util.stream.Collectors.toList()).size();
  }

  private static boolean test1(@NotNull BasicType x) {
    return 0 < x.value;
  }

  private static boolean test2(BasicType x) {
    return null != getTest();
  }

  private static void testCollectionOperations() {
    testOperations(values289 -> {
      assert values289.size() == values289.stream().collect(java.util.stream.Collectors.mapping(x291 -> {
        return x291.getValue();
      }, java.util.stream.Collectors.toList())).size();
    });
    testOperations(values296 -> {
      assert values296.size() == values296.stream()
          .collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(), x191 -> {
            return new java.util.HashSet(x191);
          })).size();
    });
    testOperations(values -> {
      assert values.size() == values.stream()
          .collect(java.util.stream.Collectors.reducing(0, (Function<? super BasicType, ? extends Integer>) x -> {
            return 1;
          }, (a, b) -> a + b));
    });
    testOperations(values -> {
      assert values.size() == values.stream().map(x -> {
        return x.getValue();
      }).collect(java.util.stream.Collectors.reducing((a, b) -> a + b)).get();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(java.util.stream.Collectors.mapping(x -> {
        return 1;
      }, java.util.stream.Collectors.reducing((a, b) -> a + b))).get();
    });
    testOperations(values -> {
      assert values.size() == values.stream()
          .collect(java.util.stream.Collectors.mapping((Function<? super BasicType, ? extends Integer>) x -> {
            return 1;
          }, java.util.stream.Collectors.counting()));
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(java.util.stream.Collectors.counting());
    });
    testOperations(values -> {
      final java.util.List<java.util.List<BasicType>> partitions = com.google.common.collect.Lists.partition(values, 2);
      partitions.forEach((java.util.function.Consumer<? super java.util.List<BasicType>>) x -> {
        assert x != null;
      });
    });
    testOperations(values -> {
      values.add(new BasicType());
      final BasicType basicType = new BasicType();
      final java.util.List<BasicType> list = java.util.Arrays.asList(basicType);
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
      if (!values.isEmpty()) {
        throw new RuntimeException();
      }
    });
  }

  private static void testIteratorOperations() {
    testOperations(values -> {
      final java.util.Iterator<BasicType> iterator = values.iterator();
      while (iterator.hasNext()) {
        iterator.next();
        iterator.remove();
      }
    });
    testOperations(values -> {
      final java.util.ListIterator<BasicType> iterator = values.listIterator();
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
        iterator.previous();
        iterator.set(new BasicType());
      }
    });
    testOperations(values -> {
      final java.util.Spliterator<BasicType> spliterator = values.spliterator();
      while (spliterator.tryAdvance((java.util.function.Consumer<? super BasicType>) x -> {
        assert null != x;
      })) {
      }
    });
  }

  private static void testStreamOperations() {
    testOperations(values -> {
      assert values.size() == values.stream()
          .flatMap((Function<? super BasicType, ? extends java.util.stream.Stream<? extends BasicType>>) x -> {
            return values.stream();
          }).distinct().count();
    });
    testOperations(values372 -> {

      final @NotNull BasicType[] array374 = values372.toArray(new BasicType[]{});
      final int inputIndex = 0;
      @NotNull
      BasicType[] outputPrototype377 = array374;

      final BasicType inputTensor = array374[inputIndex];
      final int inputDims = inputTensor.value;
      @Nonnull final BasicType result = new BasicType();
      for (int j = 0; j < outputPrototype377.length; j++) {
        final int j_ = j;
        @Nonnull final WrapperType<BasicType> inputKey = new WrapperType<BasicType>(new BasicType());
        final WrapperType[] copyInput = java.util.Arrays.stream(array374)
            .map((Function<? super BasicType, ? extends WrapperType>) x -> {
              return new WrapperType(x);
            }).toArray(i -> new WrapperType[i]);
        {
          copyInput[inputIndex] = new WrapperType(new BasicType()) {
            @Override
            public ReferenceCounting getInner() {
              return super.getInner();
            }

            public @Override
            void _free() {
              super._free();
            }
          };
        }
        @Nullable final WrapperType eval;
        try {
          eval = new WrapperType(new BasicType());
        } finally {
          for (@Nonnull
              WrapperType nnResult : copyInput) {
            nnResult.getInner().assertAlive();
          }
        }
        try {
          eval.assertAlive();
        } finally {
          eval.assertAlive();
        }
      }
      return;

    });
    testOperations(values -> {
      final int initialSize = values.size();
      for (int i = 0; i < 5; i++) {
        values.add(new BasicType());
        assert initialSize + i + 1 == values.size();
        assert initialSize + i + 1 == values.stream().count();
      }
    });
    testOperations(values_conditionalBlock -> {
      assert values_conditionalBlock.size() == values_conditionalBlock.stream().toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().map((Function<? super BasicType, ? extends BasicType>) x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).sorted().toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert values.size() == values.stream()
          .flatMap((Function<? super BasicType, ? extends java.util.stream.Stream<? extends BasicType>>) x -> {
            x.setValue(x.getValue() + 1);
            return java.util.stream.Stream.of(x);
          }).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      values.stream()
          .flatMap((Function<? super BasicType, ? extends java.util.stream.Stream<? extends BasicType>>) x -> {
            x.setValue(x.getValue() + 1);
            return java.util.stream.Stream.of(x, new BasicType());
          }).forEach((java.util.function.Consumer<? super BasicType>) x -> {
        assert x != null;
      });
    });
    testOperations(values -> {
      assert values.stream().allMatch((Predicate<? super BasicType>) x -> {
        return x != null;
      });
    });
    testOperations(values -> {
      assert values.size() == values.stream().peek((java.util.function.Consumer<? super BasicType>) x -> {
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
      assert values.stream().reduce(new BasicType("reduceInit"),
          (BiFunction<BasicType, ? super BasicType, BasicType>) (reduceOpA, reduceOpB) -> {
            return new BasicType("reduceOp");
          }, (reduceOpA, reduceOpB) -> {
            return new BasicType("reduceOp");
          }) != null;
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(java.util.stream.Collectors.toList()).size() : values.size()
          + " != " + values.stream().collect(java.util.stream.Collectors.toList()).size();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(java.util.stream.Collectors.toSet()).size();
    });
    testOperations(values -> {
      assert values.stream().anyMatch((Predicate<? super BasicType>) x -> {
        return x != null;
      });
    });
    testOperations(values -> {
      assert values.stream().noneMatch((Predicate<? super BasicType>) x -> {
        return x == null;
      });
    });
    testOperations(values -> {
      assert !values.stream().filter((Predicate<? super BasicType>) x -> {
        return x == null;
      }).findAny().isPresent();
    });
    testOperations(values -> {
      assert values.stream().filter((Predicate<? super BasicType>) x -> {
        return x != null;
      }).findFirst().isPresent();
    });
    testOperations(values -> {
      java.util.Iterator<BasicType> iter = values.stream().iterator();
      while (iter.hasNext()) {
        assert iter.next() != null;
      }
    });
    testOperations(values -> {
      @NotNull
      java.util.Spliterator<BasicType> iter = values.stream().spliterator();
      while (iter.tryAdvance((java.util.function.Consumer<? super BasicType>) x -> {
        assert x != null;
      })) {
      }
    });
    testOperations(values -> {
      @NotNull
      java.util.ListIterator<BasicType> iter = values.listIterator();
      while (iter.hasNext()) {
        assert iter.next() != null;
      }
    });
    testOperations(values -> {
      assert values
          .size() == values.stream().sorted(java.util.Comparator.naturalOrder()).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert values
          .size() == values.stream().sorted(java.util.Comparator.naturalOrder()).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().map((Function<? super BasicType, ? extends BasicType>) x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).sorted(java.util.Comparator.naturalOrder()).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert null != values.stream()
          .max(java.util.Comparator.comparing((Function<? super BasicType, ? extends Integer>) x -> {
            return x.getValue();
          })).get();
    });
    testOperations(values -> {
      assert null != values.stream()
          .min(java.util.Comparator.comparing((Function<? super BasicType, ? extends Integer>) x -> {
            return x.getValue();
          })).get();
    });
    testOperations(values -> {
      if (values.size() > 4 && values.stream().skip(1).limit(5).count() != 4) {
        throw new AssertionError();
      }
      values.stream().forEach((java.util.function.Consumer<? super BasicType>) x -> {
        x.setValue(x.getValue() + 1);
      });
    });
    testOperations(values -> {
      values.stream().parallel().forEach((java.util.function.Consumer<? super BasicType>) x -> {
        x.setValue(x.getValue() + 1);
      });
    });
    testOperations(values -> {
      final java.util.stream.Stream<BasicType> parallel = values.stream().parallel();
      if (!parallel.isParallel())
        throw new AssertionError();
      parallel.close();
    });
    testOperations(values -> {
      values.stream().forEachOrdered((java.util.function.Consumer<? super BasicType>) x -> {
        x.setValue(x.getValue() + 1);
      });
    });
    testOperations(values -> {
      assert values.size() == values.stream().sequential().unordered()
          .map((Function<? super BasicType, ? extends BasicType>) x -> {
            x.setValue(x.getValue() + 1);
            return x;
          }).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt((ToIntFunction<? super BasicType>) foobar1 -> {
        return foobar1.getValue();
      }).toArray().length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble((ToDoubleFunction<? super BasicType>) x -> {
        return x.getValue();
      }).toArray().length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong((ToLongFunction<? super BasicType>) x -> {
        return x.getValue();
      }).toArray().length;
    });
    testOperations(values -> {
      assert values.size() == values.stream()
          .flatMapToInt((Function<? super BasicType, ? extends java.util.stream.IntStream>) foobar1 -> {
            return java.util.stream.IntStream.of(foobar1.getValue());
          }).toArray().length;
    });
    testOperations(values -> {
      assert values.size() == values.stream()
          .flatMapToDouble((Function<? super BasicType, ? extends java.util.stream.DoubleStream>) x -> {
            return java.util.stream.DoubleStream.of(x.getValue());
          }).toArray().length;
    });
    testOperations(values -> {
      final long[] longs = values.stream()
          .flatMapToLong((Function<? super BasicType, ? extends java.util.stream.LongStream>) x -> {
            return java.util.stream.LongStream.of(x.getValue());
          }).toArray();
      assert values.size() == longs.length;
    });
  }

  public @Override
  void _free() {
    super._free();
  }
}
