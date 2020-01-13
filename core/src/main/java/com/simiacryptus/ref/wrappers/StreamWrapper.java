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

@RefIgnore
public class StreamWrapper<T> implements Stream<T> {
  private final Stream<T> inner;

  public StreamWrapper(@RefAware Stream<T> inner) {
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
  public boolean allMatch(@RefAware Predicate<? super T> predicate) {
    return getInner().allMatch(predicate);
  }

  @Override
  public boolean anyMatch(@RefAware Predicate<? super T> predicate) {
    return getInner().anyMatch(predicate);
  }

  @Override
  public void close() {
    getInner().close();
  }

  @Override
  public <R> R collect(@RefAware Supplier<R> supplier,
                       @RefAware BiConsumer<R, ? super T> accumulator,
                       @RefAware BiConsumer<R, R> combiner) {
    return getInner().collect(supplier, accumulator, combiner);
  }

  @Override
  public <R, A> R collect(@RefAware Collector<? super T, A, R> collector) {
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
  public Stream<T> filter(@RefAware Predicate<? super T> predicate) {
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
  public <R> Stream<R> flatMap(
      @RefAware Function<? super T, ? extends Stream<? extends R>> mapper) {
    return getInner().flatMap(mapper);
  }

  @Override
  public DoubleStream flatMapToDouble(
      @RefAware Function<? super T, ? extends DoubleStream> mapper) {
    return getInner().flatMapToDouble(mapper);
  }

  @Override
  public IntStream flatMapToInt(@RefAware Function<? super T, ? extends IntStream> mapper) {
    return getInner().flatMapToInt(mapper);
  }

  @Override
  public LongStream flatMapToLong(
      @RefAware Function<? super T, ? extends LongStream> mapper) {
    return getInner().flatMapToLong(mapper);
  }

  @Override
  public void forEach(@RefAware Consumer<? super T> action) {
    getInner().forEach(action);
  }

  @Override
  public void forEachOrdered(@RefAware Consumer<? super T> action) {
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
  public <R> Stream<R> map(@RefAware Function<? super T, ? extends R> mapper) {
    return getInner().map(mapper);
  }

  @Override
  public DoubleStream mapToDouble(@RefAware ToDoubleFunction<? super T> mapper) {
    return getInner().mapToDouble(mapper);
  }

  @Override
  public IntStream mapToInt(@RefAware ToIntFunction<? super T> mapper) {
    return getInner().mapToInt(mapper);
  }

  @Override
  public LongStream mapToLong(@RefAware ToLongFunction<? super T> mapper) {
    return getInner().mapToLong(mapper);
  }

  @NotNull
  @Override
  public Optional<T> max(@RefAware Comparator<? super T> comparator) {
    return getInner().max(comparator);
  }

  @NotNull
  @Override
  public Optional<T> min(@RefAware Comparator<? super T> comparator) {
    return getInner().min(comparator);
  }

  @Override
  public boolean noneMatch(@RefAware Predicate<? super T> predicate) {
    return getInner().noneMatch(predicate);
  }

  @NotNull
  @Override
  public Stream<T> onClose(@RefAware Runnable closeHandler) {
    return getInner().onClose(closeHandler);
  }

  @NotNull
  @Override
  public Stream<T> parallel() {
    return getInner().parallel();
  }

  @Override
  public Stream<T> peek(@RefAware Consumer<? super T> action) {
    return getInner().peek(action);
  }

  @Override
  public T reduce(@RefAware T identity,
                  @RefAware BinaryOperator<T> accumulator) {
    return getInner().reduce(identity, accumulator);
  }

  @NotNull
  @Override
  public Optional<T> reduce(@RefAware BinaryOperator<T> accumulator) {
    return getInner().reduce(accumulator);
  }

  @Override
  public <U> U reduce(@RefAware U identity,
                      @RefAware BiFunction<U, ? super T, U> accumulator,
                      @RefAware BinaryOperator<U> combiner) {
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
  public Stream<T> sorted(@RefAware Comparator<? super T> comparator) {
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
  public <A> A[] toArray(@RefAware IntFunction<A[]> generator) {
    return getInner().toArray(generator);
  }

  @NotNull
  @Override
  public Stream<T> unordered() {
    return getInner().unordered();
  }
}
