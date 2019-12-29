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
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;

/**
 * The type Ref collectors.
 */
@RefAware
@RefIgnore
public class RefCollectors {
  /**
   * To list ref collector.
   *
   * @param <T> the type parameter
   * @return the ref collector
   */
  public static <T> RefCollector<T, ?, RefList<T>> toList() {
    return new RefCollector<>(
        RefArrayList::new,
        (RefArrayList<T> list, T element) -> {
          list.add(element);
          list.freeRef();
        },
        (RefArrayList<T> left, RefArrayList<T> right) -> {
          left.addAll(right);
          return left;
        },
        Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH))
    );
  }

  /**
   * To set ref collector.
   *
   * @param <T> the type parameter
   * @return the ref collector
   */
  public static <T> RefCollector<T, ?, RefSet<T>> toSet() {
    return new RefCollector<>(
        RefHashSet::new,
        (RefHashSet<T> list, T element) -> {
          list.add(element);
          list.freeRef();
        },
        (RefHashSet<T> left, RefHashSet<T> right) -> {
          left.addAll(right);
          right.freeRef();
          return left;
        },
        Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH))
    );
  }

  /**
   * To map ref collector.
   *
   * @param <T>         the type parameter
   * @param <K>         the type parameter
   * @param <U>         the type parameter
   * @param keyMapper   the key mapper
   * @param valueMapper the value mapper
   * @return the ref collector
   */
  public static <T, K, U>
  RefCollector<T, ?, RefMap<K, U>> toMap(Function<? super T, ? extends K> keyMapper,
                                         Function<? super T, ? extends U> valueMapper) {
    return toMap(keyMapper, valueMapper, (u, v) -> {
      throw new IllegalStateException(String.format("Duplicate key %s", u));
    }, RefHashMap::new);
  }

  /**
   * To map ref collector.
   *
   * @param <T>           the type parameter
   * @param <K>           the type parameter
   * @param <U>           the type parameter
   * @param <M>           the type parameter
   * @param keyMapper     the key mapper
   * @param valueMapper   the value mapper
   * @param mergeFunction the merge function
   * @param mapSupplier   the map supplier
   * @return the ref collector
   */
  public static <T, K, U, M extends RefMap<K, U>>
  RefCollector<T, ?, M> toMap(Function<? super T, ? extends K> keyMapper,
                              Function<? super T, ? extends U> valueMapper,
                              BinaryOperator<U> mergeFunction,
                              Supplier<M> mapSupplier) {
    return new RefCollector<>(
        mapSupplier,
        RefUtil.wrapInterface((map, element) -> {
          RefUtil.freeRef(map.merge(
              keyMapper.apply(RefUtil.addRef(element)),
              valueMapper.apply(element),
              RefUtil.addRef(mergeFunction)
          ));
          map.freeRef();
        }, keyMapper, valueMapper, mergeFunction),
        RefUtil.wrapInterface((a, b) -> {
          b.forEach((k, v) -> {
            RefUtil.freeRef(a.merge(k, v, RefUtil.addRef(mergeFunction)));
          });
          RefUtil.freeRef(b);
          return a;
        }, RefUtil.addRef(mergeFunction)),
        Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH))
    );
  }

  /**
   * Grouping by ref collector.
   *
   * @param <T>        the type parameter
   * @param <K>        the type parameter
   * @param classifier the classifier
   * @return the ref collector
   */
  public static <T, K> RefCollector<T, ?, RefMap<K, RefList<T>>>
  groupingBy(Function<? super T, ? extends K> classifier) {
    return groupingBy(classifier, toList());
  }

  /**
   * Grouping by ref collector.
   *
   * @param <T>        the type parameter
   * @param <K>        the type parameter
   * @param <A>        the type parameter
   * @param <D>        the type parameter
   * @param classifier the classifier
   * @param downstream the downstream
   * @return the ref collector
   */
  public static <T, K, A, D>
  RefCollector<T, ?, RefMap<K, D>> groupingBy(Function<? super T, ? extends K> classifier,
                                              Collector<? super T, A, D> downstream) {
    return groupingBy(classifier, RefHashMap::new, downstream);
  }

  /**
   * Grouping by ref collector.
   *
   * @param <T>        the type parameter
   * @param <K>        the type parameter
   * @param <D>        the type parameter
   * @param <A>        the type parameter
   * @param <M>        the type parameter
   * @param classifier the classifier
   * @param mapFactory the map factory
   * @param downstream the downstream
   * @return the ref collector
   */
  public static <T, K, D, A, M extends RefMap<K, D>>
  RefCollector<T, ?, M> groupingBy(Function<? super T, ? extends K> classifier,
                                   Supplier<M> mapFactory,
                                   Collector<? super T, A, D> downstream) {
    final Supplier<A> downstream_supplier = downstream.supplier();
    final BiConsumer<A, ? super T> downstream_accumulator = downstream.accumulator();
    final Set<Collector.Characteristics> downstream_characteristics = downstream.characteristics();
    final BiConsumer<RefMap<K, A>, T> consumer = RefUtil.wrapInterface((map, value) -> {
      downstream_accumulator.accept(
          map.computeIfAbsent(
              classifier.apply(RefUtil.addRef(value)),
              k1 -> {
                final A a = downstream_supplier.get();
                RefUtil.freeRef(k1);
                return a;
              }
          ), value);
      map.freeRef();
    }, classifier, downstream_accumulator, downstream_supplier);
    final BinaryOperator<RefMap<K, A>> combiner = RefCollectors.mapMerger(downstream.combiner());
    final Supplier<RefMap<K, A>> supplier = (Supplier<RefMap<K, A>>) mapFactory;
    final RefCollector<T, RefMap<K, A>, M> collector;
    if (downstream_characteristics.contains(Collector.Characteristics.IDENTITY_FINISH)) {
      collector = new RefCollector<>(
          supplier, consumer, combiner,
          Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH))
      );
    } else {
      final Function<A, D> downstream_finisher = downstream.finisher();
      collector = new RefCollector<>(
          supplier, consumer, combiner,
          RefUtil.wrapInterface(intermediate -> {
            intermediate.replaceAll((k, v) -> ((Function<A, A>) downstream_finisher).apply(v));
            return (M) intermediate;
          }, downstream_finisher),
          Collections.emptySet()
      );
    }
    RefUtil.freeRef(downstream);
    return collector;
  }

  /**
   * Mapping ref collector.
   *
   * @param <T>        the type parameter
   * @param <U>        the type parameter
   * @param <A>        the type parameter
   * @param <R>        the type parameter
   * @param mapper     the mapper
   * @param downstream the downstream
   * @return the ref collector
   */
  public static <T, U, A, R>
  RefCollector<T, ?, R> mapping(Function<? super T, ? extends U> mapper,
                                Collector<? super U, A, R> downstream) {
    BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();
    final RefCollector<T, A, R> collector = new RefCollector<>(
        downstream.supplier(),
        RefUtil.wrapInterface(
            (r, t) -> downstreamAccumulator.accept(r, mapper.apply(t)),
            downstreamAccumulator, mapper
        ),
        downstream.combiner(),
        downstream.finisher(),
        downstream.characteristics()
    );
    RefUtil.freeRef(downstream);
    return collector;
  }

  /**
   * Collecting and then ref collector.
   *
   * @param <T>        the type parameter
   * @param <A>        the type parameter
   * @param <R>        the type parameter
   * @param <RR>       the type parameter
   * @param downstream the downstream
   * @param finisher   the finisher
   * @return the ref collector
   */
  public static <T, A, R, RR> RefCollector<T, A, RR> collectingAndThen(Collector<T, A, R> downstream,
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
    final Function<A, R> downstream_finisher = downstream.finisher();
    final RefCollector<T, A, RR> collector = new RefCollector<>(
        downstream.supplier(),
        downstream.accumulator(),
        downstream.combiner(),
        RefUtil.wrapInterface(downstream_finisher.andThen(finisher), downstream_finisher, finisher),
        characteristics
    );
    RefUtil.freeRef(downstream);
    return collector;
  }

  /**
   * Reducing ref collector.
   *
   * @param <T> the type parameter
   * @param op  the op
   * @return the ref collector
   */
  public static <T> RefCollector<T, ?, Optional<T>>
  reducing(BinaryOperator<T> op) {
    class OptionalBox extends ReferenceCountingBase implements Consumer<T> {
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

      @Override
      protected void _free() {
        RefUtil.freeRef(op);
        super._free();
      }
    }

    return new RefCollector<T, OptionalBox, Optional<T>>(
        OptionalBox::new,
        (optionalBox, t) -> {
          optionalBox.accept(t);
          RefUtil.freeRef(optionalBox);
        },
        (a, b) -> {
          if (b.present) a.accept(b.value);
          b.freeRef();
          return a;
        },
        a -> {
          final Optional<T> value = Optional.ofNullable(a.value);
          a.freeRef();
          return value;
        },
        Collections.emptySet()
    );
  }

  /**
   * Counting ref collector.
   *
   * @param <T> the type parameter
   * @return the ref collector
   */
  public static <T> RefCollector<T, ?, Long>
  counting() {
    return reducing(0L, e -> {
      RefUtil.freeRef(e);
      return 1L;
    }, Long::sum);
  }

  /**
   * Reducing ref collector.
   *
   * @param <T>      the type parameter
   * @param <U>      the type parameter
   * @param identity the identity
   * @param mapper   the mapper
   * @param op       the op
   * @return the ref collector
   */
  public static <T, U>
  RefCollector<T, ?, U> reducing(U identity,
                                 Function<? super T, ? extends U> mapper,
                                 BinaryOperator<U> op) {
    return new RefCollector<>(
        boxSupplier(identity),
        RefUtil.wrapInterface((a, t) -> {
          a[0] = op.apply(a[0], mapper.apply(t));
        }, mapper, RefUtil.addRef(op)),
        RefUtil.wrapInterface((a, b) -> {
          a[0] = op.apply(a[0], b[0]);
          return a;
        }, op),
        a -> a[0], Collections.emptySet());
  }

  private static <K, V, M extends RefMap<K, V>>
  BinaryOperator<M> mapMerger(BinaryOperator<V> mergeFunction) {
    return RefUtil.wrapInterface((a, b) -> {
      for (Map.Entry<K, V> e : b.entrySet()) {
        RefUtil.freeRef(a.merge(e.getKey(), e.getValue(), mergeFunction));
        RefUtil.freeRef(e);
      }
      b.freeRef();
      return a;
    }, mergeFunction);
  }

  @SuppressWarnings("unchecked")
  private static <T> Supplier<T[]> boxSupplier(T identity) {
    return RefUtil.wrapInterface(() -> (T[]) new Object[]{RefUtil.addRef(identity)}, identity);
  }

  /**
   * The type Ref collector.
   *
   * @param <T> the type parameter
   * @param <A> the type parameter
   * @param <R> the type parameter
   */
  public static class RefCollector<T, A, R> extends ReferenceCountingBase implements Collector<T, A, R> {
    private final Supplier<A> supplier;
    private final BiConsumer<A, T> accumulator;
    private final BinaryOperator<A> combiner;
    private final Function<A, R> finisher;
    private final Set<Characteristics> characteristics;

    /**
     * Instantiates a new Ref collector.
     *
     * @param supplier        the supplier
     * @param accumulator     the accumulator
     * @param combiner        the combiner
     * @param finisher        the finisher
     * @param characteristics the characteristics
     */
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

    /**
     * Instantiates a new Ref collector.
     *
     * @param supplier        the supplier
     * @param accumulator     the accumulator
     * @param combiner        the combiner
     * @param characteristics the characteristics
     */
    RefCollector(Supplier<A> supplier,
                 BiConsumer<A, T> accumulator,
                 BinaryOperator<A> combiner,
                 Set<Characteristics> characteristics) {
      this(supplier, accumulator, combiner, i -> (R) i, characteristics);
    }

    @Override
    public BiConsumer<A, T> accumulator() {
      return RefUtil.addRef(accumulator);
    }

    @Override
    public Set<Characteristics> characteristics() {
      return RefUtil.addRef(characteristics);
    }

    @Override
    public BinaryOperator<A> combiner() {
      return RefUtil.addRef(combiner);
    }

    @Override
    public Function<A, R> finisher() {
      return RefUtil.addRef(finisher);
    }

    @Override
    public Supplier<A> supplier() {
      return RefUtil.addRef(supplier);
    }

    @Override
    protected void _free() {
      RefUtil.freeRef(supplier);
      RefUtil.freeRef(accumulator);
      RefUtil.freeRef(combiner);
      RefUtil.freeRef(finisher);
      RefUtil.freeRef(characteristics);
      super._free();
    }
  }

}
