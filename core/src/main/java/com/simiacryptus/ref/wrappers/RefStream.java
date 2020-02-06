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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.*;

@RefIgnore
@SuppressWarnings("unused")
public class RefStream<T> implements Stream<T> {
  private final Stream<T> inner;
  public List<ReferenceCounting> lambdas;
  public Map<IdentityWrapper<ReferenceCounting>, AtomicInteger> refs;

  public RefStream(@RefAware Stream<T> stream) {
    this(stream, new ArrayList<>(), new ConcurrentHashMap<>());
  }

  RefStream(Stream<T> stream,
            @RefAware List<ReferenceCounting> lambdas,
            @RefAware Map<IdentityWrapper<ReferenceCounting>, AtomicInteger> refs) {
    if (stream instanceof RefStream)
      throw new IllegalArgumentException("inner class cannot be RefStream");
    this.inner = stream.onClose(() -> {
      freeAll(refs);
      synchronized (lambdas) {
        lambdas.forEach(referenceCounting -> referenceCounting.freeRef());
        lambdas.clear();
      }
    });
    this.lambdas = lambdas;
    this.refs = refs;
  }

  @Nonnull
  public Stream<T> getInner() {
    return new StreamWrapper<T>(inner) {
      @Override
      public @Nonnull
      Iterator<T> iterator() {
        final Iterator<T> iterator = super.iterator();
        if (iterator instanceof RefIterator) {
          return ((RefIterator) iterator).getInner();
        }
        return iterator;
      }

      @Override
      public @Nonnull
      Spliterator<T> spliterator() {
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

  @Nonnull
  public static <T> RefStream<T> of(@RefAware T x) {
    return new RefStream<>(Stream.of(x)).onClose(() -> {
      RefUtil.freeRef(x);
    });
  }

  @Nonnull
  public static <T> RefStream<T> of(@Nonnull @RefAware T... array) {
    return new RefStream<>(Stream.of(array).onClose(() -> {
      Arrays.stream(array).forEach(value -> RefUtil.freeRef(value));
    }));
  }

  @Nonnull
  public static <T> RefStream<T> empty() {
    return new RefStream<>(Stream.empty());
  }

  @Nonnull
  public static <T> RefStream<T> concat(@Nonnull @RefAware Stream<? extends T> a,
                                        @Nonnull @RefAware Stream<? extends T> b) {
    Stream<? extends T> a1;
    if (a instanceof RefStream) {
      RefStream<? extends T> refStream1 = (RefStream<? extends T>) a;
      a1 = refStream1.getInner();
    } else {
      a1 = a;
    }
    Stream<? extends T> b1;
    if (b instanceof RefStream) {
      RefStream<? extends T> refStream1 = (RefStream<? extends T>) b;
      b1 = refStream1.getInner();
    } else {
      b1 = b;
    }
    RefStream<T> refStream = new RefStream<>(Stream.concat(a1, b1));
    if (a instanceof RefStream) {
      RefStream<? extends T> refStream1 = (RefStream<? extends T>) a;
      refStream.refs.putAll(refStream1.refs);
      refStream1.refs.clear();
      refStream.lambdas.addAll(refStream1.lambdas);
      refStream1.lambdas.clear();
    }
    if (b instanceof RefStream) {
      RefStream<? extends T> refStream1 = (RefStream<? extends T>) b;
      refStream.refs.putAll(refStream1.refs);
      refStream1.refs.clear();
      refStream.lambdas.addAll(refStream1.lambdas);
      refStream1.lambdas.clear();
    }
    return refStream.onClose(() -> {
      a.close();
      b.close();
    });
  }

  static void freeAll(
      @Nonnull @RefAware Map<IdentityWrapper<ReferenceCounting>, AtomicInteger> refs) {
    synchronized (refs) {
      refs.forEach((k, v) -> {
        final int cnt = v.getAndSet(0);
        if (cnt > 0)
          for (int i = 0; i < cnt; i++) {
            k.inner.freeRef();
          }
      });
      refs.clear();
    }
  }

  static <U> U getRef(@RefAware U u,
                      @Nonnull @RefAware Map<IdentityWrapper<ReferenceCounting>, AtomicInteger> refs) {
    if (u instanceof ReferenceCounting) {
      final AtomicInteger refCnt = refs.computeIfAbsent(new IdentityWrapper(u), x -> new AtomicInteger(0));
      final AtomicBoolean obtained = new AtomicBoolean(false);
      int newStoredRefs = refCnt.updateAndGet(x -> {
        if (x <= 0)
          return 0;
        else {
          obtained.set(true);
          return x - 1;
        }
      });
      if (!obtained.get()) {
        RefUtil.addRef(u);
      }
    } else if (null != u && u.getClass().isArray()) {
      int length = Array.getLength(u);
      for (int i = 0; i < length; i++) {
        getRef(Array.get(u, i), refs);
      }
    }
    return u;
  }

  static <U> U storeRef(@RefAware U u,
                        @Nonnull @RefAware Map<IdentityWrapper<ReferenceCounting>, AtomicInteger> refs) {
    if (u instanceof ReferenceCounting) {
      refs.computeIfAbsent(new IdentityWrapper(u), x -> new AtomicInteger(0)).incrementAndGet();
    } else if (null != u && u.getClass().isArray()) {
      int length = Array.getLength(u);
      for (int i = 0; i < length; i++) {
        storeRef(Array.get(u, i), refs);
      }
    }
    return u;
  }

  @Override
  public boolean allMatch(@Nonnull @RefAware Predicate<? super T> predicate) {
    track(predicate);
    final boolean match = getInner().allMatch((T t) -> predicate.test(getRef(t)));
    close();
    return match;
  }

  @Override
  public boolean anyMatch(@Nonnull @RefAware Predicate<? super T> predicate) {
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
  public <R> R collect(@Nonnull @RefAware Supplier<R> supplier,
                       @Nonnull @RefAware BiConsumer<R, ? super T> accumulator,
                       @Nonnull @RefAware BiConsumer<R, R> combiner) {
    try {
      return getInner().collect(
          () -> supplier.get(),
          (R t1, T u1) -> accumulator.accept(RefUtil.addRef(t1), getRef(u1)),
          (R t, R u) -> combiner.accept(RefUtil.addRef(t), u));
    } finally {
      close();
      RefUtil.freeRef(supplier);
      RefUtil.freeRef(accumulator);
      RefUtil.freeRef(combiner);
    }
  }

  @Override
  public <R, A> R collect(@RefAware Collector<? super T, A, R> collector) {
    if (collector instanceof ReferenceCounting) {
      final Function<A, R> finisher = collector.finisher();
      final R result = finisher
          .apply(this.collect(collector.supplier(), collector.accumulator(), getBiConsumer(collector.combiner())));
      RefUtil.freeRef(finisher);
      RefUtil.freeRef(collector);
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

  @Nonnull
  @Override
  public RefStream<T> distinct() {
    return new RefStream(getInner().distinct(), lambdas, refs);
  }

  @Nonnull
  public RefStream<T> filter(@Nonnull @RefAware Predicate<? super T> predicate) {
    track(predicate);
    return new RefStream(getInner().filter(t -> predicate.test(RefUtil.addRef(t))), lambdas, refs);
  }

  @Nonnull
  @Override
  public Optional<T> findAny() {
    final Optional<T> optional = getInner().findAny().map(value -> RefUtil.addRef(value));
    close();
    return optional;
  }

  @Nonnull
  @Override
  public Optional<T> findFirst() {
    final Optional<T> optional = getInner().findFirst().map(value -> RefUtil.addRef(value));
    close();
    return optional;
  }

  @Nonnull
  @Override
  public <R> RefStream<R> flatMap(
      @Nonnull @RefAware Function<? super T, ? extends Stream<? extends R>> mapper) {
    track(mapper);
    return new RefStream<>(getInner().flatMap((T t) -> mapper.apply(getRef(t))
        //.collect(RefCollectors.toList()).stream()
        .map(u -> storeRef(u))), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefDoubleStream flatMapToDouble(
      @Nonnull @RefAware Function<? super T, ? extends DoubleStream> mapper) {
    track(mapper);
    return new RefDoubleStream(getInner().flatMapToDouble((T t) -> mapper.apply(getRef(t))), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefIntStream flatMapToInt(
      @Nonnull @RefAware Function<? super T, ? extends IntStream> mapper) {
    track(mapper);
    return new RefIntStream(getInner().flatMapToInt((T t) -> mapper.apply(getRef(t))), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefLongStream flatMapToLong(
      @Nonnull @RefAware Function<? super T, ? extends LongStream> mapper) {
    track(mapper);
    return new RefLongStream(getInner().flatMapToLong((T t) -> {
      final T ref = getRef(t);
      return mapper.apply(ref);
    }), lambdas, refs);
  }

  public void forEach(@Nonnull @RefAware Consumer<? super T> action) {
    track(action);
    getInner().forEach((T t) -> action.accept(getRef(t)));
    close();
  }

  @Override
  public void forEachOrdered(@Nonnull @RefAware Consumer<? super T> action) {
    track(action);
    getInner().forEachOrdered((T t) -> action.accept(getRef(t)));
    close();
  }

  @Nonnull
  @Override
  public RefIterator<T> iterator() {
    return new RefIterator<>(getInner().iterator()).track(new ReferenceCountingBase() {
      @Override
      protected void _free() {
        RefStream.this.close();
      }
    });
  }

  @Nonnull
  @Override
  public RefStream<T> limit(long maxSize) {
    return new RefStream(getInner().limit(maxSize), lambdas, refs);
  }

  @Nonnull
  @Override
  public <R> RefStream<R> map(@Nonnull @RefAware Function<? super T, ? extends R> mapper) {
    track(mapper);
    return new RefStream<>(getInner().map(t -> storeRef(mapper.apply(getRef(t)))), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefDoubleStream mapToDouble(@Nonnull @RefAware ToDoubleFunction<? super T> mapper) {
    track(mapper);
    return new RefDoubleStream(getInner().mapToDouble((T value) -> mapper.applyAsDouble(getRef(value))), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefIntStream mapToInt(@Nonnull @RefAware ToIntFunction<? super T> mapper) {
    track(mapper);
    return new RefIntStream(getInner().mapToInt((T value) -> mapper.applyAsInt(getRef(value))), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefLongStream mapToLong(@Nonnull @RefAware ToLongFunction<? super T> mapper) {
    track(mapper);
    return new RefLongStream(getInner().mapToLong((T value) -> mapper.applyAsLong(getRef(value))), lambdas, refs);
  }

  @Nonnull
  @Override
  public Optional<T> max(@Nonnull @RefAware Comparator<? super T> comparator) {
    track(comparator);
    final Optional<T> optional = getInner()
        .max((T o1, T o2) -> comparator.compare(RefUtil.addRef(o1), RefUtil.addRef(o2))).map(u -> getRef(u));
    close();
    return optional;
  }

  @Nonnull
  @Override
  public Optional<T> min(@Nonnull @RefAware Comparator<? super T> comparator) {
    track(comparator);
    final Optional<T> optional = getInner()
        .min((T o1, T o2) -> comparator.compare(RefUtil.addRef(o1), RefUtil.addRef(o2))).map(u -> getRef(u));
    close();
    return optional;
  }

  @Override
  public boolean noneMatch(@Nonnull @RefAware Predicate<? super T> predicate) {
    track(predicate);
    //    final boolean match = !getInner().map((T t) -> predicate.test(getRef(t))).reduce((a, b)->a || b).orElse(false);
    final boolean match = getInner().noneMatch((T t) -> predicate.test(getRef(t)));
    close();
    return match;
  }

  @Nonnull
  @Override
  public RefStream<T> onClose(@RefAware Runnable closeHandler) {
    track(closeHandler);
    return new RefStream(getInner().onClose(closeHandler), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefStream<T> parallel() {
    return new RefStream(getInner().parallel(), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefStream<T> peek(@Nonnull @RefAware Consumer<? super T> action) {
    track(action);
    return new RefStream(getInner().peek((T t) -> action.accept(getRef(t))), lambdas, refs);
  }

  @Nullable
  @Override
  public T reduce(@RefAware T identity,
                  @Nonnull @RefAware BinaryOperator<T> accumulator) {
    track(accumulator);
    try {
      return RefUtil
          .addRef(getInner().reduce(storeRef(identity), (T t, T u) -> storeRef(accumulator.apply(getRef(t), getRef(u)))));
    } finally {
      close();
    }
  }

  @Nonnull
  @Override
  public Optional<T> reduce(@Nonnull @RefAware BinaryOperator<T> accumulator) {
    track(accumulator);
    try {
      return getInner().reduce((T t, T u) -> storeRef(accumulator.apply(getRef(t), getRef(u))))
          .map(u1 -> getRef(u1));
    } finally {
      close();
    }
  }

  @Override
  public <U> U reduce(@RefAware U identity,
                      @Nonnull @RefAware BiFunction<U, ? super T, U> accumulator,
                      @Nonnull @RefAware BinaryOperator<U> combiner) {
    track(accumulator);
    track(combiner);
    final U result = getRef(
        getInner().reduce(storeRef(identity), (U t1, T u1) -> storeRef(accumulator.apply(getRef(t1), getRef(u1))),
            (U t, U u) -> storeRef(combiner.apply(getRef(t), getRef(u)))));
    close();
    return result;
  }

  @Nonnull
  @Override
  public RefStream<T> sequential() {
    return new RefStream(getInner().sequential(), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefStream<T> skip(long n) {
    return new RefStream(getInner().skip(n), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefStream<T> sorted() {
    return new RefStream(getInner().sorted((a, b) -> ((Comparable<T>) a).compareTo(RefUtil.addRef(b))), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefStream<T> sorted(@Nonnull @RefAware Comparator<? super T> comparator) {
    return new RefStream(
        getInner().sorted(
            (T o1, T o2) -> comparator.compare(RefUtil.addRef(o1), RefUtil.addRef(o2))
        ), lambdas, refs).track(comparator);
  }

  @Nonnull
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

  public <U> U storeRef(@RefAware U u) {
    return storeRef(u, refs);
  }

  @Nonnull
  @Override
  public Object[] toArray() {
    final Object[] array = getInner().map(u -> getRef(u)).toArray();
    close();
    return array;
  }

  @Nonnull
  @Override
  public <A> A[] toArray(@Nonnull @RefAware IntFunction<A[]> generator) {
    track(generator);
    final A[] array = getInner().map(u -> getRef(u)).toArray(value -> generator.apply(value));
    close();
    return array;
  }

  @Nonnull
  @Override
  public RefStream<T> unordered() {
    return new RefStream(getInner().unordered(), lambdas, refs);
  }

  @Nonnull
  RefStream<T> track(@Nonnull @RefAware Object... lambda) {
    for (Object l : lambda) {
      if (null != l && l instanceof ReferenceCounting)
        lambdas.add((ReferenceCounting) l);
    }
    return this;
  }

  @Nonnull
  @RefAware
  private <A> BiConsumer<A, A> getBiConsumer(@Nonnull @RefAware BinaryOperator<A> combiner) {
    return RefUtil.wrapInterface((t, u) -> storeRef(combiner.apply(t, u)), combiner);
  }

  private <U> U getRef(@RefAware U u) {
    return getRef(u, this.refs);
  }

  @RefIgnore
  public static class IdentityWrapper<T> {
    public final T inner;

    public IdentityWrapper(@RefAware T inner) {
      this.inner = inner;
    }

    @Override
    public boolean equals(@Nullable @RefAware Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      IdentityWrapper that = (IdentityWrapper) o;
      return inner == that.inner;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(inner);
    }
  }
}
