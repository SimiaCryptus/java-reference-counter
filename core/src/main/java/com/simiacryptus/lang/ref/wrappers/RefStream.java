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

package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.RefAware;
import com.simiacryptus.lang.ref.RefUtil;
import com.simiacryptus.lang.ref.ReferenceCounting;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static com.simiacryptus.lang.ref.RefUtil.addRef;

@RefAware
public class RefStream<T> implements Stream<T> {
  private Stream<T> inner;
  private List<ReferenceCounting> lambdas;
  private List<IdentityWrapper<ReferenceCounting>> refs;

  RefStream(Stream<T> stream) {
    this(stream, new ArrayList<>(), new ArrayList<>());
    onClose(() -> {
      this.lambdas.forEach(ReferenceCounting::freeRef);
      this.refs.forEach(x -> x.inner.freeRef());
      this.lambdas.clear();
    });
  }

  RefStream(Stream<T> stream, List<ReferenceCounting> lambdas, List<IdentityWrapper<ReferenceCounting>> refs) {
    if (stream instanceof ReferenceCounting) throw new IllegalArgumentException("inner class cannot be ref-aware");
    this.inner = stream;
    this.lambdas = lambdas;
    this.refs = refs;
  }

  public static <T> RefStream<T> of(T x) {
    return new RefStream<>(Stream.of(x));
  }

  public static <T> RefStream<T> of(T... array) {
    return new RefStream<>(Stream.of(array).onClose(() -> {
      Arrays.stream(array).forEach(RefUtil::freeRef);
    }));
  }

  @Override
  public boolean allMatch(@NotNull Predicate<? super T> predicate) {
    track(predicate);
    return inner.allMatch((T t) -> predicate.test(getRef(t)));
  }

  @Override
  public boolean anyMatch(@NotNull Predicate<? super T> predicate) {
    track(predicate);
    return inner.anyMatch((T t) -> predicate.test(getRef(t)));
  }

  @Override
  public void close() {
    inner.close();
  }

  @Override
  public <R> R collect(@NotNull Supplier<R> supplier, @NotNull BiConsumer<R, ? super T> accumulator, @NotNull BiConsumer<R, R> combiner) {
    track(supplier);
    track(accumulator);
    track(combiner);
    return inner.collect(
        () -> storeRef(supplier.get()),
        (R t1, T u1) -> accumulator.accept(addRef(t1), getRef(u1)),
        (R t, R u) -> combiner.accept(getRef(t), getRef(u))
    );
  }

  @Override
  public <R, A> R collect(Collector<? super T, A, R> collector) {
    if (collector instanceof ReferenceCounting) {
      final Function<A, R> finisher = collector.finisher();
      final R result = finisher.apply(this.collect(
          collector.supplier(),
          collector.accumulator(),
          collector.combiner()::apply));
      ((ReferenceCounting) collector).freeRef();
      return result;
    } else {
      return inner.collect(collector);
    }
  }

  @Override
  public long count() {
    final long count = inner.count();
    return count;
  }

  @NotNull
  @Override
  public RefStream<T> distinct() {
    inner = inner.distinct();
    return this;
  }

  @NotNull
  public RefStream<T> filter(@NotNull Predicate<? super T> predicate) {
    inner = inner.filter((T t) -> predicate.test(addRef(t)));
    track(predicate);
    return this;
  }

  @Override
  public Optional<T> findAny() {
    return inner.findAny().map(RefUtil::addRef);
  }

  @Override
  public Optional<T> findFirst() {
    return inner.findFirst().map(RefUtil::addRef);
  }

  @NotNull
  @Override
  public <R> RefStream<R> flatMap(@NotNull Function<? super T, ? extends Stream<? extends R>> mapper) {
    track(mapper);
    return new RefStream<>(inner.flatMap((T t) -> mapper.apply(getRef(t))
        .collect(Collectors.toList()).stream()
        .map(this::storeRef)
    ), lambdas, refs);
  }

  @Override
  public DoubleStream flatMapToDouble(@NotNull Function<? super T, ? extends DoubleStream> mapper) {
    track(mapper);
    return inner.flatMapToDouble((T t) -> mapper.apply(getRef(t)));
  }

  @Override
  public IntStream flatMapToInt(@NotNull Function<? super T, ? extends IntStream> mapper) {
    track(mapper);
    return inner.flatMapToInt((T t) -> mapper.apply(getRef(t)));
  }

  @Override
  public LongStream flatMapToLong(@NotNull Function<? super T, ? extends LongStream> mapper) {
    track(mapper);
    return inner.flatMapToLong((T t) -> mapper.apply(getRef(t)));
  }

  public void forEach(@NotNull Consumer<? super T> action) {
    track(action);
    inner.forEach((T t) -> action.accept(getRef(t)));
    close();
  }

  @Override
  public void forEachOrdered(@NotNull Consumer<? super T> action) {
    track(action);
    inner.forEachOrdered((T t) -> action.accept(getRef(t)));
  }

  private <U> U getRef(U u) {
    if (u instanceof ReferenceCounting) {
      if (!refs.remove(new IdentityWrapper(u))) {
        addRef(u);
      }
    }
    return u;
  }

  @Override
  public boolean isParallel() {
    return inner.isParallel();
  }

  @NotNull
  @Override
  public RefIterator<T> iterator() {
    return new RefIterator<>(inner.iterator());
  }

  @NotNull
  @Override
  public RefStream<T> limit(long maxSize) {
    inner = inner.limit(maxSize);
    return this;
  }

  @NotNull
  @Override
  public <R> RefStream<R> map(@NotNull Function<? super T, ? extends R> mapper) {
    track(mapper);
    return new RefStream<>(inner.map(t -> storeRef(mapper.apply(getRef(t)))), lambdas, refs);
  }

  @Override
  public DoubleStream mapToDouble(@NotNull ToDoubleFunction<? super T> mapper) {
    track(mapper);
    return inner.mapToDouble((T value) -> mapper.applyAsDouble(getRef(value)));
  }

  @Override
  public IntStream mapToInt(@NotNull ToIntFunction<? super T> mapper) {
    track(mapper);
    return inner.mapToInt((T value) -> mapper.applyAsInt(getRef(value)));
  }

  @Override
  public LongStream mapToLong(@NotNull ToLongFunction<? super T> mapper) {
    track(mapper);
    return inner.mapToLong((T value) -> mapper.applyAsLong(getRef(value)));
  }

  @Override
  public Optional<T> max(@NotNull Comparator<? super T> comparator) {
    track(comparator);
    return inner.max((T o1, T o2) -> comparator.compare(addRef(o1), addRef(o2))).map(this::getRef);
  }

  @Override
  public Optional<T> min(@NotNull Comparator<? super T> comparator) {
    track(comparator);
    return inner.min((T o1, T o2) -> comparator.compare(addRef(o1), addRef(o2))).map(this::getRef);
  }

  @Override
  public boolean noneMatch(@NotNull Predicate<? super T> predicate) {
    track(predicate);
    return inner.noneMatch((T t) -> predicate.test(getRef(t)));
  }

  @NotNull
  @Override
  public RefStream<T> onClose(Runnable closeHandler) {
    inner = inner.onClose(closeHandler);
    track(closeHandler);
    return this;
  }

  @NotNull
  @Override
  public RefStream<T> parallel() {
    inner = inner.parallel();
    return this;
  }

  @NotNull
  @Override
  public RefStream<T> peek(@NotNull Consumer<? super T> action) {
    track(action);
    inner = inner.peek((T t) -> action.accept(getRef(t)));
    return this;
  }

  @Override
  public T reduce(T identity, @NotNull BinaryOperator<T> accumulator) {
    track(accumulator);
    return inner.reduce(storeRef(identity), (T t, T u) -> storeRef(accumulator.apply(getRef(t), getRef(u))));
  }

  @Override
  public Optional<T> reduce(@NotNull BinaryOperator<T> accumulator) {
    track(accumulator);
    return inner.reduce((T t, T u) -> storeRef(accumulator.apply(getRef(t), getRef(u)))).map(this::getRef);
  }

  @Override
  public <U> U reduce(U identity, @NotNull BiFunction<U, ? super T, U> accumulator, @NotNull BinaryOperator<U> combiner) {
    track(accumulator);
    track(combiner);
    return getRef(inner.reduce(
        storeRef(identity),
        (U t1, T u1) -> storeRef(accumulator.apply(getRef(t1), getRef(u1))),
        (U t, U u) -> storeRef(combiner.apply(getRef(t), getRef(u)))));
  }

  @NotNull
  @Override
  public RefStream<T> sequential() {
    inner = inner.sequential();
    return this;
  }

  @NotNull
  @Override
  public RefStream<T> skip(long n) {
    inner = inner.skip(n);
    return this;
  }

  @NotNull
  @Override
  public RefStream<T> sorted() {
    inner = inner.sorted((a, b) -> ((Comparable<T>) a).compareTo(addRef(b)));
    return this;
  }

  @NotNull
  @Override
  public RefStream<T> sorted(@NotNull Comparator<? super T> comparator) {
    inner = inner.sorted((T o1, T o2) -> comparator.compare(addRef(o1), addRef(o2)));
    return this;
  }

  @NotNull
  @Override
  public RefSpliterator<T> spliterator() {
    return new RefSpliterator<>(inner.spliterator());
  }

  private <U> U storeRef(U u) {
    if (u instanceof ReferenceCounting) {
      refs.add(new IdentityWrapper<ReferenceCounting>((ReferenceCounting) u));
    }
    return u;
  }

  @Override
  public Object[] toArray() {
    return inner.map(this::getRef).toArray();
  }

  @Override
  public <A> A[] toArray(@NotNull IntFunction<A[]> generator) {
    track(generator);
    return inner.map(this::getRef).toArray((int value) -> generator.apply(value));
  }

  private void track(Object... lambda) {
    for (Object l : lambda) {
      if (null != l && l instanceof ReferenceCounting) lambdas.add((ReferenceCounting) l);
    }
  }

  @NotNull
  @Override
  public RefStream<T> unordered() {
    inner = inner.unordered();
    return this;
  }

  public static class IdentityWrapper<T> {
    public final T inner;

    public IdentityWrapper(T inner) {
      this.inner = inner;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      IdentityWrapper that = (IdentityWrapper) o;
      return inner == that.inner;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(inner);
    }
  }

}
