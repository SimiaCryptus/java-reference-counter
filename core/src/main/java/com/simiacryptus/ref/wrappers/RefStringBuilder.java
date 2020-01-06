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

@RefAware
@RefIgnore
@SuppressWarnings("unused")
public class RefStringBuilder implements Appendable, CharSequence, java.io.Serializable {
  private final StringBuilder inner;

  public RefStringBuilder() {
    inner = new StringBuilder();
  }

  public RefStringBuilder(@com.simiacryptus.ref.lang.RefAware CharSequence charSequence) {
    inner = new StringBuilder(charSequence);
  }

  @Override
  public RefStringBuilder append(@RefAware CharSequence csq) {
    inner.append(csq);
    return this;
  }

  @Override
  public RefStringBuilder append(@RefAware CharSequence csq, int start, int end) {
    inner.append(csq, start, end);
    return this;
  }

  public RefStringBuilder append(char[] csq, int start, int end) {
    inner.append(csq, start, end);
    return this;
  }

  @Override
  public RefStringBuilder append(char c) {
    inner.append(c);
    return this;
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

  public String substring(int from, int to) {
    return inner.substring(from, to);
  }

  public RefStringBuilder append(@RefAware Object obj) {
    inner.append(obj);
    return this;
  }

  public RefStringBuilder delete(int start, int end) {
    inner.delete(start, end);
    return this;
  }

  public RefStringBuilder reverse() {
    inner.reverse();
    return this;
  }
}