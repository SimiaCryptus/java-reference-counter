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
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCounting;
import com.simiacryptus.ref.wrappers.RefStream.IdentityWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.*;
import java.util.stream.DoubleStream;

@RefAware
@RefCoderIgnore
public class RefDoubleStream implements DoubleStream {
  private DoubleStream inner;
  private List<IdentityWrapper<ReferenceCounting>> refs;

  private List<ReferenceCounting> lambdas;

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
    return inner.allMatch(predicate::test);
  }

  @Override
  public boolean anyMatch(@NotNull DoublePredicate predicate) {
    track(predicate);
    return inner.anyMatch(predicate::test);
  }

  @Override
  public OptionalDouble average() {
    return inner.average();
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
    return inner.collect(
        () -> storeRef(supplier.get()),
        (R t1, double u1) -> accumulator.accept(RefUtil.addRef(t1), u1),
        (R t, R u) -> combiner.accept(getRef(t), getRef(u))
    );
  }

  @Override
  public long count() {
    final long count = inner.count();
    return count;
  }

  @NotNull
  @Override
  public RefDoubleStream distinct() {
    inner = inner.distinct();
    return this;
  }

  @NotNull
  public RefDoubleStream filter(@NotNull DoublePredicate predicate) {
    track(predicate);
    inner = inner.filter((double t) -> predicate.test(RefUtil.addRef(t)));
    return this;
  }

  @Override
  public OptionalDouble findAny() {
    return inner.findAny();
  }

  @Override
  public OptionalDouble findFirst() {
    return inner.findFirst();
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
  public PrimitiveIterator.OfDouble iterator() {
    return inner.iterator();
  }

  @NotNull
  @Override
  public RefDoubleStream limit(long maxSize) {
    inner = inner.limit(maxSize);
    return this;
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
    return inner.max();
  }

  @Override
  public OptionalDouble min() {
    return inner.min();
  }

  @Override
  public boolean noneMatch(@NotNull DoublePredicate predicate) {
    track(predicate);
    return inner.noneMatch((double t) -> predicate.test(getRef(t)));
  }

  @NotNull
  @Override
  public RefDoubleStream onClose(Runnable closeHandler) {
    inner = inner.onClose(closeHandler);
    track(closeHandler);
    return this;
  }

  @NotNull
  @Override
  public RefDoubleStream parallel() {
    inner = inner.parallel();
    return this;
  }

  @NotNull
  @Override
  public RefDoubleStream peek(@NotNull DoubleConsumer action) {
    track(action);
    inner = inner.peek((double t) -> action.accept(getRef(t)));
    return this;
  }

  @Override
  public double reduce(double identity, @NotNull DoubleBinaryOperator accumulator) {
    track(accumulator);
    return inner.reduce(storeRef(identity), (double t, double u) -> storeRef(accumulator.applyAsDouble(getRef(t), getRef(u))));
  }

  @Override
  public OptionalDouble reduce(@NotNull DoubleBinaryOperator accumulator) {
    track(accumulator);
    return inner.reduce((double t, double u) -> storeRef(accumulator.applyAsDouble(getRef(t), getRef(u))));
  }

  @NotNull
  @Override
  public RefDoubleStream sequential() {
    inner = inner.sequential();
    return this;
  }

  @NotNull
  @Override
  public RefDoubleStream skip(long n) {
    inner = inner.skip(n);
    return this;
  }

  @NotNull
  @Override
  public RefDoubleStream sorted() {
    inner = inner.sorted();
    return this;
  }

  @NotNull
  @Override
  public Spliterator.OfDouble spliterator() {
    return inner.spliterator();
  }

  private <U> U storeRef(U u) {
    if (u instanceof ReferenceCounting) {
      refs.add(new IdentityWrapper<ReferenceCounting>((ReferenceCounting) u));
    }
    return u;
  }

  @Override
  public double sum() {
    return inner.sum();
  }

  @Override
  public DoubleSummaryStatistics summaryStatistics() {
    return inner.summaryStatistics();
  }

  @Override
  public double[] toArray() {
    return inner.map(this::getRef).toArray();
  }

  private void track(Object... lambda) {
    for (Object l : lambda) {
      if (null != l && l instanceof ReferenceCounting) lambdas.add((ReferenceCounting) l);
    }
  }

  @NotNull
  @Override
  public RefDoubleStream unordered() {
    inner = inner.unordered();
    return this;
  }

}
