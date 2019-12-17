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
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * The type Array list container.
 */
public class ArrayListContainer extends ReferenceCountingBase {
  /**
   * Instantiates a new Array list container.
   */
  public ArrayListContainer() {
  }

  private static void testOperations(
      java.util.function.Consumer<java.util.ArrayList<BasicType>> fn) {
    java.util.ArrayList<BasicType> values = new java.util.ArrayList<>();
    for (int i = 0; i < TestOperations.count; i++) {
      values.add(new BasicType());
    }
    fn.accept(values);
  }

  private static void testArrayOperations(java.util.ArrayList<BasicType> values) {
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
      values.addAll(
          java.util.Arrays.asList(basicTypeN));
      values.add(1, basicType1);
      values.addAll(1,
          java.util.Arrays.asList(basicTypeN));
      if (values.indexOf(basicType1) != 4) {
        throw new RuntimeException();
      }
      if (values.indexOf(basicTypeN[0]) != 0) {
        throw new RuntimeException();
      }
      if (values.lastIndexOf(basicTypeN[0]) != 1) {
        throw new RuntimeException();
      }
      if (values.subList(1,
          3).size() != 2) {
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
      assert values.size() == values.stream()
          .map(x -> {
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
    if (false)
      testOperations(values110 -> {
        assert values110.size() == values110.stream()
            .map(x111 -> {
              try {
                return x111.getSelf()
                    .wrap();
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            }).map(x -> {
              return x.getInner();
            }).collect(java.util.stream.Collectors.toList()).size();
      });
    testOperations(values110 -> {
      assert values110.size() == values110.stream()
          .filter(x111 -> {
            try {
              return x111.getSelf().value > 0;
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }).collect(java.util.stream.Collectors.toList()).size();
    });
    testOperations(values110 -> {
      assert values110.size() == values110.stream()
          .filter(x111 -> {
            try {
              return -1 != x111.getSelf().value;
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }).collect(java.util.stream.Collectors.toList()).size();
    });
    if (false)
      testOperations(values110 -> {
        assert values110.size() == getCount(values110.iterator());
      });
  }

  private static long getCount(java.util.Iterator<BasicType> iterator) {
    return java.util.stream.StreamSupport
        .stream(java.util.Spliterators.spliterator(new java.util.Iterator<BasicType>() {
          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          @Override
          public BasicType next() {
            return iterator.next();
          }
        }, -1, 0), false).filter(x -> {
          return test1(x);
        }).filter(ArrayListContainer::test2).filter(x -> {
          return x.getValue() >= 0;
        }).filter(x -> {
          return x != null;
        }).count();
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
      assert values.size() == values.stream()
          .collect(java.util.stream.Collectors.mapping(x -> {
            return x.getValue();
          }, java.util.stream.Collectors.toList())).size();
    });
    testOperations(values -> {
      assert values.size() == values.stream()
          .collect(java.util.stream.Collectors
              .collectingAndThen(java.util.stream.Collectors.toList(), x191 -> {
                return new java.util.HashSet(
                    x191);
              })).size();
    });
    testOperations(values -> {
      assert values
          .size() == (int) values.stream().collect(java.util.stream.Collectors.reducing(0, x -> {
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
      assert values.size() == values.stream().collect(java.util.stream.Collectors.mapping(x -> {
        return 1;
      }, java.util.stream.Collectors.counting()));
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(java.util.stream.Collectors.counting());
    });
    testOperations(values -> {
      final java.util.List<java.util.List<BasicType>> partitions = com.google.common.collect.Lists
          .partition(values, 2);
      partitions.forEach(x -> {
        assert x != null;
      });
    });
    testOperations(values -> {
      values.add(new BasicType());
      final BasicType basicType = new BasicType();
      final java.util.List<com.simiacryptus.demo.refcount.BasicType> list = java.util.Arrays
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
      final java.util.Iterator<com.simiacryptus.demo.refcount.BasicType> iterator = values
          .iterator();
      while (iterator.hasNext()) {
        iterator.next();
        iterator.remove();
      }
    });
    testOperations(values -> {
      final java.util.ListIterator<com.simiacryptus.demo.refcount.BasicType> iterator = values
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
        assert 2 == iterator.previousIndex() : 2 + " != " + iterator.previousIndex();
        iterator.previous();
        iterator.set(new BasicType());
      }
    });
    testOperations(values -> {
      final java.util.Spliterator<com.simiacryptus.demo.refcount.BasicType> spliterator = values
          .spliterator();
      while (spliterator.tryAdvance(x -> {
        assert null != x;
      })) {
      }
    });
  }

  private static void testStreamOperations() {
    if (false)
      testOperations(values -> {
        assert values.size() == values.stream().flatMap(
            (java.util.function.Function<? super com.simiacryptus.demo.refcount.BasicType, ? extends java.util.stream.Stream<? extends com.simiacryptus.demo.refcount.BasicType>>) x -> {
              return values.stream();
            }).distinct().count();
      });
    testOperations(values_conditionalBlock -> {
      if (true) {
        assert values_conditionalBlock.size() == values_conditionalBlock.stream()
            .toArray(i -> new BasicType[i]).length;
      }
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
        return java.util.stream.Stream
            .of(x);
      }).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      values.stream().flatMap(x -> {
        x.setValue(x.getValue() + 1);
        return java.util.stream.Stream
            .of(x, new BasicType());
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
      assert values.stream()
          .reduce((reduceOpA, reduceOpB) -> {
            return new BasicType("reduceOp");
          }).isPresent();
    });
    testOperations(values -> {
      assert values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            return new BasicType("reduceOp");
          }) != null;
    });
    testOperations(values -> {
      assert values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            return new BasicType("reduceOp");
          }, (reduceOpA, reduceOpB) -> {
            return new BasicType("reduceOp");
          }) != null;
    });
    testOperations(values -> {
      assert values.size() == values.stream()
          .collect(java.util.stream.Collectors.toList()).size() : values.size() + " != " + values.stream()
          .collect(java.util.stream.Collectors.toList()).size();
    });
    testOperations(values -> {
      assert values.size() == values.stream()
          .collect(java.util.stream.Collectors.toSet()).size();
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
      java.util.Iterator<com.simiacryptus.demo.refcount.BasicType> iter = values.stream()
          .iterator();
      while (iter.hasNext()) {
        assert iter.next() != null;
      }
    });
    testOperations(values -> {
      @NotNull
      java.util.Spliterator<com.simiacryptus.demo.refcount.BasicType> iter = values.stream()
          .spliterator();
      while (iter.tryAdvance(x -> {
        assert x != null;
      })) {
      }
    });
    testOperations(values -> {
      @NotNull
      java.util.ListIterator<com.simiacryptus.demo.refcount.BasicType> iter = values
          .listIterator();
      while (iter.hasNext()) {
        assert iter.next() != null;
      }
    });
    testOperations(values -> {
      assert values.size() == values.stream()
          .sorted(java.util.Comparator.naturalOrder()).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert values.size() == values.stream()
          .sorted(java.util.Comparator.naturalOrder()).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).sorted(java.util.Comparator.naturalOrder()).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert null != values.stream()
          .max(java.util.Comparator.comparing(x -> {
            return x.getValue();
          })).get();
    });
    testOperations(values -> {
      assert null != values.stream()
          .min(java.util.Comparator.comparing(x -> {
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
      final java.util.stream.Stream<BasicType> parallel = values.stream().parallel();
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
        return java.util.stream.IntStream
            .of(foobar1.getValue());
      }).toArray().length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToDouble(x -> {
        return java.util.stream.DoubleStream
            .of(x.getValue());
      }).toArray().length;
    });
    testOperations(values -> {
      final long[] longs = values.stream().flatMapToLong(x -> {
        return java.util.stream.LongStream
            .of(x.getValue());
      }).toArray();
      assert values.size() == longs.length;
    });
  }

  public @Override
  void _free() {
    super._free();
  }
}
