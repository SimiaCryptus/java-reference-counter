package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.RefUtil;
import com.simiacryptus.lang.ref.ReferenceCounting;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static com.simiacryptus.lang.ref.RefUtil.addRef;

public class RefStream<T> implements Stream<T> {
  private Stream<T> inner;
  private List<ReferenceCounting> lambdas;

  RefStream(Stream<T> stream) {
    this(stream, new ArrayList<>());
    onClose(()-> {
      this.lambdas.forEach(ReferenceCounting::freeRef);
      this.lambdas.clear();
    });
  }

  RefStream(Stream<T> stream, List<ReferenceCounting> lambdas) {
    if (stream instanceof ReferenceCounting) throw new IllegalArgumentException("inner class cannot be ref-aware");
    this.inner = stream;
    this.lambdas = lambdas;
  }

  @Override
  public boolean allMatch(Predicate<? super T> predicate) {
    track(predicate);
    return inner.allMatch((T t) -> predicate.test(addRef(t)));
  }

  private void track(Object... lambda) {
    for (Object l : lambda) {
      if(null != l && l instanceof ReferenceCounting) lambdas.add((ReferenceCounting) l);
    }
  }

  @Override
  public boolean anyMatch(Predicate<? super T> predicate) {
    track(predicate);
    return inner.anyMatch((T t) -> predicate.test(addRef(t)));
  }

  @Override
  public void close() {
    inner.close();
  }

  @Override
  public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
    track(supplier);
    track(accumulator);
    track(combiner);
    return inner.collect(() -> supplier.get(), (R t1, T u1) -> accumulator.accept(addRef(t1), addRef(u1)), (R t, R u) -> combiner.accept(addRef(t), addRef(u)));
  }

  @Override
  public <R, A> R collect(Collector<? super T, A, R> collector) {
    if(collector instanceof ReferenceCounting) {
      final Function<A, R> finisher = collector.finisher();
      track(collector);
      return finisher.apply(this.collect(
          collector.supplier(),
          collector.accumulator(),
          collector.combiner()::apply));
    } else {
      assert false;
      return inner.collect(collector);
    }
  }

  @Override
  public long count() {
    return inner.count();
  }

  @Override
  public RefStream<T> distinct() {
    inner = inner.distinct();
    return this;
  }

  public RefStream<T> filter(Predicate<? super T> predicate) {
    inner = inner.filter((T t) -> predicate.test(addRef(t)));
    track(predicate);
    return this;
  }

  @Override
  public Optional<T> findAny() {
    return inner.findAny().map(RefUtil::addRef);
  }

  @Override
  public Optional<T> findFirst() {
    return RefUtil.wrapInterface(inner.findFirst()).map(RefUtil::addRef);
  }

  @Override
  public <R> RefStream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
    track(mapper);
    return new RefStream<>(inner.flatMap((T t) -> mapper.apply(addRef(t))), lambdas);
  }

  @Override
  public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
    track(mapper);
    return inner.flatMapToDouble((T t) -> mapper.apply(addRef(t)));
  }

  @Override
  public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
    track(mapper);
    return inner.flatMapToInt((T t) -> mapper.apply(addRef(t)));
  }

  @Override
  public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
    track(mapper);
    return inner.flatMapToLong((T t) -> mapper.apply(addRef(t)));
  }

  public void forEach(Consumer<? super T> action) {
    track(action);
    inner.forEach((T t) -> action.accept(addRef(t)));
    close();
  }

  @Override
  public void forEachOrdered(Consumer<? super T> action) {
    track(action);
    inner.forEachOrdered((T t) -> action.accept(addRef(t)));
  }

  @Override
  public boolean isParallel() {
    return inner.isParallel();
  }

  @NotNull
  @Override
  public Iterator<T> iterator() {
    return inner.iterator();
  }

  @Override
  public RefStream<T> limit(long maxSize) {
    inner = inner.limit(maxSize);
    return this;
  }

  @Override
  public <R> RefStream<R> map(Function<? super T, ? extends R> mapper) {
    track(mapper);
    return new RefStream<>(inner.map(mapper), lambdas);
  }

  @Override
  public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
    track(mapper);
    return inner.mapToDouble((T value) -> mapper.applyAsDouble(addRef(value)));
  }

  @Override
  public IntStream mapToInt(ToIntFunction<? super T> mapper) {
    track(mapper);
    return inner.mapToInt((T value) -> mapper.applyAsInt(addRef(value)));
  }

  @Override
  public LongStream mapToLong(ToLongFunction<? super T> mapper) {
    track(mapper);
    return inner.mapToLong((T value) -> mapper.applyAsLong(addRef(value)));
  }

  @Override
  public Optional<T> max(Comparator<? super T> comparator) {
    track(comparator);
    return inner.max((T o1, T o2) -> comparator.compare(addRef(o1), addRef(o2)));
  }

  @Override
  public Optional<T> min(Comparator<? super T> comparator) {
    track(comparator);
    return inner.min((T o1, T o2) -> comparator.compare(addRef(o1), addRef(o2)));
  }

  @Override
  public boolean noneMatch(Predicate<? super T> predicate) {
    track(predicate);
    return inner.noneMatch((T t) -> predicate.test(addRef(t)));
  }

  @NotNull
  @Override
  public RefStream<T> onClose(Runnable closeHandler) {
    inner = inner.onClose(closeHandler);
    track(closeHandler);
    return this;
  }

  @NotNull
  @Override
  public RefStream<T> parallel() {
    inner = inner.parallel();
    return this;
  }

  @Override
  public RefStream<T> peek(Consumer<? super T> action) {
    track(action);
    inner = inner.peek((T t) -> action.accept(addRef(t)));
    return this;
  }

  @Override
  public T reduce(T identity, BinaryOperator<T> accumulator) {
    track(identity);
    track(accumulator);
    return inner.reduce(identity, (T t, T u) -> accumulator.apply(addRef(t), addRef(u)));
  }

  @Override
  public Optional<T> reduce(BinaryOperator<T> accumulator) {
    track(accumulator);
    return inner.reduce((T t, T u) -> accumulator.apply(addRef(t), addRef(u)));
  }

  @Override
  public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
    track(identity);
    track(accumulator);
    track(combiner);
    return inner.reduce(identity,
        (U t1, T u1) -> accumulator.apply(addRef(t1), addRef(u1)),
        (U t, U u) -> combiner.apply(addRef(t), addRef(u)));
  }

  @NotNull
  @Override
  public RefStream<T> sequential() {
    inner = inner.sequential();
    return this;
  }

  @Override
  public RefStream<T> skip(long n) {
    inner = inner.skip(n);
    return this;
  }

  @Override
  public RefStream<T> sorted() {
    inner = inner.sorted();
    return this;
  }

  @Override
  public RefStream<T> sorted(Comparator<? super T> comparator) {
    inner = inner.sorted((T o1, T o2) -> comparator.compare(addRef(o1), addRef(o2)));
    return this;
  }

  @NotNull
  @Override
  public Spliterator<T> spliterator() {
    return new RefSpliterator<>(inner.spliterator());
  }

  @Override
  public Object[] toArray() {
    return inner.toArray();
  }

  @Override
  public <A> A[] toArray(IntFunction<A[]> generator) {
    track(generator);
    return inner.toArray((int value) -> generator.apply(value));
  }

  @NotNull
  @Override
  public RefStream<T> unordered() {
    inner = inner.unordered();
    return this;
  }

}
