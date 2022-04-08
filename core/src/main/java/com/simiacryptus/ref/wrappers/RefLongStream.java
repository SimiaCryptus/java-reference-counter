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
import java.util.stream.LongStream;

/**
 * This class is a wrapper for a LongStream that keeps track of references to
 * it. This is necessary for cases where a lambda is used as a stream source,
 * since in that case the stream cannot be properly closed without also closing
 * the lambda.
 *
 * @author
 * @docgenVersion 9
 * @since
 */
@RefIgnore
@SuppressWarnings("unused")
public class RefLongStream implements LongStream {
  private final LongStream inner;
  private final Map<RefStream.IdentityWrapper<ReferenceCounting>, AtomicInteger> refs;
  private final List<ReferenceCounting> lambdas;

  RefLongStream(@RefAware LongStream stream) {
    this(stream, new ArrayList<>(), new ConcurrentHashMap<>());
  }

  RefLongStream(@RefAware LongStream stream, @RefAware List<ReferenceCounting> lambdas,
                @RefAware Map<RefStream.IdentityWrapper<ReferenceCounting>, AtomicInteger> refs) {
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

  /**
   * Returns a boolean value indicating whether or not the inner stream is parallel.
   *
   * @return a boolean value indicating whether or not the inner stream is parallel
   * @docgenVersion 9
   */
  @Override
  public boolean isParallel() {
    return inner.isParallel();
  }

  /**
   * Returns a new {@code RefLongStream} that contains the elements from {@code startInclusive} to {@code endExclusive - 1},
   * converted to {@code RefLongStream}.
   *
   * @param startInclusive the (inclusive) initial element of the new stream
   * @param endExclusive   the exclusive upper bound of the new stream
   * @return the new stream
   * @throws IllegalArgumentException if {@code startInclusive} is greater than {@code endExclusive}
   * @docgenVersion 9
   */
  @Nonnull
  public static RefLongStream range(long startInclusive, final long endExclusive) {
    return new RefLongStream(LongStream.range(startInclusive, endExclusive));
  }

  /**
   * Returns a {@code RefLongStream} consisting of a single element.
   *
   * @param x the single element
   * @return a {@code RefLongStream} consisting of a single element
   * @docgenVersion 9
   */
  @Nonnull
  public static RefLongStream of(long x) {
    return new RefLongStream(LongStream.of(x));
  }

  /**
   * @param array long[]
   * @return RefLongStream
   * @docgenVersion 9
   */
  @Nonnull
  public static RefLongStream of(@Nonnull long... array) {
    return new RefLongStream(LongStream.of(array).onClose(() -> {
      Arrays.stream(array).forEach(value -> RefUtil.freeRef(value));
    }));
  }

  /**
   * @param s a LongSupplier
   * @return a new RefLongStream
   * @docgenVersion 9
   */
  @Nonnull
  public static RefLongStream generate(@Nonnull @RefAware LongSupplier s) {
    return new RefLongStream(LongStream.generate(s));
  }

  /**
   * Returns a new sequential {@code RefLongStream} that is composed of the elements of this stream followed by the
   * elements of the given stream.
   *
   * <p>This is an <a href="package-summary.html#StreamOps">intermediate operation</a>.
   *
   * @param a the first stream
   * @param b the second stream
   * @return the concatenation of the two input streams
   * @throws NullPointerException if {@code a} or {@code b} is {@code null}
   * @docgenVersion 9
   */
  @Nonnull
  public static RefLongStream concat(@Nonnull @RefAware RefLongStream a, @Nonnull @RefAware RefLongStream b) {
    return new RefLongStream(LongStream.concat(a.inner, b.inner));
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation tracks the given predicate for {@linkplain
   * #close() closing}.</p>
   *
   * @docgenVersion 9
   */
  @Override
  public boolean allMatch(@Nonnull @RefAware LongPredicate predicate) {
    track(predicate);
    final boolean allMatch = inner.allMatch((long t) -> predicate.test(getRef(t)));
    close();
    return allMatch;
  }

  /**
   * {@inheritDoc}
   *
   * <p>If a match is found, the given predicate is invoked with the element as
   * its argument.
   *
   * @param predicate a non-null {@link LongPredicate} to be invoked on each element
   * @return {@code true} if a match is found, {@code false} otherwise
   * @throws NullPointerException if the given predicate is {@code null}
   * @docgenVersion 9
   */
  @Override
  public boolean anyMatch(@Nonnull @RefAware LongPredicate predicate) {
    track(predicate);
    final boolean anyMatch = inner.anyMatch((long t) -> predicate.test(getRef(t)));
    close();
    return anyMatch;
  }

  /**
   * {@inheritDoc}
   *
   * @return a {@code RefDoubleStream}
   * @throws NullPointerException if the underlying stream is null
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefDoubleStream asDoubleStream() {
    return new RefDoubleStream(inner.asDoubleStream(), lambdas, refs);
  }

  /**
   * @return an {@code OptionalDouble} representing the arithmetic mean of the elements,
   * or an empty {@code OptionalDouble} if the stream is empty
   * @docgenVersion 9
   */
  @Override
  public OptionalDouble average() {
    final OptionalDouble average = inner.average();
    close();
    return average;
  }

  /**
   * Returns a {@code Stream} consisting of the elements of this stream,
   * each boxed to a {@code Long}.
   *
   * <p>This is an {@linkplain java.util.stream.Stream#flatMap flatMap}
   * operation.
   *
   * @return a {@code Stream} consisting of the elements of this stream,
   * each boxed to a {@code Long}
   * @docgenVersion 9
   * @see java.util.stream.Stream#flatMap(java.util.function.Function)
   * @see java.util.stream.LongStream#boxed()
   */
  @Nonnull
  @Override
  public RefStream<Long> boxed() {
    return new RefStream<>(inner.boxed(), lambdas, refs);
  }

  /**
   * Closes this stream and releases any system resources associated
   * with it. If the stream is already closed then invoking this
   * method has no effect.
   *
   * @throws IOException if an I/O error occurs
   * @docgenVersion 9
   */
  @Override
  public void close() {
    inner.close();
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation performs a {@code collect} operation in parallel.
   *
   * @param <R>         the type of the result
   * @param supplier    the supplier function for the result container
   * @param accumulator the accumulator function for combining input elements
   * @param combiner    the combiner function for combining results from different threads
   * @return the result of the {@code collect} operation
   * @throws NullPointerException if any of the arguments is {@code null}
   * @docgenVersion 9
   */
  @Override
  public <R> R collect(@Nonnull @RefAware Supplier<R> supplier, @Nonnull @RefAware ObjLongConsumer<R> accumulator,
                       @Nonnull @RefAware BiConsumer<R, R> combiner) {
    track(supplier);
    track(accumulator);
    track(combiner);
    final R collect = inner.collect(() -> storeRef(supplier.get()),
        (R t1, long u1) -> accumulator.accept(RefUtil.addRef(t1), u1),
        (R t, R u) -> combiner.accept(getRef(t), getRef(u)));
    close();
    return collect;
  }

  /**
   * @Override public long count() {
   * final long count = inner.count();
   * close();
   * return count;
   * }
   * @docgenVersion 9
   */
  @Override
  public long count() {
    final long count = inner.count();
    close();
    return count;
  }

  /**
   * {@inheritDoc}
   *
   * @return a {@code RefLongStream} consisting of the distinct elements of this stream
   * @throws NullPointerException if the element selected is null
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefLongStream distinct() {
    return new RefLongStream(inner.distinct(), lambdas, refs);
  }

  /**
   * @param predicate The predicate to use for filtering
   * @return A new RefLongStream with only the elements that match the predicate
   * @docgenVersion 9
   */
  @Nonnull
  public RefLongStream filter(@Nonnull @RefAware LongPredicate predicate) {
    track(predicate);
    return new RefLongStream(inner.filter((long t) -> predicate.test(RefUtil.addRef(t))), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>If the stream is empty, the returned {@code OptionalLong} will be empty.
   *
   * @return an {@code OptionalLong} describing the first element of this stream,
   * or an empty {@code OptionalLong} if the stream is empty
   * @throws NullPointerException if the element selected is null
   * @docgenVersion 9
   */
  @Override
  public OptionalLong findAny() {
    final OptionalLong any = inner.findAny();
    close();
    return any;
  }

  /**
   * {@inheritDoc}
   *
   * <p>If a value is present in this {@code OptionalLong}, returns an {@code OptionalLong}
   * describing the value, otherwise returns an empty {@code OptionalLong}.
   *
   * @return an {@code OptionalLong} with a present value if a value was found, otherwise an empty {@code OptionalLong}
   * @throws NullPointerException if {@code inner} is null
   * @docgenVersion 9
   */
  @Override
  public OptionalLong findFirst() {
    final OptionalLong first = inner.findFirst();
    close();
    return first;
  }

  /**
   * Returns a {@code RefLongStream} consisting of the results of replacing each element of
   * this stream with the contents of a mapped stream produced by applying
   * the provided mapping function to each element.  Each mapped stream is
   * {@linkplain java.util.stream.BaseStream#close() closed} after its contents
   * have been placed into this stream.  (If a mapped stream is {@code null}
   * an empty stream is used, instead.)
   *
   * <p>The {@code flatMap} operation has the effect of applying a one-to-many
   * transformation to the elements of the stream, and then flattening the
   * resulting elements into a new stream.
   *
   * <p><b>Implementation Requirements:</b><br>
   * The default implementation is equivalent to the following steps for this
   * {@code RefLongStream}, then returning the resulting stream:
   * <pre>{@code
   *     return RefStreams.mapToObj(this, mapper)
   *                      .flatMap(Function.identity());
   * }</pre>
   *
   * @param <R>    The element type of the new stream
   * @param mapper a mapping function to apply to each element, producing a stream
   *               of new values
   * @return the new stream
   * @throws NullPointerException if the mapper is null
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefLongStream flatMap(@Nonnull @RefAware LongFunction<? extends LongStream> mapper) {
    track(mapper);
    return new RefLongStream(inner.flatMap((long t) -> mapper.apply(getRef(t)).map(u -> storeRef(u))), lambdas, refs);
  }

  /**
   * Performs the given action for each element of the Iterable until all elements
   * have been processed or the action throws an exception.
   * Exceptions thrown by the action are relayed to the caller.
   *
   * @param action The action to be performed for each element
   * @throws NullPointerException if the specified action is null
   * @docgenVersion 9
   */
  @Override
  public void forEach(@Nonnull @RefAware LongConsumer action) {
    track(action);
    inner.forEach((long t) -> action.accept(getRef(t)));
    close();
  }

  /**
   * Performs the given action for each element of this stream, in the encounter order of the stream if the stream has a defined encounter order.
   *
   * @param action The action to perform for each element
   * @throws NullPointerException if the given action is null
   * @docgenVersion 9
   */
  @Override
  public void forEachOrdered(@Nonnull @RefAware LongConsumer action) {
    track(action);
    inner.forEachOrdered((long t) -> action.accept(getRef(t)));
    close();
  }

  /**
   * Returns an iterator over the elements in this stream.
   * The iterator does not support removal.
   *
   * <p>This method can be called only after the terminal operation has been performed,
   * such as {@link #forEach(LongConsumer)}, {@link #toArray()}, or {@link #reduce(long, LongBinaryOperator)}.
   *
   * @return an iterator over the elements in this stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefPrimitiveIterator.OfLong iterator() {
    return new RefPrimitiveIterator.OfLong(inner.iterator()).track(new ReferenceCountingBase() {
      /**
       * Frees this RefLongStream. This will close the stream.
       *
       *   @docgenVersion 9
       */
      @Override
      protected void _free() {
        RefLongStream.this.close();
      }
    });
  }

  /**
   * {@inheritDoc}
   *
   * @return a new {@code RefLongStream} with the given size limit
   * @throws IllegalArgumentException if {@code maxSize} is negative
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefLongStream limit(long maxSize) {
    return new RefLongStream(inner.limit(maxSize), lambdas, refs);
  }

  /**
   * Returns a new {@code RefLongStream} consisting of the results of applying the given
   * {@link LongUnaryOperator} to the elements of this stream.
   *
   * <p>This is an <a href="package-summary.html#StreamOps">intermediate
   * operation</a>.
   *
   * @param mapper a non-interfering, stateless
   *               {@code LongUnaryOperator} to apply to each element
   * @return the new stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefLongStream map(@Nonnull @RefAware LongUnaryOperator mapper) {
    track(mapper);
    return new RefLongStream(inner.map(t -> storeRef(mapper.applyAsLong(t))), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation tracks the given {@code mapper} function in a {@link Ref}
   * object and passes it to the {@link #inner} object's {@code mapToDouble}
   * method. The resulting {@link RefDoubleStream} object is then returned.
   *
   * @param mapper the function to apply to each element
   * @return the new stream
   * @throws NullPointerException if the given {@code mapper} is {@code null}
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefDoubleStream mapToDouble(@Nonnull @RefAware LongToDoubleFunction mapper) {
    track(mapper);
    return new RefDoubleStream(inner.mapToDouble((long value) -> mapper.applyAsDouble(getRef(value))), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>If the mapper is a {@link RefAware}, it will be tracked by this {@link RefStream}.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefIntStream mapToInt(@Nonnull @RefAware LongToIntFunction mapper) {
    track(mapper);
    return new RefIntStream(inner.mapToInt((long value) -> mapper.applyAsInt(getRef(value))), lambdas, refs);
  }

  /**
   * Returns a {@code RefStream} consisting of the results of applying the given function to the
   * elements of this stream.
   *
   * <p>This is an intermediate operation.
   *
   * <p>The returned stream is a {@code RefStream}, so it is able to store references to the
   * elements of the stream.
   *
   * @param mapper a non-interfering, stateless function to apply to each element
   * @param <U>    the element type of the new stream
   * @return the new stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public <U> RefStream<U> mapToObj(@RefAware LongFunction<? extends U> mapper) {
    return new RefStream<U>(inner.mapToObj(mapper).map(u -> storeRef(u)), lambdas, refs).track(mapper);
  }

  /**
   * {@inheritDoc}
   *
   * @return an {@code OptionalLong} describing the maximum element of this stream,
   * or an empty {@code OptionalLong} if this stream is empty
   * @throws NullPointerException if the maximum element is compared to {@code null}
   * @docgenVersion 9
   */
  @Override
  public OptionalLong max() {
    final OptionalLong max = inner.max();
    close();
    return max;
  }

  /**
   * {@inheritDoc}
   *
   * @return an {@code OptionalLong} describing the minimum element,
   * or an empty {@code OptionalLong} if this stream is empty
   * @throws NullPointerException if the minimum element is {@code null}
   * @implSpec The default implementation invokes {@link #min()} and
   * if the result is present, invokes {@link #close()} and
   * returns the result.
   * @docgenVersion 9
   */
  @Override
  public OptionalLong min() {
    final OptionalLong min = inner.min();
    close();
    return min;
  }

  /**
   * @docgenVersion 9
   * @see java.util.stream.LongStream#noneMatch(java.util.function.LongPredicate)
   */
  @Override
  public boolean noneMatch(@Nonnull @RefAware LongPredicate predicate) {
    track(predicate);
    final boolean match = inner.noneMatch((long t) -> predicate.test(getRef(t)));
    close();
    return match;
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * This implementation {@linkplain #track(Object) tracks} the given {@code closeHandler} and
   * returns a new {@link RefLongStream} with the same configuration as this one, but with the
   * given {@code closeHandler} registered.
   *
   * @return a new {@code RefLongStream} with the same configuration as this one, but with the
   * given {@code closeHandler} registered
   * @docgenVersion 9
   * @see #track(Object)
   */
  @Nonnull
  @Override
  public RefLongStream onClose(@RefAware Runnable closeHandler) {
    track(closeHandler);
    return new RefLongStream(inner.onClose(closeHandler), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns a parallel {@code RefLongStream} from the
   * underlying {@code RefStream}.</p>
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefLongStream parallel() {
    return new RefLongStream(inner.parallel(), lambdas, refs);
  }

  /**
   * Returns a new stream that is a view of the original stream with an additional intermediate operation.
   * This additional operation performs the given action on each element as elements are consumed from the
   * resulting stream.
   *
   * @param action the action to perform on each element as elements are consumed from the resulting stream
   * @return the new stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefLongStream peek(@Nonnull @RefAware LongConsumer action) {
    track(action);
    return new RefLongStream(inner.peek((long t) -> action.accept(getRef(t))), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation tracks the given accumulator function for reference
   * closing. The inner reducer is invoked with a reference-aware accumulator
   * function that stores references to the results of the accumulator function
   * in a reference queue.
   *
   * @docgenVersion 9
   */
  @Override
  public long reduce(long identity, @Nonnull @RefAware LongBinaryOperator accumulator) {
    track(accumulator);
    final long reduce = inner.reduce(storeRef(identity),
        (long t, long u) -> storeRef(accumulator.applyAsLong(getRef(t), getRef(u))));
    close();
    return reduce;
  }

  /**
   * @docgenVersion 9
   * @see java.util.stream.Stream#reduce(java.util.function.LongBinaryOperator)
   */
  @Override
  public OptionalLong reduce(@Nonnull @RefAware LongBinaryOperator accumulator) {
    track(accumulator);
    final OptionalLong optionalLong = inner
        .reduce((long t, long u) -> storeRef(accumulator.applyAsLong(getRef(t), getRef(u))));
    close();
    return optionalLong;
  }

  /**
   * {@inheritDoc}
   *
   * @return a {@code RefLongStream} providing sequential access to the elements of this stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefLongStream sequential() {
    return new RefLongStream(inner.sequential(), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * @return a new {@code RefLongStream}
   * @throws NullPointerException if {@code inner} is null
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefLongStream skip(long n) {
    return new RefLongStream(inner.skip(n), lambdas, refs);
  }

  /**
   * Returns a stream consisting of the elements of this stream, sorted
   * according to natural order.  When multiple elements are equal, they
   * will be presented in the order they occur in the source stream.
   *
   * <p>This is a <a href="package-summary.html#StreamOps">stateless
   * intermediate operation</a>.
   *
   * @return a stream consisting of the elements of this stream, sorted
   * according to natural order
   * @docgenVersion 9
   * @see #sorted(Comparator)
   */
  @Nonnull
  @Override
  public RefLongStream sorted() {
    return new RefLongStream(inner.sorted(), lambdas, refs);
  }

  /**
   * Returns a {@code Spliterator.OfLong} over the elements in this stream.
   *
   * <p>The {@code Spliterator.OfLong} reports {@link Spliterator#SIZED},
   * {@link Spliterator#SUBSIZED}, {@link Spliterator#ORDERED}, and
   * {@link Spliterator#IMMUTABLE}.
   *
   * <p>The spliterator's characteristics are further described in the
   * <a href="../Spliterator.html#binding">class documentation</a> for the
   * {@link Spliterator.OfLong} class.
   *
   * <p>The spliterator's source must be an
   * <a href="../Spliterator.html#binding">early-binding</a> spliterator.
   *
   * @return a {@code Spliterator.OfLong} over the elements in this stream
   * @docgenVersion 9
   * @see StreamSupport#longStream(Supplier, int, boolean)
   * @see StreamSupport#stream(Supplier, int, boolean)
   * @see StreamSupport#stream(Spliterator, boolean)
   * @since 1.8
   */
  @Nonnull
  @Override
  public RefSpliterator.OfLong spliterator() {
    return new RefSpliterator.OfLong(inner.spliterator()).track(new ReferenceCountingBase() {
      /**
       * Frees this RefLongStream. This will close the stream.
       *
       *   @docgenVersion 9
       */
      @Override
      protected void _free() {
        RefLongStream.this.close();
      }
    });
  }

  /**
   * @Override public long sum() {
   * final long sum = inner.sum();
   * close();
   * return sum;
   * }
   * @docgenVersion 9
   */
  @Override
  public long sum() {
    final long sum = inner.sum();
    close();
    return sum;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns a {@link LongSummaryStatistics} object computed
   * on the elements of this stream.
   *
   * <p>The returned {@code LongSummaryStatistics} object is not guaranteed to be
   * accurate if the stream is concurrently modified during summary computation.
   *
   * <p>If the stream is empty, the returned {@code LongSummaryStatistics}
   * object will have {@code count == 0}, {@code min == 0L}, {@code max == 0L}
   * and {@code sum == 0L}.
   *
   * @return a {@code LongSummaryStatistics} describing various summary data
   * about the elements of this stream
   * @throws IllegalStateException if the stream is closed
   * @docgenVersion 9
   */
  @Override
  public LongSummaryStatistics summaryStatistics() {
    final LongSummaryStatistics statistics = inner.summaryStatistics();
    close();
    return statistics;
  }

  /**
   * @Override public long[] toArray() {
   * final long[] longs = inner.toArray();
   * close();
   * return longs;
   * }
   * @docgenVersion 9
   */
  @Override
  public long[] toArray() {
    final long[] longs = inner.toArray();
    close();
    return longs;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns a new {@code RefLongStream} that is
   * unordered.
   *
   * @return a new unordered {@code RefLongStream}
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefLongStream unordered() {
    return new RefLongStream(inner.unordered(), lambdas, refs);
  }

  /**
   * @param lambda the lambdas to track
   * @return the RefLongStream
   * @throws NullPointerException if lambda is null
   * @docgenVersion 9
   */
  @Nonnull
  RefLongStream track(@Nonnull @RefAware Object... lambda) {
    for (Object l : lambda) {
      if (null != l && l instanceof ReferenceCounting)
        lambdas.add((ReferenceCounting) l);
    }
    return this;
  }

  /**
   * Returns a reference to the given object.
   *
   * @param u the object to get a reference to
   * @return a reference to the given object
   * @docgenVersion 9
   */
  private <U> U getRef(@RefAware U u) {
    return RefStream.getRef(u, this.refs);
  }

  /**
   * Stores a reference to the given object in the {@code refs} list.
   *
   * @param u the object to store a reference to
   * @return the same object that was passed in
   * @docgenVersion 9
   */
  private <U> U storeRef(@RefAware U u) {
    return RefStream.storeRef(u, refs);
  }

}
