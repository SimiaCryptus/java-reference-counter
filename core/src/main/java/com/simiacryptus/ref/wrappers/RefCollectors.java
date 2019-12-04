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

package com.simiacryptus.ref.wrappers;

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefCoderIgnore;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;

@RefAware
@RefCoderIgnore
public class RefCollectors {
  public static <T> Collector<T, ?, RefList<T>> toList() {
    final Supplier<RefArrayList<T>> supplier = () -> new RefArrayList<T>();
    final BiConsumer<RefArrayList<T>, T> accumulator = (RefArrayList<T> list, T element) -> {
      list.add(element);
      list.freeRef();
    };
    final BinaryOperator<RefArrayList<T>> combiner = (RefArrayList<T> left, RefArrayList<T> right) -> {
      left.addAll(right);
      return left;
    };
    return new RefCollector<>(
        supplier,
        accumulator,
        combiner,
        Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH))
    );
  }

  public static <T> Collector<T, ?, RefSet<T>> toSet() {
    final Supplier<RefHashSet<T>> supplier = () -> new RefHashSet<T>();
    final BiConsumer<RefHashSet<T>, T> accumulator = (RefHashSet<T> list, T element) -> {
      list.add(element);
      list.freeRef();
    };
    final BinaryOperator<RefHashSet<T>> combiner = (RefHashSet<T> left, RefHashSet<T> right) -> {
      left.addAll(right);
      return left;
    };
    return new RefCollector<>(
        supplier,
        accumulator,
        combiner,
        Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH))
    );
  }

  public static <T, K, U>
  Collector<T, ?, Map<K, U>> toMap(Function<? super T, ? extends K> keyMapper,
                                   Function<? super T, ? extends U> valueMapper) {
    return toMap(keyMapper, valueMapper, (u, v) -> {
      throw new IllegalStateException(String.format("Duplicate key %s", u));
    }, HashMap::new);
  }

  public static <T, K, U, M extends Map<K, U>>
  Collector<T, ?, M> toMap(Function<? super T, ? extends K> keyMapper,
                           Function<? super T, ? extends U> valueMapper,
                           BinaryOperator<U> mergeFunction,
                           Supplier<M> mapSupplier) {
    return new CollectorImpl<>(
        mapSupplier,
        (map, element) -> map.merge(keyMapper.apply(element), valueMapper.apply(element), mergeFunction),
        (m1, m2) -> {
          for (Map.Entry<K, U> e : m2.entrySet())
            m1.merge(e.getKey(), e.getValue(), mergeFunction);
          return m1;
        },
        Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH))
    );
  }

  public static <T, K> Collector<T, ?, RefMap<K, RefList<T>>>
  groupingBy(Function<? super T, ? extends K> classifier) {
    return groupingBy(classifier, toList());
  }

  public static <T, K, A, D>
  Collector<T, ?, RefMap<K, D>> groupingBy(Function<? super T, ? extends K> classifier,
                                           Collector<? super T, A, D> downstream) {
    return groupingBy(classifier, RefHashMap::new, downstream);
  }

  public static <T, K, D, A, M extends RefMap<K, D>>
  Collector<T, ?, M> groupingBy(Function<? super T, ? extends K> classifier,
                                Supplier<M> mapFactory,
                                Collector<? super T, A, D> downstream) {
    Supplier<A> downstreamSupplier = downstream.supplier();
    BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
    BiConsumer<RefMap<K, A>, T> accumulator = (m, t) -> {
      K key = Objects.requireNonNull(classifier.apply(t), "element cannot be mapped to a null key");
      A container = m.computeIfAbsent(key, k -> downstreamSupplier.get());
      downstreamAccumulator.accept(container, t);
    };
    BinaryOperator<RefMap<K, A>> merger = RefCollectors.<K, A, RefMap<K, A>>mapMerger(downstream.combiner());
    @SuppressWarnings("unchecked")
    Supplier<RefMap<K, A>> mangledFactory = (Supplier<RefMap<K, A>>) mapFactory;

    if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
      return new CollectorImpl<>(mangledFactory, accumulator, merger, Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH)));
    } else {
      @SuppressWarnings("unchecked")
      Function<A, A> downstreamFinisher = (Function<A, A>) downstream.finisher();
      Function<RefMap<K, A>, M> finisher = intermediate -> {
        intermediate.replaceAll((k, v) -> downstreamFinisher.apply(v));
        @SuppressWarnings("unchecked")
        M castResult = (M) intermediate;
        return castResult;
      };
      return new CollectorImpl<>(mangledFactory, accumulator, merger, finisher, Collections.emptySet());
    }
  }

  private static <K, V, M extends RefMap<K, V>>
  BinaryOperator<M> mapMerger(BinaryOperator<V> mergeFunction) {
    return (m1, m2) -> {
      for (Map.Entry<K, V> e : m2.entrySet())
        m1.merge(e.getKey(), e.getValue(), mergeFunction);
      return m1;
    };
  }

  public static <T, U, A, R>
  Collector<T, ?, R> mapping(Function<? super T, ? extends U> mapper,
                             Collector<? super U, A, R> downstream) {
    BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();
    return new CollectorImpl<>(downstream.supplier(),
        (r, t) -> downstreamAccumulator.accept(r, mapper.apply(t)),
        downstream.combiner(), downstream.finisher(),
        downstream.characteristics());
  }

  public static <T, A, R, RR> Collector<T, A, RR> collectingAndThen(Collector<T, A, R> downstream,
                                                                    Function<R, RR> finisher) {
    Set<Collector.Characteristics> characteristics = downstream.characteristics();
    if (characteristics.contains(Collector.Characteristics.IDENTITY_FINISH)) {
      if (characteristics.size() == 1)
        characteristics = Collections.emptySet();
      else {
        characteristics = EnumSet.copyOf(characteristics);
        characteristics.remove(Collector.Characteristics.IDENTITY_FINISH);
        characteristics = Collections.unmodifiableSet(characteristics);
      }
    }
    return new CollectorImpl<>(downstream.supplier(),
        downstream.accumulator(),
        downstream.combiner(),
        downstream.finisher().andThen(finisher),
        characteristics);
  }

  public static <T> Collector<T, ?, Optional<T>>
  reducing(BinaryOperator<T> op) {
    class OptionalBox implements Consumer<T> {
      T value = null;
      boolean present = false;

      @Override
      public void accept(T t) {
        if (present) {
          value = op.apply(value, t);
        } else {
          value = t;
          present = true;
        }
      }
    }

    return new CollectorImpl<T, OptionalBox, Optional<T>>(
        OptionalBox::new, OptionalBox::accept,
        (a, b) -> {
          if (b.present) a.accept(b.value);
          return a;
        },
        a -> Optional.ofNullable(a.value), Collections.emptySet());
  }

  public static <T> Collector<T, ?, Long>
  counting() {
    return reducing(0L, e -> 1L, Long::sum);
  }

  public static <T, U>
  Collector<T, ?, U> reducing(U identity,
                              Function<? super T, ? extends U> mapper,
                              BinaryOperator<U> op) {
    return new CollectorImpl<>(
        boxSupplier(identity),
        (a, t) -> {
          a[0] = op.apply(a[0], mapper.apply(t));
        },
        (a, b) -> {
          a[0] = op.apply(a[0], b[0]);
          return a;
        },
        a -> a[0], Collections.emptySet());
  }

  @SuppressWarnings("unchecked")
  private static <T> Supplier<T[]> boxSupplier(T identity) {
    return () -> (T[]) new Object[]{identity};
  }

  static class RefCollector<T, A, R> extends ReferenceCountingBase implements Collector<T, A, R> {
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
    public Set<Characteristics> characteristics() {
      return characteristics;
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
    public Supplier<A> supplier() {
      return supplier;
    }
  }

  static class CollectorImpl<T, A, R> implements Collector<T, A, R> {
    private final Supplier<A> supplier;
    private final BiConsumer<A, T> accumulator;
    private final BinaryOperator<A> combiner;
    private final Function<A, R> finisher;
    private final Set<Characteristics> characteristics;

    CollectorImpl(Supplier<A> supplier,
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

    CollectorImpl(Supplier<A> supplier,
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
    public Set<Characteristics> characteristics() {
      return characteristics;
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
    public Supplier<A> supplier() {
      return supplier;
    }
  }

}
