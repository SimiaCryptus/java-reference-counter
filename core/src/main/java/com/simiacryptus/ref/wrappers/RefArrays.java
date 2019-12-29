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
import com.simiacryptus.ref.lang.RefIgnore;
import com.simiacryptus.ref.lang.RefUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntUnaryOperator;

/**
 * The type Ref arrays.
 */
@RefAware
@RefIgnore
public class RefArrays {
  /**
   * Stream ref stream.
   *
   * @param <T>   the type parameter
   * @param array the array
   * @return the ref stream
   */
  public static <T> RefStream<T> stream(@NotNull T[] array) {
    return new RefStream<>(Arrays.stream(array).onClose(() -> {
      Arrays.stream(array).forEach(RefUtil::freeRef);
    }));
  }

  /**
   * To string string.
   *
   * @param <T>    the type parameter
   * @param values the values
   * @return the string
   */
  @NotNull
  public static <T> String toString(@NotNull T[] values) {
    final String result = Arrays.toString(values);
    Arrays.stream(values).forEach(RefUtil::freeRef);
    return result;
  }

  /**
   * As list ref list.
   *
   * @param <T>   the type parameter
   * @param items the items
   * @return the ref list
   */
  @NotNull
  public static <T> RefList<T> asList(@NotNull T... items) {
    final RefArrayList<T> ts = new RefArrayList<>(Arrays.asList(items));
    for (T item : items) {
      RefUtil.freeRef(item);
    }
    return ts;
  }

  /**
   * Copy of t [ ].
   *
   * @param <T>    the type parameter
   * @param data   the data
   * @param length the length
   * @return the t [ ]
   */
  public static <T> T[] copyOf(T[] data, int length) {
    return Arrays.copyOf(data, length);
  }

  /**
   * Copy of int [ ].
   *
   * @param data   the data
   * @param length the length
   * @return the int [ ]
   */
  public static int[] copyOf(int[] data, int length) {
    return Arrays.copyOf(data, length);
  }

  /**
   * Copy of double [ ].
   *
   * @param data   the data
   * @param length the length
   * @return the double [ ]
   */
  public static double[] copyOf(double[] data, int length) {
    return Arrays.copyOf(data, length);
  }

  /**
   * Equals boolean.
   *
   * @param l the l
   * @param r the r
   * @return the boolean
   */
  public static boolean equals(int[] l, int[] r) {
    return Arrays.equals(l, r);
  }

  /**
   * Equals boolean.
   *
   * @param l the l
   * @param r the r
   * @return the boolean
   */
  public static boolean equals(double[] l, double[] r) {
    return Arrays.equals(l, r);
  }

  /**
   * To string string.
   *
   * @param ints the ints
   * @return the string
   */
  public static String toString(int[] ints) {
    return Arrays.toString(ints);
  }

  public static String toString(byte[] ints) {
    return Arrays.toString(ints);
  }

  /**
   * To string string.
   *
   * @param ints the ints
   * @return the string
   */
  public static String toString(double[] ints) {
    return Arrays.toString(ints);
  }

  /**
   * Parallel set all.
   *
   * @param data the data
   * @param fn   the fn
   */
  public static void parallelSetAll(double[] data, IntToDoubleFunction fn) {
    Arrays.parallelSetAll(data, fn);
  }

  public static void parallelSetAll(int[] data, IntUnaryOperator fn) {
    Arrays.parallelSetAll(data, fn);
  }

  /**
   * Parallel set all.
   *
   * @param data the data
   * @param fn   the fn
   */
  public static <T> void parallelSetAll(T[] data, IntFunction<T> fn) {
    Arrays.parallelSetAll(data, fn);
  }

  /**
   * Sets all.
   *
   * @param data the data
   * @param fn   the fn
   */
  public static void setAll(double[] data, IntToDoubleFunction fn) {
    Arrays.setAll(data, fn);
  }

  /**
   * Sets all.
   *
   * @param data the data
   * @param fn   the fn
   */
  public static void setAll(int[] data, IntUnaryOperator fn) {
    Arrays.setAll(data, fn);
  }

  /**
   * Sets all.
   *
   * @param <T>  the type parameter
   * @param data the data
   * @param fn   the fn
   */
  public static <T> void setAll(T[] data, IntFunction<T> fn) {
    Arrays.setAll(data, fn);
  }

  /**
   * Stream ref double stream.
   *
   * @param data the data
   * @return the ref double stream
   */
  public static RefDoubleStream stream(double[] data) {
    return new RefDoubleStream(Arrays.stream(data));
  }

  /**
   * Stream ref int stream.
   *
   * @param data the data
   * @return the ref int stream
   */
  public static RefIntStream stream(int[] data) {
    return new RefIntStream(Arrays.stream(data));
  }

  /**
   * Stream ref long stream.
   *
   * @param data the data
   * @return the ref long stream
   */
  public static RefLongStream stream(long[] data) {
    return new RefLongStream(Arrays.stream(data));
  }

  /**
   * Binary search int.
   *
   * @param array the array
   * @param value the value
   * @return the int
   */
  public static int binarySearch(double[] array, double value) {
    return Arrays.binarySearch(array, value);
  }

  /**
   * Hash code int.
   *
   * @param data the data
   * @return the int
   */
  public static int hashCode(double[] data) {
    return Arrays.hashCode(data);
  }

  /**
   * Hash code int.
   *
   * @param data the data
   * @return the int
   */
  public static int hashCode(int[] data) {
    return Arrays.hashCode(data);
  }

  public static CharSequence deepToString(Object[] a) {
    return Arrays.deepToString(a);
  }

  public static boolean deepEquals(Object[] a, Object[] b) {
    return Arrays.deepEquals(a, b);
  }

  public static int deepHashCode(Object a[]) {
    return Arrays.deepHashCode(a);
  }

  public static <T> T[] copyOfRange(T[] original, int from, int to) {
    return Arrays.copyOfRange(original, from, to);
  }

  public static String toString(float[] obj) {
    return Arrays.toString(obj);
  }

  public static String toString(long[] obj) {
    return Arrays.toString(obj);
  }
}
