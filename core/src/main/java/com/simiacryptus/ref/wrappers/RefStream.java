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
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.*;

/**
 * This class is a wrapper for a Stream that keeps track of how many times each element in the Stream
 * has been referenced.
 *
 * @param <T> the type of element in the Stream
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public class RefStream<T> implements Stream<T> {
  private final Stream<T> inner;
  public List<ReferenceCounting> lambdas;
  public Map<IdentityWrapper<ReferenceCounting>, AtomicInteger> refs;

  public RefStream(@RefAware Stream<T> stream) {
    this(stream, new ArrayList<>(), new ConcurrentHashMap<>());
  }

  RefStream(Stream<T> stream,
            @RefAware List<ReferenceCounting> lambdas,
            @RefAware Map<IdentityWrapper<ReferenceCounting>, AtomicInteger> refs) {
    if (stream instanceof RefStream)
      throw new IllegalArgumentException("inner class cannot be RefStream");
    this.inner = stream.onClose(() -> {
      freeAll(refs);
      synchronized (lambdas) {
        lambdas.forEach(referenceCounting -> referenceCounting.freeRef());
        lambdas.clear();
      }
    });
    this.lambdas = lambdas;
    this.refs = refs;
  }

  /**
   * Returns a stream of the inner type.
   *
   * @return a stream of the inner type
   * @docgenVersion 9
   */
  @Nonnull
  public Stream<T> getInner() {
    return new StreamWrapper<T>(inner) {
      /**
       * @Override
       * @Nonnull
       * public Iterator<T> iterator() {
       *     final Iterator<T> iterator = super.iterator();
       *     if (iterator instanceof RefIterator) {
       *         return ((RefIterator) iterator).getInner();
       *     }
       *     return iterator;
       * }
       *
       *   @docgenVersion 9
       */
      @Override
      public @Nonnull
      Iterator<T> iterator() {
        final Iterator<T> iterator = super.iterator();
        if (iterator instanceof RefIterator) {
          return ((RefIterator) iterator).getInner();
        }
        return iterator;
      }

      /**
       * {@inheritDoc}
       *
       * <p>
       * If the spliterator is an instance of {@link RefSpliterator}, this method
       * returns the inner spliterator. Otherwise, it returns the spliterator as is.
       *
       * @return the inner spliterator, if the spliterator is an instance of
       *         {@link RefSpliterator}, or the spliterator as is
       *
       *   @docgenVersion 9
       */
      @Override
      public @Nonnull
      Spliterator<T> spliterator() {
        final Spliterator<T> spliterator = super.spliterator();
        if (spliterator instanceof RefSpliterator) {
          return ((RefSpliterator) spliterator).getInner();
        }
        return spliterator;
      }
    };
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
   * Creates a {@link RefStream} from a given element.
   *
   * @param <T> the type of the element
   * @param x   the element to create the stream from
   * @return a {@link RefStream} from the given element
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> RefStream<T> of(@RefAware T x) {
    return new RefStream<>(Stream.of(x)).onClose(() -> {
      RefUtil.freeRef(x);
    });
  }

  /**
   * Returns a {@link RefStream} consisting of the elements of the given
   * array. The returned stream is a {@link RefStream} and will be
   * automatically closed when the stream is finished.
   *
   * @param <T>   the type of the array elements
   * @param array the array to be converted to a stream
   * @return a {@link RefStream} consisting of the elements of the given
   * array
   * @throws NullPointerException if the given array is null
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> RefStream<T> of(@Nonnull @RefAware T... array) {
    return new RefStream<>(Stream.of(array).onClose(() -> {
      Arrays.stream(array).forEach(value -> RefUtil.freeRef(value));
    }));
  }

  /**
   * Returns an empty RefStream.
   *
   * @param <T> the type of the stream elements
   * @return an empty RefStream
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> RefStream<T> empty() {
    return new RefStream<>(Stream.empty());
  }

  /**
   * Returns a new stream that is the concatenation of the given streams.
   *
   * @param a   the first stream
   * @param b   the second stream
   * @param <T> the type of stream elements
   * @return the concatenated stream
   * @throws NullPointerException if a or b is null
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> RefStream<T> concat(@Nonnull @RefAware Stream<? extends T> a,
                                        @Nonnull @RefAware Stream<? extends T> b) {
    Stream<? extends T> a1;
    if (a instanceof RefStream) {
      RefStream<? extends T> refStream1 = (RefStream<? extends T>) a;
      a1 = refStream1.getInner();
    } else {
      a1 = a;
    }
    Stream<? extends T> b1;
    if (b instanceof RefStream) {
      RefStream<? extends T> refStream1 = (RefStream<? extends T>) b;
      b1 = refStream1.getInner();
    } else {
      b1 = b;
    }
    RefStream<T> refStream = new RefStream<>(Stream.concat(a1, b1));
    if (a instanceof RefStream) {
      mergeTrackers(refStream, (RefStream<T>) a);
    }
    if (b instanceof RefStream) {
      mergeTrackers(refStream, (RefStream<T>) b);
    }
    return refStream.onClose(() -> {
      a.close();
      b.close();
    });
  }

  /**
   * Frees all resources in the given map.
   *
   * @param refs the map of resources to free
   * @docgenVersion 9
   */
  static void freeAll(
      @Nonnull @RefAware Map<IdentityWrapper<ReferenceCounting>, AtomicInteger> refs) {
    synchronized (refs) {
      refs.forEach((k, v) -> {
        final int cnt = v.getAndSet(0);
        if (cnt > 0)
          for (int i = 0; i < cnt; i++) {
            k.inner.freeRef();
          }
      });
      refs.clear();
    }
  }

  /**
   * Returns a reference to the given object.
   *
   * @param u    the object to get a reference to
   * @param refs a map of references to their respective reference counts
   * @return a reference to the given object
   * @docgenVersion 9
   */
  static <U> U getRef(@RefAware U u,
                      @Nonnull @RefAware Map<IdentityWrapper<ReferenceCounting>, AtomicInteger> refs) {
    Class<?> uClass = null == u ? Object.class : u.getClass();
    if (!RefUtil.isRefAware(uClass)) return u;
    if (u instanceof ReferenceCounting) {
      final AtomicInteger refCnt;
      synchronized (refs) {
        refCnt = refs.computeIfAbsent(new IdentityWrapper(u), x -> new AtomicInteger(0));
      }
      final AtomicBoolean obtained = new AtomicBoolean(false);
      int newStoredRefs = refCnt.updateAndGet(x -> {
        if (x <= 0)
          return 0;
        else {
          obtained.set(true);
          return x - 1;
        }
      });
      if (!obtained.get()) {
        RefUtil.addRef(u);
      }
    } else if (null != u && uClass.isArray()) {
      int length = Array.getLength(u);
      for (int i = 0; i < length; i++) {
        getRef(Array.get(u, i), refs);
      }
    }
    return u;
  }

  /**
   * Stores a reference to the given object in the given map.
   *
   * @param u    the object to store a reference to
   * @param refs the map to store the reference in
   * @param <U>  the type of the object to store a reference to
   * @return the given object
   * @throws NullPointerException if the given map is null
   * @docgenVersion 9
   */
  static <U> U storeRef(@RefAware U u,
                        @Nonnull @RefAware Map<IdentityWrapper<ReferenceCounting>, AtomicInteger> refs) {
    Class<?> uClass = null == u ? Object.class : u.getClass();
    if (!RefUtil.isRefAware(uClass)) return u;
    if (u instanceof ReferenceCounting) {
      AtomicInteger atomicInteger;
      synchronized (refs) {
        atomicInteger = refs.computeIfAbsent(new IdentityWrapper(u), x -> new AtomicInteger(0));
      }
      atomicInteger.incrementAndGet();
    } else if (null != u && uClass.isArray()) {
      int length = Array.getLength(u);
      for (int i = 0; i < length; i++) {
        storeRef(Array.get(u, i), refs);
      }
    }
    return u;
  }

  /**
   * Merges the trackers from the source stream into the destination stream.
   *
   * @param dest   the destination stream
   * @param source the source stream
   * @param <T>    the type of object being tracked
   * @docgenVersion 9
   */
  private static <T> void mergeTrackers(RefStream<T> dest, RefStream<T> source) {
    mergeCountMaps(dest.refs, source.refs);
    dest.lambdas.addAll(source.lambdas);
    source.lambdas.clear();
  }

  /**
   * Merges the source map into the destination map, adding the values of
   * the source map's keys to the destination map's keys.
   *
   * @param dest   the destination map
   * @param source the source map
   * @param <T>    the type of the keys in the maps
   * @docgenVersion 9
   */
  private static <T> void mergeCountMaps(Map<T, AtomicInteger> dest, Map<T, AtomicInteger> source) {
    source.forEach((k, v) -> {
      AtomicInteger atomicInteger;
      synchronized (dest) {
        atomicInteger = dest.computeIfAbsent(k, x -> new AtomicInteger(0));
      }
      atomicInteger.addAndGet(v.get());
    });
    source.clear();
  }

  /**
   * @docgenVersion 9
   * @see Collection#allMatch(Predicate)
   */
  @Override
  public boolean allMatch(@Nonnull @RefAware Predicate<? super T> predicate) {
    track(predicate);
    final boolean match = getInner().allMatch((T t) -> predicate.test(getRef(t)));
    close();
    return match;
  }

  /**
   * @docgenVersion 9
   * @see java.util.stream.Stream#anyMatch(java.util.function.Predicate)
   */
  @Override
  public boolean anyMatch(@Nonnull @RefAware Predicate<? super T> predicate) {
    track(predicate);
    final boolean match = getInner().anyMatch((T t) -> predicate.test(getRef(t)));
    close();
    return match;
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
   * <p>This implementation performs a {@code collect} operation in parallel.
   *
   * @param <R>         the type of the result
   * @param supplier    the supplier function for the result container
   * @param accumulator the accumulator function for incorporating input elements
   * @param combiner    the combiner function for combining two partial results
   * @return the result of the {@code collect} operation
   * @throws NullPointerException if any of the arguments is {@code null}
   * @docgenVersion 9
   */
  @Override
  public <R> R collect(@Nonnull @RefAware Supplier<R> supplier,
                       @Nonnull @RefAware BiConsumer<R, ? super T> accumulator,
                       @Nonnull @RefAware BiConsumer<R, R> combiner) {
    try {
      return getInner().collect(
          () -> supplier.get(),
          (R t1, T u1) -> accumulator.accept(RefUtil.addRef(t1), getRef(u1)),
          (R t, R u) -> combiner.accept(RefUtil.addRef(t), u));
    } finally {
      close();
      RefUtil.freeRef(supplier);
      RefUtil.freeRef(accumulator);
      RefUtil.freeRef(combiner);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>The default implementation performs a full traversal of the tree,
   * invoking the {@code Collector}'s {@code collect} method for each node.
   *
   * @param <R>       the type of the result
   * @param <A>       the mutable accumulation type of the {@code Collector}
   * @param collector the {@code Collector} to invoke for each node
   * @return the result of the {@code collect} operation
   * @throws NullPointerException if the collector is null
   * @docgenVersion 9
   */
  @Override
  public <R, A> R collect(@RefAware Collector<? super T, A, R> collector) {
    if (collector instanceof ReferenceCounting) {
      final Function<A, R> finisher = collector.finisher();
      try {
        return finisher
            .apply(this.collect(collector.supplier(), collector.accumulator(), getBiConsumer(collector.combiner())));
      } finally {
        close();
        RefUtil.freeRef(finisher);
        RefUtil.freeRef(collector);
      }
    } else {
      try {
        return getInner().collect(collector);
      } finally {
        close();
        RefUtil.freeRef(collector);
      }
    }
  }

  /**
   * @Override public long count() {
   * final long count = getInner().count();
   * close();
   * return count;
   * }
   * @docgenVersion 9
   */
  @Override
  public long count() {
    final long count = getInner().count();
    close();
    return count;
  }

  /**
   * Returns a stream consisting of the distinct elements of this stream.
   *
   * @return a stream consisting of the distinct elements of this stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefStream<T> distinct() {
    return new RefStream(getInner().distinct(), lambdas, refs);
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
  public RefStream<T> filter(@Nonnull @RefAware Predicate<? super T> predicate) {
    track(predicate);
    return new RefStream(getInner().filter(t -> predicate.test(RefUtil.addRef(t))), lambdas, refs);
  }

  /**
   * @return an {@link Optional} containing any element of this {@link Stream},
   * or an empty {@link Optional} if the stream is empty
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Optional<T> findAny() {
    final Optional<T> optional = getInner().findAny().map(value -> RefUtil.addRef(value));
    close();
    return optional;
  }

  /**
   * @return an {@code Optional} describing the first element of this stream,
   * or an empty {@code Optional} if the stream is empty
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Optional<T> findFirst() {
    final Optional<T> optional = getInner().findFirst().map(value -> RefUtil.addRef(value));
    close();
    return optional;
  }

  /**
   * Returns a {@link RefStream} consisting of the results of replacing each element of
   * this stream with the contents of a mapped stream produced by applying
   * the provided mapping function to each element.  Each mapped stream is
   * {@link RefStream} and is closed after its contents have been placed into this
   * stream.  (If a mapped stream is {@null}, this is treated as returning
   * an empty stream.)
   *
   * <p>The {@code flatMap} operation has the effect of applying a one-to-many
   * transformation to the elements of the stream, and then flattening the
   * resulting elements into a new stream.
   *
   * <p>This is an {@link RefStream} operation.
   *
   * @param <R>    The element type of the new stream
   * @param mapper a mapping function to apply to each element, producing a stream
   *               of new values
   * @return the new stream
   * @throws NullPointerException if the mapping function is {@code null}
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public <R> RefStream<R> flatMap(
      @Nonnull @RefAware Function<? super T, ? extends Stream<? extends R>> mapper) {
    RefStream<T> self = this;
    track(mapper);
    return new RefStream<>(getInner().flatMap((T t) -> {
          Stream<? extends R> stream = mapper.apply(getRef(t));
          if (stream instanceof RefStream) {
            RefStream<? extends R> refStream = (RefStream<? extends R>) stream;
            return refStream.getInner().map(u -> self.storeRef(refStream.getRef(u)));
          } else {
            return stream.map(u -> self.storeRef(u));
          }
        }
        //.collect(RefCollectors.toList()).stream()
    ), lambdas, refs);
  }

  /**
   * Returns a {@code DoubleStream} consisting of the results of replacing each element of
   * this stream with the contents of a mapped stream produced by applying
   * the provided mapping function to each element.  Each mapped stream is
   * {@link java.util.stream.BaseStream#close() closed} after its contents
   * have been placed into this stream.  (If a mapped stream is {@code null}
   * an empty stream is used, instead.)
   *
   * <p>The {@code flatMapToDouble} operation has the effect of applying a one-to-many
   * transformation to the elements of the stream, and then flattening the
   * resulting elements into a new stream.
   *
   * <p><b>Implementation Requirements:</b><br>
   * The default implementation invokes {@link #flatMap(Function)} and then
   * flattens the element streams into a double-valued {@code Stream} using
   * {@link java.util.stream.Stream#flatMapToDouble(Function) flatMapToDouble}.
   *
   * @param <T>    The type of the stream elements
   * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
   *               <a href="package-summary.html#Statelessness">stateless</a>
   *               function to apply to each element which produces a stream
   *               of new values
   * @return the new stream
   * @docgenVersion 9
   * @see Stream#flatMap(Function)
   * @see Stream#flatMapToInt(Function)
   * @see Stream#flatMapToLong(Function)
   */
  @Nonnull
  @Override
  public RefDoubleStream flatMapToDouble(
      @Nonnull @RefAware Function<? super T, ? extends DoubleStream> mapper) {
    track(mapper);
    return new RefDoubleStream(getInner().flatMapToDouble((T t) -> mapper.apply(getRef(t))), lambdas, refs);
  }

  /**
   * Returns a new {@code RefIntStream} that is the result of flat-mapping this stream with the given {@code mapper} function.
   * The {@code mapper} function produces an {@code IntStream} for each element in this stream, and the resulting streams are concatenated.
   * The resulting stream is then flat-mapped.
   * <p>
   * This is an intermediate operation.
   *
   * @param mapper a non-interfering, stateless function that produces an {@code IntStream} for each element in this stream
   * @return the new stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefIntStream flatMapToInt(
      @Nonnull @RefAware Function<? super T, ? extends IntStream> mapper) {
    track(mapper);
    return new RefIntStream(getInner().flatMapToInt((T t) -> mapper.apply(getRef(t))), lambdas, refs);
  }

  /**
   * Returns a {@code RefLongStream} consisting of the results of replacing each element of
   * this stream with the contents of a mapped stream produced by applying
   * the provided mapping function to each element. Each mapped stream is
   * {@link java.util.stream.BaseStream#close() closed} after its contents
   * have been placed into this stream. (If a mapped stream is {@code null}
   * an empty stream is used, instead.)
   *
   * <p>The {@code flatMapToLong} operation has the effect of applying a one-to-many
   * transformation to the elements of the stream, and then flattening the
   * resulting elements into a new stream.
   *
   * <p><b>Implementation Requirements:</b><br>
   * The default implementation invokes {@link #flatMap(Function) flatMap} and casts the
   * result to a {@code RefLongStream}.
   *
   * @param <T>    The type of the stream elements
   * @param mapper a non-interfering, stateless function to apply to each element
   * @return the new stream
   * @docgenVersion 9
   * @see #flatMap(Function)
   */
  @Nonnull
  @Override
  public RefLongStream flatMapToLong(
      @Nonnull @RefAware Function<? super T, ? extends LongStream> mapper) {
    track(mapper);
    return new RefLongStream(getInner().flatMapToLong((T t) -> {
      final T ref = getRef(t);
      return mapper.apply(ref);
    }), lambdas, refs);
  }

  /**
   * Performs the given action for each element of the Iterable until all elements
   * have been processed or the action throws an exception.  Exceptions thrown by
   * the action are relayed to the caller.
   *
   * @param action The action to be performed for each element
   * @throws NullPointerException if the specified action is null
   * @docgenVersion 9
   */
  public void forEach(@Nonnull @RefAware Consumer<? super T> action) {
    track(action);
    getInner().forEach((T t) -> action.accept(getRef(t)));
    close();
  }

  /**
   * Performs the given action for each element of this stream, in the order
   * elements appear in the stream, until all elements have been processed or
   * the action throws an exception.  Actions are performed in the order of
   * encounter, if that order is specified.  Exceptions thrown by the action
   * are relayed to the caller.
   *
   * @param action The action to be performed for each element
   * @throws NullPointerException if the action is null
   * @implSpec The default implementation behaves as if:
   * <pre>{@code
   *     for (T t : this stream)
   *         action.accept(t);
   * }</pre>
   * @docgenVersion 9
   */
  @Override
  public void forEachOrdered(@Nonnull @RefAware Consumer<? super T> action) {
    track(action);
    getInner().forEachOrdered((T t) -> action.accept(getRef(t)));
    close();
  }

  /**
   * @return an iterator over the elements in this stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefIterator<T> iterator() {
    return new RefIterator<>(getInner().iterator()).track(new ReferenceCountingBase() {
      /**
       * This method closes the RefStream.
       *
       *   @docgenVersion 9
       */
      @Override
      protected void _free() {
        RefStream.this.close();
      }
    });
  }

  /**
   * @return a new RefStream consisting of the elements from this RefStream,
   * truncated to be no longer than {@code maxSize} in length
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefStream<T> limit(long maxSize) {
    return new RefStream(getInner().limit(maxSize), lambdas, refs);
  }

  /**
   * Returns a {@link RefStream} consisting of the results of applying the given
   * function to the elements of this stream.
   *
   * <p>If the mapper function is {@link RefAware}, the resulting stream will
   * contain references to the mapped elements. Otherwise, the resulting stream
   * will not contain references to the mapped elements.
   *
   * @param mapper a function to apply to each element
   * @param <R>    the element type of the new stream
   * @return the new stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public <R> RefStream<R> map(@Nonnull @RefAware Function<? super T, ? extends R> mapper) {
    track(mapper);
    return new RefStream<>(getInner().map(t -> storeRef(mapper.apply(getRef(t)))), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * This implementation tracks the given {@link ToDoubleFunction} as a reference.
   *
   * @param mapper the {@code ToDoubleFunction} to apply to each element
   * @return the new stream
   * @docgenVersion 9
   * @see #mapToDouble(ToDoubleFunction)
   */
  @Nonnull
  @Override
  public RefDoubleStream mapToDouble(@Nonnull @RefAware ToDoubleFunction<? super T> mapper) {
    track(mapper);
    return new RefDoubleStream(getInner().mapToDouble((T value) -> mapper.applyAsDouble(getRef(value))), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * This implementation tracks the given {@code mapper} function using the
   * {@link RefStreams#track(Function)} method.
   *
   * @param <T>    the type of the stream elements
   * @param mapper the mapper function used to apply to each element
   * @return the new stream
   * @throws NullPointerException if the given {@code mapper} is {@code null}
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefIntStream mapToInt(@Nonnull @RefAware ToIntFunction<? super T> mapper) {
    track(mapper);
    return new RefIntStream(getInner().mapToInt((T value) -> mapper.applyAsInt(getRef(value))), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * This implementation tracks the given {@code mapper} function in a {@link Ref}
   * object.
   *
   * @param <T>    the type of the stream elements
   * @param mapper the mapping function to apply to each element
   * @return the new stream
   * @throws NullPointerException if the given {@code mapper} is {@code null}
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefLongStream mapToLong(@Nonnull @RefAware ToLongFunction<? super T> mapper) {
    track(mapper);
    return new RefLongStream(getInner().mapToLong((T value) -> mapper.applyAsLong(getRef(value))), lambdas, refs);
  }

  /**
   * {@inheritDoc}
   *
   * @param comparator the {@code Comparator} used to compare the elements, provided
   *                   as a {@code @RefAware} object to prevent leaking references
   * @return an {@code Optional} describing the maximum element of this stream,
   * or an empty {@code Optional} if the stream is empty
   * @throws NullPointerException if the given comparator is null
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Optional<T> max(@Nonnull @RefAware Comparator<? super T> comparator) {
    track(comparator);
    final Optional<T> optional = getInner()
        .max((T o1, T o2) -> comparator.compare(RefUtil.addRef(o1), RefUtil.addRef(o2))).map(u -> getRef(u));
    close();
    return optional;
  }

  /**
   * {@inheritDoc}
   *
   * @param comparator the {@code Comparator} used to compare the elements,
   *                   provided as a {@code @RefAware} object to prevent
   *                   accidental use of unreferenced objects
   * @return an {@code Optional} describing the minimum element of this stream,
   * or an empty {@code Optional} if the stream is empty
   * @throws NullPointerException if the provided comparator is {@code null}
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Optional<T> min(@Nonnull @RefAware Comparator<? super T> comparator) {
    track(comparator);
    final Optional<T> optional = getInner()
        .min((T o1, T o2) -> comparator.compare(RefUtil.addRef(o1), RefUtil.addRef(o2))).map(u -> getRef(u));
    close();
    return optional;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This default implementation iterates over the elements in the {@code Iterable},
   * testing each element in turn for a match with the given {@code predicate}.  If
   * any element returns {@code true} for the predicate, this method immediately
   * returns {@code false}.  If either the {@code Iterable} or the {@code predicate}
   * is {@code null}, a {@code NullPointerException} is thrown.
   *
   * @param predicate a non-null {@code Predicate} to apply to elements of this {@code Iterable}
   * @return {@code true} if a single element of this {@code Iterable} does not match the given {@code predicate},
   * otherwise {@code false}
   * @throws NullPointerException if the given {@code predicate} is {@code null}
   * @docgenVersion 9
   * @see #anyMatch(Predicate)
   * @see #allMatch(Predicate)
   */
  @Override
  public boolean noneMatch(@Nonnull @RefAware Predicate<? super T> predicate) {
    track(predicate);
    //    final boolean match = !getInner().map((T t) -> predicate.test(getRef(t))).reduce((a, b)->a || b).orElse(false);
    final boolean match = getInner().noneMatch((T t) -> predicate.test(getRef(t)));
    close();
    return match;
  }

  /**
   * Returns a new Stream that is the same as this Stream, but that will run the given closeHandler when it is closed.
   * The closeHandler will be tracked for reference leaks.
   *
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefStream<T> onClose(@RefAware Runnable closeHandler) {
    track(closeHandler);
    return new RefStream(getInner().onClose(closeHandler), lambdas, refs);
  }

  /**
   * Returns a parallel {@code Stream} with this stream.
   *
   * @return a parallel {@code Stream} with this stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefStream<T> parallel() {
    return new RefStream(getInner().parallel(), lambdas, refs);
  }

  /**
   * Returns a stream consisting of the elements of this stream, additionally
   * performing the provided action on each element as elements are consumed
   * from the resulting stream.
   *
   * @param action a non-interfering action to perform on the elements as they are
   *               consumed from the stream
   * @return the new stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefStream<T> peek(@Nonnull @RefAware Consumer<? super T> action) {
    track(action);
    return new RefStream(getInner().peek((T t) -> action.accept(getRef(t))), lambdas, refs);
  }

  /**
   * @param identity    the identity value for the accumulating function
   * @param accumulator an associative, non-interfering, stateless function for
   *                    combining two values
   * @return the result of the reduction
   * @throws NullPointerException if the identity or accumulator is null
   * @docgenVersion 9
   */
  @Nullable
  @Override
  public T reduce(@RefAware T identity,
                  @Nonnull @RefAware BinaryOperator<T> accumulator) {
    track(accumulator);
    try {
      return RefUtil
          .addRef(getInner().reduce(storeRef(identity), (T t, T u) -> storeRef(accumulator.apply(getRef(t), getRef(u)))));
    } finally {
      close();
    }
  }

  /**
   * @docgenVersion 9
   * @see java.util.stream.Stream#reduce(java.util.function.BinaryOperator)
   */
  @Nonnull
  @Override
  public Optional<T> reduce(@Nonnull @RefAware BinaryOperator<T> accumulator) {
    track(accumulator);
    try {
      return getInner().reduce((T t, T u) -> storeRef(accumulator.apply(getRef(t), getRef(u))))
          .map(u1 -> getRef(u1));
    } finally {
      close();
    }
  }

  /**
   * Performs a reduction on the elements of this stream, using the provided identity, accumulator and combiner functions. This is an intermediate operation.
   *
   * <p>The {@code identity} function is used to provide an initial value for the reduction. It is also used to provide a default result if there are no input elements, in which case the {@code identity} function is also used as the combiner function.
   *
   * <p>The {@code accumulator} function takes two input arguments, the first being the current value (or identity) and the second being an element from the stream. It produces a new result by combining the current value with the stream element.
   *
   * <p>The {@code combiner} function takes two input arguments, the first being the result of the previous accumulation and the second being the result of the accumulation of the next element in the stream. It produces a new combined result.
   *
   * <p>If the stream is empty, the reduction operation will return the identity value.
   *
   * <p>If the stream has only one element, the reduction operation will return that element (regardless of the combiner function). Otherwise, the combiner function is used to combine the result of the previous accumulation with the result of the accumulation of the next element in the stream.
   *
   * <p>The {@code combiner} function must be associative in order to produce a correct result. This means that {@code combiner(a, combiner(b, c))} is equivalent to {@code combiner(combiner(a, b), c)}.
   *
   * <p>The {@code combiner} function must also be compatible with the {@code accumulator} function; that is, {@code combiner(u, accumulator(identity, t))} must equal {@code accumulator(u, t)}.
   *
   * <p>The {@code identity} value must also be compatible with the {@code accumulator} and {@code combiner} functions; that is, {@code combiner(identity, accumulator(identity, t))} must equal {@code accumulator(identity, t)} and {@code accumulator(u, identity)} must equal {@code u}.
   *
   * <p>This is
   * a terminal operation.
   *
   * @param identity    the identity value for the accumulating function
   * @param accumulator an associative, non-interfering, stateless function for combining two values
   * @param combiner    an associative, non-interfering, stateless function for combining two values
   * @return the result of the reduction
   * @throws NullPointerException if the identity, accumulator or combiner is null
   * @docgenVersion 9
   */
  @Override
  public <U> U reduce(@RefAware U identity,
                      @Nonnull @RefAware BiFunction<U, ? super T, U> accumulator,
                      @Nonnull @RefAware BinaryOperator<U> combiner) {
    track(accumulator);
    track(combiner);
    final U result = getRef(
        getInner().reduce(storeRef(identity), (U t1, T u1) -> storeRef(accumulator.apply(getRef(t1), getRef(u1))),
            (U t, U u) -> storeRef(combiner.apply(getRef(t), getRef(u)))));
    close();
    return result;
  }

  /**
   * Returns a sequential {@code Stream} with this stream as its source.
   *
   * @return a sequential {@code Stream} over the elements of this stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefStream<T> sequential() {
    return new RefStream(getInner().sequential(), lambdas, refs);
  }

  /**
   * Returns a new Stream that skips the first {@code n} elements of this Stream.
   * If this Stream has fewer than {@code n} elements, then an empty Stream is returned.
   *
   * <p>This is an intermediate operation.
   *
   * @param n the number of elements to skip
   * @return the new Stream
   * @throws IllegalArgumentException if {@code n} is negative
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefStream<T> skip(long n) {
    return new RefStream(getInner().skip(n), lambdas, refs);
  }

  /**
   * Returns a stream consisting of the elements of this stream, sorted
   * according to natural order.  When encountering multiple elements equal in
   * natural order, they will be preserved in the order they were first
   * encountered in the source.
   *
   * <p>This is a {@link TerminalOp} which expects a source {@linkplain Stream#sorted() unsorted}
   * {@code Stream} and returns a {@code Stream} with the same elements, sorted
   * according to their natural order.
   *
   * @return the new stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefStream<T> sorted() {
    return new RefStream(getInner().sorted((a, b) -> ((Comparable<T>) a).compareTo(RefUtil.addRef(b))), lambdas, refs);
  }

  /**
   * Returns a stream consisting of the elements of this stream, sorted
   * according to the provided {@link Comparator}.
   *
   * <p>If the elements of this stream are not {@link Comparable}, a
   * {@link Comparator} must be provided that defines the sort order.
   *
   * <p>For ordered streams, the sort is stable.  For unordered streams, no
   * stability guarantees are made.
   *
   * <p><b>Implementation Note:</b> This implementation is a stable, adaptive,
   * iterative mergesort that requires far fewer than n lg(n) comparisons when
   * the input array is partially sorted, while offering the performance of a
   * traditional mergesort when the input array is randomly ordered.  If the
   * input array is nearly sorted, the implementation requires approximately
   * n comparisons.  Temporary storage requirements vary from a small constant
   * for nearly sorted input arrays to n/2 object references for randomly
   * ordered input arrays.
   *
   * <p>The implementation takes equal advantage of ascending and descending
   * order in its input array, and can take advantage of ascending and
   * descending order in different parts of the same input array.  It is
   * well-suited to merging two or more sorted arrays.  The implementation is
   * stable and runs in O(n log(n)) time for any comparator.
   *
   * @param comparator a {@code Comparator} for comparing {@code T} objects
   * @return the new stream
   * @docgenVersion 9
   * @since 1.8
   */
  @Nonnull
  @Override
  public RefStream<T> sorted(@Nonnull @RefAware Comparator<? super T> comparator) {
    return new RefStream(
        getInner().sorted(
            (T o1, T o2) -> comparator.compare(RefUtil.addRef(o1), RefUtil.addRef(o2))
        ), lambdas, refs).track(comparator);
  }

  /**
   * @return a {@code RefSpliterator} over the elements in this {@code RefStream}
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefSpliterator<T> spliterator() {
    final Spliterator<T> spliterator = getInner().spliterator();
    final RefSpliterator<T> refSpliterator;
    if (spliterator instanceof RefSpliterator) {
      refSpliterator = ((RefSpliterator<T>) spliterator).addRef();
    } else {
      refSpliterator = new RefSpliterator<>(spliterator);
    }
    return refSpliterator.track(new ReferenceCountingBase() {
      /**
       * This method closes the RefStream.
       *
       *   @docgenVersion 9
       */
      @Override
      protected void _free() {
        RefStream.this.close();
      }
    });
  }

  /**
   * Stores a reference to the specified object in the {@code refs} map.
   *
   * @param u the object to store a reference to
   * @return the object that was stored
   * @docgenVersion 9
   */
  public <U> U storeRef(@RefAware U u) {
    return storeRef(u, refs);
  }

  /**
   * Returns an array containing all of the elements in this list in proper sequence
   * (from first to last element).
   *
   * @return an array containing all of the elements in this list in proper sequence
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public Object[] toArray() {
    final Object[] array = getInner().map(u -> getRef(u)).toArray();
    close();
    return array;
  }

  /**
   * @param generator The function that generates an array of the appropriate type and size.
   * @return An array of the appropriate type and size containing the elements of this stream.
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public <A> A[] toArray(@Nonnull @RefAware IntFunction<A[]> generator) {
    track(generator);
    final A[] array = getInner().map(u -> getRef(u)).toArray(value -> generator.apply(value));
    close();
    return array;
  }

  /**
   * Returns an unordered stream consisting of the elements of this stream.
   *
   * @return the unordered stream
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefStream<T> unordered() {
    return new RefStream(getInner().unordered(), lambdas, refs);
  }

  /**
   * @param lambda the lambdas to track
   * @return the RefStream
   * @docgenVersion 9
   */
  @Nonnull
  RefStream<T> track(@Nonnull @RefAware Object... lambda) {
    for (Object l : lambda) {
      if (null != l && l instanceof ReferenceCounting)
        lambdas.add((ReferenceCounting) l);
    }
    return this;
  }

  /**
   * Returns a {@link BiConsumer} that wraps the given {@link BinaryOperator}.
   *
   * @param combiner the {@link BinaryOperator} to wrap
   * @return a {@link BiConsumer} that wraps the given {@link BinaryOperator}
   * @docgenVersion 9
   */
  @Nonnull
  @RefAware
  private <A> BiConsumer<A, A> getBiConsumer(@Nonnull @RefAware BinaryOperator<A> combiner) {
    return RefUtil.wrapInterface((t, u) -> storeRef(combiner.apply(t, u)), combiner);
  }

  /**
   * Returns a reference to the specified object.
   *
   * @param u the object to get a reference to
   * @return a reference to the specified object
   * @docgenVersion 9
   */
  private <U> U getRef(@RefAware U u) {
    return getRef(u, this.refs);
  }

  /**
   * A simple wrapper class that holds a reference to an object of type T.
   *
   * @param <T> the type of the object being wrapped
   * @docgenVersion 9
   */
  @RefIgnore
  public static class IdentityWrapper<T> {
    public final T inner;

    public IdentityWrapper(@RefAware T inner) {
      this.inner = inner;
    }

    /**
     * Returns true if the given object is equal to this one.
     *
     * @param o the object to compare to this one
     * @return true if the given object is equal to this one
     * @docgenVersion 9
     */
    @Override
    public boolean equals(@Nullable @RefAware Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      IdentityWrapper that = (IdentityWrapper) o;
      return inner == that.inner;
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hash tables such as those provided by
     * {@link java.util.HashMap}.
     *
     * @return a hash code value for this object.
     * @docgenVersion 9
     */
    @Override
    public int hashCode() {
      return System.identityHashCode(inner);
    }
  }
}
