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
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.*;
import java.util.stream.IntStream;

/**
 * The type Ref int stream.
 */
@RefAware
@RefIgnore
public class RefIntStream implements IntStream {
  private final IntStream inner;
  private final List<RefStream.IdentityWrapper<ReferenceCounting>> refs;
  private final List<ReferenceCounting> lambdas;

  /**
   * Instantiates a new Ref int stream.
   *
   * @param stream the stream
   */
  RefIntStream(IntStream stream) {
    this(stream, new ArrayList<>(), new ArrayList<>());
    onClose(() -> {
      this.lambdas.forEach(ReferenceCounting::freeRef);
      this.refs.forEach(x -> x.inner.freeRef());
      this.lambdas.clear();
    });
  }

  /**
   * Instantiates a new Ref int stream.
   *
   * @param stream  the stream
   * @param lambdas the lambdas
   * @param refs    the refs
   */
  RefIntStream(IntStream stream, List<ReferenceCounting> lambdas, List<RefStream.IdentityWrapper<ReferenceCounting>> refs) {
    this.lambdas = lambdas;
    this.refs = refs;
    if (stream instanceof ReferenceCounting) throw new IllegalArgumentException("inner class cannot be ref-aware");
    this.inner = stream;
  }


  /**
   * Generate ref int stream.
   *
   * @param s the s
   * @return the ref int stream
   */
  public static RefIntStream generate(IntSupplier s) {
    return new RefIntStream(IntStream.generate(s));
  }

  /**
   * Range ref int stream.
   *
   * @param startInclusive the start inclusive
   * @param endExclusive   the end exclusive
   * @return the ref int stream
   */
  public static RefIntStream range(int startInclusive, int endExclusive) {
    return new RefIntStream(IntStream.range(startInclusive, endExclusive));
  }

  /**
   * Of ref int stream.
   *
   * @param x the x
   * @return the ref int stream
   */
  public static RefIntStream of(int x) {
    return new RefIntStream(IntStream.of(x));
  }

  /**
   * Of ref int stream.
   *
   * @param array the array
   * @return the ref int stream
   */
  public static RefIntStream of(int... array) {
    return new RefIntStream(IntStream.of(array).onClose(() -> {
      Arrays.stream(array).forEach(RefUtil::freeRef);
    }));
  }

  @Override
  public boolean allMatch(@NotNull IntPredicate predicate) {
    track(predicate);
    final boolean match = inner.allMatch((int t) -> predicate.test(getRef(t)));
    close();
    return match;
  }

  @Override
  public boolean anyMatch(@NotNull IntPredicate predicate) {
    track(predicate);
    final boolean match = inner.anyMatch((int t) -> predicate.test(getRef(t)));
    close();
    return match;
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
    final OptionalDouble average = inner.average();
    close();
    return average;
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
    final R collect = inner.collect(
        () -> storeRef(supplier.get()),
        (R t1, int u1) -> accumulator.accept(RefUtil.addRef(t1), u1),
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
  public RefIntStream distinct() {
    return new RefIntStream(inner.distinct(), lambdas, refs);
  }

  @NotNull
  public RefIntStream filter(@NotNull IntPredicate predicate) {
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
    close();
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
    return new RefPrimitiveIterator.OfInt(inner.iterator()).track(new ReferenceCountingBase() {
      @Override
      protected void _free() {
        RefIntStream.this.close();
      }
    });

  }

  @NotNull
  @Override
  public RefIntStream limit(long maxSize) {
    return new RefIntStream(inner.limit(maxSize), lambdas, refs);
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
  public boolean noneMatch(@NotNull IntPredicate predicate) {
    track(predicate);
    final boolean match = inner.noneMatch((int t) -> predicate.test(getRef(t)));
    close();
    return match;
  }

  @NotNull
  @Override
  public RefIntStream onClose(Runnable closeHandler) {
    track(closeHandler);
    return new RefIntStream(inner.onClose(closeHandler), lambdas, refs);
  }

  @NotNull
  @Override
  public RefIntStream parallel() {
    return new RefIntStream(inner.parallel(), lambdas, refs);
  }

  @NotNull
  @Override
  public RefIntStream peek(@NotNull IntConsumer action) {
    track(action);
    return new RefIntStream(inner.peek((int t) -> action.accept(getRef(t))), lambdas, refs);
  }

  @Override
  public int reduce(int identity, @NotNull IntBinaryOperator accumulator) {
    track(accumulator);
    final int reduce = inner.reduce(storeRef(identity), (int t, int u) -> storeRef(accumulator.applyAsInt(getRef(t), getRef(u))));
    close();
    return reduce;
  }

  @Override
  public OptionalInt reduce(@NotNull IntBinaryOperator accumulator) {
    track(accumulator);
    final OptionalInt reduce = inner.reduce((int t, int u) -> storeRef(accumulator.applyAsInt(getRef(t), getRef(u))));
    close();
    return reduce;
  }

  @NotNull
  @Override
  public RefIntStream sequential() {
    return new RefIntStream(inner.sequential(), lambdas, refs);
  }

  @NotNull
  @Override
  public RefIntStream skip(long n) {
    return new RefIntStream(inner.skip(n), lambdas, refs);
  }

  @NotNull
  @Override
  public RefIntStream sorted() {
    return new RefIntStream(inner.sorted(), lambdas, refs);
  }

  @NotNull
  @Override
  public Spliterator.OfInt spliterator() {
    return new RefSpliterator.OfInt(inner.spliterator()).track(new ReferenceCountingBase() {
      @Override
      protected void _free() {
        RefIntStream.this.close();
      }
    });

  }

  private <U> U storeRef(U u) {
    if (u instanceof ReferenceCounting) {
      refs.add(new RefStream.IdentityWrapper<ReferenceCounting>((ReferenceCounting) u));
    }
    return u;
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

  private void track(Object... lambda) {
    for (Object l : lambda) {
      if (null != l && l instanceof ReferenceCounting) lambdas.add((ReferenceCounting) l);
    }
  }

  @NotNull
  @Override
  public RefIntStream unordered() {
    return new RefIntStream(inner.unordered(), lambdas, refs);
  }

}
