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
import com.simiacryptus.ref.lang.RefCoderIgnore;
import com.simiacryptus.ref.lang.RefUtil;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

@RefAware
@RefCoderIgnore
public class RefComparator<T> {

  public static <T extends Comparable<T>> Comparator<? super T> naturalOrder() {
    return (a, b) -> {
      final int result = a.compareTo(RefUtil.addRef(b));
      RefUtil.freeRef(a);
      RefUtil.freeRef(b);
      return result;
    };
  }

  public static <T, U extends Comparable<? super U>> Comparator<? super T> comparing(Function<? super T, ? extends U> fn) {
    return (a, b) -> {
      return fn.apply(a).compareTo(RefUtil.addRef(fn.apply(b)));
    };
  }

  public static <T> Comparator<T> comparingInt(ToIntFunction<? super T> keyExtractor) {
    return Comparator.comparingInt(keyExtractor);
  }

  public static <T> Comparator<T> comparingLong(ToLongFunction<? super T> keyExtractor) {
    return Comparator.comparingLong(keyExtractor);
  }

  public static <T> Comparator<T> comparingDouble(ToDoubleFunction<? super T> keyExtractor) {
    return Comparator.comparingDouble(keyExtractor);
  }
}
