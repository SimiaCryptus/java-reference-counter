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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.IntStream;

@RefIgnore
@SuppressWarnings("unused")
public class RefIntStream implements IntStream {
  private final IntStream inner;
  private final Map<RefStream.IdentityWrapper<ReferenceCounting>, AtomicInteger> refs;
  private final List<ReferenceCounting> lambdas;

  RefIntStream(@RefAware IntStream stream) {
    this(stream, new ArrayList<>(), new ConcurrentHashMap<>());
    onClose(() -> {
      this.lambdas.forEach(ReferenceCounting::freeRef);
      RefStream.freeAll(this.refs);
      this.lambdas.clear();
    });
  }

  RefIntStream(@RefAware IntStream stream, @RefAware List<ReferenceCounting> lambdas,
               @RefAware Map<RefStream.IdentityWrapper<ReferenceCounting>, AtomicInteger> refs) {
    this.lambdas = lambdas;
    this.refs = refs;
    if (stream instanceof ReferenceCounting)
      throw new IllegalArgumentException("inner class cannot be ref-aware");
    this.inner = stream;
  }

  @Override
  public boolean isParallel() {
    return inner.isParallel();
  }

  @Nonnull
  public static RefIntStream generate(@Nonnull @RefAware IntSupplier s) {
    return new RefIntStream(IntStream.generate(s));
  }

  @Nonnull
  public static RefIntStream range(int startInclusive, int endExclusive) {
    return new RefIntStream(IntStream.range(startInclusive, endExclusive));
  }

  @Nonnull
  public static IntStream rangeClosed(int startInclusive, int endInclusive) {
    return new RefIntStream(IntStream.rangeClosed(startInclusive, endInclusive));
  }

  @Nonnull
  public static RefIntStream of(int x) {
    return new RefIntStream(IntStream.of(x));
  }

  @Nonnull
  public static RefIntStream of(@Nonnull int... array) {
    return new RefIntStream(IntStream.of(array).onClose(() -> {
      Arrays.stream(array).forEach(RefUtil::freeRef);
    }));
  }

  @Nonnull
  public static RefIntStream concat(@Nonnull @RefAware RefIntStream a, @Nonnull @RefAware RefIntStream b) {
    return new RefIntStream(IntStream.concat(a.inner, b.inner));
  }

  @Nonnull
  public static RefIntStream iterate(final int seed, @Nonnull final @RefAware IntUnaryOperator f) {
    return new RefIntStream(IntStream.iterate(seed, f));
  }

  @Override
  public boolean allMatch(@Nonnull @RefAware IntPredicate predicate) {
    track(predicate);
    final boolean match = inner.allMatch((int t) -> predicate.test(getRef(t)));
    close();
    return match;
  }

  @Override
  public boolean anyMatch(@Nonnull @RefAware IntPredicate predicate) {
    track(predicate);
    final boolean match = inner.anyMatch((int t) -> predicate.test(getRef(t)));
    close();
    return match;
  }

  @Nonnull
  @Override
  public RefDoubleStream asDoubleStream() {
    return new RefDoubleStream(inner.asDoubleStream(), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefLongStream asLongStream() {
    return new RefLongStream(inner.asLongStream(), lambdas, refs);
  }

  @Override
  public OptionalDouble average() {
    final OptionalDouble average = inner.average();
    close();
    return average;
  }

  @Nonnull
  @Override
  public RefStream<Integer> boxed() {
    return new RefStream<>(inner.boxed(), lambdas, refs);
  }

  @Override
  public void close() {
    inner.close();
  }

  @Override
  public <R> R collect(@Nonnull @RefAware Supplier<R> supplier, @Nonnull @RefAware ObjIntConsumer<R> accumulator,
                       @Nonnull @RefAware BiConsumer<R, R> combiner) {
    track(supplier);
    track(accumulator);
    track(combiner);
    final R collect = inner.collect(() -> storeRef(supplier.get()),
        (R t1, int u1) -> accumulator.accept(RefUtil.addRef(t1), u1),
        (R t, R u) -> combiner.accept(getRef(t), getRef(u)));
    close();
    return collect;
  }

  @Override
  public long count() {
    final long count = inner.count();
    close();
    return count;
  }

  @Nonnull
  @Override
  public RefIntStream distinct() {
    return new RefIntStream(inner.distinct(), lambdas, refs);
  }

  @Nonnull
  public RefIntStream filter(@Nonnull @RefAware IntPredicate predicate) {
    track(predicate);
    return new RefIntStream(inner.filter((int t) -> predicate.test(RefUtil.addRef(t))), lambdas, refs);
  }

  @Override
  public OptionalInt findAny() {
    final OptionalInt any = inner.findAny();
    close();
    return any;
  }

  @Override
  public OptionalInt findFirst() {
    final OptionalInt first = inner.findFirst();
    close();
    return first;
  }

  @Nonnull
  @Override
  public RefIntStream flatMap(@Nonnull @RefAware IntFunction<? extends IntStream> mapper) {
    track(mapper);
    return new RefIntStream(inner.flatMap((int t) -> mapper.apply(getRef(t)).map(this::storeRef)), lambdas, refs);
  }

  @Override
  public void forEach(@Nonnull @RefAware IntConsumer action) {
    track(action);
    inner.forEach((int t) -> action.accept(getRef(t)));
    close();
  }

  @Override
  public void forEachOrdered(@Nonnull @RefAware IntConsumer action) {
    track(action);
    inner.forEachOrdered((int t) -> action.accept(getRef(t)));
    close();
  }

  @Nonnull
  @Override
  public RefPrimitiveIterator.OfInt iterator() {
    return new RefPrimitiveIterator.OfInt(inner.iterator()).track(new ReferenceCountingBase() {
      @Override
      protected void _free() {
        RefIntStream.this.close();
      }
    });

  }

  @Nonnull
  @Override
  public RefIntStream limit(long maxSize) {
    return new RefIntStream(inner.limit(maxSize), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefIntStream map(@Nonnull @RefAware IntUnaryOperator mapper) {
    track(mapper);
    return new RefIntStream(inner.map(t -> storeRef(mapper.applyAsInt(t))), lambdas, refs).track(mapper);
  }

  @Nonnull
  @Override
  public RefDoubleStream mapToDouble(@Nonnull @RefAware IntToDoubleFunction mapper) {
    track(mapper);
    return new RefDoubleStream(inner.mapToDouble((int value) -> mapper.applyAsDouble(getRef(value))), lambdas, refs)
        .track(mapper);
  }

  @Nonnull
  @Override
  public RefLongStream mapToLong(@Nonnull @RefAware IntToLongFunction mapper) {
    track(mapper);
    return new RefLongStream(inner.mapToLong((int value) -> mapper.applyAsLong(getRef(value))), lambdas, refs)
        .track(mapper);
  }

  @Nonnull
  @Override
  public <U> RefStream<U> mapToObj(@RefAware IntFunction<? extends U> mapper) {
    return new RefStream<U>(inner.mapToObj(mapper), lambdas, refs).track(mapper);
  }

  @Override
  public OptionalInt max() {
    final OptionalInt max = inner.max();
    close();
    return max;
  }

  @Override
  public OptionalInt min() {
    final OptionalInt min = inner.min();
    close();
    return min;
  }

  @Override
  public boolean noneMatch(@Nonnull @RefAware IntPredicate predicate) {
    track(predicate);
    final boolean match = inner.noneMatch((int t) -> predicate.test(getRef(t)));
    close();
    return match;
  }

  @Nonnull
  @Override
  public RefIntStream onClose(@RefAware Runnable closeHandler) {
    track(closeHandler);
    return new RefIntStream(inner.onClose(closeHandler), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefIntStream parallel() {
    return new RefIntStream(inner.parallel(), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefIntStream peek(@Nonnull @RefAware IntConsumer action) {
    track(action);
    return new RefIntStream(inner.peek((int t) -> action.accept(getRef(t))), lambdas, refs);
  }

  @Override
  public int reduce(int identity, @Nonnull @RefAware IntBinaryOperator accumulator) {
    track(accumulator);
    final int reduce = inner.reduce(storeRef(identity),
        (int t, int u) -> storeRef(accumulator.applyAsInt(getRef(t), getRef(u))));
    close();
    return reduce;
  }

  @Override
  public OptionalInt reduce(@Nonnull @RefAware IntBinaryOperator accumulator) {
    track(accumulator);
    final OptionalInt reduce = inner.reduce((int t, int u) -> storeRef(accumulator.applyAsInt(getRef(t), getRef(u))));
    close();
    return reduce;
  }

  @Nonnull
  @Override
  public RefIntStream sequential() {
    return new RefIntStream(inner.sequential(), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefIntStream skip(long n) {
    return new RefIntStream(inner.skip(n), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefIntStream sorted() {
    return new RefIntStream(inner.sorted(), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefSpliterator.OfInt spliterator() {
    return new RefSpliterator.OfInt(inner.spliterator()).track(new ReferenceCountingBase() {
      @Override
      protected void _free() {
        RefIntStream.this.close();
      }
    });

  }

  @Override
  public int sum() {
    final int sum = inner.sum();
    close();
    return sum;
  }

  @Override
  public IntSummaryStatistics summaryStatistics() {
    final IntSummaryStatistics statistics = inner.summaryStatistics();
    close();
    return statistics;
  }

  @Override
  public int[] toArray() {
    final int[] ints = inner.toArray();
    close();
    return ints;
  }

  @Nonnull
  @Override
  public RefIntStream unordered() {
    return new RefIntStream(inner.unordered(), lambdas, refs);
  }

  @Nonnull
  RefIntStream track(@Nonnull @RefAware Object... lambda) {
    for (Object l : lambda) {
      if (null != l && l instanceof ReferenceCounting)
        lambdas.add((ReferenceCounting) l);
    }
    return this;
  }

  private <U> U getRef(@RefAware U u) {
    return RefStream.getRef(u, this.refs);
  }

  private <U> U storeRef(@RefAware U u) {
    return RefStream.storeRef(u, refs);
  }
}
