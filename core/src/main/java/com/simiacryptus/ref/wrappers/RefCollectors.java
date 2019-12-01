/*
 * Copyright (c) 2019 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import com.simiacryptus.ref.lang.ReferenceCountingBase;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

@RefAware
public class RefCollectors {
  public static <T> Collector<T, ?, RefList<T>> toList() {
    final Supplier<RefArrayList<T>> supplier = () -> new RefArrayList<T>();
    final BiConsumer<RefArrayList<T>, T> accumulator = (RefArrayList<T> list, T element) -> {
      list.add(element);
      list.freeRef();
    };
    final BinaryOperator<RefArrayList<T>> combiner = (RefArrayList<T> left, RefArrayList<T> right) -> {
      left.addAll(right);
      return left;
    };
    return new RefCollector<>(
        supplier,
        accumulator,
        combiner,
        Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH))
    );
  }

  public static <T> Collector<T, ?, RefSet<T>> toSet() {
    final Supplier<RefHashSet<T>> supplier = () -> new RefHashSet<T>();
    final BiConsumer<RefHashSet<T>, T> accumulator = (RefHashSet<T> list, T element) -> {
      list.add(element);
      list.freeRef();
    };
    final BinaryOperator<RefHashSet<T>> combiner = (RefHashSet<T> left, RefHashSet<T> right) -> {
      left.addAll(right);
      return left;
    };
    return new RefCollector<>(
        supplier,
        accumulator,
        combiner,
        Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH))
    );
  }

  static class RefCollector<T, A, R> extends ReferenceCountingBase implements Collector<T, A, R> {
    private final Supplier<A> supplier;
    private final BiConsumer<A, T> accumulator;
    private final BinaryOperator<A> combiner;
    private final Function<A, R> finisher;
    private final Set<Characteristics> characteristics;

    RefCollector(Supplier<A> supplier,
                 BiConsumer<A, T> accumulator,
                 BinaryOperator<A> combiner,
                 Function<A, R> finisher,
                 Set<Characteristics> characteristics) {
      this.supplier = supplier;
      this.accumulator = accumulator;
      this.combiner = combiner;
      this.finisher = finisher;
      this.characteristics = characteristics;
    }

    RefCollector(Supplier<A> supplier,
                 BiConsumer<A, T> accumulator,
                 BinaryOperator<A> combiner,
                 Set<Characteristics> characteristics) {
      this(supplier, accumulator, combiner, i -> (R) i, characteristics);
    }

    @Override
    public BiConsumer<A, T> accumulator() {
      return accumulator;
    }

    @Override
    public Set<Characteristics> characteristics() {
      return characteristics;
    }

    @Override
    public BinaryOperator<A> combiner() {
      return combiner;
    }

    @Override
    public Function<A, R> finisher() {
      return finisher;
    }

    @Override
    public Supplier<A> supplier() {
      return supplier;
    }
  }
}
