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

/**
 * This class is a wrapper for a DoubleStream that keeps track of how many references are pointing to it.
 * It has a map of IdentityWrappers (which point to other ReferenceCounting objects) to integers (which represent how many references are pointing to the IdentityWrapper's object),
 * and a list of ReferenceCounting objects (which represent the lambdas that are referencing this RefDoubleStream).
 *
 * @docgenVersion 9
 */
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
   * @param s the DoubleSupplier providing values for the new stream
   * @return a new RefDoubleStream
   * @throws NullPointerException if s is null
   * @docgenVersion 9
   */
  @Nonnull
  public static RefDoubleStream generate(@Nonnull @RefAware DoubleSupplier s) {
    return new RefDoubleStream(DoubleStream.generate(s));
  }

  /**
   * Creates a new RefDoubleStream by iterating over the given seed and function.
   *
   * @param seed the starting value for the iteration
   * @param f    the function to apply to each value in the iteration
   * @return a new RefDoubleStream
   * @docgenVersion 9
   */
  @Nonnull
  public static RefDoubleStream iterate(final double seed, @Nonnull final @RefAware DoubleUnaryOperator f) {
    return new RefDoubleStream(DoubleStream.iterate(seed, f));
  }

  /**
   * Returns a {@code RefDoubleStream} consisting of a single element.
   *
   * @param x the single element
   * @return a {@code RefDoubleStream} consisting of a single element
   * @docgenVersion 9
   */
  @Nonnull
  public static RefDoubleStream of(double x) {
    return new RefDoubleStream(DoubleStream.of(x));
  }

  /**
   * Returns a {@link RefDoubleStream} consisting of the elements of the specified array.
   *
   * @param array the array of doubles to be processed
   * @return the new stream
   * @docgenVersion 9
   */
  @Nonnull
  public static RefDoubleStream of(double... array) {
    return new RefDoubleStream(DoubleStream.of(array));
  }

  /**
   * Returns a new {@code RefDoubleStream} that is composed of the {@code RefDoubleStreams}
   * that are passed as arguments.
   *
   * <p>
   * The resulting stream is ordered if both of the input streams are ordered, and parallel
   * if either of the input streams is parallel. When the resulting stream is closed, the
   * close handlers for both input streams are invoked.
   *
   * @param a the first stream
   * @param b the second stream
   * @return the concatenated stream
   * @throws NullPointerException if {@code a} or {@code b} is {@code null}
   * @docgenVersion 9
   */
  @Nonnull
  public static RefDoubleStream concat(@Nonnull @RefAware RefDoubleStream a, @Nonnull @RefAware RefDoubleStream b) {
    return new RefDoubleStream(DoubleStream.concat(a.inner, b.inner));
  }

  /**
   * @docgenVersion 9
   * @see java.util.stream.DoubleStream#allMatch(java.util.function.DoublePredicate)
   */
  @Override
  public boolean allMatch(@Nonnull @RefAware DoublePredicate predicate) {
    track(predicate);
    final boolean match = inner.allMatch(value -> predicate.test(value));
    close();
    return match;
  }

  /**
   * @docgenVersion 9
   * @see java.util.stream.DoubleStream#anyMatch(java.util.function.DoublePredicate)
   */
  @Override
  public boolean anyMatch(@Nonnull @RefAware DoublePredicate predicate) {
    track(predicate);
    final boolean match = inner.anyMatch(value -> predicate.test(value));
    close();
    return match;
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
   * each boxed to a {@code Double}.
   *
   * <p>This is an {@linkplain java.util.stream.Stream#flatMap flatMap}
   * operation.
   *
   * @return a {@code Stream} consisting of the elements of this stream,
   * each boxed to a {@code Double}
   * @docgenVersion 9
   * @since 1.8
   */
  @Nonnull
  @Override
  public RefStream<Double> boxed() {
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
   * @return a {@code RefDoubleStream} consisting of the distinct elements of this stream
   * @throws NullPointerException if the element selected is null
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefDoubleStream distinct() {
    return new RefDoubleStream(inner.distinct(), lambdas, refs);
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
  public RefDoubleStream filter(@Nonnull @RefAware DoublePredicate predicate) {
    track(predicate);
    return new RefDoubleStream(inner.filter((double t) -> predicate.test(RefUtil.addRef(t))), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>If the stream is empty, the result is empty.
   *
   * @return an {@code OptionalDouble} describing any element of this stream,
   * or an empty {@code OptionalDouble} if the stream is empty
   * @throws IllegalStateException if the stream is parallel
   * @docgenVersion 9
   */
  @Override
  public OptionalDouble findAny() {
    final OptionalDouble any = inner.findAny();
    close();
    return any;
  }

  /**
   * {@inheritDoc}
   *
   * <p>If a value is present in this {@code Stream}, returns an {@link OptionalDouble}
   * describing the first element of this {@code Stream}, otherwise returns an empty
   * {@link OptionalDouble}.
   *
   * @return an {@code OptionalDouble} describing the first element of this stream,
   * or an empty {@code OptionalDouble} if the stream is empty
   * @throws IllegalStateException if the stream is already closed
   * @implSpec If a value is present, the resulting {@code OptionalDouble} instance will be
   * non-empty.
   * @docgenVersion 9
   */
  @Override
  public OptionalDouble findFirst() {
    final OptionalDouble first = inner.findFirst();
    close();
    return first;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation tracks the given {@code mapper} function in a {@link Ref} object.
   *
   * @param mapper the mapping function to apply to each element
   * @return the new stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefDoubleStream flatMap(@Nonnull @RefAware DoubleFunction<? extends DoubleStream> mapper) {
    track(mapper);
    return new RefDoubleStream(inner.flatMap((double t) -> mapper.apply(t).map(u -> storeRef(u))), lambdas, refs);
  }

  /**
   * Performs the given action for each element of the Iterable until all elements
   * have been processed or the action throws an exception.  Exceptions thrown by
   * the action are relayed to the caller.
   * <p>
   * The action is performed in the order of iteration if that order is specified.
   *
   * @param action The action to be performed for each element
   * @throws NullPointerException if the specified action is null
   * @implSpec <p>The default implementation behaves as if:
   * <pre>{@code
   *     for (T t : this)
   *         action.accept(t);
   * }</pre>
   * @docgenVersion 9
   * @since 1.8
   */
  public void forEach(@Nonnull @RefAware DoubleConsumer action) {
    track(action);
    inner.forEach(value -> action.accept(value));
    close();
  }

  /**
   * Performs the given action for each element of this stream, in the order
   * elements appear in the stream, until all elements have been processed or
   * the action throws an exception.  Upon encountering an element that is not
   * {@code null}, the given action is invoked with the element's value as its
   * argument.  Exceptions thrown by the action are relayed to the caller.
   * <p>
   * The behavior of this operation is explicitly nondeterministic.  For parallel
   * stream pipelines, this operation does <em>not</em> guarantee to respect the
   * encounter order of the stream, as doing so would sacrifice the benefit of
   * parallelism.  For any given element, the action may be performed at whatever
   * time and in whatever thread the library chooses.  If the action accesses
   * shared state, it is responsible for providing the required synchronization.
   *
   * @param action a non-interfering, stateless action to perform on the elements
   * @throws NullPointerException if the given action is null
   * @docgenVersion 9
   */
  @Override
  public void forEachOrdered(@Nonnull @RefAware DoubleConsumer action) {
    track(action);
    inner.forEachOrdered(value -> action.accept(value));
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
  public RefPrimitiveIterator.OfDouble iterator() {
    return new RefPrimitiveIterator.OfDouble(inner.iterator()).track(new ReferenceCountingBase() {
      /**
       * Frees this stream by closing it.
       *
       *   @docgenVersion 9
       */
      @Override
      protected void _free() {
        RefDoubleStream.this.close();
      }
    });
  }

  /**
   * {@inheritDoc}
   *
   * @return a new {@code RefDoubleStream} with the same elements as this stream, but no more than {@code maxSize} in total
   * @throws IllegalArgumentException if {@code maxSize} is negative
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefDoubleStream limit(long maxSize) {
    return new RefDoubleStream(inner.limit(maxSize), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation tracks the given {@link DoubleUnaryOperator} with a
   * {@link Ref}.</p>
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefDoubleStream map(@Nonnull @RefAware DoubleUnaryOperator mapper) {
    track(mapper);
    return new RefDoubleStream(inner.map(t -> mapper.applyAsDouble(t)), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * This implementation tracks the given {@code mapper} function using the
   * {@link RefUtils#track(Object)} method.
   *
   * @param mapper the mapping function to apply to each element
   * @return a {@code RefIntStream} consisting of the results of applying the
   * given mapping function to the elements of this stream
   * @throws NullPointerException if the given {@code mapper} is {@code null}
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefIntStream mapToInt(@Nonnull @RefAware DoubleToIntFunction mapper) {
    track(mapper);
    return new RefIntStream(inner.mapToInt((double value) -> mapper.applyAsInt(getRef(value))), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation tracks the given {@code mapper} function using a
   * {@link Ref} object. The resulting {@code RefLongStream} contains references
   * to the input values as well as the result of the mapping function.</p>
   *
   * @param mapper the mapping function to apply to each element
   * @return the new stream
   * @throws NullPointerException if the given {@code mapper} is {@code null}
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefLongStream mapToLong(@Nonnull @RefAware DoubleToLongFunction mapper) {
    track(mapper);
    return new RefLongStream(inner.mapToLong((double value) -> mapper.applyAsLong(getRef(value))), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>The returned {@code RefStream} is backed by this stream, and will reflect
   * any changes that are made to this stream.  The returned stream is also
   * {@code RefAware}.
   *
   * @param mapper a non-interfering, stateless function to apply to each element
   * @return the new stream
   * @throws NullPointerException if the given mapper is null
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public <U> RefStream<U> mapToObj(@Nonnull @RefAware DoubleFunction<? extends U> mapper) {
    return new RefStream<U>(inner.mapToObj(mapper).map(u -> storeRef(u)), lambdas, refs).track(mapper);
  }

  /**
   * {@inheritDoc}
   *
   * @implSpec The default implementation invokes {@link #close()} after invoking the
   * {@code max} method of the {@code Stream} returned by {@link #inner}.
   * @docgenVersion 9
   */
  @Override
  public OptionalDouble max() {
    final OptionalDouble max = inner.max();
    close();
    return max;
  }

  /**
   * {@inheritDoc}
   *
   * @return an {@code OptionalDouble} describing the minimum element,
   * or an empty {@code OptionalDouble} if the stream is empty
   * @throws NullPointerException if the minimum element is {@code null}
   * @implSpec The default implementation invokes {@link #min()} and
   * if present, returns the result.
   * @docgenVersion 9
   */
  @Override
  public OptionalDouble min() {
    final OptionalDouble min = inner.min();
    close();
    return min;
  }

  /**
   * @docgenVersion 9
   * @see java.util.stream.DoubleStream#noneMatch(java.util.function.DoublePredicate)
   */
  @Override
  public boolean noneMatch(@Nonnull @RefAware DoublePredicate predicate) {
    track(predicate);
    final boolean match = inner.noneMatch((double t) -> predicate.test(getRef(t)));
    close();
    return match;
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * This implementation {@linkplain #track(Runnable) tracks} the given close
   * handler, and returns a new {@code RefDoubleStream} with the given handler.
   *
   * @return a new {@code RefDoubleStream} with the given handler
   * @docgenVersion 9
   * @see #track(Runnable)
   */
  @Nonnull
  @Override
  public RefDoubleStream onClose(@RefAware Runnable closeHandler) {
    track(closeHandler);
    return new RefDoubleStream(inner.onClose(closeHandler), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation creates a new {@code RefDoubleStream} that is
   * parallel and contains the same elements as this stream.
   *
   * @return a parallel {@code RefDoubleStream} with the same elements
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefDoubleStream parallel() {
    return new RefDoubleStream(inner.parallel(), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation {@linkplain #track(java.util.function.DoubleConsumer) tracks} the given action,
   * wrapping it in a {@linkplain RefAware} action if necessary.
   *
   * @return a new {@code RefDoubleStream}
   * @throws NullPointerException if the given action is {@code null}
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefDoubleStream peek(@Nonnull @RefAware DoubleConsumer action) {
    track(action);
    return new RefDoubleStream(inner.peek((double t) -> action.accept(getRef(t))), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation tracks references to the accumulator function,
   * and closes them when the {@code reduce} operation is complete.</p>
   *
   * @docgenVersion 9
   */
  @Override
  public double reduce(double identity, @Nonnull @RefAware DoubleBinaryOperator accumulator) {
    track(accumulator);
    final double reduce = inner.reduce(storeRef(identity),
        (double t, double u) -> storeRef(accumulator.applyAsDouble(getRef(t), getRef(u))));
    close();
    return reduce;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation tracks the given accumulator function with a
   * {@link Ref} and closes the tracker when the result is obtained.
   *
   * @docgenVersion 9
   */
  @Override
  public OptionalDouble reduce(@Nonnull @RefAware DoubleBinaryOperator accumulator) {
    track(accumulator);
    final OptionalDouble optionalDouble = inner
        .reduce((double t, double u) -> accumulator.applyAsDouble(t, u));
    close();
    return optionalDouble;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation creates a new {@code RefDoubleStream} that is a sequential view of this stream.
   *
   * @return a sequential view of this stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefDoubleStream sequential() {
    return new RefDoubleStream(inner.sequential(), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * @return a new {@code RefDoubleStream} with the specified number of elements skipped
   * @throws IllegalArgumentException if {@code n} is negative
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefDoubleStream skip(long n) {
    return new RefDoubleStream(inner.skip(n), lambdas, refs);
  }

  /**
   * Returns a stream consisting of the elements of this stream, sorted
   * according to natural order.  When multiple elements are equivalent,
   * they will be presented in the encounter order of the source.
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
  public RefDoubleStream sorted() {
    return new RefDoubleStream(inner.sorted(), lambdas, refs);
  }

  /**
   * Returns a {@code Spliterator} for the elements of this stream.
   *
   * <p>The {@code Spliterator} reports {@link Spliterator#SIZED},
   * {@link Spliterator#SUBSIZED}, {@link Spliterator#ORDERED}, and
   * {@link Spliterator#IMMUTABLE}.  Implementations should document the reporting
   * of additional characteristic values.
   *
   * <p>The spliterator's splittability is determined by the spliterator of the
   * underlying {@code Stream}.  If the underlying stream is unordered, the
   * spliterator should report {@code ORDERED}.  To preserve stability for
   * parallel computations, implementations should also report
   * {@link Spliterator#IMMUTABLE} or {@link Spliterator#CONCURRENT}, as
   * appropriate.
   *
   * <p>The spliterator's characteristics must be reported accurately using the
   * mechanism defined in {@link java.util.Spliterator#characteristics()}.
   *
   * <p>The spliterator's encounter order is determined by the spliterator of the
   * underlying {@code Stream}.  If the underlying stream is unordered, a
   * spliterator for a parallel stream should report {@code ORDERED}.
   *
   * <p>If the action associated with a parallel {@code Stream} is not
   * stateless, the spliterator should report {@link Spliterator#CONCURRENT}.
   * Otherwise, the spliterator could report {@link Spliterator#IMMUTABLE} or
   * {@link Spliterator#SIZED}.
   *
   * @return a {@code Spliterator} for the elements of this stream
   * @apiSince 24
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefSpliterator.OfDouble spliterator() {
    return new RefSpliterator.OfDouble(inner.spliterator()).track(new ReferenceCountingBase() {
      /**
       * Frees this stream by closing it.
       *
       *   @docgenVersion 9
       */
      @Override
      protected void _free() {
        RefDoubleStream.this.close();
      }
    });
  }

  /**
   * @Override public double sum() {
   * final double sum = inner.sum();
   * close();
   * return sum;
   * }
   * @docgenVersion 9
   */
  @Override
  public double sum() {
    final double sum = inner.sum();
    close();
    return sum;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns the {@linkplain DoubleSummaryStatistics
   * statistics} from the underlying {@linkplain #inner}, and closes the
   * {@code CloseableDoubleStream}.</p>
   *
   * @return the summary statistics
   * @throws IllegalStateException if the stream has already been closed
   * @docgenVersion 9
   */
  @Override
  public DoubleSummaryStatistics summaryStatistics() {
    final DoubleSummaryStatistics statistics = inner.summaryStatistics();
    close();
    return statistics;
  }

  /**
   * @Override public double[] toArray() {
   * final double[] array = inner.toArray();
   * close();
   * return array;
   * }
   * @docgenVersion 9
   */
  @Override
  public double[] toArray() {
    final double[] array = inner.toArray();
    close();
    return array;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation returns a new {@code RefDoubleStream} that is
   * unordered.
   *
   * @return a new unordered stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefDoubleStream unordered() {
    return new RefDoubleStream(inner.unordered(), lambdas, refs);
  }

  /**
   * @param lambda the lambdas to track
   * @return the current stream
   * @docgenVersion 9
   */
  @Nonnull
  public RefDoubleStream track(@Nonnull @RefAware Object... lambda) {
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
