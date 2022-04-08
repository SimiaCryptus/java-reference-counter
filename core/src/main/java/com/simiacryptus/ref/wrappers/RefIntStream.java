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

/**
 * This class is a wrapper for IntStream that keeps track of how many references there are to it.
 * It has a map of IdentityWrappers (which wrap around ReferenceCounting objects) to integers,
 * which represents how many references there are to each object. It also has a list of
 * ReferenceCounting objects, which are the lambdas that are referencing this stream.
 *
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public class RefIntStream implements IntStream {
  private final IntStream inner;
  private final Map<RefStream.IdentityWrapper<ReferenceCounting>, AtomicInteger> refs;
  private final List<ReferenceCounting> lambdas;

  RefIntStream(@RefAware IntStream stream) {
    this(stream, new ArrayList<>(), new ConcurrentHashMap<>());
  }

  RefIntStream(@RefAware IntStream stream, @RefAware List<ReferenceCounting> lambdas,
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
   * @param s the IntSupplier providing values for the new RefIntStream
   * @return a new RefIntStream
   * @docgenVersion 9
   */
  @Nonnull
  public static RefIntStream generate(@Nonnull @RefAware IntSupplier s) {
    return new RefIntStream(IntStream.generate(s));
  }

  /**
   * Returns a new RefIntStream over the given int range.
   *
   * @param startInclusive the (inclusive) initial value
   * @param endExclusive   the exclusive upper bound
   * @return a new RefIntStream
   * @docgenVersion 9
   */
  @Nonnull
  public static RefIntStream range(int startInclusive, int endExclusive) {
    return new RefIntStream(IntStream.range(startInclusive, endExclusive));
  }

  /**
   * Returns a new IntStream that contains the integers from startInclusive to endInclusive, inclusive.
   *
   * @param startInclusive the (inclusive) initial value
   * @param endInclusive   the (inclusive) final value
   * @return the new IntStream
   * @docgenVersion 9
   */
  @Nonnull
  public static IntStream rangeClosed(int startInclusive, int endInclusive) {
    return new RefIntStream(IntStream.rangeClosed(startInclusive, endInclusive));
  }

  /**
   * Returns a new RefIntStream of one element.
   *
   * @param x the element
   * @return a new RefIntStream of one element
   * @docgenVersion 9
   */
  @Nonnull
  public static RefIntStream of(int x) {
    return new RefIntStream(IntStream.of(x));
  }

  /**
   * @param array The int array to be turned into a RefIntStream
   * @return A RefIntStream that contains the given int array
   * @docgenVersion 9
   */
  @Nonnull
  public static RefIntStream of(@Nonnull int... array) {
    return new RefIntStream(IntStream.of(array).onClose(() -> {
      Arrays.stream(array).forEach(value -> RefUtil.freeRef(value));
    }));
  }

  /**
   * Returns a new {@code RefIntStream} that is composed of the {@code RefIntStreams}
   * that are passed as arguments.
   *
   * <p>If any of the arguments is {@code null}, a {@code NullPointerException} will be thrown.
   *
   * @param a the first {@code RefIntStream}
   * @param b the second {@code RefIntStream}
   * @return the composed {@code RefIntStream}
   * @throws NullPointerException if any of the arguments is {@code null}
   * @docgenVersion 9
   */
  @Nonnull
  public static RefIntStream concat(@Nonnull @RefAware RefIntStream a, @Nonnull @RefAware RefIntStream b) {
    return new RefIntStream(IntStream.concat(a.inner, b.inner));
  }

  /**
   * Creates a new RefIntStream by iterating a seed value through a function.
   *
   * @param seed the initial value for the iteration
   * @param f    the function to apply to the seed value on each iteration
   * @return a new RefIntStream
   * @docgenVersion 9
   */
  @Nonnull
  public static RefIntStream iterate(final int seed, @Nonnull final @RefAware IntUnaryOperator f) {
    return new RefIntStream(IntStream.iterate(seed, f));
  }

  /**
   * @docgenVersion 9
   * @see java.util.stream.Stream#allMatch(java.util.function.IntPredicate)
   */
  @Override
  public boolean allMatch(@Nonnull @RefAware IntPredicate predicate) {
    track(predicate);
    final boolean match = inner.allMatch((int t) -> predicate.test(getRef(t)));
    close();
    return match;
  }

  /**
   * @docgenVersion 9
   * @see java.util.stream.Stream#anyMatch(java.util.function.IntPredicate)
   */
  @Override
  public boolean anyMatch(@Nonnull @RefAware IntPredicate predicate) {
    track(predicate);
    final boolean match = inner.anyMatch((int t) -> predicate.test(getRef(t)));
    close();
    return match;
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
   * {@inheritDoc}
   *
   * @return a {@code RefLongStream}
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefLongStream asLongStream() {
    return new RefLongStream(inner.asLongStream(), lambdas, refs);
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
   * Returns a Stream consisting of the elements of this Stream, each boxed
   * into an {@code Integer}.
   *
   * @return a Stream consisting of the elements of this Stream, each boxed
   * into an {@code Integer}
   * @docgenVersion 9
   * @see Stream#boxed()
   */
  @Nonnull
  @Override
  public RefStream<Integer> boxed() {
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
   * @return a {@code RefIntStream} with distinct elements
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefIntStream distinct() {
    return new RefIntStream(inner.distinct(), lambdas, refs);
  }

  /**
   * Returns a stream consisting of the elements of this stream that match
   * the given predicate.
   *
   * @param predicate a non-null predicate
   * @return the new stream
   * @docgenVersion 9
   */
  @Nonnull
  public RefIntStream filter(@Nonnull @RefAware IntPredicate predicate) {
    track(predicate);
    return new RefIntStream(inner.filter(predicate), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>If a value is present, it is returned.
   * Otherwise, the result is empty.
   *
   * @return an {@code OptionalInt} containing a value, if present, otherwise an empty {@code OptionalInt}
   * @docgenVersion 9
   */
  @Override
  public OptionalInt findAny() {
    final OptionalInt any = inner.findAny();
    close();
    return any;
  }

  /**
   * @Override public OptionalInt findFirst() {
   * final OptionalInt first = inner.findFirst();
   * close();
   * return first;
   * }
   * @docgenVersion 9
   */
  @Override
  public OptionalInt findFirst() {
    final OptionalInt first = inner.findFirst();
    close();
    return first;
  }

  /**
   * Returns a new stream by flat mapping each element of this stream to a new stream
   * using the provided mapper function.
   *
   * @param mapper the mapper function used to create new streams
   * @return the new stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefIntStream flatMap(@Nonnull @RefAware IntFunction<? extends IntStream> mapper) {
    track(mapper);
    return new RefIntStream(inner.flatMap(mapper).map(u -> storeRef(u)), lambdas, refs);
  }

  /**
   * @Override public void forEach(@Nonnull @RefAware IntConsumer action) {
   * track(action);
   * inner.forEach(action);
   * close();
   * }
   * @docgenVersion 9
   */
  @Override
  public void forEach(@Nonnull @RefAware IntConsumer action) {
    track(action);
    inner.forEach(action);
    close();
  }

  /**
   * Performs the given action for each element of this stream, in the encounter order of the stream if the stream has a defined encounter order.
   *
   * @param action The action to perform on each element
   * @docgenVersion 9
   */
  @Override
  public void forEachOrdered(@Nonnull @RefAware IntConsumer action) {
    track(action);
    inner.forEachOrdered(action);
    close();
  }

  /**
   * Returns an iterator over the elements in this stream.
   *
   * @return an iterator over the elements in this stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefPrimitiveIterator.OfInt iterator() {
    return new RefPrimitiveIterator.OfInt(inner.iterator()).track(new ReferenceCountingBase() {
      /**
       * Frees this resource.
       *
       *   @docgenVersion 9
       */
      @Override
      protected void _free() {
        RefIntStream.this.close();
      }
    });
  }

  /**
   * {@inheritDoc}
   *
   * @return a new {@code RefIntStream} with the given size limit
   * @throws IllegalArgumentException if {@code maxSize} is negative
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefIntStream limit(long maxSize) {
    return new RefIntStream(inner.limit(maxSize), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * This implementation invokes {@link #map(IntUnaryOperator) map} and returns
   * the result.
   *
   * @param mapper the mapper function used to apply to each element
   * @return the new stream
   * @docgenVersion 9
   * @see #map(IntUnaryOperator)
   */
  @Nonnull
  @Override
  public RefIntStream map(@Nonnull @RefAware IntUnaryOperator mapper) {
    return new RefIntStream(inner.map(mapper), lambdas, refs).track(mapper);
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * This implementation invokes {@link #mapToDouble(IntToDoubleFunction)}, and
   * returns a new {@code RefDoubleStream} with the resulting values.
   *
   * @param mapper the {@code IntToDoubleFunction} used to map the values
   * @return the new {@code RefDoubleStream}
   * @docgenVersion 9
   * @see #mapToDouble(IntToDoubleFunction)
   */
  @Nonnull
  @Override
  public RefDoubleStream mapToDouble(@Nonnull @RefAware IntToDoubleFunction mapper) {
    return new RefDoubleStream(inner.mapToDouble(mapper), lambdas, refs).track(mapper);
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * This implementation invokes {@link #mapToLong(IntToLongFunction)} and returns a new {@code RefLongStream} with the results.
   *
   * @param mapper the {@code IntToLongFunction} to apply to each element
   * @return the new {@code RefLongStream}
   * @throws NullPointerException if {@code mapper} is {@code null}
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefLongStream mapToLong(@Nonnull @RefAware IntToLongFunction mapper) {
    return new RefLongStream(inner.mapToLong(mapper), lambdas, refs).track(mapper);
  }

  /**
   * Returns a {@code Stream} consisting of the results of applying the given
   * function to the elements of this stream.
   *
   * <p>This is an intermediate operation.
   *
   * <p>The function is applied lazily, invoked only when needed to produce the next
   * element in the resulting stream. This is different from most other intermediate
   * operations, which are eager, and are invoked immediately upon invocation of the
   * parent operation.
   *
   * <p>For parallel stream pipelines, the implementation may parallelize the
   * computation of the function by querying the {@link #isParallel()} method.
   *
   * @param <U>    the element type of the new stream
   * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
   *               <a href="package-summary.html#Statelessness">stateless</a>
   *               function to apply to each element
   * @return the new stream
   * @docgenVersion 9
   * @see Stream#mapToObj(IntFunction)
   */
  @Nonnull
  @Override
  public <U> RefStream<U> mapToObj(@RefAware IntFunction<? extends U> mapper) {
    track(mapper);
    return new RefStream<U>(inner.mapToObj(x -> storeRef(mapper.apply(x))), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * @implSpec The default implementation invokes {@link #close()} after invoking
   * {@link #inner}.
   * @docgenVersion 9
   * @since 1.8
   */
  @Override
  public OptionalInt max() {
    final OptionalInt max = inner.max();
    close();
    return max;
  }

  /**
   * {@inheritDoc}
   *
   * <p>If the stream is empty, the returned {@code OptionalInt} is empty.
   *
   * @return an {@code OptionalInt} describing the minimum element of this stream,
   * or an empty {@code OptionalInt} if the stream is empty
   * @docgenVersion 9
   */
  @Override
  public OptionalInt min() {
    final OptionalInt min = inner.min();
    close();
    return min;
  }

  /**
   * @docgenVersion 9
   * @see java.util.stream.Stream#noneMatch(java.util.function.IntPredicate)
   */
  @Override
  public boolean noneMatch(@Nonnull @RefAware IntPredicate predicate) {
    track(predicate);
    final boolean match = inner.noneMatch((int t) -> predicate.test(getRef(t)));
    close();
    return match;
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * This implementation {@linkplain #track(Runnable) tracks} the given close
   * handler, and returns a new {@code RefIntStream} with the given close handler.
   *
   * @return a new {@code RefIntStream} with the given close handler
   * @docgenVersion 9
   * @see #untrack(Runnable)
   * @see #untrackAll()
   */
  @Nonnull
  @Override
  public RefIntStream onClose(@RefAware Runnable closeHandler) {
    track(closeHandler);
    return new RefIntStream(inner.onClose(closeHandler), lambdas, refs);
  }

  /**
   * Returns a parallel {@code RefIntStream} with this stream.
   *
   * @return a parallel {@code RefIntStream} with this stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefIntStream parallel() {
    return new RefIntStream(inner.parallel(), lambdas, refs);
  }

  /**
   * @param action a consumer that takes in an integer
   * @return a new RefIntStream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefIntStream peek(@Nonnull @RefAware IntConsumer action) {
    track(action);
    return new RefIntStream(inner.peek((int t) -> action.accept(getRef(t))), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation tracks the given accumulator function for reference
   * closing. The result is stored and returned as an integer.</p>
   *
   * @docgenVersion 9
   */
  @Override
  public int reduce(int identity, @Nonnull @RefAware IntBinaryOperator accumulator) {
    track(accumulator);
    final int reduce = inner.reduce(storeRef(identity),
        (int t, int u) -> storeRef(accumulator.applyAsInt(getRef(t), getRef(u))));
    close();
    return reduce;
  }

  /**
   * @docgenVersion 9
   * @see java.util.stream.Stream#reduce(java.util.function.IntBinaryOperator)
   */
  @Override
  public OptionalInt reduce(@Nonnull @RefAware IntBinaryOperator accumulator) {
    track(accumulator);
    final OptionalInt reduce = inner.reduce((int t, int u) -> storeRef(accumulator.applyAsInt(getRef(t), getRef(u))));
    close();
    return reduce;
  }

  /**
   * {@inheritDoc}
   *
   * @return a {@code RefIntStream} providing sequential access to the elements of this stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefIntStream sequential() {
    return new RefIntStream(inner.sequential(), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * @return a new {@code RefIntStream} with the given number of elements skipped
   * @throws NullPointerException if {@code inner} is null
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefIntStream skip(long n) {
    return new RefIntStream(inner.skip(n), lambdas, refs);
  }

  /**
   * @return a new RefIntStream consisting of the elements of this stream, sorted
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefIntStream sorted() {
    return new RefIntStream(inner.sorted(), lambdas, refs);
  }

  /**
   * Returns a {@code Spliterator} over the elements of this stream.
   *
   * <p>The {@code Spliterator} reports {@link Spliterator#SIZED},
   * {@link Spliterator#SUBSIZED}, {@link Spliterator#ORDERED}, and
   * {@link Spliterator#IMMUTABLE}.
   *
   * <p>The spliterator's comparator (see
   * {@link java.util.Spliterator#getComparator()}) is {@code null} if
   * the stream's comparator (see {@link #comparator()}) is {@code null}.
   * Otherwise, the spliterator's comparator is the same as or imposes the
   * same total ordering as the stream's comparator.
   *
   * @return a {@code Spliterator} over the elements of this stream
   * @implNote The {@code Spliterator} additionally reports
   * {@link Spliterator#NONNULL}.
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefSpliterator.OfInt spliterator() {
    return new RefSpliterator.OfInt(inner.spliterator()).track(new ReferenceCountingBase() {
      /**
       * Frees this resource.
       *
       *   @docgenVersion 9
       */
      @Override
      protected void _free() {
        RefIntStream.this.close();
      }
    });
  }

  /**
   * @Override public int sum() {
   * final int sum = inner.sum();
   * close();
   * return sum;
   * }
   * @docgenVersion 9
   */
  @Override
  public int sum() {
    final int sum = inner.sum();
    close();
    return sum;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns the {@linkplain IntSummaryStatistics#IntSummaryStatistics()
   * statistics} of the elements in this stream, closes the stream, and returns
   * the statistics.
   *
   * @return the summary statistics
   * @docgenVersion 9
   */
  @Override
  public IntSummaryStatistics summaryStatistics() {
    final IntSummaryStatistics statistics = inner.summaryStatistics();
    close();
    return statistics;
  }

  /**
   * @Override public int[] toArray() {
   * final int[] ints = inner.toArray();
   * close();
   * return ints;
   * }
   * @docgenVersion 9
   */
  @Override
  public int[] toArray() {
    final int[] ints = inner.toArray();
    close();
    return ints;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns a new {@code RefIntStream} that is
   * unordered.
   *
   * @return a new unordered stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefIntStream unordered() {
    return new RefIntStream(inner.unordered(), lambdas, refs);
  }

  /**
   * @param lambda the lambdas to track
   * @return the current stream
   * @throws NullPointerException if lambda is null
   * @docgenVersion 9
   */
  @Nonnull
  RefIntStream track(@Nonnull @RefAware Object... lambda) {
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
