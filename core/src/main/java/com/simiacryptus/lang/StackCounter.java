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

package com.simiacryptus.lang;

import com.simiacryptus.ref.lang.RefAware;
import com.simiacryptus.ref.lang.RefIgnore;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Class that counts stack frames.
 *
 * @docgenVersion 9
 */
@RefIgnore
public class StackCounter {

  @Nonnull
  Map<StackFrame, DoubleStatistics> stats = new ConcurrentHashMap<>();

  /**
   * @param left  the left stack counter
   * @param right the right stack counter
   * @param fn    the function to apply
   * @return the string representation
   * @docgenVersion 9
   */
  public static String toString(@Nonnull final @RefAware StackCounter left,
                                @Nonnull final @RefAware StackCounter right,
                                @Nonnull final @RefAware BiFunction<DoubleStatistics, DoubleStatistics, Number> fn) {
    Comparator<StackFrame> comparing = Comparator.comparing(key -> {
      return -fn.apply(left.stats.get(key), right.stats.get(key)).doubleValue();
    });
    comparing = comparing.thenComparing(key -> key.toString());
    return Stream.concat(left.stats.keySet().stream(), right.stats.keySet().stream()).distinct()
        .filter(k -> left.stats.containsKey(k) && right.stats.containsKey(k)).sorted(comparing)
        .map(key -> String.format("%s - %s", key.toString(), fn.apply(left.stats.get(key), right.stats.get(key))))
        .limit(100).reduce((a, b) -> a + "\n" + b).orElse("");
  }

  /**
   * Increments the value of the specified {@code long} length by one.
   *
   * @param length the {@code long} length to increment
   * @docgenVersion 9
   */
  public void increment(final long length) {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    for (@Nonnull final StackTraceElement frame : stackTrace) {
      stats.computeIfAbsent(new StackFrame(frame), f -> new DoubleStatistics()).accept(length);
    }
  }

  /**
   * @Override public String toString() {
   * return toString(value -> summaryStat(value));
   * }
   * @docgenVersion 9
   */
  @Override
  public String toString() {
    return toString(value -> summaryStat(value));
  }

  /**
   * Returns a string representation of the value returned by the given function applied to the statistics.
   *
   * @param fn the function to apply to the statistics
   * @return a string representation of the value returned by the given function applied to the statistics
   * @throws NullPointerException if fn is null
   * @docgenVersion 9
   */
  public String toString(@Nonnull final @RefAware Function<DoubleStatistics, Number> fn) {
    Comparator<Map.Entry<StackFrame, DoubleStatistics>> comparing = Comparator
        .comparing(e -> -fn.apply(e.getValue()).doubleValue());
    comparing = comparing.thenComparing(Comparator.comparing(e -> e.getKey().toString()));
    return stats.entrySet().stream().sorted(comparing)
        .map(e -> String.format("%s - %s", e.getKey().toString(), fn.apply(e.getValue()))).limit(100)
        .reduce((a, b) -> a + "\n" + b).orElse(super.toString());
  }

  /**
   * @param other the other stack counter
   * @param fn    the function to apply
   * @return the string representation
   * @docgenVersion 9
   */
  @SuppressWarnings("unused")
  public CharSequence toString(@Nonnull final @RefAware StackCounter other,
                               @Nonnull final @RefAware BiFunction<DoubleStatistics, DoubleStatistics, Number> fn) {
    return StackCounter.toString(this, other, fn);
  }

  /**
   * @param value the value to use
   * @return the summary statistic
   * @throws NullPointerException if value is null
   * @docgenVersion 9
   */
  @Nonnull
  protected Number summaryStat(@Nonnull final @RefAware DoubleStatistics value) {
    return (int) value.getSum();
  }

  /**
   * A stack frame in a Java stack trace.
   *
   * @docgenVersion 9
   */
  @RefIgnore
  public static class StackFrame {
    public final String declaringClass;
    public final String fileName;
    public final int lineNumber;
    public final String methodName;

    public StackFrame(@Nonnull final @RefAware StackTraceElement frame) {
      this(frame.getClassName(), frame.getMethodName(), frame.getFileName(), frame.getLineNumber());
    }

    public StackFrame(final @RefAware String declaringClass,
                      final @RefAware String methodName,
                      final @RefAware String fileName, final int lineNumber) {
      this.declaringClass = declaringClass;
      this.methodName = methodName;
      this.fileName = fileName;
      this.lineNumber = lineNumber;
    }

    /**
     * @param o the object to compare this instance with
     * @return true if the specified object is equal to this instance
     * @docgenVersion 9
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final @RefAware Object o) {
      if (this == o)
        return true;
      if (!(o instanceof StackFrame))
        return false;

      @Nonnull final StackFrame that = (StackFrame) o;

      if (lineNumber != that.lineNumber)
        return false;
      if (declaringClass != null ? !declaringClass.equals(that.declaringClass) : that.declaringClass != null) {
        return false;
      }
      if (methodName != null ? !methodName.equals(that.methodName) : that.methodName != null)
        return false;
      return fileName != null ? fileName.equals(that.fileName) : that.fileName == null;
    }

    /**
     * @Override public int hashCode();
     * @docgenVersion 9
     */
    @Override
    public int hashCode() {
      int result = declaringClass != null ? declaringClass.hashCode() : 0;
      result = 31 * result + (methodName != null ? methodName.hashCode() : 0);
      result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
      result = 31 * result + lineNumber;
      return result;
    }

    /**
     * @Override public String toString() {
     * return String.format("%s.%s(%s:%s)", declaringClass, methodName, fileName, lineNumber);
     * }
     * @docgenVersion 9
     */
    @Override
    public String toString() {
      return String.format("%s.%s(%s:%s)", declaringClass, methodName, fileName, lineNumber);
    }
  }
}
