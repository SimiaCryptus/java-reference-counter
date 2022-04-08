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
import com.simiacryptus.ref.lang.RefUtil;
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;

/**
 * The RefCollectors class is a utility class that provides static methods for
 * collecting references.
 *
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public class RefCollectors {
  /**
   * Returns a {@link RefCollector} that accumulates the input elements into a
   * new {@link RefList}, in encounter order.*
   *
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> RefCollector<T, ?, RefList<T>> toList() {
    return new RefCollector<>(() -> new RefArrayList<T>(), (RefArrayList<T> list, T element) -> {
      list.add(element);
      list.freeRef();
    }, (RefArrayList<T> left, RefArrayList<T> right) -> {
      left.addAll(right);
      return left;
    }, Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH)));
  }

  /**
   * Returns a {@link RefCollector} that accumulates the input elements into a
   * new {@link RefSet}.
   *
   * @param <T> the type of the input elements
   * @return a {@link RefCollector} which collects all the input elements into a
   * {@link RefSet}, in encounter order
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> RefCollector<T, ?, RefSet<T>> toSet() {
    return new RefCollector<>(() -> new RefHashSet<T>(), (RefHashSet<T> list, T element) -> {
      list.add(element);
      list.freeRef();
    }, (RefHashSet<T> left, RefHashSet<T> right) -> {
      left.addAll(right);
      right.freeRef();
      return left;
    }, Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH)));
  }

  /**
   * Returns a {@link RefCollector} that accumulates elements into a {@link RefMap} whose keys and values are the result of applying the provided mapping functions to the input elements.
   *
   * @param <T>         the type of the input elements
   * @param <K>         the output type of the key mapping function
   * @param <U>         the output type of the value mapping function
   * @param keyMapper   a mapping function to produce keys
   * @param valueMapper a mapping function to produce values
   * @return a {@code RefCollector} which collects elements into a {@code RefMap} with keys and values mapped by the provided mapping functions
   * @throws NullPointerException if the keyMapper or valueMapper is null
   * @docgenVersion 9
   */
  @Nonnull
  public static <T, K, U> RefCollector<T, ?, RefMap<K, U>> toMap(
      @Nonnull @RefAware Function<? super T, ? extends K> keyMapper,
      @Nonnull @RefAware Function<? super T, ? extends U> valueMapper) {
    return toMap(keyMapper, valueMapper, (u, v) -> {
      throw new IllegalStateException(String.format("Duplicate key %s", u));
    }, () -> new RefHashMap<K, U>());
  }

  /**
   * Returns a {@link RefCollector} that accumulates elements into a {@link RefMap} whose keys and values are the result of applying the provided mapping functions to the input elements.
   *
   * @param <T>           the type of the input elements
   * @param <K>           the output type of the key mapping function
   * @param <U>           the output type of the value mapping function
   * @param <M>           the type of the resulting {@link RefMap}
   * @param keyMapper     a mapping function to produce keys
   * @param valueMapper   a mapping function to produce values
   * @param mergeFunction a merge function, used to resolve collisions between values associated with the same key, as supplied to {@link Map#merge(Object, Object, BiFunction)}
   * @param mapSupplier   a {@link Supplier} which returns a new, empty {@link RefMap} into which the results will be inserted
   * @return a {@link RefCollector} which collects elements into a {@link RefMap} whose keys are the result of applying a key mapping function to the input elements, and whose values are the result of applying a value mapping function to all input elements equal to the key and combining them using the merge function
   * @docgenVersion 9
   */
  @Nonnull
  public static <T, K, U, M extends RefMap<K, U>> RefCollector<T, ?, M> toMap(
      @Nonnull @RefAware Function<? super T, ? extends K> keyMapper,
      @Nonnull @RefAware Function<? super T, ? extends U> valueMapper,
      @RefAware BinaryOperator<U> mergeFunction,
      @RefAware Supplier<M> mapSupplier) {
    BiConsumer<M, T> accumulator = RefUtil.wrapInterface((map, element) -> {
      try {
        RefUtil.freeRef(map.merge(
            keyMapper.apply(RefUtil.addRef(element)),
            valueMapper.apply(element),
            RefUtil.addRef(mergeFunction)
        ));
      } finally {
        map.freeRef();
      }
    }, keyMapper, valueMapper, mergeFunction);
    BinaryOperator<M> combiner = RefUtil.wrapInterface((a, b) -> {
      try {
        b.forEach((k, v) -> {
          RefUtil.freeRef(a.merge(k, v, RefUtil.addRef(mergeFunction)));
        });
        return a;
      } finally {
        RefUtil.freeRef(b);
      }
    }, RefUtil.addRef(mergeFunction));
    return new RefCollector<>(
        mapSupplier, accumulator, combiner,
        Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH))
    );
  }

  /**
   * Returns a {@code RefCollector} that groups the input elements according
   * to a classification function, and collects the results in a {@code RefMap}
   * whose keys are the values returned by the classification function and whose
   * values are {@code RefList}s containing the input elements.
   *
   * @param <T>        the type of the input elements
   * @param <K>        the type of the keys in the resulting map
   * @param classifier the classification function
   * @return a {@code RefCollector} implementing the group-by operation
   * @throws NullPointerException if the classification function is null
   * @docgenVersion 9
   */
  @Nonnull
  public static <T, K> RefCollector<T, ?, RefMap<K, RefList<T>>> groupingBy(
      @Nonnull @RefAware RefFunction<? super T, ? extends K> classifier) {
    return groupingBy(classifier, toList());
  }

  /**
   * Returns a {@code RefCollector} implementing a "group by" operation on input elements of type {@code T}, grouping
   * elements according to a classification function, and collecting the results in a {@code RefMap}.
   *
   * <p>The classification function maps elements to some key type {@code K}. The collector produces a {@code RefMap<K, D>}
   * whose keys are the values resulting from applying the classification function to the input elements, and whose
   * corresponding values are the results of collecting all input elements that map to a given key using the downstream
   * collector.
   *
   * <p>There are no guarantees on the type, mutability, serializability, or thread-safety of the {@code RefMap} or
   * {@code RefCollector} objects returned by this method.  Unless otherwise specified, passing a {@code null} argument to
   * any method in this class will cause a {@link NullPointerException} to be thrown.
   *
   * @param <T>        the type of the input elements
   * @param <K>        the type of the keys
   * @param <A>        the intermediate accumulation type of the downstream collector
   * @param <D>        the final result type of the downstream collector
   * @param classifier a classifier function mapping input elements to keys
   * @param downstream a {@code RefCollector} implementing the downstream reduction
   * @return a {@code RefCollector} implementing the intermediate "group by" operation
   * @throws NullPointerException if the classifier or downstream collector is {@code null}
   * @docgenVersion 9
   */
  @Nonnull
  public static <T, K, A, D> RefCollector<T, ?, RefMap<K, D>> groupingBy(
      @Nonnull @RefAware RefFunction<? super T, ? extends K> classifier,
      @Nonnull @RefAware RefCollector<? super T, A, D> downstream) {
    return groupingBy(classifier, () -> new RefHashMap<K, D>(), downstream);
  }

  /**
   * Returns a {@code RefCollector} implementing a "group by" operation on input elements of
   * type {@code T}, grouping elements according to a classification function, and collecting the
   * results in a {@code RefMap} whose keys are the values returned by the classification function
   * and whose values are the results of collecting the input elements in sub-groups whose keys
   * are the values returned by the classification function.
   *
   * <p>If the classification function is applied to an input element {@code t} and the result is
   * {@code k}, and a {@code RefMap<K,A>} is created whose keys are the values of type {@code K}
   * returned by the classification function, then the input element {@code t} is added to the
   * sub-group of elements whose key is {@code k} if and only if the following criteria are met:
   * <ul>
   * <li>the result of invoking {@link RefMap#containsKey(Object)} on the {@code RefMap} is
   * {@code false}, or</li>
   * <li>the result of invoking {@link RefMap#get(Object)} on the {@code RefMap} with key
   * {@code k} is {@code null}, or</li>
   * <li>the result of invoking {@link RefMap#get(Object)} on the {@code RefMap} with key {@code k}
   * is an {@code empty} {@code RefStream}, or</li>
   * <li>the result of invoking {@link RefMap#get(Object)} on the {@code RefMap} with key {@code k}
   * is equivalent to the input element {@code t} according to the {@link Object#equals(Object)}
   * method.</li>
   * </ul>
   *
   * <p>If the {@code RefMap} does not yet contain a sub-group for a given key {@code k}, it is
   * created (as if by invoking the {@link RefMap#computeIfAbsent(Object, java.util.function.Function)}
   * method) and the input element is added to the newly created sub-group.
   *
   * <p>If the {@code RefMap} already contains
   *  a sub-group for a given key {@code k}, the input element is added to that sub-group.
   *
   * <p>The classification function is applied to each input element. Elements are added to the
   * sub-group for a given key only if the classification function returns the same key for every
   * input element.
   *
   * <p>The classification function itself is not invoked on elements that are added to a sub-group
   * for a given key.
   *
   * <p>The order in which elements are added to sub-groups is not specified.
   *
   * <p>The returned {@code RefCollector} stores its results in a {@code RefMap}. There are no
   * guarantees on the type, mutability, serializability, or thread-safety of the {@code RefMap} or
   * {@code RefMap.Entry} objects returned by the {@code RefCollector}.
   *
   * <p>If the downstream collector is {@link RefCollectors#toList() toList()} or
   * {@link RefCollectors#toSet() toSet()}, the resulting {@code RefMap} will have {@code List} or
   * {@code Set} values, respectively.
   *
   * <p>When the input elements are of type {@code Map.Entry<K,V>}, the returned {@code RefMap} will
   * have {@code Map.Entry<K,List<V>>} or {@code Map.Entry<K,Set<V>>} values. In this case, the
   * classification function extracts the key from the input element.
   *
   * <p>If the downstream collector is {@link RefCollectors#toMap() toMap()}, the resulting
   * {@code RefMap} will have the same type as the input {@code RefMap}, and the values in the output
   * {@code RefMap} will be the result of collecting the input values using the downstream collector.
   *
   * <p>If the downstream collector is {@link RefCollectors#toConcurrentMap() toConcurrentMap()},
   * the resulting {@code RefMap} will have a {@code ConcurrentMap} as its type, and the values in the
   * output {@code RefMap} will be the result of collecting the input
   *  values using the downstream collector.
   *
   * <p>If the downstream collector is {@link RefCollectors#toMap() toMap()} with a merge function,
   * the resulting {@code RefMap} will have the same type as the input {@code RefMap}, and the
   * values in the output {@code RefMap} will be the result of collecting the input values using the
   * downstream collector. The merge function is used to resolve collisions between values associated
   * with the same key. The merge function itself is not invoked on elements that are added to a
   * sub-group for a given key.
   *
   * <p>If the downstream collector is {@link RefCollectors#toConcurrentMap() toConcurrentMap()}
   * with a merge function, the resulting {@code RefMap} will have a {@code ConcurrentMap} as its
   * type, and the values in the output {@code RefMap} will be the result of collecting the input
   *  values using the downstream collector. The merge function is used to resolve collisions between
   * values associated with the same key. The merge function itself is not invoked on elements that
   * are added to a sub-group for a given key.
   *
   * <p>If the downstream collector is {@link RefCollectors#toMap() toMap()} with a merge function
   * and a map supplier, the resulting {@code RefMap} will be supplied by the map supplier. The
   * values in the output {@code RefMap} will be the result of collecting the input values using the
   * downstream collector. The merge function is used to resolve collisions between values associated
   * with the same key. The merge function itself is not invoked on elements that are added to a
   * sub-group for a given key.
   *
   * <p>If the downstream collector is {@link RefCollectors#toConcurrentMap() toConcurrentMap()}
   * with a merge function and a map supplier, the resulting {@code RefMap} will be supplied by the
   * map supplier. The values in the output {@code RefMap} will be the result of collecting the input
   * values using the downstream collector. The merge function is used to resolve collisions between
   * <p>
   *  values associated with the same key. The merge function itself is not invoked on elements that are
   * added to a sub-group for a given key.
   *
   * @param <T>        the type of the input elements
   * @param <K>        the type of the keys
   * @param <D>        the type of the downstream reduction result
   * @param <A>        the intermediate accumulation type of the downstream collector
   * @param <M>        the type of the resulting {@code RefMap}
   * @param classifier the classifier function mapping input elements to keys
   * @param mapFactory a {@code Supplier} which returns a new, empty {@code RefMap} into which the
   *                   results will be inserted
   * @param downstream a {@code RefCollector} implementing the downstream reduction
   * @return a {@code RefCollector} implementing the "group by" operation
   * @throws NullPointerException if the classifier is {@code null}, or if the map factory is
   *                              {@code null}, or if the downstream collector is {@code null}
   *                              <p>
   *                              @docgenVersion 9
   */
  @Nonnull
  public static <T, K, D, A, M extends RefMap<K, D>> RefCollector<T, RefMap<K, A>, M> groupingBy(
      @Nonnull @RefAware RefFunction<? super T, ? extends K> classifier,
      @RefAware Supplier<M> mapFactory,
      @Nonnull @RefAware RefCollector<? super T, A, D> downstream) {
    final Supplier<A> downstream_supplier = downstream.supplier();
    final BiConsumer<A, ? super T> downstream_accumulator = downstream.accumulator();
    final BiConsumer<RefMap<K, A>, T> consumer = RefUtil.wrapInterface((map, value) -> {
      A a;
      synchronized (map) {
        a = map.computeIfAbsent(classifier.apply(RefUtil.addRef(value)), k1 -> {
          RefUtil.freeRef(k1);
          return downstream_supplier.get();
        });
      }
      map.freeRef();
      downstream_accumulator.accept(a, value);
    }, classifier, downstream_accumulator, downstream_supplier);
    final Set<Collector.Characteristics> downstream_characteristics = downstream.characteristics();
    final BinaryOperator<RefMap<K, A>> combiner = RefCollectors.mapMerger(downstream.combiner());
    final Supplier<RefMap<K, A>> supplier = (Supplier<RefMap<K, A>>) mapFactory;
    final RefCollector<T, RefMap<K, A>, M> collector;
    if (downstream_characteristics.contains(Collector.Characteristics.IDENTITY_FINISH)) {
      collector = new RefCollector<>(
          supplier,
          consumer,
          combiner,
          Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH)));
    } else {
      final RefFunction<A, D> downstream_finisher = downstream.finisher();
      RefFunction<RefMap<K, A>, M> finisher = RefUtil.wrapInterface((RefMap<K, A> intermediate) -> {
        try {
          return (M) intermediate.mapValues(RefUtil.addRef(downstream_finisher));
        } finally {
          intermediate.freeRef();
        }
      }, downstream_finisher);
      collector = new RefCollector<>(
          supplier,
          consumer,
          combiner,
          finisher,
          Collections.emptySet());
    }
    RefUtil.freeRef(downstream);
    return collector;
  }

  /**
   * Returns a {@link RefCollector} that applies a mapping function to the input elements
   * and then performs a {@link RefCollector} operation on the result.
   *
   * <p>If the mapping function returns {@code null}, the result will be a
   * {@link NullPointerException}.
   *
   * @param <T>        the type of the input elements
   * @param <U>        the type of the mapped elements
   * @param <A>        the mutable accumulation type of the downstream {@link RefCollector}
   * @param <R>        the final result type of the downstream {@link RefCollector}
   * @param mapper     a mapping function to apply to each input element
   * @param downstream a {@link RefCollector} to perform on the mapped elements
   * @return a {@link RefCollector} that performs the given mapping function
   * and then performs the given {@link RefCollector} operation
   * @throws NullPointerException if the mapper or downstream is null
   * @docgenVersion 9
   */
  @Nonnull
  public static <T, U, A, R> RefCollector<T, ?, R> mapping(
      @Nonnull @RefAware Function<? super T, ? extends U> mapper,
      @Nonnull @RefAware RefCollector<? super U, A, R> downstream) {
    BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();
    final RefCollector<T, A, R> collector = new RefCollector<>(
        downstream.supplier(),
        RefUtil.wrapInterface((r, t) -> downstreamAccumulator.accept(r, mapper.apply(t)), downstreamAccumulator, mapper),
        downstream.combiner(),
        downstream.finisher(),
        downstream.characteristics()
    );
    RefUtil.freeRef(downstream);
    return collector;
  }

  /**
   * Returns a {@link RefCollector} which performs the given action after this collector's operation.
   *
   * @param downstream the {@link RefCollector} to use
   * @param finisher   the action to perform after the collector's operation
   * @param <T>        the type of input elements
   * @param <A>        the mutable accumulation type of the {@link RefCollector}
   * @param <R>        the result type of the {@link RefCollector}
   * @param <RR>       the result type of the {@link RefFunction}
   * @return a {@link RefCollector} which performs the given action after this collector's operation
   * @docgenVersion 9
   */
  @Nonnull
  public static <T, A, R, RR> RefCollector<T, A, RR> collectingAndThen(
      @Nonnull @RefAware RefCollector<T, A, R> downstream,
      @Nonnull @RefAware RefFunction<R, RR> finisher) {
    Set<Collector.Characteristics> characteristics = downstream.characteristics();
    if (characteristics.contains(Collector.Characteristics.IDENTITY_FINISH)) {
      if (characteristics.size() == 1)
        characteristics = Collections.emptySet();
      else {
        characteristics = EnumSet.copyOf(characteristics);
        characteristics.remove(Collector.Characteristics.IDENTITY_FINISH);
        characteristics = Collections.unmodifiableSet(characteristics);
      }
    }
    final RefFunction<A, R> downstream_finisher = downstream.finisher();
    final RefCollector<T, A, RR> collector = new RefCollector<>(
        downstream.supplier(),
        downstream.accumulator(),
        downstream.combiner(),
        RefUtil.wrapInterface(downstream_finisher.andThen(finisher), downstream_finisher, finisher),
        characteristics);
    RefUtil.freeRef(downstream);
    return collector;
  }

  /**
   * Returns a {@link RefCollector} that performs a reduction on the input elements.
   *
   * <p>The {@code BinaryOperator} specified as a parameter is used to perform the reduction.
   *
   * @param <T> the type of the input elements
   * @param op  the {@code BinaryOperator} used to reduce the input elements
   * @return a {@link RefCollector} that performs a reduction on the input elements
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> RefCollector<T, OptionalBox<T>, Optional<T>> reducing(
      @Nonnull @RefAware BinaryOperator<T> op) {
    return new RefCollector<T, OptionalBox<T>, Optional<T>>(() -> {
      return new OptionalBox<>(op);
    }, (OptionalBox<T> optionalBox, T t) -> {
      optionalBox.accept(t);
      RefUtil.freeRef(optionalBox);
    }, (OptionalBox<T> a, OptionalBox<T> b) -> {
      if (b.present)
        a.accept(b.getValue());
      b.freeRef();
      return a;
    }, (OptionalBox<T> a) -> {
      final Optional<T> value;
      if (a.present) {
        value = Optional.ofNullable(a.getValue());
      } else {
        value = Optional.empty();
      }
      a.freeRef();
      return value;
    }, Collections.emptySet());
  }

  /**
   * Returns a {@link RefCollector} that produces the sum of a long-valued
   * function applied to the input elements.  If no elements are present,
   * the result is 0.
   *
   * <p>Like {@link #reducing(long, LongUnaryOperator)}, this is a
   * {@link TerminalOp} whose input is unbounded, but has an accumulator
   * function with a fixed signature of type {@code long, T -> long}.
   *
   * <p>The accumulator function itself is often an instance of
   * {@link LongBinaryOperator}, in which case it is often more convenient to
   * use {@link #reducing(LongBinaryOperator)} or
   * {@link #reducing(long, LongBinaryOperator)}.
   *
   * @param <T>      the type of the input elements
   * @param identity the identity value
   * @param mapper   a long-valued mapping function to be applied to each input element
   * @param op       an associative, commutative, non-interfering, stateless function for
   *                 combining two values
   * @return a {@code RefCollector} implementing the summation operation
   * @docgenVersion 9
   * @see #reducing(long, LongUnaryOperator)
   * @see #reducing(LongBinaryOperator)
   * @see #reducing(long, LongBinaryOperator)
   */
  @Nonnull
  public static <T> RefCollector<T, Long[], Long> counting() {
    return reducing(0L, (T e) -> {
      RefUtil.freeRef(e);
      return 1L;
    }, (a, b) -> Long.sum(a, b));
  }

  /**
   * Returns a {@link RefCollector} that accumulates the input elements into a
   * new array, in encounter order, using the provided {@code identity} value and
   * the given {@code mapper} and {@code op} functions.
   *
   * <p>If the mapper function is applied to an input element that is {@code null},
   * the result is the application of the identity function to that element;
   * otherwise, the result is the application of the mapper function to that
   * element.  If the identity function is applied to a {@code null} input element,
   * or the mapper function is applied to a {@code null} input element, the result
   * is {@code null}.  In any case, the result of the binary operator is the result
   * of applying its {@code reduce} method to the two results.
   *
   * <p>The {@code identity} value must be an identity element for the reduction
   * operation and must be non-{@code null}.  Unlike the {@code reduce} operation
   * of an {@code Iterable}, the identity value is returned for an empty input.
   *
   * <p>The {@code mapper} function used for transformation must be
   * non-{@code null}.
   *
   * <p>The {@code op} function used for reduction must be non-{@code null}.
   *
   * @param <T>      the type of the input elements
   * @param <U>      the type of the identity and result elements
   * @param identity the identity element for the reduction operation
   * @param mapper   a non-interfering, stateless function to apply to each input element
   * @param op       a non-interfering, stateless function to apply to the result of the reduction operation
   * @return a {@code RefCollector} implementing the reduction operation
   * @throws NullPointerException if the identity, mapper, or op is {@code null}
   * @apiNote It is common for either the identity element or the result of the reduction
   * to be of the same type as the input elements.  In this case, the collector
   * can be created by passing {@code reducing()} a function that is an identity
   * function on the input element type.  For example, given a stream of
   * {@code Person}, to calculate the sum of their ages, you could use:
   * <pre>{@code
   *     RefCollector<Person, ?, Integer> summingInt = RefCollectors.reducing(
   *             0, Person::getAge, Integer::sum);
   * }</pre>
   *
   * <p>If the result of the reduction is to be a {@code Collection}, it is common
   * to use {@code reducing()} with a {@code BinaryOperator<Collection<T>>} that
   * performs
   * {@code Collection::addAll} on the result of the reduction.  For example, given
   * a stream of {@code Person}, to accumulate their names in a {@code List} in
   * encounter order, you could use:
   * <pre>{@code
   *     RefCollector<Person, ?, List<String>> namesList = RefCollectors.reducing(
   *             new ArrayList<>(), Person::getName, (l, e) -> { l.add(e); return l; });
   * }</pre>
   * @docgenVersion 9
   * @see #reducing(BinaryOperator)
   * @see #reducing(Object, Function)
   * @see #reducing(Object, Function, BinaryOperator, RefCollector.Characteristics...)
   */
  @Nonnull
  public static <T, U> RefCollector<T, U[], U> reducing(@RefAware U identity,
                                                        @Nonnull @RefAware Function<? super T, ? extends U> mapper,
                                                        @Nonnull @RefAware BinaryOperator<U> op) {
    return new RefCollector<>(
        boxSupplier(identity),
        RefUtil.wrapInterface((U[] a, T t) -> {
          a[0] = op.apply(a[0], mapper.apply(t));
        }, mapper, RefUtil.addRef(op)),
        RefUtil.wrapInterface((a, b) -> {
          a[0] = op.apply(a[0], b[0]);
          return a;
        }, op),
        a -> a[0],
        Collections.emptySet());
  }

  /**
   * Returns a {@code RefCollector} that accumulates the input elements into a
   * new {@code String}, in encounter order.
   *
   * @param s the {@code CharSequence} to use as a separator
   * @return a {@code RefCollector} which collects elements into a {@code String},
   * in encounter order
   * @docgenVersion 9
   */
  @Nonnull
  public static RefCollector<CharSequence, ?, String> joining(@Nonnull @RefAware CharSequence s) {
    return joining(s, "", "");
  }

  /**
   * Returns a {@link RefCollector} that accumulates the input elements into a
   * new {@code String}, in encounter order.  The {@code String} is constructed by
   * concatenating the input elements delimited by the specified {@code delimiter},
   * with the {@code prefix} and {@code suffix} applied to the result.
   *
   * <p>If any of the input elements is {@code null}, or if the {@code delimiter},
   * {@code prefix} or {@code suffix} is {@code null}, a {@code NullPointerException}
   * will be thrown.
   *
   * @param <T>       the type of the input elements
   * @param delimiter the delimiter to be used between each element
   * @param prefix    the sequence of characters to be used at the beginning of the
   *                  resulting {@code String}
   * @param suffix    the sequence of characters to be used at the end of the
   *                  resulting {@code String}
   * @return a {@code RefCollector} which collects all the input elements into a
   * {@code String}, in encounter order
   * @throws NullPointerException if any of the input elements is {@code null},
   *                              or if the {@code delimiter}, {@code prefix} or {@code suffix} is
   *                              {@code null}
   * @docgenVersion 9
   */
  @Nonnull
  public static RefCollector<CharSequence, ?, String> joining(
      @Nonnull @RefAware CharSequence delimiter,
      @Nonnull @RefAware CharSequence prefix,
      @Nonnull @RefAware CharSequence suffix) {
    return new RefCollector<>(() -> new StringJoiner(delimiter, prefix, suffix), (stringJoiner2, newElement) -> stringJoiner2.add(newElement), (stringJoiner1, other) -> stringJoiner1.merge(other),
        stringJoiner -> stringJoiner.toString(), Collections.emptySet());
  }

  /**
   * @param <K>           key type
   * @param <V>           value type
   * @param <M>           map type
   * @param mergeFunction merge function
   * @return map merger
   * @docgenVersion 9
   */
  @Nonnull
  private static <K, V, M extends RefMap<K, V>> BinaryOperator<M> mapMerger(
      @Nonnull @RefAware BinaryOperator<V> mergeFunction) {
    return RefUtil.wrapInterface((a, b) -> {
      b.forEach((k, v) -> {
        RefUtil.freeRef(a.merge(k, v, RefUtil.addRef(mergeFunction)));
      });
      b.freeRef();
      return a;
    }, mergeFunction);
  }

  /**
   * Returns a supplier that supplies an array of boxed instances of the given type.
   * The returned supplier is ref-aware and will add a reference to the supplied array.
   *
   * @param <T>      the type of the array to be supplied
   * @param identity the array to be supplied
   * @return a supplier that supplies an array of boxed instances of the given type
   * @docgenVersion 9
   */
  @Nonnull
  @SuppressWarnings("unchecked")
  private static <T> Supplier<T[]> boxSupplier(@RefAware T... identity) {
    return RefUtil.wrapInterface(() -> RefUtil.addRef(identity), identity);
  }

  /**
   * This class is a collector that collects references.
   *
   * @param <A> the type of the result
   * @param <T> the type of the input
   * @param <R> the type of the output
   * @docgenVersion 9
   */
  @RefIgnore
  public static class RefCollector<T, A, R> extends ReferenceCountingBase implements Collector<T, A, R> {
    private final Supplier<A> supplier;
    private final BiConsumer<A, T> accumulator;
    private final BinaryOperator<A> combiner;
    private final RefFunction<A, R> finisher;
    private final Set<Characteristics> characteristics;

    RefCollector(@RefAware Supplier<A> supplier,
                 @RefAware BiConsumer<A, T> accumulator,
                 @RefAware BinaryOperator<A> combiner,
                 @RefAware RefFunction<A, R> finisher,
                 @RefAware Set<Characteristics> characteristics) {
      this.supplier = supplier;
      this.accumulator = accumulator;
      this.combiner = combiner;
      this.finisher = finisher;
      this.characteristics = characteristics;
    }

    RefCollector(@RefAware Supplier<A> supplier,
                 @RefAware BiConsumer<A, T> accumulator,
                 @RefAware BinaryOperator<A> combiner,
                 @RefAware Set<Characteristics> characteristics) {
      this(supplier, accumulator, combiner, i -> (R) i, characteristics);
    }

    /**
     * @return the accumulator function for use in a {@link java.util.stream.Stream Stream}, or
     * {@code null} if one is not provided.
     * @docgenVersion 9
     */
    @Nullable
    @Override
    public BiConsumer<A, T> accumulator() {
      return RefUtil.addRef(accumulator);
    }

    /**
     * @return a set of characteristics of this spliterator
     * @Nullable
     * @docgenVersion 9
     */
    @Nullable
    @Override
    public Set<Characteristics> characteristics() {
      return RefUtil.addRef(characteristics);
    }

    /**
     * @return a {@link BinaryOperator} that can be used to combine two elements of type &lt;A&gt;, or {@code null} if not specified
     * @docgenVersion 9
     */
    @Nullable
    @Override
    public BinaryOperator<A> combiner() {
      return RefUtil.addRef(combiner);
    }

    /**
     * @return a function that finishes the computation and returns the result, or null if no result is needed
     * @docgenVersion 9
     */
    @Nullable
    @Override
    public RefFunction<A, R> finisher() {
      return RefUtil.addRef(finisher);
    }

    /**
     * @return a {@link Supplier} of {@link A} objects, or {@code null} if none is available
     * @docgenVersion 9
     */
    @Nullable
    @Override
    public Supplier<A> supplier() {
      return RefUtil.addRef(supplier);
    }

    /**
     * Frees resources used by this object.
     *
     * @docgenVersion 9
     * @see RefUtil#freeRef(Object)
     */
    @Override
    protected void _free() {
      RefUtil.freeRef(supplier);
      RefUtil.freeRef(accumulator);
      RefUtil.freeRef(combiner);
      RefUtil.freeRef(finisher);
      RefUtil.freeRef(characteristics);
      super._free();
    }
  }

  /**
   * This class represents an optional box that may or may not contain a value.
   * If a value is present, it is accessed using the {@link #get()} method.
   * If no value is present, {@link #get()} will return {@code null}.
   * The class also provides a {@link #isPresent()} method to check if a value is present.
   *
   * @param <T> the type of the optional value
   * @docgenVersion 9
   */
  @RefIgnore
  private static class OptionalBox<T> extends ReferenceCountingBase implements Consumer<T> {
    private final BinaryOperator<T> op;
    @Nullable
    private T value = null;
    private boolean present = false;

    public OptionalBox(BinaryOperator<T> op) {
      this.op = op;
    }

    /**
     * Returns the value of this {@code Optional}, or {@code null} if there is no value present.
     *
     * @return the value of this {@code Optional}, or {@code null} if there is no value present
     * @docgenVersion 9
     */
    @Nullable
    public synchronized T getValue() {
      return RefUtil.addRef(value);
    }

    /**
     * Sets the value of this reference.
     *
     * @param value the new value of this reference
     * @docgenVersion 9
     */
    public synchronized void setValue(@Nullable T value) {
      RefUtil.freeRef(this.value);
      this.value = value;
    }

    /**
     * @Override public synchronized void accept(@RefAware T t) {
     * if (present) {
     * setValue(op.apply(getValue(), t));
     * } else {
     * setValue(t);
     * present = true;
     * }
     * }
     * @docgenVersion 9
     */
    @Override
    public synchronized void accept(@RefAware T t) {
      if (present) {
        setValue(op.apply(getValue(), t));
      } else {
        setValue(t);
        present = true;
      }
    }

    /**
     * Frees resources used by this object.
     *
     * @docgenVersion 9
     * @see Ref#freeRef()
     */
    @Override
    protected void _free() {
      RefUtil.freeRef(op);
      RefUtil.freeRef(value);
      super._free();
    }
  }

}
