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
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;

/**
 * This class is used to represent the standard system input, output, and error streams.
 *
 * @author Your Name
 * @version 1.0
 * @docgenVersion 9
 */
@RefIgnore
@SuppressWarnings("unused")
public final class RefSystem {

  public static final PrintStream err = System.err;
  public static PrintStream out = System.out;
  public static InputStream in = System.in;

  /**
   * Returns the system properties.
   * This is a convenience method for {@link System#getProperties()}.
   *
   * @return the system properties
   * @docgenVersion 9
   * @see System#getProperties()
   */
  public static Properties getProperties() {
    return System.getProperties();
  }

  /**
   * Sets the standard output stream to the given {@link PrintStream}.
   *
   * @param printStream the new standard output stream
   * @docgenVersion 9
   */
  public static void setOut(@RefAware PrintStream printStream) {
    out = printStream;
    System.setOut(printStream);
  }

  /**
   * Returns the current value of the most precise available system timer,
   * in nanoseconds.
   * <p>
   * This method can only be used to measure elapsed time and is not related to any other notion of system or wall-clock time.
   * The value returned represents nanoseconds since some fixed but arbitrary time (perhaps in the future, so values may be negative).
   * This method provides nanosecond precision, but not necessarily nanosecond accuracy.
   * No guarantees are made about the accuracy of this method.
   *
   * @return the current value of the system timer, in nanoseconds.
   * @docgenVersion 9
   */
  public static long nanoTime() {
    return System.nanoTime();
  }

  /**
   * Copies an array from the specified source array, beginning at the specified position,
   * to the specified position of the destination array.
   *
   * @param src    - the source array.
   * @param srcPos - starting position in the source array.
   * @param dst    - the destination array.
   * @param dstPos - starting position in the destination data.
   * @param length - the number of array elements to be copied.
   * @docgenVersion 9
   */
  public static void arraycopy(@Nonnull @RefAware Object src, int srcPos,
                               @Nonnull @RefAware Object dst, int dstPos, int length) {
    System.arraycopy(src, srcPos, dst, dstPos, length);
  }

  /**
   * Returns the current time in milliseconds.
   *
   * @return the current time in milliseconds
   * @docgenVersion 9
   */
  public static long currentTimeMillis() {
    return System.currentTimeMillis();
  }

  /**
   * Returns the same hash code for the given object as
   * {@link System#identityHashCode(Object)}, but also frees any references
   * held by the object.
   *
   * @param obj the object for which to calculate the hash code
   * @return the hash code
   * @docgenVersion 9
   */
  public static int identityHashCode(@RefAware Object obj) {
    final int hashCode = System.identityHashCode(obj);
    RefUtil.freeRef(obj);
    return hashCode;
  }

  /**
   * Sets the value of the specified system property.
   *
   * @param key   the name of the system property
   * @param value the value of the system property
   * @docgenVersion 9
   */
  public static void setProperty(@Nonnull @RefAware String key,
                                 @Nonnull @RefAware String value) {
    System.setProperty(key, value);
  }

  /**
   * Terminates the currently running Java Virtual Machine.
   *
   * @param code the exit status.
   * @docgenVersion 9
   */
  public static void exit(int code) {
    System.exit(code);
  }

  /**
   * Returns the value of the system property with the specified key.
   *
   * @param key the key of the system property
   * @return the value of the system property, or {@code null} if there is no property with that key
   * @docgenVersion 9
   */
  public static String getProperty(@Nonnull @RefAware String key) {
    return System.getProperty(key);
  }

  /**
   * Get the value of the system property with the specified key.
   *
   * @param key          the name of the system property
   * @param defaultValue the default value to use if the property is not defined
   * @return the value of the system property, or the default value if the property is not defined
   * @docgenVersion 9
   */
  public static String getProperty(@Nonnull @RefAware String key,
                                   @RefAware String defaultValue) {
    return System.getProperty(key, defaultValue);
  }

  /**
   * Runs the garbage collector.
   *
   * @docgenVersion 9
   */
  public static void gc() {
    System.gc();
  }

  /**
   * Returns the value of the environment variable with the given name.
   *
   * @param key the name of the environment variable
   * @return the value of the environment variable, or {@code null} if it doesn't exist
   * @docgenVersion 9
   */
  public static String getenv(@RefAware String key) {
    return System.getenv(key);
  }
}
