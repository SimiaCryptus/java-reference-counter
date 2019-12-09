/*
 * Copyright (c) 2019 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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

import java.util.Spliterator;
import java.util.stream.StreamSupport;

@RefAware
@RefIgnore
public class RefStreamSupport {
  public static <T> RefStream<T> stream(Spliterator<T> spliterator, boolean parallel) {
    if (spliterator instanceof RefSpliterator) {
      final RefSpliterator refSpliterator = (RefSpliterator) spliterator;
      final RefStream refStream = new RefStream<>(StreamSupport.stream(refSpliterator.getInner(), parallel));
      refSpliterator.freeRef();
      return refStream;
    } else {
      return new RefStream<>(StreamSupport.stream(spliterator, parallel));
    }
  }
}
