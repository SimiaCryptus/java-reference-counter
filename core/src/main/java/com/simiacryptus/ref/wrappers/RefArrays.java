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
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;

@RefAware
@RefCoderIgnore
public class RefArrays {
  public static <T> RefStream<T> stream(@NotNull T[] array) {
    return new RefStream<>(Arrays.stream(array).onClose(() -> {
      Arrays.stream(array).forEach(RefUtil::freeRef);
    }));
  }

  @NotNull
  public static <T> String toString(@NotNull T[] values) {
    final String result = Arrays.toString(values);
    Arrays.stream(values).forEach(RefUtil::freeRef);
    return result;
  }

  @NotNull
  public static <T> RefList<T> asList(@NotNull T... items) {
    final RefArrayList<T> ts = new RefArrayList<>(Arrays.asList(items));
    for (T item : items) {
      RefUtil.freeRef(item);
    }
    return ts;
  }

  public static <T> T[] copyOf(T[] data, int length) {
    throw new RuntimeException();
  }

  public static int[] copyOf(int[] data, int length) {
    throw new RuntimeException();
  }

  public static double[] copyOf(double[] data, int length) {
    throw new RuntimeException();
  }

  public static boolean equals(int[] l, int[] r) {
    throw new RuntimeException();
  }

  public static boolean equals(double[] l, double[] r) {
    throw new RuntimeException();
  }

  public static String toString(int[] ints) {
    throw new RuntimeException();
  }

  public static String toString(double[] ints) {
    throw new RuntimeException();
  }

  public static void parallelSetAll(double[] data, IntToDoubleFunction fn) {
    throw new RuntimeException();
  }

  public static void setAll(double[] data, IntToDoubleFunction fn) {
    throw new RuntimeException();
  }

  public static <T> void setAll(T[] data, IntFunction<T> fn) {
    throw new RuntimeException();
  }

  public static RefDoubleStream stream(double[] data) {
    throw new RuntimeException();
  }

  public static RefIntStream stream(int[] data) {
    throw new RuntimeException();
  }

  public static RefLongStream stream(long[] data) {
    throw new RuntimeException();
  }

  public static int binarySearch(double[] array, double value) {
    throw new RuntimeException();
  }

  public static int hashCode(double[] data) {
    throw new RuntimeException();
  }

  public static int hashCode(int[] data) {
    throw new RuntimeException();
  }

}
