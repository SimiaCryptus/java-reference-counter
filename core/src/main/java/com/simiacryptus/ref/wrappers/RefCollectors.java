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

package com.simiacryptus.ref.wrappers;

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;

@RefIgnore
@SuppressWarnings("unused")
public class RefCollectors {
  @Nonnull
  public static <T> RefCollector<T, ?, RefList<T>> toList() {
    return new RefCollector<>(() -> new RefArrayList<T>(), (RefArrayList<T> list, T element) -> {
      list.add(element);
      list.freeRef();
    }, (RefArrayList<T> left, RefArrayList<T> right) -> {
      left.addAll(right);
      return left;
    }, Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH)));
  }

  @Nonnull
  public static <T> RefCollector<T, ?, RefSet<T>> toSet() {
    return new RefCollector<>(() -> new RefHashSet<T>(), (RefHashSet<T> list, T element) -> {
      list.add(element);
      list.freeRef();
    }, (RefHashSet<T> left, RefHashSet<T> right) -> {
      left.addAll(right);
      right.freeRef();
      return left;
    }, Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH)));
  }

  @Nonnull
  public static <T, K, U> RefCollector<T, ?, RefMap<K, U>> toMap(
      @Nonnull @RefAware Function<? super T, ? extends K> keyMapper,
      @Nonnull @RefAware Function<? super T, ? extends U> valueMapper) {
    return toMap(keyMapper, valueMapper, (u, v) -> {
      throw new IllegalStateException(String.format("Duplicate key %s", u));
    }, () -> new RefHashMap<K, U>());
  }

  @Nonnull
  public static <T, K, U, M extends RefMap<K, U>> RefCollector<T, ?, M> toMap(
      @Nonnull @RefAware Function<? super T, ? extends K> keyMapper,
      @Nonnull @RefAware Function<? super T, ? extends U> valueMapper,
      @RefAware BinaryOperator<U> mergeFunction,
      @RefAware Supplier<M> mapSupplier) {
    BiConsumer<M, T> accumulator = RefUtil.wrapInterface((map, element) -> {
      try {
        RefUtil.freeRef(map.merge(
            keyMapper.apply(RefUtil.addRef(element)),
            valueMapper.apply(element),
            RefUtil.addRef(mergeFunction)
        ));
      } finally {
        map.freeRef();
      }
    }, keyMapper, valueMapper, mergeFunction);
    BinaryOperator<M> combiner = RefUtil.wrapInterface((a, b) -> {
      try {
        b.forEach((k, v) -> {
          RefUtil.freeRef(a.merge(k, v, RefUtil.addRef(mergeFunction)));
        });
        return a;
      } finally {
        RefUtil.freeRef(b);
      }
    }, RefUtil.addRef(mergeFunction));
    return new RefCollector<>(
        mapSupplier, accumulator, combiner,
        Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH))
    );
  }

  @Nonnull
  public static <T, K> RefCollector<T, ?, RefMap<K, RefList<T>>> groupingBy(
      @Nonnull @RefAware RefFunction<? super T, ? extends K> classifier) {
    return groupingBy(classifier, toList());
  }

  @Nonnull
  public static <T, K, A, D> RefCollector<T, ?, RefMap<K, D>> groupingBy(
      @Nonnull @RefAware RefFunction<? super T, ? extends K> classifier,
      @Nonnull @RefAware RefCollector<? super T, A, D> downstream) {
    return groupingBy(classifier, () -> new RefHashMap<K, D>(), downstream);
  }

  @Nonnull
  public static <T, K, D, A, M extends RefMap<K, D>> RefCollector<T, RefMap<K, A>, M> groupingBy(
      @Nonnull @RefAware RefFunction<? super T, ? extends K> classifier,
      @RefAware Supplier<M> mapFactory,
      @Nonnull @RefAware RefCollector<? super T, A, D> downstream) {
    final Supplier<A> downstream_supplier = downstream.supplier();
    final BiConsumer<A, ? super T> downstream_accumulator = downstream.accumulator();
    final BiConsumer<RefMap<K, A>, T> consumer = RefUtil.wrapInterface((map, value) -> {
      A a;
      synchronized (map) {
        a = map.computeIfAbsent(classifier.apply(RefUtil.addRef(value)), k1 -> {
          RefUtil.freeRef(k1);
          return downstream_supplier.get();
        });
      }
      map.freeRef();
      downstream_accumulator.accept(a, value);
    }, classifier, downstream_accumulator, downstream_supplier);
    final Set<Collector.Characteristics> downstream_characteristics = downstream.characteristics();
    final BinaryOperator<RefMap<K, A>> combiner = RefCollectors.mapMerger(downstream.combiner());
    final Supplier<RefMap<K, A>> supplier = (Supplier<RefMap<K, A>>) mapFactory;
    final RefCollector<T, RefMap<K, A>, M> collector;
    if (downstream_characteristics.contains(Collector.Characteristics.IDENTITY_FINISH)) {
      collector = new RefCollector<>(
          supplier,
          consumer,
          combiner,
          Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH)));
    } else {
      final RefFunction<A, D> downstream_finisher = downstream.finisher();
      RefFunction<RefMap<K, A>, M> finisher = RefUtil.wrapInterface((RefMap<K, A> intermediate) -> {
        try {
          return (M) intermediate.mapValues(RefUtil.addRef(downstream_finisher));
        } finally {
          intermediate.freeRef();
        }
      }, downstream_finisher);
      collector = new RefCollector<>(
          supplier,
          consumer,
          combiner,
          finisher,
          Collections.emptySet());
    }
    RefUtil.freeRef(downstream);
    return collector;
  }

  @Nonnull
  public static <T, U, A, R> RefCollector<T, ?, R> mapping(
      @Nonnull @RefAware Function<? super T, ? extends U> mapper,
      @Nonnull @RefAware RefCollector<? super U, A, R> downstream) {
    BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();
    final RefCollector<T, A, R> collector = new RefCollector<>(
        downstream.supplier(),
        RefUtil.wrapInterface((r, t) -> downstreamAccumulator.accept(r, mapper.apply(t)), downstreamAccumulator, mapper),
        downstream.combiner(),
        downstream.finisher(),
        downstream.characteristics()
    );
    RefUtil.freeRef(downstream);
    return collector;
  }

  @Nonnull
  public static <T, A, R, RR> RefCollector<T, A, RR> collectingAndThen(
      @Nonnull @RefAware RefCollector<T, A, R> downstream,
      @Nonnull @RefAware RefFunction<R, RR> finisher) {
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
    final RefFunction<A, R> downstream_finisher = downstream.finisher();
    final RefCollector<T, A, RR> collector = new RefCollector<>(
        downstream.supplier(),
        downstream.accumulator(),
        downstream.combiner(),
        RefUtil.wrapInterface(downstream_finisher.andThen(finisher), downstream_finisher, finisher),
        characteristics);
    RefUtil.freeRef(downstream);
    return collector;
  }

  @Nonnull
  public static <T> RefCollector<T, OptionalBox<T>, Optional<T>> reducing(
      @Nonnull @RefAware BinaryOperator<T> op) {
    return new RefCollector<T, OptionalBox<T>, Optional<T>>(() -> {
      return new OptionalBox<>(op);
    }, (OptionalBox<T> optionalBox, T t) -> {
      optionalBox.accept(t);
      RefUtil.freeRef(optionalBox);
    }, (OptionalBox<T> a, OptionalBox<T> b) -> {
      if (b.present)
        a.accept(b.getValue());
      b.freeRef();
      return a;
    }, (OptionalBox<T> a) -> {
      final Optional<T> value;
      if (a.present) {
        value = Optional.ofNullable(a.getValue());
      } else {
        value = Optional.empty();
      }
      a.freeRef();
      return value;
    }, Collections.emptySet());
  }

  @Nonnull
  public static <T> RefCollector<T, Long[], Long> counting() {
    return reducing(0L, (T e) -> {
      RefUtil.freeRef(e);
      return 1L;
    }, (a, b) -> Long.sum(a, b));
  }

  @Nonnull
  public static <T, U> RefCollector<T, U[], U> reducing(@RefAware U identity,
                                                        @Nonnull @RefAware Function<? super T, ? extends U> mapper,
                                                        @Nonnull @RefAware BinaryOperator<U> op) {
    return new RefCollector<>(
        boxSupplier(identity),
        RefUtil.wrapInterface((U[] a, T t) -> {
          a[0] = op.apply(a[0], mapper.apply(t));
        }, mapper, RefUtil.addRef(op)),
        RefUtil.wrapInterface((a, b) -> {
          a[0] = op.apply(a[0], b[0]);
          return a;
        }, op),
        a -> a[0],
        Collections.emptySet());
  }

  @Nonnull
  public static RefCollector<CharSequence, ?, String> joining(@Nonnull @RefAware CharSequence s) {
    return joining(s, "", "");
  }

  @Nonnull
  public static RefCollector<CharSequence, ?, String> joining(
      @Nonnull @RefAware CharSequence delimiter,
      @Nonnull @RefAware CharSequence prefix,
      @Nonnull @RefAware CharSequence suffix) {
    return new RefCollector<>(() -> new StringJoiner(delimiter, prefix, suffix), (stringJoiner2, newElement) -> stringJoiner2.add(newElement), (stringJoiner1, other) -> stringJoiner1.merge(other),
        stringJoiner -> stringJoiner.toString(), Collections.emptySet());
  }

  @Nonnull
  private static <K, V, M extends RefMap<K, V>> BinaryOperator<M> mapMerger(
      @Nonnull @RefAware BinaryOperator<V> mergeFunction) {
    return RefUtil.wrapInterface((a, b) -> {
      b.forEach((k, v) -> {
        RefUtil.freeRef(a.merge(k, v, RefUtil.addRef(mergeFunction)));
      });
      b.freeRef();
      return a;
    }, mergeFunction);
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  private static <T> Supplier<T[]> boxSupplier(@RefAware T... identity) {
    return RefUtil.wrapInterface(() -> RefUtil.addRef(identity), identity);
  }

  @RefIgnore
  public static class RefCollector<T, A, R> extends ReferenceCountingBase implements Collector<T, A, R> {
    private final Supplier<A> supplier;
    private final BiConsumer<A, T> accumulator;
    private final BinaryOperator<A> combiner;
    private final RefFunction<A, R> finisher;
    private final Set<Characteristics> characteristics;

    RefCollector(@RefAware Supplier<A> supplier,
                 @RefAware BiConsumer<A, T> accumulator,
                 @RefAware BinaryOperator<A> combiner,
                 @RefAware RefFunction<A, R> finisher,
                 @RefAware Set<Characteristics> characteristics) {
      this.supplier = supplier;
      this.accumulator = accumulator;
      this.combiner = combiner;
      this.finisher = finisher;
      this.characteristics = characteristics;
    }

    RefCollector(@RefAware Supplier<A> supplier,
                 @RefAware BiConsumer<A, T> accumulator,
                 @RefAware BinaryOperator<A> combiner,
                 @RefAware Set<Characteristics> characteristics) {
      this(supplier, accumulator, combiner, i -> (R) i, characteristics);
    }

    @Nullable
    @Override
    public BiConsumer<A, T> accumulator() {
      return RefUtil.addRef(accumulator);
    }

    @Nullable
    @Override
    public Set<Characteristics> characteristics() {
      return RefUtil.addRef(characteristics);
    }

    @Nullable
    @Override
    public BinaryOperator<A> combiner() {
      return RefUtil.addRef(combiner);
    }

    @Nullable
    @Override
    public RefFunction<A, R> finisher() {
      return RefUtil.addRef(finisher);
    }

    @Nullable
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

  @RefIgnore
  private static class OptionalBox<T> extends ReferenceCountingBase implements Consumer<T> {
    private final BinaryOperator<T> op;
    @Nullable
    private T value = null;
    private boolean present = false;

    public OptionalBox(BinaryOperator<T> op) {
      this.op = op;
    }

    @Nullable
    public synchronized T getValue() {
      return RefUtil.addRef(value);
    }

    public synchronized void setValue(@Nullable T value) {
      RefUtil.freeRef(this.value);
      this.value = value;
    }

    @Override
    public synchronized void accept(@RefAware T t) {
      if (present) {
        setValue(op.apply(getValue(), t));
      } else {
        setValue(t);
        present = true;
      }
    }

    @Override
    protected void _free() {
      RefUtil.freeRef(op);
      RefUtil.freeRef(value);
      super._free();
    }
  }

}
