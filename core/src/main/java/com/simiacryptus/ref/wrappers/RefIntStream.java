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
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.*;
import java.util.stream.IntStream;

@RefAware
@RefCoderIgnore
public class RefIntStream implements IntStream {
  private IntStream inner;
  private List<RefStream.IdentityWrapper<ReferenceCounting>> refs;
  private List<ReferenceCounting> lambdas;

  RefIntStream(IntStream stream) {
    this(stream, new ArrayList<>(), new ArrayList<>());
    onClose(() -> {
      this.lambdas.forEach(ReferenceCounting::freeRef);
      this.refs.forEach(x -> x.inner.freeRef());
      this.lambdas.clear();
    });
  }

  RefIntStream(IntStream stream, List<ReferenceCounting> lambdas, List<RefStream.IdentityWrapper<ReferenceCounting>> refs) {
    this.lambdas = lambdas;
    this.refs = refs;
    if (stream instanceof ReferenceCounting) throw new IllegalArgumentException("inner class cannot be ref-aware");
    this.inner = stream;
  }


  public static RefIntStream generate(IntSupplier s) {
    return new RefIntStream(IntStream.generate(s));
  }

  public static RefIntStream range(int startInclusive, int endExclusive) {
    return new RefIntStream(IntStream.range(startInclusive, endExclusive));
  }

  public static RefIntStream of(int x) {
    return new RefIntStream(IntStream.of(x));
  }

  public static RefIntStream of(int... array) {
    return new RefIntStream(IntStream.of(array).onClose(() -> {
      Arrays.stream(array).forEach(RefUtil::freeRef);
    }));
  }

  @Override
  public boolean allMatch(@NotNull IntPredicate predicate) {
    track(predicate);
    return inner.allMatch((int t) -> predicate.test(getRef(t)));
  }

  @Override
  public boolean anyMatch(@NotNull IntPredicate predicate) {
    track(predicate);
    return inner.anyMatch((int t) -> predicate.test(getRef(t)));
  }

  @Override
  public RefDoubleStream asDoubleStream() {
    return new RefDoubleStream(inner.asDoubleStream(), lambdas, refs);
  }

  @Override
  public RefLongStream asLongStream() {
    return new RefLongStream(inner.asLongStream(), lambdas, refs);
  }

  @Override
  public OptionalDouble average() {
    return inner.average();
  }

  @Override
  public RefStream<Integer> boxed() {
    return new RefStream<>(inner.boxed(), lambdas, refs);
  }

  @Override
  public void close() {
    inner.close();
  }

  @Override
  public <R> R collect(@NotNull Supplier<R> supplier, @NotNull ObjIntConsumer<R> accumulator, @NotNull BiConsumer<R, R> combiner) {
    track(supplier);
    track(accumulator);
    track(combiner);
    return inner.collect(
        () -> storeRef(supplier.get()),
        (R t1, int u1) -> accumulator.accept(RefUtil.addRef(t1), u1),
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
  public RefIntStream distinct() {
    inner = inner.distinct();
    return this;
  }

  @NotNull
  public RefIntStream filter(@NotNull IntPredicate predicate) {
    inner = inner.filter((int t) -> predicate.test(RefUtil.addRef(t)));
    track(predicate);
    return this;
  }

  @Override
  public OptionalInt findAny() {
    return inner.findAny();
  }

  @Override
  public OptionalInt findFirst() {
    return inner.findFirst();
  }

  @NotNull
  @Override
  public RefIntStream flatMap(@NotNull IntFunction<? extends IntStream> mapper) {
    track(mapper);
    return new RefIntStream(inner.flatMap((int t) -> mapper.apply(getRef(t))
        .map(this::storeRef)
    ), lambdas, refs);
  }

  @Override
  public void forEach(@NotNull IntConsumer action) {
    track(action);
    inner.forEach((int t) -> action.accept(getRef(t)));
    close();
  }

  @Override
  public void forEachOrdered(@NotNull IntConsumer action) {
    track(action);
    inner.forEachOrdered((int t) -> action.accept(getRef(t)));
  }

  private <U> U getRef(U u) {
    if (u instanceof ReferenceCounting) {
      if (!refs.remove(new RefStream.IdentityWrapper(u))) {
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
  public PrimitiveIterator.OfInt iterator() {
    return inner.iterator();
  }

  @NotNull
  @Override
  public RefIntStream limit(long maxSize) {
    inner = inner.limit(maxSize);
    return this;
  }

  @NotNull
  @Override
  public RefIntStream map(@NotNull IntUnaryOperator mapper) {
    track(mapper);
    return new RefIntStream(inner.map(t -> storeRef(mapper.applyAsInt(t))), lambdas, refs);
  }

  @Override
  public RefDoubleStream mapToDouble(@NotNull IntToDoubleFunction mapper) {
    track(mapper);
    return new RefDoubleStream(inner.mapToDouble((int value) -> mapper.applyAsDouble(getRef(value))), lambdas, refs);
  }

  @Override
  public RefLongStream mapToLong(@NotNull IntToLongFunction mapper) {
    track(mapper);
    return new RefLongStream(inner.mapToLong((int value) -> mapper.applyAsLong(getRef(value))), lambdas, refs);
  }

  @Override
  public <U> RefStream<U> mapToObj(IntFunction<? extends U> mapper) {
    return new RefStream<>(inner.mapToObj(mapper), lambdas, refs);
  }

  @Override
  public OptionalInt max() {
    return inner.max();
  }

  @Override
  public OptionalInt min() {
    return inner.min();
  }

  @Override
  public boolean noneMatch(@NotNull IntPredicate predicate) {
    track(predicate);
    return inner.noneMatch((int t) -> predicate.test(getRef(t)));
  }

  @NotNull
  @Override
  public RefIntStream onClose(Runnable closeHandler) {
    inner = inner.onClose(closeHandler);
    track(closeHandler);
    return this;
  }

  @NotNull
  @Override
  public RefIntStream parallel() {
    inner = inner.parallel();
    return this;
  }

  @NotNull
  @Override
  public RefIntStream peek(@NotNull IntConsumer action) {
    track(action);
    inner = inner.peek((int t) -> action.accept(getRef(t)));
    return this;
  }

  @Override
  public int reduce(int identity, @NotNull IntBinaryOperator accumulator) {
    track(accumulator);
    return inner.reduce(storeRef(identity), (int t, int u) -> storeRef(accumulator.applyAsInt(getRef(t), getRef(u))));
  }

  @Override
  public OptionalInt reduce(@NotNull IntBinaryOperator accumulator) {
    track(accumulator);
    return inner.reduce((int t, int u) -> storeRef(accumulator.applyAsInt(getRef(t), getRef(u))));
  }

  @NotNull
  @Override
  public RefIntStream sequential() {
    inner = inner.sequential();
    return this;
  }

  @NotNull
  @Override
  public RefIntStream skip(long n) {
    inner = inner.skip(n);
    return this;
  }

  @NotNull
  @Override
  public RefIntStream sorted() {
    inner = inner.sorted();
    return this;
  }

  @NotNull
  @Override
  public Spliterator.OfInt spliterator() {
    return inner.spliterator();
  }

  private <U> U storeRef(U u) {
    if (u instanceof ReferenceCounting) {
      refs.add(new RefStream.IdentityWrapper<ReferenceCounting>((ReferenceCounting) u));
    }
    return u;
  }

  @Override
  public int sum() {
    return 0;
  }

  @Override
  public IntSummaryStatistics summaryStatistics() {
    return inner.summaryStatistics();
  }

  @Override
  public int[] toArray() {
    return inner.toArray();
  }

  private void track(Object... lambda) {
    for (Object l : lambda) {
      if (null != l && l instanceof ReferenceCounting) lambdas.add((ReferenceCounting) l);
    }
  }

  @NotNull
  @Override
  public RefIntStream unordered() {
    inner = inner.unordered();
    return this;
  }

}
