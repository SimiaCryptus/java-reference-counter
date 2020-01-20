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

import com.google.common.collect.Lists;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefIteratorBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.*;

@SuppressWarnings("unused")
public class ArrayListContainer extends ReferenceCountingBase {
  public ArrayListContainer() {
  }

  @Nonnull
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

  private static void testOperations(@Nonnull Consumer<ArrayList<BasicType>> fn) {
    ArrayList<BasicType> values = new ArrayList<>();
    for (int i = 0; i < TestOperations.count; i++) {
      values.add(new BasicType());
    }
    fn.accept(values);
  }

  private static void testArrayOperations(@Nonnull ArrayList<BasicType> values) {
    if (0 == values.size()) {
      throw new RuntimeException();
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
    });
  }

  private static void testCodeGen() {
    testOperations(values -> {
      assert values.size() == values.stream().map(x -> {
        return x;
      }).collect(Collectors.toList()).size();
    });
    testOperations(values101 -> {
      assert values101.size() == values101.stream().map(x156 -> {
        return x156.getValue();
      }).collect(Collectors.toList()).size();
    });
    testOperations(values110 -> {
      assert values110.size() == values110.stream().map(x111 -> {
        try {
          return x111.getValue();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).collect(Collectors.toList()).size();
    });
    testOperations(values110 -> {
      assert values110.size() == values110.stream().map(x111 -> {
        try {
          return x111.getSelf().getValue();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).collect(Collectors.toList()).size();
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
      }).collect(Collectors.toList()).size();
    });
    testOperations(values110 -> {
      assert values110.size() == values110.stream().filter(x111 -> {
        try {
          return x111.getSelf().value > 0;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).collect(Collectors.toList()).size();
    });
    testOperations(values110 -> {
      assert values110.size() == values110.stream().filter(x111 -> {
        try {
          return -1 != x111.getSelf().value;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).collect(Collectors.toList()).size();
    });
    testOperations(values110 -> {
      assert values110.size() == getCount1(values110.iterator());
    });
    testOperations(values110 -> {
      assert values110.size() == getCount2(values110.iterator());
    });
  }

  private static long getCount1(@Nonnull Iterator<BasicType> iterator) {
    return StreamSupport.stream(Spliterators.spliterator(new RefIteratorBase<BasicType>() {
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
      return true;
    }).count();
  }

  private static long getCount2(@Nonnull Iterator<BasicType> iterator) {
    return StreamSupport.stream(Spliterators.spliterator(new RefIteratorBase<BasicType>() {
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
      return true;
    }).collect(Collectors.toList()).size();
  }

  private static boolean test1(@Nonnull BasicType x) {
    return 0 < x.value;
  }

  private static boolean test2(BasicType x) {
    return true;
  }

  private static void testCollectionOperations() {
    testOperations(values289 -> {
      assert values289.size() == values289.stream().collect(Collectors.mapping(x291 -> {
        return x291.getValue();
      }, Collectors.toList())).size();
    });
    testOperations(values296 -> {
      assert values296.size() == values296.stream().collect(Collectors.collectingAndThen(Collectors.toList(), x191 -> {
        return new HashSet(x191);
      })).size();
    });
    testOperations(values -> {
      assert values.size() == values.stream()
          .collect(Collectors.reducing(0, (Function<? super BasicType, Integer>) x -> {
            return 1;
          }, (a, b) -> a + b));
    });
    testOperations(values -> {
      assert values.size() == RefUtil.get(values.stream().map(x -> {
        return x.getValue();
      }).collect(Collectors.reducing((a, b) -> a + b)));
    });
    testOperations(values -> {
      assert values.size() == RefUtil.get(values.stream().collect(Collectors.mapping(x -> {
        return 1;
      }, Collectors.reducing((a, b) -> a + b))));
    });
    testOperations(values -> {
      assert values.size() == values.stream()
          .collect(Collectors.mapping((Function<? super BasicType, Integer>) x -> {
            return 1;
          }, Collectors.counting()));
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(Collectors.counting());
    });
    testOperations(values -> {
      final List<List<BasicType>> partitions = Lists.partition(values, 2);
      partitions.forEach((Consumer<? super List<BasicType>>) x -> {
        assert x != null;
      });
    });
    testOperations(values -> {
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
        iterator.previous();
        iterator.set(new BasicType());
      }
    });
    testOperations(values -> {
      final Spliterator<BasicType> spliterator = values.spliterator();
      while (spliterator.tryAdvance((Consumer<? super BasicType>) x -> {
        assert null != x;
      })) {
      }
    });
  }

  private static void testStreamOperations() {
    testOperations(values -> {
      assert values.size() == values.stream()
          .flatMap((Function<? super BasicType, Stream<BasicType>>) x -> {
            return values.stream();
          }).distinct().count();
    });
    testOperations(values372 -> {

      final @Nonnull BasicType[] array374 = values372.toArray(new BasicType[]{});
      final int inputIndex = 0;
      @Nonnull
      BasicType[] outputPrototype377 = array374;

      final BasicType inputTensor = array374[inputIndex];
      final int inputDims = inputTensor.value;
      @Nonnull final BasicType result = new BasicType();
      for (int j = 0; j < outputPrototype377.length; j++) {
        final int j_ = j;
        @Nonnull final WrapperType<BasicType> inputKey = new WrapperType<BasicType>(new BasicType());
        final WrapperType[] copyInput = Arrays.stream(array374)
            .map((Function<? super BasicType, WrapperType>) x -> {
              return new WrapperType(x);
            }).toArray(i -> new WrapperType[i]);
        copyInput[inputIndex] = new WrapperType(new BasicType()) {
          @Nullable
          @Override
          public ReferenceCounting getInner() {
            return super.getInner();
          }

          public @Override
          void _free() {
            super._free();
          }
        };
        @Nullable final WrapperType eval;
        try {
          eval = new WrapperType(new BasicType());
        } finally {
          for (@Nonnull
              WrapperType nnResult : copyInput) {
            assert nnResult.getInner() != null;
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
      assert values.size() == values.stream().map((Function<? super BasicType, BasicType>) x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).sorted().toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert values
          .size() == values.stream().flatMap((Function<? super BasicType, Stream<BasicType>>) x -> {
        x.setValue(x.getValue() + 1);
        return Stream.of(x);
      }).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      values.stream().flatMap((Function<? super BasicType, Stream<BasicType>>) x -> {
        x.setValue(x.getValue() + 1);
        return Stream.of(x, new BasicType());
      }).forEach((Consumer<? super BasicType>) x -> {
        assert x != null;
      });
    });
    testOperations(values -> {
      assert values.stream().allMatch((Predicate<? super BasicType>) x -> {
        return x != null;
      });
    });
    testOperations(values -> {
      assert values.size() == values.stream().peek((Consumer<? super BasicType>) x -> {
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
      assert values.size() == values.stream().collect(Collectors.toList()).size() : values.size() + " != "
          + values.stream().collect(Collectors.toList()).size();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(Collectors.toSet()).size();
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
      Iterator<BasicType> iter = values.stream().iterator();
      while (iter.hasNext()) {
        assert iter.next() != null;
      }
    });
    testOperations(values -> {
      @Nonnull
      Spliterator<BasicType> iter = values.stream().spliterator();
      while (iter.tryAdvance((Consumer<? super BasicType>) x -> {
        assert x != null;
      })) {
      }
    });
    testOperations(values -> {
      @Nonnull
      ListIterator<BasicType> iter = values.listIterator();
      while (iter.hasNext()) {
        assert iter.next() != null;
      }
    });
    testOperations(values -> {
      assert values.size() == values.stream().sorted(Comparator.naturalOrder()).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().sorted(Comparator.naturalOrder()).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().map((Function<? super BasicType, BasicType>) x -> {
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
      final Stream<BasicType> parallel = values.stream().parallel();
      if (!parallel.isParallel())
        throw new AssertionError();
      parallel.close();
    });
    testOperations(values -> {
      values.stream().forEachOrdered(x -> {
        x.setValue(x.getValue() + 1);
      });
    });
    testOperations(values -> {
      assert values.size() == values.stream().sequential().unordered()
          .map(x -> {
            x.setValue(x.getValue() + 1);
            return x;
          }).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        return foobar1.getValue();
      }).toArray().length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(x -> {
        return x.getValue();
      }).toArray().length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(x -> {
        return x.getValue();
      }).toArray().length;
    });
    testOperations(values -> {
      assert values
          .size() == values.stream().flatMapToInt(foobar1 -> {
        return IntStream.of(foobar1.getValue());
      }).toArray().length;
    });
    testOperations(values -> {
      assert values
          .size() == values.stream().flatMapToDouble(x -> {
        return DoubleStream.of(x.getValue());
      }).toArray().length;
    });
    testOperations(values -> {
      final long[] longs = values.stream().flatMapToLong(x -> {
        return LongStream.of(x.getValue());
      }).toArray();
      assert values.size() == longs.length;
    });
  }

  public @Override
  void _free() {
    super._free();
  }
}
