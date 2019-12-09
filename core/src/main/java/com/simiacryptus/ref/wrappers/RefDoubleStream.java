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

import com.simiacryptus.ref.lang.*;
import com.simiacryptus.ref.wrappers.RefStream.IdentityWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.*;
import java.util.stream.DoubleStream;

@RefAware
@RefIgnore
public class RefDoubleStream implements DoubleStream {
  private final DoubleStream inner;
  private final List<IdentityWrapper<ReferenceCounting>> refs;
  private final List<ReferenceCounting> lambdas;

  RefDoubleStream(DoubleStream stream) {
    this(stream, new ArrayList<>(), new ArrayList<>());
    onClose(() -> {
      this.lambdas.forEach(ReferenceCounting::freeRef);
      this.refs.forEach(x -> x.inner.freeRef());
      this.lambdas.clear();
    });
  }

  RefDoubleStream(DoubleStream stream, List<ReferenceCounting> lambdas, List<IdentityWrapper<ReferenceCounting>> refs) {
    this.lambdas = lambdas;
    this.refs = refs;
    if (stream instanceof ReferenceCounting) throw new IllegalArgumentException("inner class cannot be ref-aware");
    this.inner = stream;
  }

  public static RefDoubleStream generate(DoubleSupplier s) {
    return new RefDoubleStream(DoubleStream.generate(s));
  }

  public static RefDoubleStream iterate(final double seed, final DoubleUnaryOperator f) {
    return new RefDoubleStream(DoubleStream.iterate(seed, f));
  }

  public static RefDoubleStream of(double x) {
    return new RefDoubleStream(DoubleStream.of(x));
  }

  public static RefDoubleStream of(double... array) {
    return new RefDoubleStream(DoubleStream.of(array));
  }

  @Override
  public boolean allMatch(@NotNull DoublePredicate predicate) {
    track(predicate);
    final boolean match = inner.allMatch(predicate::test);
    close();
    return match;
  }

  @Override
  public boolean anyMatch(@NotNull DoublePredicate predicate) {
    track(predicate);
    final boolean match = inner.anyMatch(predicate::test);
    close();
    return match;
  }

  @Override
  public OptionalDouble average() {
    final OptionalDouble average = inner.average();
    close();
    return average;
  }

  @Override
  public RefStream<Double> boxed() {
    return new RefStream<>(inner.boxed(), lambdas, refs);
  }

  @Override
  public void close() {
    inner.close();
  }

  @Override
  public <R> R collect(@NotNull Supplier<R> supplier, @NotNull ObjDoubleConsumer<R> accumulator, @NotNull BiConsumer<R, R> combiner) {
    track(supplier);
    track(accumulator);
    track(combiner);
    final R collect = inner.collect(
        () -> storeRef(supplier.get()),
        (R t1, double u1) -> accumulator.accept(RefUtil.addRef(t1), u1),
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
  public RefDoubleStream distinct() {
    return new RefDoubleStream(inner.distinct(), lambdas, refs);
  }

  @NotNull
  public RefDoubleStream filter(@NotNull DoublePredicate predicate) {
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

  @NotNull
  @Override
  public RefDoubleStream flatMap(@NotNull DoubleFunction<? extends DoubleStream> mapper) {
    track(mapper);
    return new RefDoubleStream(inner.flatMap((double t) -> mapper.apply(t).map(this::storeRef)), lambdas, refs);
  }

  public void forEach(@NotNull DoubleConsumer action) {
    track(action);
    inner.forEach(action::accept);
    close();
  }

  @Override
  public void forEachOrdered(@NotNull DoubleConsumer action) {
    track(action);
    inner.forEachOrdered(action::accept);
    close();
  }

  private <U> U getRef(U u) {
    if (u instanceof ReferenceCounting) {
      if (!refs.remove(new IdentityWrapper(u))) {
        RefUtil.addRef(u);
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
  public RefPrimitiveIterator.OfDouble iterator() {
    return new RefPrimitiveIterator.OfDouble(inner.iterator()).track(new ReferenceCountingBase() {
      @Override
      protected void _free() {
        RefDoubleStream.this.close();
      }
    });
  }

  @NotNull
  @Override
  public RefDoubleStream limit(long maxSize) {
    return new RefDoubleStream(inner.limit(maxSize), lambdas, refs);
  }

  @NotNull
  @Override
  public RefDoubleStream map(@NotNull DoubleUnaryOperator mapper) {
    track(mapper);
    return new RefDoubleStream(inner.map(t -> storeRef(mapper.applyAsDouble(t))), lambdas, refs);
  }

  @Override
  public RefIntStream mapToInt(@NotNull DoubleToIntFunction mapper) {
    track(mapper);
    return new RefIntStream(inner.mapToInt((double value) -> mapper.applyAsInt(getRef(value))), lambdas, refs);
  }

  @Override
  public RefLongStream mapToLong(@NotNull DoubleToLongFunction mapper) {
    track(mapper);
    return new RefLongStream(inner.mapToLong((double value) -> mapper.applyAsLong(getRef(value))), lambdas, refs);
  }

  @Override
  public <U> RefStream<U> mapToObj(DoubleFunction<? extends U> mapper) {
    return new RefStream<>(inner.mapToObj(value -> mapper.apply(value)), lambdas, refs);
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
  public boolean noneMatch(@NotNull DoublePredicate predicate) {
    track(predicate);
    final boolean match = inner.noneMatch((double t) -> predicate.test(getRef(t)));
    close();
    return match;
  }

  @NotNull
  @Override
  public RefDoubleStream onClose(Runnable closeHandler) {
    track(closeHandler);
    return new RefDoubleStream(inner.onClose(closeHandler), lambdas, refs);
  }

  @NotNull
  @Override
  public RefDoubleStream parallel() {
    return new RefDoubleStream(inner.parallel(), lambdas, refs);
  }

  @NotNull
  @Override
  public RefDoubleStream peek(@NotNull DoubleConsumer action) {
    track(action);
    return new RefDoubleStream(inner.peek((double t) -> action.accept(getRef(t))), lambdas, refs);
  }

  @Override
  public double reduce(double identity, @NotNull DoubleBinaryOperator accumulator) {
    track(accumulator);
    final double reduce = inner.reduce(storeRef(identity), (double t, double u) -> storeRef(accumulator.applyAsDouble(getRef(t), getRef(u))));
    close();
    return reduce;
  }

  @Override
  public OptionalDouble reduce(@NotNull DoubleBinaryOperator accumulator) {
    track(accumulator);
    final OptionalDouble optionalDouble = inner.reduce((double t, double u) -> storeRef(accumulator.applyAsDouble(getRef(t), getRef(u))));
    close();
    return optionalDouble;
  }

  @NotNull
  @Override
  public RefDoubleStream sequential() {
    return new RefDoubleStream(inner.sequential(), lambdas, refs);
  }

  @NotNull
  @Override
  public RefDoubleStream skip(long n) {
    return new RefDoubleStream(inner.skip(n), lambdas, refs);
  }

  @NotNull
  @Override
  public RefDoubleStream sorted() {
    return new RefDoubleStream(inner.sorted(), lambdas, refs);
  }

  @NotNull
  @Override
  public RefSpliterator.OfDouble spliterator() {
    return new RefSpliterator.OfDouble(inner.spliterator()).track(new ReferenceCountingBase() {
      @Override
      protected void _free() {
        RefDoubleStream.this.close();
      }
    });
  }

  private <U> U storeRef(U u) {
    if (u instanceof ReferenceCounting) {
      refs.add(new IdentityWrapper<ReferenceCounting>((ReferenceCounting) u));
    }
    return u;
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
    final double[] array = inner.map(this::getRef).toArray();
    close();
    return array;
  }

  private void track(Object... lambda) {
    for (Object l : lambda) {
      if (null != l && l instanceof ReferenceCounting) lambdas.add((ReferenceCounting) l);
    }
  }

  @NotNull
  @Override
  public RefDoubleStream unordered() {
    return new RefDoubleStream(inner.unordered(), lambdas, refs);
  }

}
