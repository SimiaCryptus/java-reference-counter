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
import java.io.Serializable;

/**
 * A class that represents a string builder.
 *
 * @param inner The inner string builder.
 * @author Your Name
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public class RefStringBuilder implements Appendable, CharSequence, Serializable {
  @Nonnull
  private final StringBuilder inner;

  public RefStringBuilder() {
    inner = new StringBuilder();
  }

  public RefStringBuilder(@Nonnull @RefAware CharSequence charSequence) {
    inner = new StringBuilder(charSequence);
  }

  /**
   * Appends the specified character sequence to this sequence.
   *
   * @param csq the character sequence to append.
   * @return a reference to this object.
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefStringBuilder append(@RefAware CharSequence csq) {
    inner.append(csq);
    RefUtil.freeRef(csq);
    return this;
  }

  /**
   * Appends a subsequence of the specified {@linkplain CharSequence character
   * sequence} to this sequence.
   * <p>
   * The subsequence appended starts at the specified {@code start} and
   * extends to the character at index {@code end - 1}. Thus the length of the
   * substring is {@code end-start}. Characters in the subsequence are appended,
   * in order, to the contents of this sequence. If {@code csq} is
   * {@code null}, then the four characters {@code "null"} are appended to
   * this sequence.
   *
   * @param csq   the character sequence from which a subsequence will be
   *              appended.  If {@code csq} is {@code null}, the subsequence
   *              appended is the four characters {@code "null"}.
   * @param start the index of the first character in the subsequence
   * @param end   the index of the character following the last character in the
   *              subsequence
   * @return a reference to this object
   * @throws IndexOutOfBoundsException if {@code start} or {@code end} are negative,
   *                                   or {@code start} is greater than {@code end} or {@code end} is greater than
   *                                   {@code csq.length()}
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefStringBuilder append(@RefAware CharSequence csq, int start, int end) {
    inner.append(csq, start, end);
    RefUtil.freeRef(csq);
    return this;
  }

  /**
   * Appends the contents of the specified character array to this sequence.
   *
   * @param csq   the character array to append.
   * @param start the starting index of the characters to append.
   * @param end   the index of the last character to append.
   * @return a reference to this object.
   * @throws IndexOutOfBoundsException if {@code start} or {@code end} are negative, or {@code start} is greater than {@code end} or {@code end} is greater than {@code csq.length}
   * @docgenVersion 9
   */
  @Nonnull
  public RefStringBuilder append(char[] csq, int start, int end) {
    inner.append(csq, start, end);
    return this;
  }

  /**
   * Appends the specified character to this sequence.
   *
   * @param c the character to append
   * @return a reference to this object
   * @throws NullPointerException if c is null
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public RefStringBuilder append(char c) {
    inner.append(c);
    return this;
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
   * Returns a new string that is a substring of this string. The
   * substring begins at the specified beginIndex and extends to the
   * character at index endIndex - 1. Thus, the length of the
   * substring is endIndex-beginIndex.
   *
   * @param beginIndex the beginning index, inclusive.
   * @param endIndex   the ending index, exclusive.
   * @return the specified substring.
   * @throws IndexOutOfBoundsException if the
   *                                   beginIndex is negative, or
   *                                   endIndex is larger than the length of
   *                                   this String object, or
   *                                   beginIndex is larger than
   *                                   endIndex.
   * @docgenVersion 9
   */
  public String substring(int from, int to) {
    return inner.substring(from, to);
  }

  /**
   * Appends the specified object to this {@code RefStringBuilder}.
   * <p>
   * The overall effect is exactly as if the second argument were converted
   * to a string by the method {@link String#valueOf(Object)}, and the
   * characters of that string were then {@link #append(String) appended}
   * to this character sequence.
   *
   * @param obj the object to append.
   * @return a reference to this object.
   * @docgenVersion 9
   * @see String#valueOf(Object)
   */
  @Nonnull
  public RefStringBuilder append(@RefAware Object obj) {
    inner.append(obj);
    RefUtil.freeRef(obj);
    return this;
  }

  /**
   * Deletes a specified range of characters from the {@link RefStringBuilder} and returns the resulting string.
   *
   * @param start the starting index (inclusive)
   * @param end   the ending index (exclusive)
   * @return the resulting string
   * @throws StringIndexOutOfBoundsException if start or end is out of bounds
   * @docgenVersion 9
   */
  @Nonnull
  public RefStringBuilder delete(int start, int end) {
    inner.delete(start, end);
    return this;
  }

  /**
   * Reverses the characters in this {@code RefStringBuilder} and returns a reference to this object.
   *
   * @return a reference to this {@code RefStringBuilder} object
   * @docgenVersion 9
   */
  @Nonnull
  public RefStringBuilder reverse() {
    inner.reverse();
    return this;
  }

  /**
   * @return the string representation of the inner object
   * @docgenVersion 9
   */
  @Override
  public String toString() {
    return inner.toString();
  }
}
