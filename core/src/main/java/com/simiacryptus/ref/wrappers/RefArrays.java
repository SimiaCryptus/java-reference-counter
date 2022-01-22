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

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntUnaryOperator;
import java.util.stream.Stream;

@RefIgnore
@SuppressWarnings("unused")
public class RefArrays {
  @Nonnull
  public static <T> RefStream<T> stream(@Nonnull @RefAware T[] array) {
    if (null == array) return new RefStream<>(Stream.empty());
    return new RefStream<>(Arrays.stream(array).onClose(() -> {
      Arrays.stream(array).forEach(value -> RefUtil.freeRef(value));
    }));
  }

  @Nonnull
  public static <T> String toString(@Nonnull @RefAware T[] values) {
    final String result = Arrays.toString(values);
    Arrays.stream(values).forEach(value -> RefUtil.freeRef(value));
    return result;
  }

  @Nonnull
  public static <T> RefList<T> asList(@Nonnull @RefAware T... items) {
    final RefArrayList<T> ts = new RefArrayList<>(items.length);
    for (T item : items) {
      ts.add(item);
    }
    return ts;
  }

  @Nonnull
  public static <T> T[] copyOf(@Nonnull @RefAware T[] data, int length) {
    assert data.length == length;
    return Arrays.copyOf(data, length);
  }

  @Nonnull
  public static int[] copyOf(@Nonnull int[] data, int length) {
    return Arrays.copyOf(data, length);
  }

  @Nonnull
  public static byte[] copyOf(@Nonnull byte[] data, int length) {
    return Arrays.copyOf(data, length);
  }

  @Nonnull
  public static char[] copyOf(@Nonnull char[] data, int length) {
    return Arrays.copyOf(data, length);
  }

  @Nonnull
  public static double[] copyOf(@Nonnull double[] data, int length) {
    return Arrays.copyOf(data, length);
  }

  public static boolean equals(int[] l, int[] r) {
    return Arrays.equals(l, r);
  }

  public static boolean equals(long[] l, long[] r) {
    return Arrays.equals(l, r);
  }

  public static boolean equals(float[] l, float[] r) {
    return Arrays.equals(l, r);
  }

  public static boolean equals(byte[] l, byte[] r) {
    return Arrays.equals(l, r);
  }

  public static boolean equals(char[] l, char[] r) {
    return Arrays.equals(l, r);
  }

  public static boolean equals(double[] l, double[] r) {
    return Arrays.equals(l, r);
  }

  @Nonnull
  public static String toString(int[] ints) {
    return Arrays.toString(ints);
  }

  @Nonnull
  public static String toString(byte[] ints) {
    return Arrays.toString(ints);
  }

  @Nonnull
  public static String toString(double[] ints) {
    return Arrays.toString(ints);
  }

  public static void parallelSetAll(@Nonnull double[] data, @Nonnull @RefAware IntToDoubleFunction fn) {
    Arrays.parallelSetAll(data, fn);
  }

  public static void parallelSetAll(@Nonnull int[] data, @Nonnull @RefAware IntUnaryOperator fn) {
    Arrays.parallelSetAll(data, fn);
  }

  public static <T> void parallelSetAll(@Nonnull @RefAware T[] data, @Nonnull @RefAware IntFunction<T> fn) {
    RefUtil.freeRef(data);
    Arrays.parallelSetAll(data, fn);
    RefUtil.freeRef(fn);
  }

  public static void setAll(@Nonnull double[] data, @Nonnull @RefAware IntToDoubleFunction fn) {
    Arrays.setAll(data, fn);
  }

  public static void setAll(@Nonnull int[] data, @Nonnull @RefAware IntUnaryOperator fn) {
    Arrays.setAll(data, fn);
  }

  public static <T> void setAll(@Nonnull @RefAware T[] data, @Nonnull @RefAware IntFunction<T> fn) {
    Arrays.setAll(data, fn);
  }

  @Nonnull
  public static RefDoubleStream stream(@Nonnull double[] data) {
    return new RefDoubleStream(Arrays.stream(data));
  }

  @Nonnull
  public static RefIntStream stream(@Nonnull int[] data) {
    return new RefIntStream(Arrays.stream(data));
  }

  @Nonnull
  public static RefLongStream stream(@Nonnull long[] data) {
    return new RefLongStream(Arrays.stream(data));
  }

  @SuppressWarnings("unused")
  public static int binarySearch(@Nonnull double[] array, double value) {
    return Arrays.binarySearch(array, value);
  }

  public static int binarySearch(@Nonnull int[] array, int value) {
    return Arrays.binarySearch(array, value);
  }

  public static int hashCode(double[] data) {
    return Arrays.hashCode(data);
  }

  public static int hashCode(int[] data) {
    return Arrays.hashCode(data);
  }

  public static int hashCode(float[] data) {
    return Arrays.hashCode(data);
  }

  public static int hashCode(long[] data) {
    return Arrays.hashCode(data);
  }

  public static int hashCode(byte[] data) {
    return Arrays.hashCode(data);
  }

  public static int hashCode(char[] data) {
    return Arrays.hashCode(data);
  }

  @Nonnull
  public static CharSequence deepToString(@RefAware Object[] a) {
    return Arrays.deepToString(a);
  }

  public static boolean deepEquals(@RefAware Object[] a, @RefAware Object[] b) {
    return Arrays.deepEquals(a, b);
  }

  public static int deepHashCode(@RefAware Object a[]) {
    return Arrays.deepHashCode(a);
  }

  @Nonnull
  public static <T> T[] copyOfRange(@Nonnull @RefAware T[] original, int from, int to) {
    T[] copy = Arrays.copyOfRange(original, from, to);
    RefUtil.addRefs(copy);
    RefUtil.freeRefs(original);
    return copy;
  }

  @Nonnull
  public static String toString(float[] obj) {
    return Arrays.toString(obj);
  }

  @Nonnull
  public static String toString(long[] obj) {
    return Arrays.toString(obj);
  }

  public static void fill(@Nonnull @RefAware Object[] array, @RefAware Object value) {
    Arrays.fill(array, value);
  }

  public static void fill(@Nonnull double[] array, double value) {
    Arrays.fill(array, value);
  }

  public static void fill(@Nonnull float[] array, float value) {
    Arrays.fill(array, value);
  }

  public static void fill(@Nonnull int[] array, int value) {
    Arrays.fill(array, value);
  }

  public static void fill(@Nonnull long[] array, long value) {
    Arrays.fill(array, value);
  }

  public static void fill(@Nonnull char[] array, char value) {
    Arrays.fill(array, value);
  }

  public static void fill(@Nonnull int[] array, int fromIndex, int toIndex, int value) {
    Arrays.fill(array, fromIndex, toIndex, value);
  }

  public static void fill(@Nonnull long[] array, int fromIndex, int toIndex, long value) {
    Arrays.fill(array, fromIndex, toIndex, value);
  }

  public static void fill(@Nonnull double[] array, int fromIndex, int toIndex, double value) {
    Arrays.fill(array, fromIndex, toIndex, value);
  }

  public static void fill(@Nonnull float[] array, int fromIndex, int toIndex, float value) {
    Arrays.fill(array, fromIndex, toIndex, value);
  }

  public static void fill(@Nonnull byte[] array, int fromIndex, int toIndex, byte value) {
    Arrays.fill(array, fromIndex, toIndex, value);
  }

  public static void fill(@Nonnull char[] array, int fromIndex, int toIndex, char value) {
    Arrays.fill(array, fromIndex, toIndex, value);
  }

  @Nonnull
  public static float[] copyOf(@Nonnull float[] floats, int length) {
    return Arrays.copyOf(floats, length);
  }

  @Nonnull
  public static float[] copyOfRange(@Nonnull float[] floats, int from, int to) {
    return Arrays.copyOfRange(floats, from, to);
  }

  @Nonnull
  public static int[] copyOfRange(@Nonnull int[] floats, int from, int to) {
    return Arrays.copyOfRange(floats, from, to);
  }

  @Nonnull
  public static char[] copyOfRange(@Nonnull char[] floats, int from, int to) {
    return Arrays.copyOfRange(floats, from, to);
  }

  @Nonnull
  public static byte[] copyOfRange(@Nonnull byte[] floats, int from, int to) {
    return Arrays.copyOfRange(floats, from, to);
  }
}
