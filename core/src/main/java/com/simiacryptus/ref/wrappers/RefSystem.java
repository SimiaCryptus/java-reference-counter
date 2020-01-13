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

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;

@RefIgnore
@SuppressWarnings("unused")
public final class RefSystem {

  public static final PrintStream err = System.err;
  public static PrintStream out = System.out;
  public static InputStream in = System.in;

  public static Properties getProperties() {
    return System.getProperties();
  }

  public static void setOut(@com.simiacryptus.ref.lang.RefAware PrintStream printStream) {
    out = printStream;
    System.setOut(printStream);
  }

  public static long nanoTime() {
    return System.nanoTime();
  }

  public static void arraycopy(@com.simiacryptus.ref.lang.RefAware Object src, int srcPos,
                               @com.simiacryptus.ref.lang.RefAware Object dst, int dstPos, int length) {
    System.arraycopy(src, srcPos, dst, dstPos, length);
  }

  public static long currentTimeMillis() {
    return System.currentTimeMillis();
  }

  public static int identityHashCode(@RefAware Object obj) {
    final int hashCode = System.identityHashCode(obj);
    RefUtil.freeRef(obj);
    return hashCode;
  }

  public static void setProperty(@com.simiacryptus.ref.lang.RefAware String key,
                                 @com.simiacryptus.ref.lang.RefAware String value) {
    System.setProperty(key, value);
  }

  public static void exit(int code) {
    System.exit(code);
  }

  public static String getProperty(@com.simiacryptus.ref.lang.RefAware String key) {
    return System.getProperty(key);
  }

  public static String getProperty(@com.simiacryptus.ref.lang.RefAware String key,
                                   @com.simiacryptus.ref.lang.RefAware String defaultValue) {
    return System.getProperty(key, defaultValue);
  }

  public static void gc() {
    System.gc();
  }

  public static String getenv(@com.simiacryptus.ref.lang.RefAware String key) {
    return System.getenv(key);
  }
}
