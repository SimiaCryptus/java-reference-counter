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
import javax.annotation.Nullable;

/**
 * The RefAssert class provides assertions for reference types.
 *
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public class RefAssert {

  /**
   * Asserts that two objects are equal. If they are not,
   * an {@link AssertionError} is thrown with the given message.
   *
   * @param expected the expected object
   * @param actual   the actual object
   * @docgenVersion 9
   */
  public static void assertEquals(@RefAware Object expected, @RefAware Object actual) {
    assertEquals(null, expected, actual);
  }

  /**
   * Asserts that two objects are equal. If they are not,
   * an {@link AssertionError} is thrown with the given message.
   *
   * @param message  the message to be displayed if the assertion fails
   * @param expected the expected value
   * @param actual   the actual value
   * @docgenVersion 9
   */
  public static void assertEquals(@Nullable @RefAware String message, @RefAware Object expected,
                                  @RefAware Object actual) {
    try {
      if (!equalsRegardingNull(RefUtil.addRef(expected), RefUtil.addRef(actual))) {
        if (expected instanceof String && actual instanceof String) {
          String cleanMessage = message == null ? "" : message;
          throw new ComparisonFailure(cleanMessage, (String) expected, (String) actual);
        } else {
          failNotEquals(message, expected, actual);
        }
      }
    } finally {
      RefUtil.freeRef(expected);
      RefUtil.freeRef(actual);
    }
  }

  /**
   * Asserts that two longs are equal. If they are not,
   * an AssertionError, with the given message, is thrown.
   *
   * @param message  the identifying message for the AssertionError (optional)
   * @param expected expected long value
   * @param actual   actual long value
   * @throws AssertionError if the two longs are not equal
   * @docgenVersion 9
   */
  public static void assertEquals(@Nullable @RefAware String message, long expected, long actual) {
    if (expected != actual) {
      String cleanMessage = message == null ? "" : message;
      throw new ComparisonFailure(cleanMessage, Long.toString(expected), Long.toString(actual));
    }
  }

  /**
   * Asserts that two integers are equal. If they are not,
   * an AssertionError, with the given message, is thrown.
   *
   * @param message  the identifying message for the AssertionError (optional)
   * @param expected the expected value
   * @param actual   the actual value
   * @throws AssertionError if the two integers are not equal
   * @docgenVersion 9
   */
  public static void assertEquals(@Nullable @RefAware String message, int expected, int actual) {
    if (expected != actual) {
      String cleanMessage = message == null ? "" : message;
      throw new ComparisonFailure(cleanMessage, Integer.toString(expected), Integer.toString(actual));
    }
  }

  /**
   * Fails a test with the given message.
   *
   * @param message the message to include in the failure
   * @docgenVersion 9
   */
  public static void fail(@Nullable @RefAware String message) {
    if (message == null) {
      throw new AssertionError();
    } else {
      throw new AssertionError(message);
    }
  }

  /**
   * Asserts that two int arrays are equal. If they are not,
   * an AssertionError is thrown.
   *
   * @param expected the expected array
   * @param actuals  the actual array
   * @docgenVersion 9
   */
  public static void assertArrayEquals(@Nonnull int[] expected, @Nonnull int[] actuals) {
    assertEquals(expected.length, actuals.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], actuals[i]);
    }
  }

  /**
   * Asserts that two int arrays are equal. If they are not,
   * an AssertionError, with the given message, is thrown.
   *
   * @param message  the identifying message for the AssertionError (optional)
   * @param expected the expected array
   * @param actuals  the actual array
   * @docgenVersion 9
   */
  public static void assertArrayEquals(@RefAware String message, @Nonnull int[] expected, @Nonnull int[] actuals) {
    assertEquals(message, expected.length, actuals.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(message, expected[i], actuals[i]);
    }
  }

  /**
   * Determines whether two objects are equal, taking into account that one or both
   * objects may be null.
   *
   * @param expected the expected object
   * @param actual   the actual object
   * @return true if the objects are equal, false otherwise
   * @docgenVersion 9
   */
  private static boolean equalsRegardingNull(@Nullable @RefAware Object expected, @Nullable @RefAware Object actual) {
    if (expected == null) {
      final boolean b = actual == null;
      RefUtil.freeRef(actual);
      return b;
    } else {
      return isEquals(expected, actual);
    }
  }

  /**
   * Determines if two objects are equal.
   *
   * @param expected The expected object.
   * @param actual   The actual object.
   * @return True if the objects are equal, false otherwise.
   * @docgenVersion 9
   */
  private static boolean isEquals(@Nonnull @RefAware Object expected, @RefAware Object actual) {
    final boolean equals = expected.equals(actual);
    RefUtil.freeRef(equals);
    return equals;
  }

  /**
   * Fails a test with the given message if the two objects are not equal.
   * If <code>expected</code> and <code>actual</code> are <code>null</code>,
   * they are considered equal.
   *
   * @param message  the identifying message for the {@link AssertionError} (<code>null</code>
   *                 okay)
   * @param expected expected value
   * @param actual   actual value
   * @docgenVersion 9
   */
  private static void failNotEquals(@RefAware String message, @RefAware Object expected, @RefAware Object actual) {
    fail(format(message, expected, actual));
  }

  /**
   * Formats a message with the expected and actual values.
   *
   * @param message  the message to format
   * @param expected the expected value
   * @param actual   the actual value
   * @return the formatted message
   * @throws NullPointerException if the message is null
   * @docgenVersion 9
   */
  @Nonnull
  private static String format(@Nullable @RefAware String message, @RefAware Object expected, @RefAware Object actual) {
    String formatted = "";
    if (message != null && !"".equals(message)) {
      formatted = message + " ";
    }

    String expectedString = String.valueOf(expected);
    String actualString = String.valueOf(actual);
    return equalsRegardingNull(expectedString, actualString)
        ? formatted + "expected: " + formatClassAndValue(expected, expectedString) + " but was: "
        + formatClassAndValue(actual, actualString)
        : formatted + "expected:<" + expectedString + "> but was:<" + actualString + ">";
  }

  /**
   * Formats a class and value for display.
   *
   * @param value       the value to format
   * @param valueString the string representation of the value
   * @return the formatted class and value
   * @docgenVersion 9
   */
  @Nonnull
  private static String formatClassAndValue(@Nullable @RefAware Object value, @RefAware String valueString) {
    String className = value == null ? "null" : value.getClass().getName();
    return className + "<" + valueString + ">";
  }

  /**
   * A {@link ComparisonFailure} is thrown when an assertion fails.
   *
   * @param expected the expected value
   * @param actual   the actual value
   * @docgenVersion 9
   */
  @RefIgnore
  private static class ComparisonFailure extends AssertionError {
    private final String expected;
    private final String actual;

    public ComparisonFailure(@RefAware String message, @RefAware String expected, @RefAware String actual) {
      super(message);
      this.expected = expected;
      this.actual = actual;
    }

    /**
     * Returns the actual value of the String.
     *
     * @return the actual value of the String
     * @docgenVersion 9
     */
    public String getActual() {
      return actual;
    }

    /**
     * Returns the expected value.
     *
     * @return the expected value
     * @docgenVersion 9
     */
    public String getExpected() {
      return expected;
    }
  }
}
