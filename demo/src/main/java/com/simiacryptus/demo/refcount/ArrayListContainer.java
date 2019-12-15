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
    fn.freeRef();
  }

  private static void testArrayOperations(com.simiacryptus.ref.wrappers.RefArrayList<BasicType> values) {
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
    com.simiacryptus.demo.refcount.BasicType[] temp3894 = values.toArray(new BasicType[] {});
    if (values.size() != temp3894.length) {
      values.freeRef();
      throw new RuntimeException();
    }
    com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp3894);
    values.freeRef();
  }

  private static void testElementOperations() {
    testOperations(values -> {
      // Test
      values.clear();
      // Test
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
      com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> temp6820 = values.iterator();
      if (!values.add(temp6820.next())) {
        values.freeRef();
        basicType1.freeRef();
        throw new RuntimeException();
      }
      temp6820.freeRef();
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
      com.simiacryptus.ref.wrappers.RefArrayList<com.simiacryptus.demo.refcount.BasicType> temp7075 = values.subList(1,
          3);
      if (temp7075.size() != 2) {
        values.freeRef();
        throw new RuntimeException();
      }
      temp7075.freeRef();
      values.clear();
      if (!values.isEmpty()) {
        values.freeRef();
        throw new RuntimeException();
      }
      values.freeRef();
    });
  }

  private static void testCodeGen() {
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp5617 = values.stream()
          .map(x -> {
            return x;
          }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values.size() == temp5617.size();
      temp5617.freeRef();
      values.freeRef();
    });
    testOperations(values101 -> {
      com.simiacryptus.ref.wrappers.RefList<java.lang.Integer> temp5619 = values101.stream().map(x156 -> {
        if (1 == 1) {
          int temp2643 = x156.getValue();
          x156.freeRef();
          return temp2643;
        } else {
          x156.freeRef();
          throw new RuntimeException();
        }
      }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values101.size() == temp5619.size();
      temp5619.freeRef();
      values101.freeRef();
    });
    testOperations(values110 -> {
      com.simiacryptus.ref.wrappers.RefList<java.lang.Integer> temp3024 = values110.stream().map(x111 -> {
        try {
          int temp7325 = x111.getValue();
          x111.freeRef();
          return temp7325;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values110.size() == temp3024.size();
      temp3024.freeRef();
      values110.freeRef();
    });
    testOperations(values110 -> {
      com.simiacryptus.ref.wrappers.RefList<java.lang.Integer> temp8075 = values110.stream().map(x111 -> {
        try {
          com.simiacryptus.demo.refcount.BasicType temp8156 = x111.getSelf();
          int temp2316 = x111.getSelf().getValue();
          temp8156.freeRef();
          x111.freeRef();
          return temp2316;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values110.size() == temp8075.size();
      temp8075.freeRef();
      values110.freeRef();
    });
    if (false)
      testOperations(values110 -> {
        com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp8861 = values110.stream()
            .map(x111 -> {
              try {
                com.simiacryptus.demo.refcount.WrapperType<com.simiacryptus.demo.refcount.BasicType> temp3432 = x111
                    .getSelf().wrap();
                x111.freeRef();
                return temp3432;
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            }).map(x -> {
              com.simiacryptus.demo.refcount.BasicType temp2961 = x.getInner();
              x.freeRef();
              return temp2961;
            }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
        assert values110.size() == temp8861.size();
        temp8861.freeRef();
        values110.freeRef();
      });
    testOperations(values110 -> {
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp1379 = values110.stream()
          .filter(x111 -> {
            try {
              boolean temp2272 = x111.getSelf().value > 0;
              x111.freeRef();
              return temp2272;
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values110.size() == temp1379.size();
      temp1379.freeRef();
      values110.freeRef();
    });
    testOperations(values110 -> {
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp6416 = values110.stream()
          .filter(x111 -> {
            try {
              boolean temp9212 = -1 != x111.getSelf().value;
              x111.freeRef();
              return temp9212;
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }).collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values110.size() == temp6416.size();
      temp6416.freeRef();
      values110.freeRef();
    });
    if (false)
      testOperations(values110 -> {
        assert values110.size() == getCount(values110.iterator());
        values110.freeRef();
      });
  }

  private static long getCount(com.simiacryptus.ref.wrappers.RefIterator<BasicType> iterator) {
    long temp6335 = com.simiacryptus.ref.wrappers.RefStreamSupport.stream(com.simiacryptus.ref.wrappers.RefSpliterators
        .spliterator(com.simiacryptus.ref.lang.RefUtil.wrapInterface(new java.util.Iterator<BasicType>() {
          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          @Override
          public BasicType next() {
            return iterator.next();
          }
        }, com.simiacryptus.ref.lang.RefUtil.addRef(iterator)), -1, 0), false).filter(x -> {
          return test1(x.addRef());
        }).filter(ArrayListContainer::test2).filter(x -> {
          return x.getValue() >= 0;
        }).filter(x -> {
          return x != null;
        }).count();
    iterator.freeRef();
    return temp6335;
  }

  private static boolean test1(BasicType x) {
    boolean temp4009 = 0 < x.value;
    x.freeRef();
    return temp4009;
  }

  private static boolean test2(BasicType x) {
    x.freeRef();
    java.util.function.Predicate<com.simiacryptus.demo.refcount.BasicType> temp5034 = getTest();
    boolean temp7687 = null != getTest();
    com.simiacryptus.ref.lang.RefUtil.freeRef(temp5034);
    return temp7687;
  }

  @NotNull
  private static Predicate<BasicType> getTest() {
    return ArrayListContainer::test1;
  }

  private static void testCollectionOperations() {
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefList<java.lang.Integer> temp1778 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.mapping(x -> {
            int temp1298 = x.getValue();
            x.freeRef();
            return temp1298;
          }, com.simiacryptus.ref.wrappers.RefCollectors.toList()));
      assert values.size() == temp1778.size();
      temp1778.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefHashSet temp3379 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors
              .collectingAndThen(com.simiacryptus.ref.wrappers.RefCollectors.toList(), x191 -> {
                com.simiacryptus.ref.wrappers.RefHashSet temp7359 = new com.simiacryptus.ref.wrappers.RefHashSet(x191);
                return temp7359;
              }));
      assert values.size() == temp3379.size();
      temp3379.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      assert values
          .size() == (int) values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.reducing(0, x -> {
            x.freeRef();
            return 1;
          }, (a, b) -> a + b));
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().map(x -> {
        int temp8312 = x.getValue();
        x.freeRef();
        return temp8312;
      }).collect(com.simiacryptus.ref.wrappers.RefCollectors.reducing((a, b) -> a + b)).get();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.mapping(x -> {
        x.freeRef();
        return 1;
      }, com.simiacryptus.ref.wrappers.RefCollectors.reducing((a, b) -> a + b))).get();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.mapping(x -> {
        x.freeRef();
        return 1;
      }, com.simiacryptus.ref.wrappers.RefCollectors.counting()));
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.counting());
      values.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.ref.wrappers.RefList<BasicType>> partitions = com.simiacryptus.ref.wrappers.RefLists
          .partition(values, 2);
      partitions.forEach(x -> {
        assert x != null;
        x.freeRef();
      });
      partitions.freeRef();
    });
    testOperations(values -> {
      values.add(new BasicType());
      final BasicType basicType = new BasicType();
      final com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> list = com.simiacryptus.ref.wrappers.RefArrays
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
      values.freeRef();
      while (iterator.hasNext()) {
        com.simiacryptus.ref.lang.RefUtil.freeRef(iterator.next());
        iterator.remove();
      }
      iterator.freeRef();
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefListIterator<com.simiacryptus.demo.refcount.BasicType> iterator = values
          .listIterator();
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
        assert 2 == iterator.previousIndex() : 2 + " != " + iterator.previousIndex();
        com.simiacryptus.ref.lang.RefUtil.freeRef(iterator.previous());
        iterator.set(new BasicType());
      }
      iterator.freeRef();
    });
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

  private static void testStreamOperations() {
    if (false)
      testOperations(values -> {
        assert values.size() == values.stream().flatMap(
            (java.util.function.Function<? super com.simiacryptus.demo.refcount.BasicType, ? extends com.simiacryptus.ref.wrappers.RefStream<? extends com.simiacryptus.demo.refcount.BasicType>>) com.simiacryptus.ref.lang.RefUtil
                .wrapInterface(
                    (java.util.function.Function<? super com.simiacryptus.demo.refcount.BasicType, ? extends com.simiacryptus.ref.wrappers.RefStream<? extends com.simiacryptus.demo.refcount.BasicType>>) x -> {
                      x.freeRef();
                      return values.stream();
                    }, values))
            .distinct().count();
      });
    testOperations(values_conditionalBlock -> {
      if (true) {
        com.simiacryptus.demo.refcount.BasicType[] temp7178 = values_conditionalBlock.stream()
            .toArray(i -> new BasicType[i]);
        assert values_conditionalBlock.size() == temp7178.length;
        com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp7178);
      }
      values_conditionalBlock.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp3117 = values.stream().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).sorted().toArray(i -> new BasicType[i]);
      assert values.size() == temp3117.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp3117);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp1297 = values.stream().flatMap(x -> {
        x.setValue(x.getValue() + 1);
        com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp6259 = com.simiacryptus.ref.wrappers.RefStream
            .of(x);
        return temp6259;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp1297.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp1297);
      values.freeRef();
    });
    testOperations(values -> {
      values.stream().flatMap(x -> {
        x.setValue(x.getValue() + 1);
        com.simiacryptus.ref.wrappers.RefStream<com.simiacryptus.demo.refcount.BasicType> temp3864 = com.simiacryptus.ref.wrappers.RefStream
            .of(x, new BasicType());
        return temp3864;
      }).forEach(x -> {
        assert x != null;
        x.freeRef();
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().allMatch(x -> {
        boolean temp3765 = x != null;
        x.freeRef();
        return temp3765;
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
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp2655 = values.stream()
          .reduce((reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp2655.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp2655);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp4179 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp4179 != null;
      temp4179.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp2971 = values.stream().reduce(new BasicType("reduceInit"),
          (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          }, (reduceOpA, reduceOpB) -> {
            reduceOpB.freeRef();
            reduceOpA.freeRef();
            return new BasicType("reduceOp");
          });
      assert temp2971 != null;
      temp2971.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp4188 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      com.simiacryptus.ref.wrappers.RefList<com.simiacryptus.demo.refcount.BasicType> temp1594 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toList());
      assert values.size() == temp4188.size() : values.size() + " != " + temp1594.size();
      temp1594.freeRef();
      temp4188.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefSet<com.simiacryptus.demo.refcount.BasicType> temp2690 = values.stream()
          .collect(com.simiacryptus.ref.wrappers.RefCollectors.toSet());
      assert values.size() == temp2690.size();
      temp2690.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().anyMatch(x -> {
        boolean temp6039 = x != null;
        x.freeRef();
        return temp6039;
      });
      values.freeRef();
    });
    testOperations(values -> {
      assert values.stream().noneMatch(x -> {
        boolean temp4825 = x == null;
        x.freeRef();
        return temp4825;
      });
      values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp3969 = values.stream().filter(x -> {
        boolean temp5405 = x == null;
        x.freeRef();
        return temp5405;
      }).findAny();
      assert !temp3969.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp3969);
      values.freeRef();
    });
    testOperations(values -> {
      java.util.Optional<com.simiacryptus.demo.refcount.BasicType> temp7046 = values.stream().filter(x -> {
        boolean temp9938 = x != null;
        x.freeRef();
        return temp9938;
      }).findFirst();
      assert temp7046.isPresent();
      com.simiacryptus.ref.lang.RefUtil.freeRef(temp7046);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iter = values.stream()
          .iterator();
      values.freeRef();
      while (iter.hasNext()) {
        com.simiacryptus.demo.refcount.BasicType temp2383 = iter.next();
        assert temp2383 != null;
        temp2383.freeRef();
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
      com.simiacryptus.ref.wrappers.RefListIterator<com.simiacryptus.demo.refcount.BasicType> iter = values
          .listIterator();
      values.freeRef();
      while (iter.hasNext()) {
        com.simiacryptus.demo.refcount.BasicType temp6390 = iter.next();
        assert temp6390 != null;
        temp6390.freeRef();
      }
      iter.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp9043 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp9043.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp9043);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp7566 = values.stream()
          .sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp7566.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp7566);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType[] temp4796 = values.stream().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).sorted(com.simiacryptus.ref.wrappers.RefComparator.naturalOrder()).toArray(i -> new BasicType[i]);
      assert values.size() == temp4796.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp4796);
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp6476 = values.stream()
          .max(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
            int temp5498 = x.getValue();
            x.freeRef();
            return temp5498;
          })).get();
      assert null != temp6476;
      temp6476.freeRef();
      values.freeRef();
    });
    testOperations(values -> {
      com.simiacryptus.demo.refcount.BasicType temp7141 = values.stream()
          .min(com.simiacryptus.ref.wrappers.RefComparator.comparing(x -> {
            int temp3663 = x.getValue();
            x.freeRef();
            return temp3663;
          })).get();
      assert null != temp7141;
      temp7141.freeRef();
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
      com.simiacryptus.demo.refcount.BasicType[] temp9203 = values.stream().sequential().unordered().map(x -> {
        x.setValue(x.getValue() + 1);
        return x;
      }).toArray(i -> new BasicType[i]);
      assert values.size() == temp9203.length;
      com.simiacryptus.ref.lang.ReferenceCounting.freeRefs(temp9203);
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        int temp7554 = foobar1.getValue();
        foobar1.freeRef();
        return temp7554;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(x -> {
        int temp2612 = x.getValue();
        x.freeRef();
        return temp2612;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(x -> {
        int temp1617 = x.getValue();
        x.freeRef();
        return temp1617;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToInt(foobar1 -> {
        com.simiacryptus.ref.wrappers.RefIntStream temp9213 = com.simiacryptus.ref.wrappers.RefIntStream
            .of(foobar1.getValue());
        foobar1.freeRef();
        return temp9213;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToDouble(x -> {
        com.simiacryptus.ref.wrappers.RefDoubleStream temp7813 = com.simiacryptus.ref.wrappers.RefDoubleStream
            .of(x.getValue());
        x.freeRef();
        return temp7813;
      }).toArray().length;
      values.freeRef();
    });
    testOperations(values -> {
      final long[] longs = values.stream().flatMapToLong(x -> {
        com.simiacryptus.ref.wrappers.RefLongStream temp7550 = com.simiacryptus.ref.wrappers.RefLongStream
            .of(x.getValue());
        x.freeRef();
        return temp7550;
      }).toArray();
      assert values.size() == longs.length;
      values.freeRef();
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
