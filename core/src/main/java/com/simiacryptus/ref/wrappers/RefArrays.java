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
@SuppressWarnings("unused")
public class RefArrays {
  /**
   * Stream ref stream.
   *
   * @param <T>   the type parameter
   * @param array the array
   * @return the ref stream
   */
  @NotNull
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
  @NotNull
  public static <T> T[] copyOf(@NotNull T[] data, int length) {
    return Arrays.copyOf(data, length);
  }

  /**
   * Copy of int [ ].
   *
   * @param data   the data
   * @param length the length
   * @return the int [ ]
   */
  @NotNull
  public static int[] copyOf(@NotNull int[] data, int length) {
    return Arrays.copyOf(data, length);
  }

  /**
   * Copy of double [ ].
   *
   * @param data   the data
   * @param length the length
   * @return the double [ ]
   */
  @NotNull
  public static double[] copyOf(@NotNull double[] data, int length) {
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
  @NotNull
  public static String toString(int[] ints) {
    return Arrays.toString(ints);
  }

  /**
   * To string string.
   *
   * @param ints the ints
   * @return the string
   */
  @NotNull
  public static String toString(byte[] ints) {
    return Arrays.toString(ints);
  }

  /**
   * To string string.
   *
   * @param ints the ints
   * @return the string
   */
  @NotNull
  public static String toString(double[] ints) {
    return Arrays.toString(ints);
  }

  /**
   * Parallel set all.
   *
   * @param data the data
   * @param fn   the fn
   */
  public static void parallelSetAll(@NotNull double[] data, @NotNull IntToDoubleFunction fn) {
    Arrays.parallelSetAll(data, fn);
  }

  /**
   * Parallel set all.
   *
   * @param data the data
   * @param fn   the fn
   */
  public static void parallelSetAll(@NotNull int[] data, @NotNull IntUnaryOperator fn) {
    Arrays.parallelSetAll(data, fn);
  }

  /**
   * Parallel set all.
   *
   * @param <T>  the type parameter
   * @param data the data
   * @param fn   the fn
   */
  public static <T> void parallelSetAll(@NotNull T[] data, @NotNull IntFunction<T> fn) {
    Arrays.parallelSetAll(data, fn);
  }

  /**
   * Sets all.
   *
   * @param data the data
   * @param fn   the fn
   */
  public static void setAll(@NotNull double[] data, @NotNull IntToDoubleFunction fn) {
    Arrays.setAll(data, fn);
  }

  /**
   * Sets all.
   *
   * @param data the data
   * @param fn   the fn
   */
  public static void setAll(@NotNull int[] data, @NotNull IntUnaryOperator fn) {
    Arrays.setAll(data, fn);
  }

  /**
   * Sets all.
   *
   * @param <T>  the type parameter
   * @param data the data
   * @param fn   the fn
   */
  public static <T> void setAll(@NotNull T[] data, @NotNull IntFunction<T> fn) {
    Arrays.setAll(data, fn);
  }

  /**
   * Stream ref double stream.
   *
   * @param data the data
   * @return the ref double stream
   */
  @NotNull
  public static RefDoubleStream stream(@NotNull double[] data) {
    return new RefDoubleStream(Arrays.stream(data));
  }

  /**
   * Stream ref int stream.
   *
   * @param data the data
   * @return the ref int stream
   */
  @NotNull
  public static RefIntStream stream(@NotNull int[] data) {
    return new RefIntStream(Arrays.stream(data));
  }

  /**
   * Stream ref long stream.
   *
   * @param data the data
   * @return the ref long stream
   */
  @NotNull
  public static RefLongStream stream(@NotNull long[] data) {
    return new RefLongStream(Arrays.stream(data));
  }

  /**
   * Binary search int.
   *
   * @param array the array
   * @param value the value
   * @return the int
   */
  @SuppressWarnings("unused")
  public static int binarySearch(@NotNull double[] array, double value) {
    return Arrays.binarySearch(array, value);
  }

  /**
   * Binary search int.
   *
   * @param array the array
   * @param value the value
   * @return the int
   */
  public static int binarySearch(@NotNull int[] array, int value) {
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

  /**
   * Deep to string char sequence.
   *
   * @param a the a
   * @return the char sequence
   */
  @NotNull
  public static CharSequence deepToString(Object[] a) {
    return Arrays.deepToString(a);
  }

  /**
   * Deep equals boolean.
   *
   * @param a the a
   * @param b the b
   * @return the boolean
   */
  public static boolean deepEquals(Object[] a, Object[] b) {
    return Arrays.deepEquals(a, b);
  }

  /**
   * Deep hash code int.
   *
   * @param a the a
   * @return the int
   */
  public static int deepHashCode(Object a[]) {
    return Arrays.deepHashCode(a);
  }

  /**
   * Copy of range t [ ].
   *
   * @param <T>      the type parameter
   * @param original the original
   * @param from     the from
   * @param to       the to
   * @return the t [ ]
   */
  @NotNull
  public static <T> T[] copyOfRange(@NotNull T[] original, int from, int to) {
    return Arrays.copyOfRange(original, from, to);
  }

  /**
   * To string string.
   *
   * @param obj the obj
   * @return the string
   */
  @NotNull
  public static String toString(float[] obj) {
    return Arrays.toString(obj);
  }

  /**
   * To string string.
   *
   * @param obj the obj
   * @return the string
   */
  @NotNull
  public static String toString(long[] obj) {
    return Arrays.toString(obj);
  }

  /**
   * Fill.
   *
   * @param array the array
   * @param o     the o
   */
  public static void fill(@NotNull Object[] array, Object o) {
    Arrays.fill(array, o);
  }

  /**
   * Fill.
   *
   * @param floats the floats
   * @param v      the v
   */
  public static void fill(@NotNull double[] floats, double v) {
    Arrays.fill(floats, v);
  }

  /**
   * Fill.
   *
   * @param floats the floats
   * @param v      the v
   */
  public static void fill(@NotNull float[] floats, float v) {
    Arrays.fill(floats, v);
  }

  /**
   * Copy of float [ ].
   *
   * @param floats the floats
   * @param length the length
   * @return the float [ ]
   */
  @NotNull
  public static float[] copyOf(@NotNull float[] floats, int length) {
    return Arrays.copyOf(floats, length);
  }

  /**
   * Copy of range float [ ].
   *
   * @param floats the floats
   * @param from   the from
   * @param to     the to
   * @return the float [ ]
   */
  @NotNull
  public static float[] copyOfRange(@NotNull float[] floats, int from, int to) {
    return Arrays.copyOfRange(floats, from, to);
  }

  /**
   * Copy of range int [ ].
   *
   * @param floats the floats
   * @param from   the from
   * @param to     the to
   * @return the int [ ]
   */
  @NotNull
  public static int[] copyOfRange(@NotNull int[] floats, int from, int to) {
    return Arrays.copyOfRange(floats, from, to);
  }
}
