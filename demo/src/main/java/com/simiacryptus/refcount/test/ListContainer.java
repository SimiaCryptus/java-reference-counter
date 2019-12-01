package com.simiacryptus.refcount.test;

import com.simiacryptus.lang.ref.ReferenceCountingBase;
import org.jetbrains.annotations.NotNull;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class ListContainer extends ReferenceCountingBase {
  public java.util.ArrayList<BasicType> values = new java.util.ArrayList<>();

  public ListContainer(BasicType... values) {
    for (BasicType value : values) {
      this.values.add(value);
    }
  }

  private static void testOperations(Consumer<java.util.ArrayList<BasicType>> fn) {
    java.util.ArrayList<BasicType> values = new java.util.ArrayList<>();
    for (int i = 0; i < 5; i++)
      values.add(new BasicType());
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
    if (values.size() != values.toArray(new BasicType[] {}).length)
      throw new RuntimeException();
  }

  private static void testElementOperations(java.util.ArrayList<BasicType> values) {
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
    if (!values.isEmpty())
      throw new RuntimeException();
  }

  private static void testCollectionOperations(java.util.ArrayList<BasicType> values) {
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
    if (!values.isEmpty())
      throw new RuntimeException();
  }

  public @Override void _free() {
    super._free();
  }

  public void test() {
    System.out.println(String.format("Increment %s", this));
    if (this.values.size() != new java.util.ArrayList<>(this.values).size())
      throw new RuntimeException();
    for (int i = 0; i < TestOperations.count; i++) {
      testCollectionOperations(new java.util.ArrayList<>(this.values));
      testElementOperations(new java.util.ArrayList<>());
      testStreamOperations();
      testIteratorOperations();
    }
  }

  private void testIteratorOperations() {
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
        assert 1 == iterator.previousIndex() : 1 + " != " + iterator.previousIndex();
        iterator.previous();
        iterator.set(new BasicType());
      }
    });
    testOperations(values -> {
      final Spliterator<BasicType> spliterator = values.spliterator();
      final Spliterator<BasicType> split = spliterator.trySplit();
      if (null != split)
        while (split.tryAdvance(x -> {
          assert null != x;
        })) {
        }
      while (spliterator.tryAdvance(x -> {
        assert null != x;
      })) {
      }
    });
  }

  private void testStreamOperations() {
    if (false)
      testOperations(values -> {
        assert values.size() == values.stream().flatMap(x -> {
          return values.stream();
        }).distinct().count();
      });
    if (false)
      testOperations(values_conditionalBlock -> {
        if (true) {
          assert values_conditionalBlock
              .size() == values_conditionalBlock.stream().toArray(i -> new BasicType[i]).length;
        }
      });
    testOperations(values -> {
      assert values.size() == values.stream().map(x -> {
        x.value++;
        return x;
      }).sorted().toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMap(x -> {
        x.value++;
        return java.util.stream.Stream.of(x);
      }).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      values.stream().flatMap(x -> {
        x.value++;
        return java.util.stream.Stream.of(x, new BasicType());
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
      values.stream().peek(x -> {
        assert x != null;
      });
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
      assert values.size() == values.stream().collect(java.util.stream.Collectors.toList()).size();
    });
    testOperations(values -> {
      assert values.size() == values.stream().collect(java.util.stream.Collectors.toSet()).size();
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
      @NotNull
      Spliterator<BasicType> iter = values.stream().spliterator();
      while (iter.tryAdvance(x -> {
        assert x != null;
      })) {
      }
    });
    testOperations(values -> {
      @NotNull
      ListIterator<BasicType> iter = values.listIterator();
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
      assert values.size() == values.stream().map(x -> {
        x.value++;
        return x;
      }).sorted(java.util.Comparator.naturalOrder()).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert null != values.stream().max(java.util.Comparator.comparing(x -> {
        return x.value;
      })).get();
    });
    testOperations(values -> {
      assert null != values.stream().min(java.util.Comparator.comparing(x -> {
        return x.value;
      })).get();
    });
    testOperations(values -> {
      if (values.size() > 4 && values.stream().skip(1).limit(5).count() != 4) {
        throw new AssertionError();
      }
      values.stream().forEach(x -> {
        x.value++;
      });
    });
    testOperations(values -> {
      values.stream().parallel().forEach(x -> {
        x.value++;
      });
    });
    testOperations(values -> {
      if (!values.stream().parallel().isParallel())
        throw new AssertionError();
    });
    testOperations(values -> {
      values.stream().forEachOrdered(x -> {
        x.value++;
      });
    });
    testOperations(values -> {
      assert values.size() == values.stream().sequential().unordered().map(x -> {
        x.value++;
        return x;
      }).toArray(i -> new BasicType[i]).length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToInt(foobar1 -> {
        return foobar1.value;
      }).toArray().length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToDouble(x -> {
        return x.value;
      }).toArray().length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().mapToLong(x -> {
        return x.value;
      }).toArray().length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToInt(foobar1 -> {
        return IntStream.of(foobar1.value);
      }).toArray().length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToDouble(x -> {
        return DoubleStream.of(x.value);
      }).toArray().length;
    });
    testOperations(values -> {
      assert values.size() == values.stream().flatMapToLong(x -> {
        return LongStream.of(x.value);
      }).toArray().length;
    });
  }

  @Override
  public String toString() {
    return "ListContainer{" + "values=" + values + '}';
  }
}
