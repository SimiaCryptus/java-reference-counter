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
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.*;
import java.util.stream.*;

@RefAware
@RefIgnore
public class StreamWrapper<T> implements Stream<T> {
  private final Stream<T> inner;

  public StreamWrapper(Stream<T> inner) {
    this.inner = inner;
  }

  public Stream<T> getInner() {
    return inner;
  }

  @Override
  public boolean isParallel() {
    return getInner().isParallel();
  }

  @Override
  public boolean allMatch(Predicate<? super T> predicate) {
    return getInner().allMatch(predicate);
  }

  @Override
  public boolean anyMatch(Predicate<? super T> predicate) {
    return getInner().anyMatch(predicate);
  }

  @Override
  public void close() {
    getInner().close();
  }

  @Override
  public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
    return getInner().collect(supplier, accumulator, combiner);
  }

  @Override
  public <R, A> R collect(Collector<? super T, A, R> collector) {
    return getInner().collect(collector);
  }

  @Override
  public long count() {
    return getInner().count();
  }

  @Override
  public Stream<T> distinct() {
    return getInner().distinct();
  }

  @Override
  public Stream<T> filter(Predicate<? super T> predicate) {
    return getInner().filter(predicate);
  }

  @NotNull
  @Override
  public Optional<T> findAny() {
    return getInner().findAny();
  }

  @NotNull
  @Override
  public Optional<T> findFirst() {
    return getInner().findFirst();
  }

  @Override
  public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
    return getInner().flatMap(mapper);
  }

  @Override
  public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
    return getInner().flatMapToDouble(mapper);
  }

  @Override
  public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
    return getInner().flatMapToInt(mapper);
  }

  @Override
  public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
    return getInner().flatMapToLong(mapper);
  }

  @Override
  public void forEach(Consumer<? super T> action) {
    getInner().forEach(action);
  }

  @Override
  public void forEachOrdered(Consumer<? super T> action) {
    getInner().forEachOrdered(action);
  }

  @NotNull
  @Override
  public Iterator<T> iterator() {
    return getInner().iterator();
  }

  @Override
  public Stream<T> limit(long maxSize) {
    return getInner().limit(maxSize);
  }

  @Override
  public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
    return getInner().map(mapper);
  }

  @Override
  public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
    return getInner().mapToDouble(mapper);
  }

  @Override
  public IntStream mapToInt(ToIntFunction<? super T> mapper) {
    return getInner().mapToInt(mapper);
  }

  @Override
  public LongStream mapToLong(ToLongFunction<? super T> mapper) {
    return getInner().mapToLong(mapper);
  }

  @NotNull
  @Override
  public Optional<T> max(Comparator<? super T> comparator) {
    return getInner().max(comparator);
  }

  @NotNull
  @Override
  public Optional<T> min(Comparator<? super T> comparator) {
    return getInner().min(comparator);
  }

  @Override
  public boolean noneMatch(Predicate<? super T> predicate) {
    return getInner().noneMatch(predicate);
  }

  @NotNull
  @Override
  public Stream<T> onClose(Runnable closeHandler) {
    return getInner().onClose(closeHandler);
  }

  @NotNull
  @Override
  public Stream<T> parallel() {
    return getInner().parallel();
  }

  @Override
  public Stream<T> peek(Consumer<? super T> action) {
    return getInner().peek(action);
  }

  @Override
  public T reduce(T identity, BinaryOperator<T> accumulator) {
    return getInner().reduce(identity, accumulator);
  }

  @NotNull
  @Override
  public Optional<T> reduce(BinaryOperator<T> accumulator) {
    return getInner().reduce(accumulator);
  }

  @Override
  public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
    return getInner().reduce(identity, accumulator, combiner);
  }

  @NotNull
  @Override
  public Stream<T> sequential() {
    return getInner().sequential();
  }

  @Override
  public Stream<T> skip(long n) {
    return getInner().skip(n);
  }

  @Override
  public Stream<T> sorted() {
    return getInner().sorted();
  }

  @Override
  public Stream<T> sorted(Comparator<? super T> comparator) {
    return getInner().sorted(comparator);
  }

  @NotNull
  @Override
  public Spliterator<T> spliterator() {
    return getInner().spliterator();
  }

  @NotNull
  @Override
  public Object[] toArray() {
    return getInner().toArray();
  }

  @NotNull
  @Override
  public <A> A[] toArray(IntFunction<A[]> generator) {
    return getInner().toArray(generator);
  }

  @NotNull
  @Override
  public Stream<T> unordered() {
    return getInner().unordered();
  }
}
