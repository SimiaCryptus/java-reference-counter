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

  @Nonnull
  public static String format(@Nonnull @RefAware String format, @RefAware @Nonnull Object... args) {
    final String string = new Formatter().format(format, args).toString();
    RefUtil.freeRef(args);
    return string;
  }

  @Override
  public int length() {
    return inner.length();
  }

  @Override
  public char charAt(int index) {
    return inner.charAt(index);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return inner.subSequence(start, end);
  }

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
