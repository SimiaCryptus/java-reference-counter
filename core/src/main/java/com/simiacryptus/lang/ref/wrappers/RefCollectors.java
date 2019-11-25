package com.simiacryptus.lang.ref.wrappers;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class RefCollectors {
  public static <T> Collector<T, ?, List<T>> toList() {
    return new RefCollector<>(
        () -> new RefList<T>(),
        (RefList<T> list, T element) -> list.add(element),
        (left, right) -> {
          left.addAll(right);
          return left;
        },
        Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH))
    );
  }

  public static <T> Collector<T, ?, List<T>> toSet() {
    return new RefCollector<>(
        () -> new RefSet<T>(),
        (RefSet<T> list, T element) -> list.add(element),
        (left, right) -> {
          left.addAll(right);
          return left;
        },
        Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH))
    );
  }

  static class RefCollector<T, A, R> implements Collector<T, A, R> {
    private final Supplier<A> supplier;
    private final BiConsumer<A, T> accumulator;
    private final BinaryOperator<A> combiner;
    private final Function<A, R> finisher;
    private final Set<Characteristics> characteristics;

    RefCollector(Supplier<A> supplier,
                 BiConsumer<A, T> accumulator,
                 BinaryOperator<A> combiner,
                 Function<A, R> finisher,
                 Set<Characteristics> characteristics) {
      this.supplier = supplier;
      this.accumulator = accumulator;
      this.combiner = combiner;
      this.finisher = finisher;
      this.characteristics = characteristics;
    }

    RefCollector(Supplier<A> supplier,
                 BiConsumer<A, T> accumulator,
                 BinaryOperator<A> combiner,
                 Set<Characteristics> characteristics) {
      this(supplier, accumulator, combiner, i -> (R) i, characteristics);
    }

    @Override
    public BiConsumer<A, T> accumulator() {
      return accumulator;
    }

    @Override
    public Supplier<A> supplier() {
      return supplier;
    }

    @Override
    public BinaryOperator<A> combiner() {
      return combiner;
    }

    @Override
    public Function<A, R> finisher() {
      return finisher;
    }

    @Override
    public Set<Characteristics> characteristics() {
      return characteristics;
    }
  }
}
