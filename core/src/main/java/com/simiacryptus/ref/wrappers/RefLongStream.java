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
import java.util.stream.LongStream;

@RefAware
@RefCoderIgnore
public class RefLongStream implements LongStream {
  private LongStream inner;
  private List<RefStream.IdentityWrapper<ReferenceCounting>> refs;
  private List<ReferenceCounting> lambdas;

  RefLongStream(LongStream stream) {
    this(stream, new ArrayList<>(), new ArrayList<>());
    onClose(() -> {
      this.lambdas.forEach(ReferenceCounting::freeRef);
      this.refs.forEach(x -> x.inner.freeRef());
      this.lambdas.clear();
    });
  }

  RefLongStream(LongStream stream, List<ReferenceCounting> lambdas, List<RefStream.IdentityWrapper<ReferenceCounting>> refs) {
    this.lambdas = lambdas;
    this.refs = refs;
    if (stream instanceof ReferenceCounting) throw new IllegalArgumentException("inner class cannot be ref-aware");
    this.inner = stream;
  }

  public static RefLongStream of(long x) {
    return new RefLongStream(LongStream.of(x));
  }

  public static RefLongStream of(long... array) {
    return new RefLongStream(LongStream.of(array).onClose(() -> {
      Arrays.stream(array).forEach(RefUtil::freeRef);
    }));
  }

  @Override
  public boolean allMatch(@NotNull LongPredicate predicate) {
    track(predicate);
    return inner.allMatch((long t) -> predicate.test(getRef(t)));
  }

  @Override
  public boolean anyMatch(@NotNull LongPredicate predicate) {
    track(predicate);
    return inner.anyMatch((long t) -> predicate.test(getRef(t)));
  }

  @Override
  public RefDoubleStream asDoubleStream() {
    return new RefDoubleStream(inner.asDoubleStream(), lambdas, refs);
  }

  @Override
  public OptionalDouble average() {
    return inner.average();
  }

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
    return inner.collect(
        () -> storeRef(supplier.get()),
        (R t1, long u1) -> accumulator.accept(RefUtil.addRef(t1), u1),
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
  public RefLongStream distinct() {
    inner = inner.distinct();
    return this;
  }

  @NotNull
  public RefLongStream filter(@NotNull LongPredicate predicate) {
    inner = inner.filter((long t) -> predicate.test(RefUtil.addRef(t)));
    track(predicate);
    return this;
  }

  @Override
  public OptionalLong findAny() {
    return inner.findAny();
  }

  @Override
  public OptionalLong findFirst() {
    return inner.findFirst();
  }

  @NotNull
  @Override
  public RefLongStream flatMap(@NotNull LongFunction<? extends LongStream> mapper) {
    track(mapper);
    return new RefLongStream(inner.flatMap((long t) -> mapper.apply(getRef(t))
        .map(this::storeRef)
    ), lambdas, refs);
  }

  public void forEach(@NotNull Consumer action) {
    track(action);
    inner.forEach((long t) -> action.accept(getRef(t)));
    close();
  }

  @Override
  public void forEach(LongConsumer action) {

  }

  @Override
  public void forEachOrdered(@NotNull LongConsumer action) {
    track(action);
    inner.forEachOrdered((long t) -> action.accept(getRef(t)));
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
  public PrimitiveIterator.OfLong iterator() {
    return inner.iterator();
  }

  @NotNull
  @Override
  public RefLongStream limit(long maxSize) {
    inner = inner.limit(maxSize);
    return this;
  }

  @NotNull
  @Override
  public RefLongStream map(@NotNull LongUnaryOperator mapper) {
    track(mapper);
    return new RefLongStream(inner.map(t -> storeRef(mapper.applyAsLong(t))), lambdas, refs);
  }

  @Override
  public RefDoubleStream mapToDouble(@NotNull LongToDoubleFunction mapper) {
    track(mapper);
    return new RefDoubleStream(inner.mapToDouble((long value) -> mapper.applyAsDouble(getRef(value))), lambdas, refs);
  }

  @Override
  public RefIntStream mapToInt(@NotNull LongToIntFunction mapper) {
    track(mapper);
    return new RefIntStream(inner.mapToInt((long value) -> mapper.applyAsInt(getRef(value))), lambdas, refs);
  }

  @Override
  public <U> RefStream<U> mapToObj(LongFunction<? extends U> mapper) {
    return new RefStream<>(inner.mapToObj(mapper), lambdas, refs);
  }

  @Override
  public OptionalLong max() {
    return inner.max();
  }

  @Override
  public OptionalLong min() {
    return inner.min();
  }

  @Override
  public boolean noneMatch(@NotNull LongPredicate predicate) {
    track(predicate);
    return inner.noneMatch((long t) -> predicate.test(getRef(t)));
  }

  @NotNull
  @Override
  public RefLongStream onClose(Runnable closeHandler) {
    inner = inner.onClose(closeHandler);
    track(closeHandler);
    return this;
  }

  @NotNull
  @Override
  public RefLongStream parallel() {
    inner = inner.parallel();
    return this;
  }

  @NotNull
  @Override
  public RefLongStream peek(@NotNull LongConsumer action) {
    track(action);
    inner = inner.peek((long t) -> action.accept(getRef(t)));
    return this;
  }

  @Override
  public long reduce(long identity, @NotNull LongBinaryOperator accumulator) {
    track(accumulator);
    return inner.reduce(storeRef(identity), (long t, long u) -> storeRef(accumulator.applyAsLong(getRef(t), getRef(u))));
  }

  @Override
  public OptionalLong reduce(@NotNull LongBinaryOperator accumulator) {
    track(accumulator);
    return inner.reduce((long t, long u) -> storeRef(accumulator.applyAsLong(getRef(t), getRef(u))));
  }

  @NotNull
  @Override
  public RefLongStream sequential() {
    inner = inner.sequential();
    return this;
  }

  @NotNull
  @Override
  public RefLongStream skip(long n) {
    inner = inner.skip(n);
    return this;
  }

  @NotNull
  @Override
  public RefLongStream sorted() {
    inner = inner.sorted();
    return this;
  }

  @NotNull
  @Override
  public Spliterator.OfLong spliterator() {
    return inner.spliterator();
  }

  private <U> U storeRef(U u) {
    if (u instanceof ReferenceCounting) {
      refs.add(new RefStream.IdentityWrapper<ReferenceCounting>((ReferenceCounting) u));
    }
    return u;
  }

  @Override
  public long sum() {
    return 0;
  }

  @Override
  public LongSummaryStatistics summaryStatistics() {
    return inner.summaryStatistics();
  }

  @Override
  public long[] toArray() {
    return inner.map(this::getRef).toArray();
  }

  private void track(Object... lambda) {
    for (Object l : lambda) {
      if (null != l && l instanceof ReferenceCounting) lambdas.add((ReferenceCounting) l);
    }
  }

  @NotNull
  @Override
  public RefLongStream unordered() {
    inner = inner.unordered();
    return this;
  }

}
