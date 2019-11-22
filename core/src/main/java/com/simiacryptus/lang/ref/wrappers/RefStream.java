package com.simiacryptus.lang.ref.wrappers;

import com.simiacryptus.lang.ref.ReferenceCounting;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class RefStream<T extends ReferenceCounting> {
  private final Stream<T> inner;

  RefStream(Stream<T> stream) {
    this.inner = stream;
  }

  public RefStream<T> filter(Predicate<? super T> predicate) {
    final RefStream<T> result = new RefStream<>(inner.map(x -> (T) x.addRef()).filter(predicate));
    if(predicate instanceof ReferenceCounting) ((ReferenceCounting) predicate).freeRef();
    return result;
  }

  public <R extends ReferenceCounting> RefStream<R> map(Function<? super T, ? extends R> mapper) {
    final RefStream<R> result = new RefStream<>(inner.map(mapper));
    if(mapper instanceof ReferenceCounting) ((ReferenceCounting) mapper).freeRef();
    return result;
  }

  public <A,R> R map(Collector<? super T, A, R> collector) {
    final R result = inner.collect(collector);
    if(collector instanceof ReferenceCounting) ((ReferenceCounting) collector).freeRef();
    return result;
  }

  public void forEach(Consumer<? super T> action) {
    inner.forEach(action);
    if(action instanceof ReferenceCounting) ((ReferenceCounting) action).freeRef();
  }

  public T[] toArray(IntFunction<T[]> generator) {
    final T[] result = inner.toArray(generator);
    if(generator instanceof ReferenceCounting) ((ReferenceCounting) generator).freeRef();
    return result;
  }
}
