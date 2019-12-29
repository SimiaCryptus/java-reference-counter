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
public @com.simiacryptus.ref.lang.RefAware
class DeququeContainer extends ReferenceCountingBase {
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

  public static DeququeContainer[] addRefs(DeququeContainer[] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(DeququeContainer::addRef)
        .toArray((x) -> new DeququeContainer[x]);
  }

  public static DeququeContainer[][] addRefs(DeququeContainer[][] array) {
    return java.util.Arrays.stream(array).filter((x) -> x != null).map(DeququeContainer::addRefs)
        .toArray((x) -> new DeququeContainer[x][]);
  }

  private static void testOperations(
      com.simiacryptus.ref.wrappers.RefConsumer<com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<BasicType>> fn) {
    com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<BasicType> values = new com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<>();
    for (int i = 0; i < TestOperations.count; i++) {
      values.add(new BasicType());
    }
    fn.accept(values);
  }

  private static void testArrayOperations(com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<BasicType> values) {
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

  private static void testElementOperations(com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<BasicType> values) {
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
    final BasicType basicType2 = new BasicType();
    if (!values.add(basicType2)) {
      throw new RuntimeException();
    }
    if (values.size() != 2) {
      throw new RuntimeException();
    }
    if (!values.poll().equals(basicType1)) {
      throw new RuntimeException();
    }
    if (values.size() != 1) {
      throw new RuntimeException();
    }
    values.clear();
    if (!values.isEmpty()) {
      throw new RuntimeException();
    }
  }

  private static void testCollectionOperations(
      com.simiacryptus.ref.wrappers.RefConcurrentLinkedDeque<BasicType> values) {
    values.add(new BasicType());
    final BasicType basicType = new BasicType();
    final com.simiacryptus.ref.wrappers.RefList<BasicType> list = com.simiacryptus.ref.wrappers.RefArrays
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
  }

  private static void testDequeOperations() {
    testOperations(values -> {
      final int initSize = values.size();
      final BasicType basicType1 = new BasicType();
      final BasicType basicType2 = new BasicType();
      values.addFirst(basicType1);
      values.addLast(basicType2);
      assert values.size() == initSize + 2;
      assert values.element() != null;
      assert values.getFirst() == basicType1;
      assert values.getLast() == basicType2;
      assert values.size() == initSize + 2;
    });
    testOperations(values -> {
      final int initSize = values.size();
      final BasicType basicType1 = new BasicType();
      final BasicType basicType2 = new BasicType();
      values.addFirst(basicType1);
      values.addLast(basicType2);
      assert values.size() == initSize + 2;
      assert values.peek() != null;
      assert values.peekFirst() == basicType1;
      assert values.peekLast() == basicType2;
      assert values.size() == initSize + 2;
    });
    testOperations(values -> {
      final int initSize = values.size();
      final BasicType basicType1 = new BasicType();
      final BasicType basicType2 = new BasicType();
      values.addFirst(basicType1);
      values.addLast(basicType2);
      assert values.size() == initSize + 2;
      assert values.pollFirst() == basicType1;
      assert values.pollLast() == basicType2;
      assert values.poll() != null;
      assert values.size() == initSize - 1;
    });
    testOperations(values -> {
      final int initSize = values.size();
      final BasicType basicType0 = new BasicType();
      final BasicType basicType1 = new BasicType();
      final BasicType basicType2 = new BasicType();
      assert values.offer(basicType0);
      assert values.offerFirst(basicType1);
      assert values.offerLast(basicType2);
      assert values.size() == initSize + 3;
      assert values.removeFirst() == basicType1;
      assert values.removeLast() == basicType2;
      assert values.remove(basicType0);
      assert values.size() == initSize;
    });
    testOperations(values -> {
      final int initSize = values.size();
      final BasicType basicType0 = new BasicType();
      final BasicType basicType1 = new BasicType();
      final BasicType basicType2 = new BasicType();
      assert values.offer(basicType0);
      assert values.offerFirst(basicType1);
      assert values.offerLast(basicType2);
      assert values.size() == initSize + 3;
      assert values.removeFirstOccurrence(basicType1);
      assert values.removeLastOccurrence(basicType2);
      assert values.remove() != null;
      assert values.size() == initSize;
    });
    testOperations(values -> {
      values.clear();
      final int initSize = values.size();
      final BasicType basicType1 = new BasicType();
      values.push(basicType1);
      assert values.size() == initSize + 1;
      assert values.pop() == basicType1;
      assert values.size() == initSize;
    });
  }

  private static void testIteratorOperations() {
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iterator48093 = values
          .iterator();
      while (iterator48093.hasNext()) {
        iterator48093.next();
        iterator48093.remove();
      }
    });
    testOperations(values -> {
      final com.simiacryptus.ref.wrappers.RefIterator<com.simiacryptus.demo.refcount.BasicType> iterator48093 = values
          .descendingIterator();
      while (iterator48093.hasNext()) {
        iterator48093.next();
        iterator48093.remove();
      }
    });
    if (false) {
      testOperations(values -> {
        final com.simiacryptus.ref.wrappers.RefSpliterator<com.simiacryptus.demo.refcount.BasicType> spliterator = values
            .spliterator();
        while (spliterator.tryAdvance(x -> {
          assert null != x;
        })) {
        }
      });
    }
  }

  private static void testStreamOperations() {
    if (false)
      testOperations(values -> {
        assert values.size() == values.stream().flatMap(
            (java.util.function.Function<? super BasicType, ? extends com.simiacryptus.ref.wrappers.RefStream<? extends BasicType>>) x -> {
              return values.stream();
            }).distinct().count();
      });
    testOperations(values_conditionalBlock -> {
      if (true) {
        assert values_conditionalBlock.size() == values_conditionalBlock.stream().toArray(i -> new BasicType[i]).length;
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
      assert values.size() == values.stream().collect(com.simiacryptus.ref.wrappers.RefCollectors.toList()).size();
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
      com.simiacryptus.ref.wrappers.RefSpliterator<com.simiacryptus.demo.refcount.BasicType> iter = values.stream()
          .spliterator();
      while (iter.tryAdvance(x -> {
        assert x != null;
      })) {
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

  public @Override
  void _free() {
    super._free();
  }

  public @Override
  DeququeContainer addRef() {
    return (DeququeContainer) super.addRef();
  }
}
