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
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.stream.Collector;

/**
 * This class is used to compute statistics for a stream of double values.
 *
 * @docgenVersion 9
 */
@RefIgnore
public class DoubleStatistics extends DoubleSummaryStatistics {

  @Nonnull
  @SuppressWarnings("unused")
  public static Collector<Double, DoubleStatistics, DoubleStatistics> COLLECTOR = Collector.of(() -> new DoubleStatistics(),
      (doubleStatistics, value) -> doubleStatistics.accept(value), (doubleStatistics, other) -> doubleStatistics.combine(other), d -> d);

  @Nonnull
  @SuppressWarnings("unused")
  public static Collector<Number, DoubleStatistics, DoubleStatistics> NUMBERS = Collector.of(() -> new DoubleStatistics(),
      (a, n) -> a.accept(n.doubleValue()), (doubleStatistics, other) -> doubleStatistics.combine(other), d -> d);

  private double simpleSumOfSquare; // Used to compute right sum for non-finite inputs
  private double sumOfSquare = 0.0d;
  private double sumOfSquareCompensation; // Low order bits of sum

  /**
   * Returns the standard deviation of the values that have been added to this
   * Statistic. Returns 0 if no values have been added.
   *
   * @docgenVersion 9
   */
  public final double getStandardDeviation() {
    return getCount() > 0 ? Math.sqrt(getSumOfSquare() / getCount() - Math.pow(getAverage(), 2)) : 0.0d;
  }

  /**
   * Returns the sum of squares.
   *
   * @return the sum of squares
   * @docgenVersion 9
   */
  public double getSumOfSquare() {
    final double tmp = sumOfSquare + sumOfSquareCompensation;
    if (Double.isNaN(tmp) && Double.isInfinite(simpleSumOfSquare)) {
      return simpleSumOfSquare;
    }
    return tmp;
  }

  /**
   * {@inheritDoc}
   * <p>
   * This implementation also computes the sum of squares for the values.
   *
   * @docgenVersion 9
   */
  @Override
  public synchronized void accept(final double value) {
    super.accept(value);
    final double squareValue = value * value;
    simpleSumOfSquare += squareValue;
    sumOfSquareWithCompensation(squareValue);
  }

  /**
   * @param value the value
   * @return the double statistics
   * @throws NullPointerException if the value is null
   * @docgenVersion 9
   */
  @Nonnull
  public DoubleStatistics accept(@Nonnull final double[] value) {
    Arrays.stream(value).forEach(value1 -> accept(value1));
    return this;
  }

  /**
   * Combines the statistics of another {@linkplain DoubleStatistics} into this one.
   *
   * @param other the other statistics to combine
   * @return this statistics, for chaining
   * @docgenVersion 9
   */
  @Nonnull
  public DoubleStatistics combine(@Nonnull final @RefAware DoubleStatistics other) {
    super.combine(other);
    simpleSumOfSquare += other.simpleSumOfSquare;
    sumOfSquareWithCompensation(other.sumOfSquare);
    sumOfSquareWithCompensation(other.sumOfSquareCompensation);
    return this;
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   * @docgenVersion 9
   */
  @Nonnull
  @Override
  public String toString() {
    return toString(1).toString();
  }

  /**
   * Returns a string representation of the statistics, scaled by the given factor.
   *
   * @param scale the scale factor
   * @return the string representation
   * @docgenVersion 9
   */
  public CharSequence toString(final double scale) {
    return String.format("%.4e +- %.4e [%.4e - %.4e] (%d#)", getAverage() * scale, getStandardDeviation() * scale,
        getMin() * scale, getMax() * scale, getCount());
  }

  /**
   * This method calculates the sum of squares with compensation.
   *
   * @param value the value to be used in the calculation
   * @docgenVersion 9
   */
  private void sumOfSquareWithCompensation(final double value) {
    final double tmp = value - sumOfSquareCompensation;
    final double velvel = sumOfSquare + tmp; // Little wolf of rounding error
    sumOfSquareCompensation = velvel - sumOfSquare - tmp;
    sumOfSquare = velvel;
  }
}
