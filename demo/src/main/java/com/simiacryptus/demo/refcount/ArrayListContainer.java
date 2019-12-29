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

import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.lang.ReferenceCountingBase;
import com.simiacryptus.ref.wrappers.RefIteratorBase;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * The type Array list container.
 */
public @com.simiacryptus.ref.lang.RefAware class ArrayListContainer extends ReferenceCountingBase {
  /**
   * Instantiates a new Array list container.
   */
  public ArrayListContainer() {
  }

  private static void testOperations(
      com.simiacryptus.ref.wrappers.RefConsumer<com.simiacryptus.ref.wrappers.RefArrayList<BasicType>> fn) {
    com.simiacryptus.ref.wrappers.RefArrayList<BasicType> values = new com.simiacryptus.ref.wrappers.RefArrayList<>();
    for (int i = 0; i < TestOperations.count; i++) {
      values.add(new BasicType());
    }
    fn.accept(values);
  }

  private static void testArrayOperations(com.simiacryptus.ref.wrappers.RefArrayList<BasicType> values) {
    if (0 == values.size()) {
      throw new RuntimeException();
    }
    if (false) {
      if (values.size() != values.toArray().length) {
        throw new RuntimeException();
      }
    }
    if (values.size() != values.toArray(new BasicType[] {}).length) {
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
      final BasicType[] basicTypeN = new BasicType[] { new BasicType(), new BasicType(), new BasicType() };
      values.addAll(com.simiacryptus.ref.wrappers.RefArrays.asList(basicTypeN));
      values.add(1, basicType1);
      values.addAll(1, com.simiacryptus.ref.wrappers.RefArrays.asList(basicTypeN));
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
      }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList()).size();
    });
    testOperations(values101 -> {
      assert values101.size() == values101.stream().map(x156 -> {
        if (1 == 1) {
          return x156.getValue();
        } else {
          throw new RuntimeException();
        }
      }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList()).size();
    });
    testOperations(values110 -> {
      assert values110.size() == values110.stream().map(x111 -> {
        try {
          return x111.getValue();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList()).size();
    });
    testOperations(values110 -> {
      assert values110.size() == values110.stream().map(x111 -> {
        try {
          return x111.getSelf().getValue();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList()).size();
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
      }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList()).size();
    });
    testOperations(values110 -> {
      assert values110.size() == values110.stream().filter(x111 -> {
        try {
          return x111.getSelf().value > 0;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList()).size();
    });
    testOperations(values110 -> {
      assert values110.size() == values110.stream().filter(x111 -> {
        try {
          return -1 != x111.getSelf().value;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList()).size();
    });
    testOperations(values110 -> {
      assert values110.size() == getCount1(values110.iterator());
    });
    testOperations(values110 -> {
      assert values110.size() == getCount2(values110.iterator());
    });
  }

  private static long getCount1(com.simiacryptus.ref.wrappers.RefIterator<BasicType> iterator) {
    return com.simiacryptus.ref.wrappers.RefStreamSupport.stream(com.simiacryptus.ref.wrappers.RefSpliterators
        .spliterator(new com.simiacryptus.ref.wrappers.RefIteratorBase<BasicType>() {
          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          @Override
          public BasicType next() {
            return iterator.next();
          }

          public @Override void _free() {
            super._free();
          }
        }, -1, 0), false).filter(x -> {
          return test1(x);
        }).filter(ArrayListContainer::test2).filter(x -> {
          return x.getValue() >= 0;
        }).filter(x -> {
          return x != null;
        }).count();
  }

  private static long getCount2(com.simiacryptus.ref.wrappers.RefIterator<BasicType> iterator) {
    return com.simiacryptus.ref.wrappers.RefStreamSupport
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
            super._free();
          }
        }, -1, 0), false).filter(x -> {
          return test1(x);
        }).filter(ArrayListContainer::test2).filter(x -> {
          return x.getValue() >= 0;
        }).filter(x -> {
          return x != null;
        }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList()).size();
  }

  private static boolean test1(BasicType x) {
    return 0 < x.value;
  }

  private static boolean test2(BasicType x) {
    return null != getTest();
  }

  @NotNull
  private static Predicate<BasicType> getTest() {
    return ArrayListContainer::test1;
  }

  private static void testCollectionOperations() {
    testOperations(values -> {
      assert values.size() == values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.mapping(x -> {
        return x.getValue();
      }, com.simiacryptus.ref.wrappers.RefCollectors.toList())).size();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors
          .collectingAndThen(com.simiacryptus.ref.wrappers.RefCollectors.toList(), x191 -> {
            return new com.simiacryptus.ref.wrappers.RefHashSet(x191);
          })).size();
    });
    testOperations(values -> {
      assert values
          .size() == (int) values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.reducing(0, x -> {
            return 1;
          }, (a, b) -> a + b));
    });
    testOperations(values -> {
      assert values.size() == values.stream().map(x -> {
        return x.getValue();
      }).collect(com.simiacryptus.ref.wrappers.RefCollectors.reducing((a, b) -> a + b)).get();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.mapping(x -> {
        return 1;
      }, com.simiacryptus.ref.wrappers.RefCollectors.reducing((a, b) -> a + b))).get();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.mapping(x -> {
        return 1;
      }, com.simiacryptus.ref.wrappers.RefCollectors.counting()));
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.counting());
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.ref.wrappers.RefList<BasicType>> partitions = com.simiacryptus.ref.wrappers.RefLists
          .partition(values, 2);
      partitions.forEach(x -> {
        assert x != null;
      });
    });
    testOperations(values -> {
      values.add(new BasicType());
      final BasicType basicType = new BasicType();
      final com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> list = com.simiacryptus.ref.wrappers.RefArrays
          .asList(basicType);
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

  private static void testIteratorOperations() {
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iterator = values
          .iterator();
      while (iterator.hasNext()) {
        iterator.next();
        iterator.remove();
      }
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefListIterator<com.simiacryptus.demo.refcount.BasicType> iterator = values
          .listIterator();
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
      final com.simiacryptus.ref.wrappers.RefSpliterator<com.simiacryptus.demo.refcount.BasicType> spliterator = values
          .spliterator();
      while (spliterator.tryAdvance(x -> {
        assert null != x;
      })) {
      }
    });
  }

  private static void testStreamOperations() {
    testOperations(values -> {
      assert values.size() == values.stream().flatMap(
          (java.util.function.Function<? super com.simiacryptus.demo.refcount.BasicType, ? extends com.simiacryptus.ref.wrappers.RefStream<? extends com.simiacryptus.demo.refcount.BasicType>>) x -> {
            return values.stream();
          }).distinct().count();
    });
    testOperations(values372 -> {

      final @NotNull BasicType[] array374 = values372.toArray(new BasicType[] {});
      final int inputIndex = 0;
      @NotNull
      BasicType[] outputPrototype377 = array374;

      {
        final BasicType inputTensor = array374[inputIndex];
        final int inputDims = inputTensor.value;
        @Nonnull
        final BasicType result = new BasicType();
        for (int j = 0; j < outputPrototype377.length; j++) {
          final int j_ = j;
          @Nonnull
          final WrapperType<BasicType> inputKey = new WrapperType<BasicType>(new BasicType());
          final WrapperType[] copyInput = com.simiacryptus.ref.wrappers.RefArrays.stream(array374).map(x -> {
            return new WrapperType(x);
          }).toArray(i -> new WrapperType[i]);
          {
            copyInput[inputIndex] = new WrapperType(new BasicType()) {
              @Override
              public ReferenceCounting getInner() {
                return super.getInner();
              }

              public @Override void _free() {
                super._free();
              }
            };
          }
          @Nullable
          final WrapperType eval;
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
      }

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
      assert values.size() == values.stream().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).sorted().toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMap(x -> {
        x.setValue(x.getValue() + 1);
        return com.simiacryptus.ref.wrappers.RefStream.of(x);
      }).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      values.stream().flatMap(x -> {
        x.setValue(x.getValue() + 1);
        return com.simiacryptus.ref.wrappers.RefStream.of(x, new BasicType());
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
      assert values.size() == values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.toList())
          .size() : values.size() + " != "
              + values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.toList()).size();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.toSet()).size();
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
      com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iter = values.stream()
          .iterator();
      while (iter.hasNext()) {
        assert iter.next() != null;
      }
    });
    testOperations(values -> {
      @NotNull
      com.simiacryptus.ref.wrappers.RefSpliterator<com.simiacryptus.demo.refcount.BasicType> iter = values.stream()
          .spliterator();
      while (iter.tryAdvance(x -> {
        assert x != null;
      })) {
      }
    });
    testOperations(values -> {
      @NotNull
      com.simiacryptus.ref.wrappers.RefListIterator<com.simiacryptus.demo.refcount.BasicType> iter = values
          .listIterator();
      while (iter.hasNext()) {
        assert iter.next() != null;
      }
    });
    testOperations(values -> {
      assert values.size() == values.stream().sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder())
          .toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder())
          .toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert null != values.stream().max(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
        return x.getValue();
      })).get();
    });
    testOperations(values -> {
      assert null != values.stream().min(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
        return x.getValue();
      })).get();
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
      final com.simiacryptus.ref.wrappers.RefStream<BasicType> parallel = values.stream().parallel();
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
      assert values.size() == values.stream().sequential().unordered().map(x -> {
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
      assert values.size() == values.stream().flatMapToInt(foobar1 -> {
        return com.simiacryptus.ref.wrappers.RefIntStream.of(foobar1.getValue());
      }).toArray().length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToDouble(x -> {
        return com.simiacryptus.ref.wrappers.RefDoubleStream.of(x.getValue());
      }).toArray().length;
    });
    testOperations(values -> {
      final long[] longs = values.stream().flatMapToLong(x -> {
        return com.simiacryptus.ref.wrappers.RefLongStream.of(x.getValue());
      }).toArray();
      assert values.size() == longs.length;
    });
  }

  public @Override void _free() {
    super._free();
  }

  public @Override ArrayListContainer addRef() {
    return (ArrayListContainer) super.addRef();
  }

  public static ArrayListContainer[] addRefs(ArrayListContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ArrayListContainer::addRef)
        .toArray((x) -> new ArrayListContainer[x]);
  }

  public static ArrayListContainer[][] addRefs(ArrayListContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(ArrayListContainer::addRefs)
        .toArray((x) -> new ArrayListContainer[x][]);
  }
}
