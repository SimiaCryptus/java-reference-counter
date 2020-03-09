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
import com.simiacryptus.ref.wrappers.RefStream.IdentityWrapper;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.DoubleStream;

@RefIgnore
@SuppressWarnings("unused")
public class RefDoubleStream implements DoubleStream {
  private final DoubleStream inner;
  private final Map<IdentityWrapper<ReferenceCounting>, AtomicInteger> refs;
  private final List<ReferenceCounting> lambdas;

  RefDoubleStream(@RefAware DoubleStream stream) {
    this(stream, new ArrayList<>(), new ConcurrentHashMap<>());
  }

  RefDoubleStream(@RefAware DoubleStream stream, @RefAware List<ReferenceCounting> lambdas,
                  @RefAware Map<IdentityWrapper<ReferenceCounting>, AtomicInteger> refs) {
    this.lambdas = lambdas;
    this.refs = refs;
    if (stream instanceof ReferenceCounting)
      throw new IllegalArgumentException("inner class cannot be ref-aware");
    this.inner = stream.onClose(() -> {
      RefStream.freeAll(refs);
      synchronized (lambdas) {
        lambdas.forEach(referenceCounting -> referenceCounting.freeRef());
        lambdas.clear();
      }
    });
  }

  @Override
  public boolean isParallel() {
    return inner.isParallel();
  }

  @Nonnull
  public static RefDoubleStream generate(@Nonnull @RefAware DoubleSupplier s) {
    return new RefDoubleStream(DoubleStream.generate(s));
  }

  @Nonnull
  public static RefDoubleStream iterate(final double seed, @Nonnull final @RefAware DoubleUnaryOperator f) {
    return new RefDoubleStream(DoubleStream.iterate(seed, f));
  }

  @Nonnull
  public static RefDoubleStream of(double x) {
    return new RefDoubleStream(DoubleStream.of(x));
  }

  @Nonnull
  public static RefDoubleStream of(double... array) {
    return new RefDoubleStream(DoubleStream.of(array));
  }

  @Nonnull
  public static RefDoubleStream concat(@Nonnull @RefAware RefDoubleStream a, @Nonnull @RefAware RefDoubleStream b) {
    return new RefDoubleStream(DoubleStream.concat(a.inner, b.inner));
  }

  @Override
  public boolean allMatch(@Nonnull @RefAware DoublePredicate predicate) {
    track(predicate);
    final boolean match = inner.allMatch(value -> predicate.test(value));
    close();
    return match;
  }

  @Override
  public boolean anyMatch(@Nonnull @RefAware DoublePredicate predicate) {
    track(predicate);
    final boolean match = inner.anyMatch(value -> predicate.test(value));
    close();
    return match;
  }

  @Override
  public OptionalDouble average() {
    final OptionalDouble average = inner.average();
    close();
    return average;
  }

  @Nonnull
  @Override
  public RefStream<Double> boxed() {
    return new RefStream<>(inner.boxed(), lambdas, refs);
  }

  @Override
  public void close() {
    inner.close();
  }

  @Override
  public <R> R collect(@Nonnull @RefAware Supplier<R> supplier, @Nonnull @RefAware ObjDoubleConsumer<R> accumulator,
                       @Nonnull @RefAware BiConsumer<R, R> combiner) {
    track(supplier);
    track(accumulator);
    track(combiner);
    final R collect = inner.collect(() -> storeRef(supplier.get()),
        (R t1, double u1) -> accumulator.accept(RefUtil.addRef(t1), u1),
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
  public RefDoubleStream distinct() {
    return new RefDoubleStream(inner.distinct(), lambdas, refs);
  }

  @Nonnull
  public RefDoubleStream filter(@Nonnull @RefAware DoublePredicate predicate) {
    track(predicate);
    return new RefDoubleStream(inner.filter((double t) -> predicate.test(RefUtil.addRef(t))), lambdas, refs);
  }

  @Override
  public OptionalDouble findAny() {
    final OptionalDouble any = inner.findAny();
    close();
    return any;
  }

  @Override
  public OptionalDouble findFirst() {
    final OptionalDouble first = inner.findFirst();
    close();
    return first;
  }

  @Nonnull
  @Override
  public RefDoubleStream flatMap(@Nonnull @RefAware DoubleFunction<? extends DoubleStream> mapper) {
    track(mapper);
    return new RefDoubleStream(inner.flatMap((double t) -> mapper.apply(t).map(u -> storeRef(u))), lambdas, refs);
  }

  public void forEach(@Nonnull @RefAware DoubleConsumer action) {
    track(action);
    inner.forEach(value -> action.accept(value));
    close();
  }

  @Override
  public void forEachOrdered(@Nonnull @RefAware DoubleConsumer action) {
    track(action);
    inner.forEachOrdered(value -> action.accept(value));
    close();
  }

  @Nonnull
  @Override
  public RefPrimitiveIterator.OfDouble iterator() {
    return new RefPrimitiveIterator.OfDouble(inner.iterator()).track(new ReferenceCountingBase() {
      @Override
      protected void _free() {
        RefDoubleStream.this.close();
      }
    });
  }

  @Nonnull
  @Override
  public RefDoubleStream limit(long maxSize) {
    return new RefDoubleStream(inner.limit(maxSize), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefDoubleStream map(@Nonnull @RefAware DoubleUnaryOperator mapper) {
    track(mapper);
    return new RefDoubleStream(inner.map(t -> mapper.applyAsDouble(t)), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefIntStream mapToInt(@Nonnull @RefAware DoubleToIntFunction mapper) {
    track(mapper);
    return new RefIntStream(inner.mapToInt((double value) -> mapper.applyAsInt(getRef(value))), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefLongStream mapToLong(@Nonnull @RefAware DoubleToLongFunction mapper) {
    track(mapper);
    return new RefLongStream(inner.mapToLong((double value) -> mapper.applyAsLong(getRef(value))), lambdas, refs);
  }

  @Nonnull
  @Override
  public <U> RefStream<U> mapToObj(@Nonnull @RefAware DoubleFunction<? extends U> mapper) {
    return new RefStream<U>(inner.mapToObj(mapper).map(u -> storeRef(u)), lambdas, refs).track(mapper);
  }

  @Override
  public OptionalDouble max() {
    final OptionalDouble max = inner.max();
    close();
    return max;
  }

  @Override
  public OptionalDouble min() {
    final OptionalDouble min = inner.min();
    close();
    return min;
  }

  @Override
  public boolean noneMatch(@Nonnull @RefAware DoublePredicate predicate) {
    track(predicate);
    final boolean match = inner.noneMatch((double t) -> predicate.test(getRef(t)));
    close();
    return match;
  }

  @Nonnull
  @Override
  public RefDoubleStream onClose(@RefAware Runnable closeHandler) {
    track(closeHandler);
    return new RefDoubleStream(inner.onClose(closeHandler), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefDoubleStream parallel() {
    return new RefDoubleStream(inner.parallel(), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefDoubleStream peek(@Nonnull @RefAware DoubleConsumer action) {
    track(action);
    return new RefDoubleStream(inner.peek((double t) -> action.accept(getRef(t))), lambdas, refs);
  }

  @Override
  public double reduce(double identity, @Nonnull @RefAware DoubleBinaryOperator accumulator) {
    track(accumulator);
    final double reduce = inner.reduce(storeRef(identity),
        (double t, double u) -> storeRef(accumulator.applyAsDouble(getRef(t), getRef(u))));
    close();
    return reduce;
  }

  @Override
  public OptionalDouble reduce(@Nonnull @RefAware DoubleBinaryOperator accumulator) {
    track(accumulator);
    final OptionalDouble optionalDouble = inner
        .reduce((double t, double u) -> accumulator.applyAsDouble(t, u));
    close();
    return optionalDouble;
  }

  @Nonnull
  @Override
  public RefDoubleStream sequential() {
    return new RefDoubleStream(inner.sequential(), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefDoubleStream skip(long n) {
    return new RefDoubleStream(inner.skip(n), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefDoubleStream sorted() {
    return new RefDoubleStream(inner.sorted(), lambdas, refs);
  }

  @Nonnull
  @Override
  public RefSpliterator.OfDouble spliterator() {
    return new RefSpliterator.OfDouble(inner.spliterator()).track(new ReferenceCountingBase() {
      @Override
      protected void _free() {
        RefDoubleStream.this.close();
      }
    });
  }

  @Override
  public double sum() {
    final double sum = inner.sum();
    close();
    return sum;
  }

  @Override
  public DoubleSummaryStatistics summaryStatistics() {
    final DoubleSummaryStatistics statistics = inner.summaryStatistics();
    close();
    return statistics;
  }

  @Override
  public double[] toArray() {
    final double[] array = inner.toArray();
    close();
    return array;
  }

  @Nonnull
  @Override
  public RefDoubleStream unordered() {
    return new RefDoubleStream(inner.unordered(), lambdas, refs);
  }

  @Nonnull
  public RefDoubleStream track(@Nonnull @RefAware Object... lambda) {
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
