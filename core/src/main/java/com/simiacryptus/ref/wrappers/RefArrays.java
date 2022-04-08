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

/**
 * This class contains methods for manipulating arrays of references.
 *
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public class RefArrays {
  /**
   * Returns a {@link RefStream} of the given array.
   *
   * @param array the array to stream
   * @param <T>   the type of the array
   * @return a {@link RefStream} of the given array
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> RefStream<T> stream(@Nonnull @RefAware T[] array) {
    if (null == array) return new RefStream<>(Stream.empty());
    return new RefStream<>(Arrays.stream(array).onClose(() -> {
      Arrays.stream(array).forEach(value -> RefUtil.freeRef(value));
    }));
  }

  /**
   * Converts an array of objects to a string.
   *
   * @param values the array of objects to convert to a string
   * @return the string representation of the array of objects
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> String toString(@Nonnull @RefAware T[] values) {
    final String result = Arrays.toString(values);
    Arrays.stream(values).forEach(value -> RefUtil.freeRef(value));
    return result;
  }

  /**
   * Returns a list containing the specified elements.
   *
   * @param <T>   the type of the elements in the list
   * @param items the elements to be contained in the list
   * @return a list containing the specified elements
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> RefList<T> asList(@Nonnull @RefAware T... items) {
    final RefArrayList<T> ts = new RefArrayList<>(items.length);
    for (T item : items) {
      ts.add(item);
    }
    return ts;
  }

  /**
   * Returns a copy of the specified array, truncating or padding with nulls
   * (if necessary) so the copy has the specified length.  This method is
   * equivalent to calling {@link Arrays#copyOf(Object[], int)} with a
   * null source array.
   *
   * @param <T>    the class of the objects in the array
   * @param data   the array to be copied
   * @param length the length of the copy to be returned
   * @return a copy of the specified array, truncated or padded with nulls
   * if necessary to obtain the specified length
   * @throws NullPointerException     if {@code data} is null
   * @throws IllegalArgumentException if {@code length} is negative
   * @docgenVersion 9
   * @see Arrays#copyOf(Object[], int)
   */
  @Nonnull
  public static <T> T[] copyOf(@Nonnull @RefAware T[] data, int length) {
    assert data.length == length;
    return Arrays.copyOf(data, length);
  }

  /**
   * Returns a copy of the specified array, truncated or padded with zeros
   * to the specified length.
   *
   * @param data   the array to be copied
   * @param length the length of the copy to be returned
   * @return a copy of the specified array, truncated or padded with zeros
   * to the specified length
   * @throws NullPointerException if <tt>data</tt> is null
   * @docgenVersion 9
   */
  @Nonnull
  public static int[] copyOf(@Nonnull int[] data, int length) {
    return Arrays.copyOf(data, length);
  }

  /**
   * Returns a copy of the specified array, truncated or padded with zeros
   * to the specified length.
   *
   * @param data   the array to be copied
   * @param length the length of the copy to be returned
   * @return a copy of the specified array, truncated or padded with zeros
   * to the specified length
   * @throws NullPointerException if {@code data} is null
   * @docgenVersion 9
   */
  @Nonnull
  public static byte[] copyOf(@Nonnull byte[] data, int length) {
    return Arrays.copyOf(data, length);
  }

  /**
   * Returns a copy of the specified array, truncated or padded with zeros
   * to the specified length.
   *
   * @param data   the array to be copied
   * @param length the length of the copy to be returned
   * @return a copy of the specified array, truncated or padded with zeros
   * to the specified length
   * @throws NullPointerException if {@code data} is null
   * @docgenVersion 9
   */
  @Nonnull
  public static char[] copyOf(@Nonnull char[] data, int length) {
    return Arrays.copyOf(data, length);
  }

  /**
   * Returns a copy of the specified array, truncating or padding with zeros
   * (if necessary) so the copy has the specified length.  For all indices
   * that are valid in both the original array and the copy, the two arrays will
   * contain identical values.  For any indices that are valid in the copy but not
   * the original, the copy will contain 0.0.  Such indices will exist if and only
   * if the specified length is greater than that of the original array.
   *
   * @param data   the array to be copied
   * @param length the length of the copy to be returned
   * @return a copy of the original array, truncated or padded with zeros
   * as necessary to obtain the specified length
   * @throws NullPointerException if <tt>data</tt> is null
   * @docgenVersion 9
   */
  @Nonnull
  public static double[] copyOf(@Nonnull double[] data, int length) {
    return Arrays.copyOf(data, length);
  }

  /**
   * Returns true if the two specified arrays of ints are equal to one another.
   *
   * @param l the first array of ints to compare
   * @param r the second array of ints to compare
   * @return true if the two arrays are equal
   * @docgenVersion 9
   */
  public static boolean equals(int[] l, int[] r) {
    return Arrays.equals(l, r);
  }

  /**
   * Returns true if the two specified arrays of longs are equal to one another.
   * Two arrays are considered equal if both arrays contain the same number of elements,
   * and all corresponding pairs of elements in the two arrays are equal.
   * In other words, two arrays are equal if they contain the same elements in the same order.
   * Also, two array references are considered equal if both are null.
   *
   * @param l the first array of longs to be tested for equality
   * @param r the second array of longs to be tested for equality
   * @return true if the two arrays are equal
   * @docgenVersion 9
   */
  public static boolean equals(long[] l, long[] r) {
    return Arrays.equals(l, r);
  }

  /**
   * Returns true if the two specified arrays of floats are equal to one another.
   * The two arrays are considered equal if both arrays contain the same number of elements,
   * and all corresponding pairs of elements in the two arrays are equal.
   * In other words, two arrays are equal if they contain the same elements in the same order.
   * Also, two array references are considered equal if both are null.
   *
   * @param l the first array of floats to be tested for equality
   * @param r the second array of floats to be tested for equality
   * @return true if the two arrays are equal
   * @docgenVersion 9
   */
  public static boolean equals(float[] l, float[] r) {
    return Arrays.equals(l, r);
  }

  /**
   * Returns true if the two specified arrays of bytes are equal to one another.
   * Two arrays are considered equal if both arrays contain the same number of elements,
   * and all corresponding pairs of elements in the two arrays are equal.
   * In other words, two arrays are equal if they contain the same elements in the same order.
   * Also, two array references are considered equal if both are null.
   *
   * @param l the first array of bytes to be tested for equality
   * @param r the second array of bytes to be tested for equality
   * @return true if the two arrays are equal
   * @docgenVersion 9
   */
  public static boolean equals(byte[] l, byte[] r) {
    return Arrays.equals(l, r);
  }

  /**
   * Returns true if the two specified arrays of chars are equal to one another.
   * Two arrays are considered equal if both arrays contain the same number of elements,
   * and all corresponding pairs of elements in the two arrays are equal.
   * In other words, two arrays are equal if they contain the same elements in the same order.
   * Also, two array references are considered equal if both are null.
   *
   * @param l the first array of chars to be tested for equality
   * @param r the second array of chars to be tested for equality
   * @return true if the two arrays are equal
   * @docgenVersion 9
   */
  public static boolean equals(char[] l, char[] r) {
    return Arrays.equals(l, r);
  }

  /**
   * Returns true if the two specified arrays of doubles are equal to one another.
   * Two arrays are considered equal if both arrays contain the same number of elements,
   * and all corresponding pairs of elements in the two arrays are equal.
   * In other words, two arrays are equal if they contain the same elements in the same order.
   * Also, two array references are considered equal if both are null.
   *
   * @param l the first array of doubles to be tested for equality
   * @param r the second array of doubles to be tested for equality
   * @return true if the two arrays are equal
   * @docgenVersion 9
   */
  public static boolean equals(double[] l, double[] r) {
    return Arrays.equals(l, r);
  }

  /**
   * Returns a string representation of the contents of the specified array.
   * The string representation consists of a list of the array's elements, enclosed in square brackets ("[]"). Adjacent elements are separated by the characters ", " (a comma followed by a space). Elements are converted to strings as by String.valueOf(int). Returns "null" if a is null.
   *
   * @param ints the array whose string representation to return
   * @return a string representation of a
   * @docgenVersion 9
   */
  @Nonnull
  public static String toString(int[] ints) {
    return Arrays.toString(ints);
  }

  /**
   * Returns a string representation of the contents of the specified byte array.
   *
   * @param ints the byte array whose string representation to return
   * @return a string representation of {@code ints}
   * @throws NullPointerException if {@code ints} is null
   * @docgenVersion 9
   */
  @Nonnull
  public static String toString(byte[] ints) {
    return Arrays.toString(ints);
  }

  /**
   * Returns a string representation of the contents of the specified array.
   * The string representation consists of a list of the array's elements, enclosed in square brackets ("[]"). Adjacent elements are separated by the characters ", " (a comma followed by a space). Elements are converted to strings as by String.valueOf(double). Returns "null" if a is null.
   *
   * @param a the array whose string representation to return
   * @return a string representation of a
   * @docgenVersion 9
   */
  @Nonnull
  public static String toString(double[] ints) {
    return Arrays.toString(ints);
  }

  /**
   * Sets each element of the given array to the result of applying the given
   * function to the index of that element.
   *
   * <p>This method is equivalent to:
   * <pre>{@code
   *     for (int i = 0; i < data.length; i++) {
   *         data[i] = fn.applyAsDouble(i);
   *     }
   * }</pre>
   * <p>
   * but may execute in parallel if the {@code Arrays} class contains a parallel
   * implementation.
   *
   * @param data the array to be operated on
   * @param fn   the function to apply to each index
   * @throws NullPointerException if the data array or the function is null
   * @docgenVersion 9
   */
  public static void parallelSetAll(@Nonnull double[] data, @Nonnull @RefAware IntToDoubleFunction fn) {
    Arrays.parallelSetAll(data, fn);
  }

  /**
   * Sets all elements of the specified array, using the provided
   * generator function to compute each element.
   *
   * <p>The generator function is called once for each index {@code i} of the array,
   * in ascending order, with the index {@code i} as its first argument.
   * It is not invoked for indexes that have already been set.
   * The array passed to the generator function is the same one that this method is
   * setting.  If the generator function modifies the array directly, the
   * returned array will reflect those changes.
   *
   * <p>Parallel implementations of this method should override this default
   * implementation.
   *
   * @param data the array to be populated
   * @param fn   the generator function to be applied to each index
   * @throws NullPointerException if either data or fn is null
   * @docgenVersion 9
   * @since 1.8
   */
  public static void parallelSetAll(@Nonnull int[] data, @Nonnull @RefAware IntUnaryOperator fn) {
    Arrays.parallelSetAll(data, fn);
  }

  /**
   * Sets each element of the given array to the result of applying the given
   * function to its index.  The index of the first element is 0.
   *
   * <p>The {@code setAll} operation is parallelizable as long as the given
   * function is parallelizable.  Array indexes are given to the function in an
   * arbitrarily chosen order.  The effect of this call is the same as if it
   * were to invoke the given function as follows:
   * <pre>{@code
   * for (int i = 0; i < data.length; i++)
   *     data[i] = fn.apply(i);
   * }</pre>
   *
   * <p>
   *
   * @param <T>  type of elements of the array
   * @param data the array to be initialized
   * @param fn   a function to apply to each array element
   * @throws NullPointerException if the specified array is null
   * @throws NullPointerException if the specified function is null
   * @docgenVersion 9
   * @since 1.8
   */
  public static <T> void parallelSetAll(@Nonnull @RefAware T[] data, @Nonnull @RefAware IntFunction<T> fn) {
    RefUtil.freeRef(data);
    Arrays.parallelSetAll(data, fn);
    RefUtil.freeRef(fn);
  }

  /**
   * Sets all elements of the given array by applying the given function to each index.
   *
   * @param data the array to modify
   * @param fn   the function to apply to each index
   * @throws NullPointerException if {@code data} or {@code fn} is null
   * @docgenVersion 9
   */
  public static void setAll(@Nonnull double[] data, @Nonnull @RefAware IntToDoubleFunction fn) {
    Arrays.setAll(data, fn);
  }

  /**
   * Sets all elements of the given array by applying the given function to each index.
   *
   * @param data the array to modify
   * @param fn   the function to apply to each index
   * @throws NullPointerException if {@code data} or {@code fn} is null
   * @docgenVersion 9
   */
  public static void setAll(@Nonnull int[] data, @Nonnull @RefAware IntUnaryOperator fn) {
    Arrays.setAll(data, fn);
  }

  /**
   * Sets all elements of the specified array, using the provided
   * generator function to compute each element.
   *
   * @param <T>  type of the elements of the array
   * @param data the array to be initialized
   * @param fn   a function accepting an index and producing the desired
   *             element
   * @throws NullPointerException if the array is null or the generator
   *                              function is null
   * @docgenVersion 9
   * @since 1.8
   */
  public static <T> void setAll(@Nonnull @RefAware T[] data, @Nonnull @RefAware IntFunction<T> fn) {
    Arrays.setAll(data, fn);
  }

  /**
   * Returns a {@link RefDoubleStream} for the given data.
   *
   * @param data the data to stream
   * @return a {@link RefDoubleStream} for the given data
   * @docgenVersion 9
   */
  @Nonnull
  public static RefDoubleStream stream(@Nonnull double[] data) {
    return new RefDoubleStream(Arrays.stream(data));
  }

  /**
   * Returns a {@link RefIntStream} for the given int array.
   *
   * @param data the int array to stream
   * @return a {@link RefIntStream} for the given int array
   * @docgenVersion 9
   */
  @Nonnull
  public static RefIntStream stream(@Nonnull int[] data) {
    return new RefIntStream(Arrays.stream(data));
  }

  /**
   * Returns a {@link RefLongStream} for the given array.
   *
   * @param data the array to stream
   * @return a {@link RefLongStream} for the given array
   * @docgenVersion 9
   */
  @Nonnull
  public static RefLongStream stream(@Nonnull long[] data) {
    return new RefLongStream(Arrays.stream(data));
  }

  /**
   * Searches the specified array of doubles for the specified value using the binary search algorithm.
   * The array must be sorted before making this call. If it is not sorted, the results are undefined.
   * If the array contains multiple elements with the specified value, there is no guarantee which one will be found.
   *
   * @param array the array to be searched
   * @param value the value to be searched for
   * @return the index of the value, if it is contained in the array; otherwise, (-(insertion point) - 1)
   * @throws NullPointerException if the array is null
   * @docgenVersion 9
   */
  @SuppressWarnings("unused")
  public static int binarySearch(@Nonnull double[] array, double value) {
    return Arrays.binarySearch(array, value);
  }

  /**
   * Searches the specified array of ints for the specified value using the binary search algorithm.
   *
   * @param array the array to be searched
   * @param value the value to be searched for
   * @return index of the search key, if it is contained in the array; otherwise, (-(insertion point) - 1). The insertion point is defined as the point at which the key would be inserted into the array: the index of the first element greater than the key, or a.length if all elements in the array are less than the specified key. Note that this guarantees that the return value will be &gt;= 0 if and only if the key is found.
   * @throws NullPointerException if array is null
   * @docgenVersion 9
   */
  public static int binarySearch(@Nonnull int[] array, int value) {
    return Arrays.binarySearch(array, value);
  }

  /**
   * Returns a hash code for the given double array.
   *
   * @param data the double array to calculate the hash code for
   * @return a hash code for the given array
   * @docgenVersion 9
   */
  public static int hashCode(double[] data) {
    return Arrays.hashCode(data);
  }

  /**
   * Returns the hash code for the given array.
   *
   * @param data the array to calculate the hash code for
   * @return the hash code for the given array
   * @docgenVersion 9
   */
  public static int hashCode(int[] data) {
    return Arrays.hashCode(data);
  }

  /**
   * Returns a hash code for the given array of floats.
   *
   * @param data the array of floats to calculate a hash code for
   * @return a hash code for the given array of floats
   * @docgenVersion 9
   */
  public static int hashCode(float[] data) {
    return Arrays.hashCode(data);
  }

  /**
   * Returns a hash code for the specified array.
   *
   * @param data the array for which a hash code is to be computed
   * @return a hash code value for the specified array
   * @docgenVersion 9
   */
  public static int hashCode(long[] data) {
    return Arrays.hashCode(data);
  }

  /**
   * Returns the hash code for the specified array of bytes.
   *
   * @param data the array of bytes for which to calculate the hash code
   * @return the hash code for the specified array of bytes
   * @docgenVersion 9
   */
  public static int hashCode(byte[] data) {
    return Arrays.hashCode(data);
  }

  /**
   * Returns a hash code for the specified array of chars. The hash code is
   * generated as if all the chars in the array were contained in a
   * {@link java.util.List}, and that list was hashed by calling
   * {@link java.util.List#hashCode()}.
   *
   * @param data the array of chars for which a hash code is to be computed
   * @return a hash code value for the specified array of chars
   * @docgenVersion 9
   */
  public static int hashCode(char[] data) {
    return Arrays.hashCode(data);
  }

  /**
   * Returns a string representation of the "deep contents" of the specified
   * array.  If the array contains other arrays as elements, the string
   * representation contains their contents and so on.  This method is designed
   * for converting multidimensional arrays to strings.
   *
   * @param a the array whose string representation to return
   * @return a string representation of the "deep contents" of the specified array
   * @docgenVersion 9
   */
  @Nonnull
  public static CharSequence deepToString(@RefAware Object[] a) {
    return Arrays.deepToString(a);
  }

  /**
   * Returns true if the two specified arrays are deeply equal to one another.
   *
   * @param a the first array to compare
   * @param b the second array to compare
   * @return true if the two arrays are equal, false otherwise
   * @docgenVersion 9
   */
  public static boolean deepEquals(@RefAware Object[] a, @RefAware Object[] b) {
    return Arrays.deepEquals(a, b);
  }

  /**
   * Returns a hash code for the given array. If the array contains
   * references to other arrays, the hash code is computed recursively.
   *
   * @param a the array for which to compute a hash code
   * @return a hash code for the given array
   * @docgenVersion 9
   */
  public static int deepHashCode(@RefAware Object a[]) {
    return Arrays.deepHashCode(a);
  }

  /**
   * @param original The original array
   * @param from     The start index (inclusive)
   * @param to       The end index (exclusive)
   * @param <T>      The array type
   * @return A copy of the specified range of the original array
   * @docgenVersion 9
   */
  @Nonnull
  public static <T> T[] copyOfRange(@Nonnull @RefAware T[] original, int from, int to) {
    T[] copy = Arrays.copyOfRange(original, from, to);
    RefUtil.addRefs(copy);
    RefUtil.freeRefs(original);
    return copy;
  }

  /**
   * Returns a string representation of the contents of the specified array.
   * The string representation consists of a list of the array's elements, enclosed in square brackets ("[]"). Adjacent elements are separated by the characters ", " (a comma followed by a space). Elements are converted to strings as by String.valueOf(float). Returns "null" if obj is null.
   *
   * @param obj the array whose string representation to return
   * @return a string representation of obj
   * @docgenVersion 9
   */
  @Nonnull
  public static String toString(float[] obj) {
    return Arrays.toString(obj);
  }

  /**
   * Returns a string representation of the contents of the specified long array.
   * The string representation consists of a list of the array's elements, enclosed in square brackets ("[]").
   * Adjacent elements are separated by the characters ", " (a comma followed by a space).
   * Elements are converted to strings as by String.valueOf(long).
   * Returns "null" if obj is null.
   *
   * @param obj the array whose string representation to return
   * @return a string representation of obj
   * @docgenVersion 9
   */
  @Nonnull
  public static String toString(long[] obj) {
    return Arrays.toString(obj);
  }

  /**
   * Fills the specified array with the specified value.
   *
   * @param array the array to be filled
   * @param value the value to fill the array with
   * @throws NullPointerException if the array is null
   * @docgenVersion 9
   */
  public static void fill(@Nonnull @RefAware Object[] array, @RefAware Object value) {
    Arrays.fill(array, value);
  }

  /**
   * Fills the given array with the given value.
   *
   * @param array the array to fill
   * @param value the value to fill the array with
   * @docgenVersion 9
   */
  public static void fill(@Nonnull double[] array, double value) {
    Arrays.fill(array, value);
  }

  /**
   * Fills the given array with the given value.
   *
   * @param array the array to fill
   * @param value the value to fill the array with
   * @docgenVersion 9
   */
  public static void fill(@Nonnull float[] array, float value) {
    Arrays.fill(array, value);
  }

  /**
   * Fills the given array with the given value.
   *
   * @param array the array to fill
   * @param value the value to fill the array with
   * @throws NullPointerException if the array is null
   * @docgenVersion 9
   */
  public static void fill(@Nonnull int[] array, int value) {
    Arrays.fill(array, value);
  }

  /**
   * Fills the specified array with the specified value.
   *
   * @param array the array to be filled
   * @param value the value to fill the array with
   * @throws NullPointerException if the array is null
   * @docgenVersion 9
   */
  public static void fill(@Nonnull long[] array, long value) {
    Arrays.fill(array, value);
  }

  /**
   * Fills the given array with the given value.
   *
   * @param array the array to fill
   * @param value the value to fill the array with
   * @throws NullPointerException if the array is null
   * @docgenVersion 9
   */
  public static void fill(@Nonnull char[] array, char value) {
    Arrays.fill(array, value);
  }

  /**
   * Fills the specified array with the given value from the given start index (inclusive) to the given end index (exclusive).
   *
   * @param array     the array to fill
   * @param fromIndex the start index (inclusive)
   * @param toIndex   the end index (exclusive)
   * @param value     the value to fill the array with
   * @throws NullPointerException           if the array is null
   * @throws IllegalArgumentException       if fromIndex &gt; toIndex
   * @throws ArrayIndexOutOfBoundsException if fromIndex &lt; 0 or toIndex &gt; array.length
   * @docgenVersion 9
   */
  public static void fill(@Nonnull int[] array, int fromIndex, int toIndex, int value) {
    Arrays.fill(array, fromIndex, toIndex, value);
  }

  /**
   * Fills the specified array with the given value from the given start index to the given end index.
   *
   * @param array     the array to fill
   * @param fromIndex the start index (inclusive)
   * @param toIndex   the end index (exclusive)
   * @param value     the value to fill the array with
   * @throws NullPointerException           if the array is null
   * @throws IllegalArgumentException       if fromIndex &gt; toIndex
   * @throws ArrayIndexOutOfBoundsException if fromIndex &lt; 0 or toIndex &gt; array.length
   * @docgenVersion 9
   */
  public static void fill(@Nonnull long[] array, int fromIndex, int toIndex, long value) {
    Arrays.fill(array, fromIndex, toIndex, value);
  }

  /**
   * Fills the specified array with the given value from the given start index (inclusive) to the given end index (exclusive).
   *
   * @param array     the array to fill
   * @param fromIndex the start index (inclusive)
   * @param toIndex   the end index (exclusive)
   * @param value     the value to fill the array with
   * @throws NullPointerException           if the array is null
   * @throws IllegalArgumentException       if fromIndex &gt; toIndex
   * @throws ArrayIndexOutOfBoundsException if fromIndex &lt; 0 or toIndex &gt; array.length
   * @docgenVersion 9
   */
  public static void fill(@Nonnull double[] array, int fromIndex, int toIndex, double value) {
    Arrays.fill(array, fromIndex, toIndex, value);
  }

  /**
   * Fills the specified array with the given value from the given start index (inclusive) to the given end index (exclusive).
   *
   * @param array     the array to fill
   * @param fromIndex the start index (inclusive)
   * @param toIndex   the end index (exclusive)
   * @param value     the value to fill the array with
   * @throws NullPointerException           if the array is null
   * @throws IllegalArgumentException       if fromIndex &gt; toIndex
   * @throws ArrayIndexOutOfBoundsException if fromIndex &lt; 0 or toIndex &gt; array.length
   * @docgenVersion 9
   */
  public static void fill(@Nonnull float[] array, int fromIndex, int toIndex, float value) {
    Arrays.fill(array, fromIndex, toIndex, value);
  }

  /**
   * Fills the specified array with the specified value from the
   * specified start index to the specified end index.
   *
   * @param array     the array to fill
   * @param fromIndex the start index to fill from
   * @param toIndex   the end index to fill to
   * @param value     the value to fill the array with
   * @throws NullPointerException           if the array is null
   * @throws IllegalArgumentException       if fromIndex &gt; toIndex
   * @throws ArrayIndexOutOfBoundsException if fromIndex &lt; 0 or toIndex &gt;= array.length
   * @docgenVersion 9
   */
  public static void fill(@Nonnull byte[] array, int fromIndex, int toIndex, byte value) {
    Arrays.fill(array, fromIndex, toIndex, value);
  }

  /**
   * Fills the specified array with the given value.
   *
   * @param array     the array to fill
   * @param fromIndex the index to start filling from (inclusive)
   * @param toIndex   the index to stop filling at (exclusive)
   * @param value     the value to fill the array with
   * @throws NullPointerException           if the array is null
   * @throws IllegalArgumentException       if fromIndex &lt; 0 or toIndex &gt; array.length
   * @throws ArrayIndexOutOfBoundsException if fromIndex &gt; toIndex
   * @docgenVersion 9
   */
  public static void fill(@Nonnull char[] array, int fromIndex, int toIndex, char value) {
    Arrays.fill(array, fromIndex, toIndex, value);
  }

  /**
   * Returns a copy of the specified array, truncating or padding with zeros
   * (if necessary) so the copy has the specified length.  For all indices
   * that are valid in both the original array and the copy, the two arrays will
   * contain identical values.  For any indices that are valid in the copy but not
   * the original, the copy will contain 0.0f.  Such indices will exist if and only
   * if the specified length is greater than that of the original array.
   *
   * @param floats the array to be copied
   * @param length the length of the copy to be returned
   * @return a copy of the original array, truncated or padded with zeros
   * as necessary to obtain the specified length
   * @throws NullPointerException if {@code floats} is null
   * @docgenVersion 9
   */
  @Nonnull
  public static float[] copyOf(@Nonnull float[] floats, int length) {
    return Arrays.copyOf(floats, length);
  }

  /**
   * Returns a new array that is a copy of a range of the original array.
   * The original array must not be null, and the specified range must be valid.
   *
   * @param floats the original array
   * @param from   the initial index of the range to be copied, inclusive
   * @param to     the final index of the range to be copied, exclusive
   * @return a copy of the specified range of the original array
   * @throws NullPointerException      if the original array is null
   * @throws IndexOutOfBoundsException if the specified range is not valid
   * @docgenVersion 9
   */
  @Nonnull
  public static float[] copyOfRange(@Nonnull float[] floats, int from, int to) {
    return Arrays.copyOfRange(floats, from, to);
  }

  /**
   * Returns a new array that is a copy of a range of the original array.
   * The original array must not be null, and the specified range must be valid.
   *
   * @param floats the original array
   * @param from   the initial index of the range to be copied, inclusive
   * @param to     the final index of the range to be copied, exclusive
   * @return a copy of the specified range of the original array
   * @throws NullPointerException     if the original array is null
   * @throws IllegalArgumentException if from &lt; 0 or from &gt; to or to &gt; floats.length
   * @docgenVersion 9
   * @since 1.6
   */
  @Nonnull
  public static int[] copyOfRange(@Nonnull int[] floats, int from, int to) {
    return Arrays.copyOfRange(floats, from, to);
  }

  /**
   * Returns a new char array that is a copy of a range of the original char array, from the index "from", inclusive, to the index "to", exclusive.
   *
   * @param floats the original char array
   * @param from   the initial index of the range to be copied, inclusive
   * @param to     the final index of the range to be copied, exclusive
   * @return a copy of the specified range of the original char array
   * @throws NullPointerException      if floats is null
   * @throws IndexOutOfBoundsException if from &lt; 0 or to &gt; floats.length
   * @throws IllegalArgumentException  if from &gt; to
   * @docgenVersion 9
   */
  @Nonnull
  public static char[] copyOfRange(@Nonnull char[] floats, int from, int to) {
    return Arrays.copyOfRange(floats, from, to);
  }

  /**
   * Returns a copy of the specified array of bytes from {@code from}
   * (inclusive) to {@code to} (exclusive).
   *
   * @param floats the array of bytes to copy
   * @param from   the initial index of the range to copy, inclusive
   * @param to     the final index of the range to copy, exclusive
   * @return a new array of bytes containing the specified range from
   * the original array
   * @throws NullPointerException      if {@code floats} is null
   * @throws IndexOutOfBoundsException if {@code from < 0 || from > floats.length ||
   *                                   to < from || to > floats.length}
   * @docgenVersion 9
   * @since 1.6
   */
  @Nonnull
  public static byte[] copyOfRange(@Nonnull byte[] floats, int from, int to) {
    return Arrays.copyOfRange(floats, from, to);
  }
}
