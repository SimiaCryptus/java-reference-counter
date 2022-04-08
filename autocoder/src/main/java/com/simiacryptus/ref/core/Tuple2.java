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

package com.simiacryptus.ref.core;

import java.io.Serializable;

/**
 * A tuple of two elements.
 *
 * @param <A> the type of the first element
 * @param <B> the type of the second element
 * @docgenVersion 9
 */
@SuppressWarnings("unused")
public class Tuple2<A, B> implements Serializable {
  public final A _1;
  public final B _2;

  public Tuple2() {
    this(null, null);
  }

  public Tuple2(final A a, final B b) {
    _1 = a;
    _2 = b;
  }

  /**
   * Returns the first element in the list.
   *
   * @return the first element in the list
   * @docgenVersion 9
   */
  public A getFirst() {
    return _1;
  }

  /**
   * Returns the second element of the tuple.
   *
   * @return the second element of the tuple
   * @docgenVersion 9
   */
  public B getSecond() {
    return _2;
  }
}
