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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.LongStream;

@RefAware
@RefIgnore
@SuppressWarnings("unused")
public class RefLongStream implements LongStream {
  private final LongStream inner;
  private final Map<RefStream.IdentityWrapper<ReferenceCounting>, AtomicInteger> refs;
  private final List<ReferenceCounting> lambdas;

  RefLongStream(LongStream stream) {
    this(stream, new ArrayList<>(), new ConcurrentHashMap<>());
    onClose(() -> {
      this.lambdas.forEach(ReferenceCounting::freeRef);
      RefStream.freeAll(this.refs);
      this.lambdas.clear();
    });
  }

  RefLongStream(LongStream stream, List<ReferenceCounting> lambdas, Map<RefStream.IdentityWrapper<ReferenceCounting>, AtomicInteger> refs) {
    this.lambdas = lambdas;
    this.refs = refs;
    if (stream instanceof ReferenceCounting) throw new IllegalArgumentException("inner class cannot be ref-aware");
    this.inner = stream;
  }

  @Override
  public boolean isParallel() {
    return inner.isParallel();
  }

  @NotNull
  public static RefLongStream range(long startInclusive, final long endExclusive) {
    return new RefLongStream(LongStream.range(startInclusive, endExclusive));
  }

  @NotNull
  public static RefLongStream of(long x) {
    return new RefLongStream(LongStream.of(x));
  }

  @NotNull
  public static RefLongStream of(@NotNull long... array) {
    return new RefLongStream(LongStream.of(array).onClose(() -> {
      Arrays.stream(array).forEach(RefUtil::freeRef);
    }));
  }

  @NotNull
  public static RefLongStream generate(@NotNull LongSupplier s) {
    return new RefLongStream(LongStream.generate(s));
  }

  @NotNull
  public static RefLongStream concat(@NotNull RefLongStream a, @NotNull RefLongStream b) {
    return new RefLongStream(LongStream.concat(a.inner, b.inner));
  }

  @Override
  public boolean allMatch(@NotNull LongPredicate predicate) {
    track(predicate);
    final boolean allMatch = inner.allMatch((long t) -> predicate.test(getRef(t)));
    close();
    return allMatch;
  }

  @Override
  public boolean anyMatch(@NotNull LongPredicate predicate) {
    track(predicate);
    final boolean anyMatch = inner.anyMatch((long t) -> predicate.test(getRef(t)));
    close();
    return anyMatch;
  }

  @NotNull
  @Override
  public RefDoubleStream asDoubleStream() {
    return new RefDoubleStream(inner.asDoubleStream(), lambdas, refs);
  }

  @Override
  public OptionalDouble average() {
    final OptionalDouble average = inner.average();
    close();
    return average;
  }

  @NotNull
  @Override
  public RefStream<Long> boxed() {
    return new RefStream<>(inner.boxed(), lambdas, refs);
  }

  @Override
  public void close() {
    inner.close();
  }

  @Override
  public <R> R collect(@NotNull Supplier<R> supplier, @NotNull ObjLongConsumer<R> accumulator, @NotNull BiConsumer<R, R> combiner) {
    track(supplier);
    track(accumulator);
    track(combiner);
    final R collect = inner.collect(
        () -> storeRef(supplier.get()),
        (R t1, long u1) -> accumulator.accept(RefUtil.addRef(t1), u1),
        (R t, R u) -> combiner.accept(getRef(t), getRef(u))
    );
    close();
    return collect;
  }

  @Override
  public long count() {
    final long count = inner.count();
    close();
    return count;
  }

  @NotNull
  @Override
  public RefLongStream distinct() {
    return new RefLongStream(inner.distinct(), lambdas, refs);
  }

  @NotNull
  public RefLongStream filter(@NotNull LongPredicate predicate) {
    track(predicate);
    return new RefLongStream(inner.filter((long t) -> predicate.test(RefUtil.addRef(t))), lambdas, refs);
  }

  @Override
  public OptionalLong findAny() {
    final OptionalLong any = inner.findAny();
    close();
    return any;
  }

  @Override
  public OptionalLong findFirst() {
    final OptionalLong first = inner.findFirst();
    close();
    return first;
  }

  @NotNull
  @Override
  public RefLongStream flatMap(@NotNull LongFunction<? extends LongStream> mapper) {
    track(mapper);
    return new RefLongStream(inner.flatMap((long t) -> mapper.apply(getRef(t))
        .map(this::storeRef)
    ), lambdas, refs);
  }

  @Override
  public void forEach(@NotNull LongConsumer action) {
    track(action);
    inner.forEach((long t) -> action.accept(getRef(t)));
    close();
  }

  @Override
  public void forEachOrdered(@NotNull LongConsumer action) {
    track(action);
    inner.forEachOrdered((long t) -> action.accept(getRef(t)));
    close();
  }

  @NotNull
  @Override
  public RefPrimitiveIterator.OfLong iterator() {
    return new RefPrimitiveIterator.OfLong(inner.iterator()).track(new ReferenceCountingBase() {
      @Override
      protected void _free() {
        RefLongStream.this.close();
      }
    });

  }

  @NotNull
  @Override
  public RefLongStream limit(long maxSize) {
    return new RefLongStream(inner.limit(maxSize), lambdas, refs);
  }

  @NotNull
  @Override
  public RefLongStream map(@NotNull LongUnaryOperator mapper) {
    track(mapper);
    return new RefLongStream(inner.map(t -> storeRef(mapper.applyAsLong(t))), lambdas, refs);
  }

  @NotNull
  @Override
  public RefDoubleStream mapToDouble(@NotNull LongToDoubleFunction mapper) {
    track(mapper);
    return new RefDoubleStream(inner.mapToDouble((long value) -> mapper.applyAsDouble(getRef(value))), lambdas, refs);
  }

  @NotNull
  @Override
  public RefIntStream mapToInt(@NotNull LongToIntFunction mapper) {
    track(mapper);
    return new RefIntStream(inner.mapToInt((long value) -> mapper.applyAsInt(getRef(value))), lambdas, refs);
  }

  @NotNull
  @Override
  public <U> RefStream<U> mapToObj(LongFunction<? extends U> mapper) {
    return new RefStream<>(inner.mapToObj(mapper), lambdas, refs);
  }

  @Override
  public OptionalLong max() {
    final OptionalLong max = inner.max();
    close();
    return max;
  }

  @Override
  public OptionalLong min() {
    final OptionalLong min = inner.min();
    close();
    return min;
  }

  @Override
  public boolean noneMatch(@NotNull LongPredicate predicate) {
    track(predicate);
    final boolean match = inner.noneMatch((long t) -> predicate.test(getRef(t)));
    close();
    return match;
  }

  @NotNull
  @Override
  public RefLongStream onClose(Runnable closeHandler) {
    track(closeHandler);
    return new RefLongStream(inner.onClose(closeHandler), lambdas, refs);
  }

  @NotNull
  @Override
  public RefLongStream parallel() {
    return new RefLongStream(inner.parallel(), lambdas, refs);
  }

  @NotNull
  @Override
  public RefLongStream peek(@NotNull LongConsumer action) {
    track(action);
    return new RefLongStream(inner.peek((long t) -> action.accept(getRef(t))), lambdas, refs);
  }

  @Override
  public long reduce(long identity, @NotNull LongBinaryOperator accumulator) {
    track(accumulator);
    final long reduce = inner.reduce(storeRef(identity), (long t, long u) -> storeRef(accumulator.applyAsLong(getRef(t), getRef(u))));
    close();
    return reduce;
  }

  @Override
  public OptionalLong reduce(@NotNull LongBinaryOperator accumulator) {
    track(accumulator);
    final OptionalLong optionalLong = inner.reduce((long t, long u) -> storeRef(accumulator.applyAsLong(getRef(t), getRef(u))));
    close();
    return optionalLong;
  }

  @NotNull
  @Override
  public RefLongStream sequential() {
    return new RefLongStream(inner.sequential(), lambdas, refs);
  }

  @NotNull
  @Override
  public RefLongStream skip(long n) {
    return new RefLongStream(inner.skip(n), lambdas, refs);
  }

  @NotNull
  @Override
  public RefLongStream sorted() {
    return new RefLongStream(inner.sorted(), lambdas, refs);
  }

  @NotNull
  @Override
  public RefSpliterator.OfLong spliterator() {
    return new RefSpliterator.OfLong(inner.spliterator()).track(new ReferenceCountingBase() {
      @Override
      protected void _free() {
        RefLongStream.this.close();
      }
    });
  }

  @Override
  public long sum() {
    final long sum = inner.sum();
    close();
    return sum;
  }

  @Override
  public LongSummaryStatistics summaryStatistics() {
    final LongSummaryStatistics statistics = inner.summaryStatistics();
    close();
    return statistics;
  }

  @Override
  public long[] toArray() {
    final long[] longs = inner.toArray();
    close();
    return longs;
  }

  @NotNull
  @Override
  public RefLongStream unordered() {
    return new RefLongStream(inner.unordered(), lambdas, refs);
  }

  private <U> U getRef(U u) {
    return RefStream.getRef(u, this.refs);
  }

  private <U> U storeRef(U u) {
    return RefStream.storeRef(u, refs);
  }

  RefLongStream track(@NotNull Object... lambda) {
    for (Object l : lambda) {
      if (null != l && l instanceof ReferenceCounting) lambdas.add((ReferenceCounting) l);
    }
    return this;
  }

}
