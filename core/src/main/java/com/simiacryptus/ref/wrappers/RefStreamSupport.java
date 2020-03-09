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
import com.simiacryptus.ref.lang.ReferenceCounting;

import java.util.ArrayList;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

@RefIgnore
public class RefStreamSupport {
  public static <T> RefStream<T> stream(@RefAware Spliterator<T> spliterator,
                                        boolean parallel) {
    if (spliterator instanceof RefSpliterator) {
      final RefSpliterator refSpliterator = (RefSpliterator) spliterator;
      ConcurrentHashMap<RefStream.IdentityWrapper<ReferenceCounting>, AtomicInteger> refs = new ConcurrentHashMap<>();
      return new RefStream<>(
          StreamSupport.stream(refSpliterator.getInner(), parallel)
              .peek(u -> RefStream.storeRef(RefUtil.addRef(u), refs)),
          new ArrayList<ReferenceCounting>(),
          refs
      ).onClose(() -> refSpliterator.freeRef());
    } else {
      final RefStream<T> refStream = new RefStream<>(StreamSupport.stream(spliterator, parallel));
      return refStream.peek(u -> refStream.storeRef(u));
    }
  }
}
