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

/**
 * The type Ref arrays.
 */
@RefAware
@RefIgnore
public class RefAssert {

  public static void assertEquals(Object expected, Object actual) {
    assertEquals(expected, actual, null);
  }

  public static void assertEquals(Object expected, Object actual, String message) {
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

  public static void fail(String message) {
    if (message == null) {
      throw new AssertionError();
    } else {
      throw new AssertionError(message);
    }
  }

  public static void assertArrayEquals(int[] expected, int[] actuals) {
    assertEquals(expected.length, actuals.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], actuals[i]);
    }
  }

  public static void assertArrayEquals(String message, int[] expected, int[] actuals) {
    assertEquals(expected.length, actuals.length, message);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], actuals[i], message);
    }
  }

  private static boolean equalsRegardingNull(Object expected, Object actual) {
    if (expected == null) {
      final boolean b = actual == null;
      RefUtil.freeRef(actual);
      return b;
    } else {
      return isEquals(expected, actual);
    }
  }

  private static boolean isEquals(Object expected, Object actual) {
    final boolean equals = expected.equals(actual);
    RefUtil.freeRef(equals);
    return equals;
  }

  private static void failNotEquals(String message, Object expected, Object actual) {
    fail(format(message, expected, actual));
  }

  private static String format(String message, Object expected, Object actual) {
    String formatted = "";
    if (message != null && !"".equals(message)) {
      formatted = message + " ";
    }

    String expectedString = String.valueOf(expected);
    String actualString = String.valueOf(actual);
    return equalsRegardingNull(expectedString, actualString) ? formatted + "expected: " + formatClassAndValue(expected, expectedString) + " but was: " + formatClassAndValue(actual, actualString) : formatted + "expected:<" + expectedString + "> but was:<" + actualString + ">";
  }

  private static String formatClassAndValue(Object value, String valueString) {
    String className = value == null ? "null" : value.getClass().getName();
    return className + "<" + valueString + ">";
  }

  @RefAware
  @RefIgnore
  private static class ComparisonFailure extends AssertionError {
    private final String expected;
    private final String actual;

    public ComparisonFailure(String message, String expected, String actual) {
      super(message);
      this.expected = expected;
      this.actual = actual;
    }

    public String getActual() {
      return actual;
    }

    public String getExpected() {
      return expected;
    }
  }
}
