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

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.*;
import java.util.stream.*;

/**
 * A wrapper class for a Stream.
 *
 * @param <T> the type of the stream
 * @docgenVersion 9
 */
@RefIgnore
public class StreamWrapper<T> implements Stream<T> {
  private final Stream<T> inner;

  public StreamWrapper(@RefAware Stream<T> inner) {
    this.inner = inner;
  }

  /**
   * Returns a stream of the inner objects.
   *
   * @return a stream of the inner objects
   * @docgenVersion 9
   */
  public Stream<T> getInner() {
    return inner;
  }

  /**
   * @return a boolean indicating whether or not this
   * {@code Stream} is parallel
   * @docgenVersion 9
   */
  @Override
  public boolean isParallel() {
    return getInner().isParallel();
  }

  /**
   * @docgenVersion 9
   * @see java.util.stream.Stream#allMatch(java.util.function.Predicate)
   */
  @Override
  public boolean allMatch(@RefAware Predicate<? super T> predicate) {
    return getInner().allMatch(predicate);
  }

  /**
   * {@inheritDoc}
   *
   * @implSpec This implementation delegates to the {@code anyMatch}
   * method of the underlying {@code Stream}.
   * @docgenVersion 9
   */
  @Override
  public boolean anyMatch(@RefAware Predicate<? super T> predicate) {
    return getInner().anyMatch(predicate);
  }

  /**
   * Closes the inner object.
   *
   * @docgenVersion 9
   */
  @Override
  public void close() {
    getInner().close();
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation delegates to the {@link #getInner() inner}
   * {@code Stream}'s {@code collect} operation.
   *
   * @param <R>         the type of the result
   * @param supplier    a {@code Supplier} whose result is used as the initial
   *                    (accumulator) value of the reduction
   * @param accumulator an {@code BiConsumer} that accumulates the intermediate
   *                    results of the reduction
   * @param combiner    a {@code BiConsumer} that combines the intermediate results
   *                    of the reduction
   * @return the result of the reduction
   * @throws NullPointerException if any argument is {@code null}
   * @docgenVersion 9
   */
  @Override
  public <R> R collect(@RefAware Supplier<R> supplier,
                       @RefAware BiConsumer<R, ? super T> accumulator,
                       @RefAware BiConsumer<R, R> combiner) {
    return getInner().collect(supplier, accumulator, combiner);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation delegates to the {@link #getInner() inner}
   * {@link Stream Stream's} {@link Stream#collect(Collector)} method.</p>
   *
   * @docgenVersion 9
   */
  @Override
  public <R, A> R collect(@RefAware Collector<? super T, A, R> collector) {
    return getInner().collect(collector);
  }

  /**
   * @Override public long count() {
   * return getInner().count();
   * }
   * @docgenVersion 9
   */
  @Override
  public long count() {
    return getInner().count();
  }

  /**
   * @return a stream consisting of the distinct elements of this stream
   * @docgenVersion 9
   */
  @Override
  public Stream<T> distinct() {
    return getInner().distinct();
  }

  /**
   * @Override public Stream<T> filter(@RefAware Predicate<? super T> predicate) {
   * return getInner().filter(predicate);
   * }
   * @docgenVersion 9
   */
  @Override
  public Stream<T> filter(@RefAware Predicate<? super T> predicate) {
    return getInner().filter(predicate);
  }

  /**
   * @return an {@code Optional} describing some element of this stream, or an empty {@code Optional} if the stream is empty
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Optional<T> findAny() {
    return getInner().findAny();
  }

  /**
   * @return an {@code Optional} describing the first element of this stream,
   * or an empty {@code Optional} if the stream is empty
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Optional<T> findFirst() {
    return getInner().findFirst();
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation delegates to the {@link #getInner() inner}
   * {@link Stream} and returns the result of {@link Stream#flatMap(Function)}
   * applied to the inner stream and the given {@code mapper}.
   *
   * @param mapper a {@code Function} to apply to each element of the inner
   *               stream, producing a {@code Stream} of new values
   * @param <R>    the element type of the new stream
   * @return the new stream
   * @throws NullPointerException if {@code mapper} is {@code null}
   * @docgenVersion 9
   */
  @Override
  public <R> Stream<R> flatMap(
      @RefAware Function<? super T, ? extends Stream<? extends R>> mapper) {
    return getInner().flatMap(mapper);
  }

  /**
   * {@inheritDoc}
   *
   * @param mapper the mapper function used to flat-map the stream
   * @return the new stream
   * @throws NullPointerException if {@code mapper} is null
   * @implSpec The default implementation invokes {@code mapper} and returns the result.
   * @docgenVersion 9
   */
  @Override
  public DoubleStream flatMapToDouble(
      @RefAware Function<? super T, ? extends DoubleStream> mapper) {
    return getInner().flatMapToDouble(mapper);
  }

  /**
   * @docgenVersion 9
   * @see java.util.stream.Stream#flatMapToInt(java.util.function.Function)
   */
  @Override
  public IntStream flatMapToInt(@RefAware Function<? super T, ? extends IntStream> mapper) {
    return getInner().flatMapToInt(mapper);
  }

  /**
   * @docgenVersion 9
   * @see java.util.stream.Stream#flatMapToLong(java.util.function.Function)
   */
  @Override
  public LongStream flatMapToLong(
      @RefAware Function<? super T, ? extends LongStream> mapper) {
    return getInner().flatMapToLong(mapper);
  }

  /**
   * @Override public void forEach(@RefAware Consumer<? super T> action) {
   * getInner().forEach(action);
   * }
   * @docgenVersion 9
   */
  @Override
  public void forEach(@RefAware Consumer<? super T> action) {
    getInner().forEach(action);
  }

  /**
   * Performs the given action for each element of this stream, in the order
   * elements appear in the stream, until all elements have been processed or
   * the action throws an exception.  Actions are performed in the order of
   * encounter, if that order is specified, and the results may be unpredictable
   * if the action modifies the stream source.  Unless otherwise specified,
   * elements are processed in the encounter order of the stream source.  If an
   * action encounters an element that is the result of a previous action, the
   * action may process that element again or may terminate with an
   * {@code IllegalStateException}.  A side-effect of the action may be that the
   * source of the stream is modified.  If this is the case, unless the source
   * of the stream is an unmodifiable source, such as an {@code ArrayList}, a
   * {@code ConcurrentModificationException} may be thrown when the terminal
   * operation commences.  This behavior is also specified in the
   * documentation for the {@link Stream#forEach(Consumer)} method.
   *
   * <p>This is a <a href="package-summary.html#StreamOps">terminal
   * operation</a>.
   *
   * <p>When an action is provided as a lambda expression, it is applied as if it
   * were an instance of {@link Consumer}; if the lambda expression throws an
   * exception, the exception is wrapped as an {@link IllegalStateException} when
   * it is thrown by the terminal operation.
   *
   * @param action a <a href="package-summary.html#NonInterference">non-interfering</a>
   *               action to perform on the elements as they are consumed from
   *               the stream
   * @docgenVersion 9
   * @see Stream#forEach(Consumer)
   * @see Stream#forEachOrdered(Consumer)
   */
  @Override
  public void forEachOrdered(@RefAware Consumer<? super T> action) {
    getInner().forEachOrdered(action);
  }

  /**
   * @return an iterator over the elements in this list in proper sequence
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Iterator<T> iterator() {
    return getInner().iterator();
  }

  /**
   * Returns a stream consisting of the elements of this stream, truncated
   * to be no longer than {@code maxSize} in length.
   *
   * @param maxSize the number of elements the resulting stream should be
   *                no longer than
   * @return the new stream
   * @throws IllegalArgumentException if {@code maxSize} is negative
   * @docgenVersion 9
   */
  @Override
  public Stream<T> limit(long maxSize) {
    return getInner().limit(maxSize);
  }

  /**
   * {@inheritDoc}
   *
   * @implSpec The default implementation invokes {@link #getInner()} and returns the
   * result of invoking {@link Stream#map(Function)} on that instance, passing
   * {@code mapper} as the argument.
   * @docgenVersion 9
   */
  @Override
  public <R> Stream<R> map(@RefAware Function<? super T, ? extends R> mapper) {
    return getInner().map(mapper);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation delegates to the {@link #getInner() inner}
   * {@link Stream stream}.
   *
   * @param mapper a {@code ToDoubleFunction} to apply to each element
   * @return a {@code DoubleStream} consisting of the results of applying the
   * given function to the elements of this stream
   * @throws NullPointerException if the mapper is null
   * @docgenVersion 9
   */
  @Override
  public DoubleStream mapToDouble(@RefAware ToDoubleFunction<? super T> mapper) {
    return getInner().mapToDouble(mapper);
  }

  /**
   * {@inheritDoc}
   *
   * @implSpec The default implementation invokes {@link #getInner()} and then
   * {@link IntStream#mapToInt(ToIntFunction) mapToInt} on the inner stream.
   * @docgenVersion 9
   */
  @Override
  public IntStream mapToInt(@RefAware ToIntFunction<? super T> mapper) {
    return getInner().mapToInt(mapper);
  }

  /**
   * {@inheritDoc}
   *
   * @implSpec The default implementation invokes {@link #getInner()} and passes the result to {@link LongStream#mapToLong(ToLongFunction)}.
   * @docgenVersion 9
   */
  @Override
  public LongStream mapToLong(@RefAware ToLongFunction<? super T> mapper) {
    return getInner().mapToLong(mapper);
  }

  /**
   * @return the maximum element according to the provided {@link Comparator},
   * or an empty {@link Optional} if this {@link Stream} is empty
   * @throws NullPointerException if the provided comparator is null
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Optional<T> max(@RefAware Comparator<? super T> comparator) {
    return getInner().max(comparator);
  }

  /**
   * @return the minimum element of this stream according to the provided {@link Comparator},
   * or {@link Optional#empty()} if the stream is empty
   * @throws NullPointerException if the provided comparator is null
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Optional<T> min(@RefAware Comparator<? super T> comparator) {
    return getInner().min(comparator);
  }

  /**
   * {@inheritDoc}
   *
   * @implSpec This implementation delegates to the {@code noneMatch} method
   * of the underlying {@code Stream}.
   * @docgenVersion 9
   */
  @Override
  public boolean noneMatch(@RefAware Predicate<? super T> predicate) {
    return getInner().noneMatch(predicate);
  }

  /**
   * @docgenVersion 9
   * @see java.util.stream.BaseStream#onClose(java.lang.Runnable)
   */
  @Nonnull
  @Override
  public Stream<T> onClose(@RefAware Runnable closeHandler) {
    return getInner().onClose(closeHandler);
  }

  /**
   * Returns a possibly parallel {@code Stream} with this collection as its
   * source.  It is allowable for this method to return a sequential stream.
   *
   * <p>This method should be overridden when the {@link #spliterator()}
   * method cannot return a spliterator that is {@code IMMUTABLE},
   * {@code CONCURRENT}, or <em>late-binding</em>.  (See {@link #spliterator()}
   * for details.)
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Stream<T> parallel() {
    return getInner().parallel();
  }

  /**
   * @docgenVersion 9
   * @see java.util.stream.Stream#peek(java.util.function.Consumer)
   */
  @Override
  public Stream<T> peek(@RefAware Consumer<? super T> action) {
    return getInner().peek(action);
  }

  /**
   * {@inheritDoc}
   *
   * @param identity    the identity value for the accumulating function
   * @param accumulator the accumulating function
   * @return the result of the reduction
   * @docgenVersion 9
   */
  @Override
  public T reduce(@RefAware T identity,
                  @RefAware BinaryOperator<T> accumulator) {
    return getInner().reduce(identity, accumulator);
  }

  /**
   * @docgenVersion 9
   * @see java.util.stream.Stream#reduce(java.util.function.BinaryOperator)
   */
  @Nonnull
  @Override
  public Optional<T> reduce(@RefAware BinaryOperator<T> accumulator) {
    return getInner().reduce(accumulator);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation delegates to the {@link #getInner() inner}
   * {@link Stream Stream}'s {@link Stream#reduce(Object, BiFunction, BinaryOperator)
   * reduce} method.
   *
   * @param identity    the identity value for the accumulating function
   * @param accumulator an {@code BiFunction} that accumulates the intermediate results of the reduction
   * @param combiner    an {@code BinaryOperator} that combines the intermediate results of the reduction
   * @param <U>         the type of the result of the reduction
   * @return the result of the reduction
   * @throws NullPointerException if the identity, accumulator, or combiner is null, or if the inner
   *                              {@code Stream} is null
   * @docgenVersion 9
   */
  @Override
  public <U> U reduce(@RefAware U identity,
                      @RefAware BiFunction<U, ? super T, U> accumulator,
                      @RefAware BinaryOperator<U> combiner) {
    return getInner().reduce(identity, accumulator, combiner);
  }

  /**
   * Returns a sequential {@code Stream} with this collection as its source.
   *
   * @return a sequential {@code Stream} over the elements in this collection
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Stream<T> sequential() {
    return getInner().sequential();
  }

  /**
   * {@inheritDoc}
   *
   * @docgenVersion 9
   */
  @Override
  public Stream<T> skip(long n) {
    return getInner().skip(n);
  }

  /**
   * Returns a stream consisting of the elements of this stream, sorted
   * according to natural order.  If the elements of this stream are not
   * {@code Comparable}, a {@code NullPointerException} may be thrown when
   * the terminal operation is executed.
   *
   * <p>When executed, the {@code sorted} operation behaves as if by invoking
   * the {@link java.util.Collections#sort(java.util.List)} method on a list
   * containing the elements of this stream in encounter order.  The
   * sort is stable.  The specified {@code Comparator} is used to break ties.
   *
   * <p>This is a <a href="package-summary.html#StreamOps">stateless intermediate
   * operation</a>.
   *
   * @return a stream consisting of the elements of this stream, sorted
   * according to natural order
   * @throws NullPointerException if the elements of this stream are not
   *                              {@code Comparable} and the specified comparator is
   *                              {@code null}
   * @docgenVersion 9
   */
  @Override
  public Stream<T> sorted() {
    return getInner().sorted();
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation delegates to {@link #getInner()}.
   *
   * @param comparator the {@code Comparator} to use to sort the elements
   * @return a {@code Stream} that is a sorted view of the elements in this {@code Stream}
   * @throws NullPointerException if {@code comparator} is {@code null}
   * @docgenVersion 9
   */
  @Override
  public Stream<T> sorted(@RefAware Comparator<? super T> comparator) {
    return getInner().sorted(comparator);
  }

  /**
   * @return a {@code Spliterator} over the elements in this {@code Stream}
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Spliterator<T> spliterator() {
    return getInner().spliterator();
  }

  /**
   * @return an array containing all of the elements in this list in proper sequence
   * @throws NullPointerException if the inner list is null
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Object[] toArray() {
    return getInner().toArray();
  }

  /**
   * @param generator a function which produces a new array of the desired type and whose input is the length of the new array
   * @return an array containing all of the elements in this list
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public <A> A[] toArray(@RefAware IntFunction<A[]> generator) {
    return getInner().toArray(generator);
  }

  /**
   * Returns a stream consisting of the elements of this stream, in
   * encounter order if the stream has a defined encounter order.
   *
   * <p>This is a <a href="package-summary.html#StreamOps">terminal
   * operation</a>.
   *
   * @return the corresponding stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Stream<T> unordered() {
    return getInner().unordered();
  }
}
