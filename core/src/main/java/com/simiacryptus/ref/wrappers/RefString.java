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
import java.io.Serializable;
import java.util.Formatter;

/**
 * A class that represents a reference to a string.
 *
 * @author <a href="mailto:example@example.com">Example Author</a>
 * @version 1.0
 * @docgenVersion 9
 * @since 1.0
 */
@RefIgnore
@SuppressWarnings("unused")
public final class RefString extends ReferenceCountingBase
    implements Serializable, Comparable<CharSequence>, CharSequence {

  @Nonnull
  private final CharSequence inner;

  public RefString() {
    inner = new String();
  }

  public RefString(@Nonnull @RefAware String original) {
    inner = new String(original);
  }

  public RefString(@Nonnull char value[]) {
    inner = new String(value);
  }

  public RefString(@Nonnull char value[], int offset, int count) {
    inner = new String(value, offset, count);
  }

  public RefString(@Nonnull int[] codePoints, int offset, int count) {
    inner = new String(codePoints, offset, count);
  }

  @Deprecated
  public RefString(@Nonnull byte ascii[], int hibyte, int offset, int count) {
    inner = new String(ascii, hibyte, offset, count);
  }

  @Deprecated
  public RefString(@Nonnull byte ascii[], int hibyte) {
    inner = new String(ascii, hibyte);
  }

  /**
   * @param format the format string
   * @param args   the format arguments
   * @return the formatted string
   * @docgenVersion 9
   */
  @Nonnull
  public static String format(@Nonnull @RefAware String format, @RefAware @Nonnull Object... args) {
    final String string = new Formatter().format(format, args).toString();
    RefUtil.freeRef(args);
    return string;
  }

  /**
   * @Override public int length() {
   * return inner.length();
   * }
   * @docgenVersion 9
   */
  @Override
  public int length() {
    return inner.length();
  }

  /**
   * Returns the character at the specified index.
   *
   * @param index the index of the character to return
   * @return the character at the specified index
   * @docgenVersion 9
   */
  @Override
  public char charAt(int index) {
    return inner.charAt(index);
  }

  /**
   * Returns a new character sequence that is a subsequence of this sequence.
   *
   * @param start the start index, inclusive
   * @param end   the end index, exclusive
   * @return the specified subsequence
   * @throws IndexOutOfBoundsException if {@code start} or {@code end} are negative,
   *                                   if {@code end} is greater than {@code length()},
   *                                   or if {@code start} is greater than {@code end}
   * @docgenVersion 9
   */
  @Override
  public CharSequence subSequence(int start, int end) {
    return inner.subSequence(start, end);
  }

  /**
   * Compares this {@code CharSequence} to another {@code CharSequence}, returning an integer
   * indicating whether this {@code CharSequence} is less than, equal to, or greater than the
   * specified {@code CharSequence}.
   *
   * <p>A {@code CharSequence} is less than another {@code CharSequence} if it compares lexicographically
   * before the other sequence. A {@code CharSequence} is equal to another {@code CharSequence} if it
   * compares lexicographically equal to the other sequence. A {@code CharSequence} is greater than
   * another {@code CharSequence} if it compares lexicographically after the other sequence.
   *
   * @param other the {@code CharSequence} to be compared.
   * @return the value {@code 0} if the argument {@code CharSequence} is equal to this
   * {@code CharSequence}; a value less than {@code 0} if this {@code CharSequence}
   * is lexicographically less than the {@code CharSequence} argument; and a value
   * greater than {@code 0} if this {@code CharSequence} is lexicographically greater
   * than the {@code CharSequence} argument.
   * @docgenVersion 9
   */
  @Override
  public int compareTo(@Nonnull @RefAware CharSequence other) {
    int len1 = inner.length();
    int len2 = other.length();
    int lim = Math.min(len1, len2);
    int k = 0;
    while (k < lim) {
      char c1 = inner.charAt(k);
      char c2 = other.charAt(k);
      if (c1 != c2) {
        return c1 - c2;
      }
      k++;
    }
    return len1 - len2;
  }
}
