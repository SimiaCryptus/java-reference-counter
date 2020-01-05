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

import com.simiacryptus.ref.lang.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.*;

@RefAware
@RefIgnore
@SuppressWarnings("unused")
public class RefStream<T> implements Stream<T> {
  private final Stream<T> inner;
  public List<ReferenceCounting> lambdas;
  public Map<IdentityWrapper<ReferenceCounting>, AtomicInteger> refs;

  public RefStream(Stream<T> stream) {
    this(stream, new ArrayList<>(), new ConcurrentHashMap<>());
    onClose(() -> {
      this.lambdas.forEach(ReferenceCounting::freeRef);
      freeAll(this.refs);
      this.lambdas.clear();
    });
  }

  RefStream(Stream<T> stream, List<ReferenceCounting> lambdas, Map<IdentityWrapper<ReferenceCounting>, AtomicInteger> refs) {
    if (stream instanceof ReferenceCounting) throw new IllegalArgumentException("inner class cannot be ref-aware");
    this.inner = stream;
    this.lambdas = lambdas;
    this.refs = refs;
  }

  @NotNull
  public Stream<T> getInner() {
    return new StreamWrapper<T>(inner) {
      @Override
      public @NotNull Iterator<T> iterator() {
        final Iterator<T> iterator = super.iterator();
        if (iterator instanceof RefIterator) {
          return ((RefIterator) iterator).getInner();
        }
        return iterator;
      }

      @Override
      public @NotNull Spliterator<T> spliterator() {
        final Spliterator<T> spliterator = super.spliterator();
        if (spliterator instanceof RefSpliterator) {
          return ((RefSpliterator) spliterator).getInner();
        }
        return spliterator;
      }
    };
  }

  @Override
  public boolean isParallel() {
    return getInner().isParallel();
  }

  @NotNull
  public static <T> RefStream<T> of(T x) {
    return new RefStream<>(Stream.of(x)).onClose(() -> {
      RefUtil.freeRef(x);
    });
  }

  @NotNull
  public static <T> RefStream<T> of(@NotNull T... array) {
    return new RefStream<>(Stream.of(array).onClose(() -> {
      Arrays.stream(array).forEach(RefUtil::freeRef);
    }));
  }

  @NotNull
  public static <T> RefStream<T> empty() {
    return new RefStream<>(Stream.empty());
  }

  @NotNull
  public static <T> RefStream<T> concat(@NotNull Stream<? extends T> a, @NotNull Stream<? extends T> b) {
    return new RefStream<>(Stream.concat(a, b));
  }

  static void freeAll(@NotNull Map<IdentityWrapper<ReferenceCounting>, AtomicInteger> refs) {
    refs.forEach((k, v) -> {
      final int cnt = v.getAndSet(0);
      if (cnt > 0) for (int i = 0; i < cnt; i++) {
        k.inner.freeRef();
      }
    });
  }

  static <U> U getRef(U u, @NotNull Map<IdentityWrapper<ReferenceCounting>, AtomicInteger> refs) {
    if (u instanceof ReferenceCounting) {
      final AtomicInteger refCnt = refs.computeIfAbsent(new IdentityWrapper(u), x -> new AtomicInteger(0));
      final AtomicBoolean obtained = new AtomicBoolean(false);
      refCnt.updateAndGet(x -> {
        if (x <= 0) return 0;
        else {
          obtained.set(true);
          return x - 1;
        }
      });
      if (!obtained.get()) {
        RefUtil.addRef(u);
      }
    }
    return u;
  }

  static <U> U storeRef(U u, @NotNull Map<IdentityWrapper<ReferenceCounting>, AtomicInteger> refs) {
    if (u instanceof ReferenceCounting) {
      refs.computeIfAbsent(new IdentityWrapper(u), x -> new AtomicInteger(0)).incrementAndGet();
    }
    return u;
  }

  @Override
  public boolean allMatch(@NotNull Predicate<? super T> predicate) {
    track(predicate);
    final boolean match = getInner().allMatch((T t) -> predicate.test(getRef(t)));
    close();
    return match;
  }

  @Override
  public boolean anyMatch(@NotNull Predicate<? super T> predicate) {
    track(predicate);
    final boolean match = getInner().anyMatch((T t) -> predicate.test(getRef(t)));
    close();
    return match;
  }

  @Override
  public void close() {
    getInner().close();
  }

  @Override
  public <R> R collect(@NotNull Supplier<R> supplier,
                       @NotNull BiConsumer<R, ? super T> accumulator,
                       @NotNull BiConsumer<R, R> combiner
  ) {
    final R collect = getInner().collect(
        () -> supplier.get(),
        (R t1, T u1) -> accumulator.accept(RefUtil.addRef(t1), getRef(u1)),
        (R t, R u) -> combiner.accept(RefUtil.addRef(t), u)
    );
    close();
    RefUtil.freeRef(supplier);
    RefUtil.freeRef(accumulator);
    RefUtil.freeRef(combiner);
    return collect;
  }

  @Override
  public <R, A> R collect(Collector<? super T, A, R> collector) {
    if (collector instanceof ReferenceCounting) {
      final Function<A, R> finisher = collector.finisher();
      final R result = finisher.apply(this.collect(
          collector.supplier(),
          collector.accumulator(),
          getBiConsumer(collector.combiner())
      ));
      RefUtil.freeRef(finisher);
      ((ReferenceCounting) collector).freeRef();
      close();
      return result;
    } else {
      final R collect = getInner().collect(collector);
      close();
      return collect;
    }
  }

  @Override
  public long count() {
    final long count = getInner().count();
    close();
    return count;
  }

  @NotNull
  @Override
  public RefStream<T> distinct() {
    return new RefStream(getInner().distinct(), lambdas, refs);
  }

  @NotNull
  public RefStream<T> filter(@NotNull Predicate<? super T> predicate) {
    track(predicate);
    return new RefStream(getInner().filter((T t) -> predicate.test(getRef(t))), lambdas, refs);
  }

  @NotNull
  @Override
  public Optional<T> findAny() {
    final Optional<T> optional = getInner().findAny().map(RefUtil::addRef);
    close();
    return optional;
  }

  @NotNull
  @Override
  public Optional<T> findFirst() {
    final Optional<T> optional = getInner().findFirst().map(RefUtil::addRef);
    close();
    return optional;
  }

  @NotNull
  @Override
  public <R> RefStream<R> flatMap(@NotNull Function<? super T, ? extends Stream<? extends R>> mapper) {
    track(mapper);
    return new RefStream<>(getInner().flatMap((T t) -> mapper.apply(getRef(t))
        //.collect(RefCollectors.toList()).stream()
        .map(this::storeRef)
    ), lambdas, refs);
  }

  @NotNull
  @Override
  public RefDoubleStream flatMapToDouble(@NotNull Function<? super T, ? extends DoubleStream> mapper) {
    track(mapper);
    return new RefDoubleStream(getInner().flatMapToDouble((T t) -> mapper.apply(getRef(t))), lambdas, refs);
  }

  @NotNull
  @Override
  public RefIntStream flatMapToInt(@NotNull Function<? super T, ? extends IntStream> mapper) {
    track(mapper);
    return new RefIntStream(getInner().flatMapToInt((T t) -> mapper.apply(getRef(t))), lambdas, refs);
  }

  @NotNull
  @Override
  public RefLongStream flatMapToLong(@NotNull Function<? super T, ? extends LongStream> mapper) {
    track(mapper);
    return new RefLongStream(getInner().flatMapToLong((T t) -> {
      final T ref = getRef(t);
      return mapper.apply(ref);
    }), lambdas, refs);
  }

  public void forEach(@NotNull Consumer<? super T> action) {
    track(action);
    getInner().forEach((T t) -> action.accept(getRef(t)));
    close();
  }

  @Override
  public void forEachOrdered(@NotNull Consumer<? super T> action) {
    track(action);
    getInner().forEachOrdered((T t) -> action.accept(getRef(t)));
    close();
  }

  @NotNull
  @Override
  public RefIterator<T> iterator() {
    return new RefIterator<>(getInner().iterator()).track(new ReferenceCountingBase() {
      @Override
      protected void _free() {
        RefStream.this.close();
      }
    });
  }

  @NotNull
  @Override
  public RefStream<T> limit(long maxSize) {
    return new RefStream(getInner().limit(maxSize), lambdas, refs);
  }

  @NotNull
  @Override
  public <R> RefStream<R> map(@NotNull Function<? super T, ? extends R> mapper) {
    track(mapper);
    return new RefStream<>(getInner().map(t -> storeRef(mapper.apply(getRef(t)))), lambdas, refs);
  }

  @NotNull
  @Override
  public RefDoubleStream mapToDouble(@NotNull ToDoubleFunction<? super T> mapper) {
    track(mapper);
    return new RefDoubleStream(getInner().mapToDouble((T value) -> mapper.applyAsDouble(getRef(value))), lambdas, refs);
  }

  @NotNull
  @Override
  public RefIntStream mapToInt(@NotNull ToIntFunction<? super T> mapper) {
    track(mapper);
    return new RefIntStream(getInner().mapToInt((T value) -> mapper.applyAsInt(getRef(value))), lambdas, refs);
  }

  @NotNull
  @Override
  public RefLongStream mapToLong(@NotNull ToLongFunction<? super T> mapper) {
    track(mapper);
    return new RefLongStream(getInner().mapToLong((T value) -> mapper.applyAsLong(getRef(value))), lambdas, refs);
  }

  @NotNull
  @Override
  public Optional<T> max(@NotNull Comparator<? super T> comparator) {
    track(comparator);
    final Optional<T> optional = getInner().max((T o1, T o2) -> comparator.compare(RefUtil.addRef(o1), RefUtil.addRef(o2))).map(this::getRef);
    close();
    return optional;
  }

  @NotNull
  @Override
  public Optional<T> min(@NotNull Comparator<? super T> comparator) {
    track(comparator);
    final Optional<T> optional = getInner().min((T o1, T o2) -> comparator.compare(RefUtil.addRef(o1), RefUtil.addRef(o2))).map(this::getRef);
    close();
    return optional;
  }

  @Override
  public boolean noneMatch(@NotNull Predicate<? super T> predicate) {
    track(predicate);
//    final boolean match = !getInner().map((T t) -> predicate.test(getRef(t))).reduce((a, b)->a || b).orElse(false);
    final boolean match = getInner().noneMatch((T t) -> predicate.test(getRef(t)));
    close();
    return match;
  }

  @NotNull
  @Override
  public RefStream<T> onClose(Runnable closeHandler) {
    track(closeHandler);
    return new RefStream(getInner().onClose(closeHandler), lambdas, refs);
  }

  @NotNull
  @Override
  public RefStream<T> parallel() {
    return new RefStream(getInner().parallel(), lambdas, refs);
  }

  @NotNull
  @Override
  public RefStream<T> peek(@NotNull Consumer<? super T> action) {
    track(action);
    return new RefStream(getInner().peek((T t) -> action.accept(getRef(t))), lambdas, refs);
  }

  @Nullable
  @Override
  public T reduce(T identity, @NotNull BinaryOperator<T> accumulator) {
    track(accumulator);
    final T reduce = RefUtil.addRef(getInner().reduce(storeRef(identity), (T t, T u) -> storeRef(accumulator.apply(getRef(t), getRef(u)))));
    close();
    return reduce;
  }

  @NotNull
  @Override
  public Optional<T> reduce(@NotNull BinaryOperator<T> accumulator) {
    track(accumulator);
    final Optional<T> optional = getInner().reduce((T t, T u) -> storeRef(accumulator.apply(getRef(t), getRef(u)))).map(this::getRef);
    close();
    return optional;
  }

  @Override
  public <U> U reduce(U identity, @NotNull BiFunction<U, ? super T, U> accumulator, @NotNull BinaryOperator<U> combiner) {
    track(accumulator);
    track(combiner);
    final U result = getRef(getInner().reduce(
        storeRef(identity),
        (U t1, T u1) -> storeRef(accumulator.apply(getRef(t1), getRef(u1))),
        (U t, U u) -> storeRef(combiner.apply(getRef(t), getRef(u)))));
    close();
    return result;
  }

  @NotNull
  @Override
  public RefStream<T> sequential() {
    return new RefStream(getInner().sequential(), lambdas, refs);
  }

  @NotNull
  @Override
  public RefStream<T> skip(long n) {
    return new RefStream(getInner().skip(n), lambdas, refs);
  }

  @NotNull
  @Override
  public RefStream<T> sorted() {
    return new RefStream(getInner().sorted((a, b) -> ((Comparable<T>) a).compareTo(RefUtil.addRef(b))), lambdas, refs);
  }

  @NotNull
  @Override
  public RefStream<T> sorted(@NotNull Comparator<? super T> comparator) {
    return new RefStream(getInner().sorted((T o1, T o2) -> comparator.compare(RefUtil.addRef(o1), RefUtil.addRef(o2))), lambdas, refs);
  }

  @NotNull
  @Override
  public RefSpliterator<T> spliterator() {
    final Spliterator<T> spliterator = getInner().spliterator();
    final RefSpliterator<T> refSpliterator;
    if (spliterator instanceof RefSpliterator) {
      refSpliterator = ((RefSpliterator<T>) spliterator).addRef();
    } else {
      refSpliterator = new RefSpliterator<>(spliterator);
    }
    return refSpliterator.track(new ReferenceCountingBase() {
      @Override
      protected void _free() {
        RefStream.this.close();
      }
    });
  }

  public <U> U storeRef(U u) {
    return storeRef(u, refs);
  }

  @NotNull
  @Override
  public Object[] toArray() {
    final Object[] array = getInner().map(this::getRef).toArray();
    close();
    return array;
  }

  @NotNull
  @Override
  public <A> A[] toArray(@NotNull IntFunction<A[]> generator) {
    track(generator);
    final A[] array = getInner().map(this::getRef).toArray((int value) -> generator.apply(value));
    close();
    return array;
  }

  @NotNull
  @Override
  public RefStream<T> unordered() {
    return new RefStream(getInner().unordered(), lambdas, refs);
  }

  RefStream<T> track(@NotNull Object... lambda) {
    for (Object l : lambda) {
      if (null != l && l instanceof ReferenceCounting) lambdas.add((ReferenceCounting) l);
    }
    return this;
  }

  @NotNull
  private <A> BiConsumer<A, A> getBiConsumer(@NotNull BinaryOperator<A> combiner) {
    return RefUtil.wrapInterface((t, u) -> RefUtil.freeRef(combiner.apply(t, u)), combiner);
  }

  private <U> U getRef(U u) {
    return getRef(u, this.refs);
  }

  @RefAware
  @RefIgnore
  public static class IdentityWrapper<T> {
    public final T inner;

    public IdentityWrapper(T inner) {
      this.inner = inner;
    }

    @Override
    public boolean equals(@Nullable Object o) {
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
