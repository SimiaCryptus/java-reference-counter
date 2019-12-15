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
import com.simiacryptus.ref.lang.ReferenceCounting;

import java.util.Iterator;
import java.util.Spliterators;

@RefAware
public class RefSpliterators {
  public static <T> RefSpliterator<T> spliterator(Iterator<T> iterator, int size, int characteristics) {
    if (iterator instanceof RefIterator) {
      Iterator<T> inner = ((RefIterator<T>) iterator).getInner();
      assert null != inner;
      return new RefSpliterator<>(Spliterators.spliterator(inner, size, characteristics)).track((ReferenceCounting) iterator);
    } else {
      RefSpliterator<T> refSpliterator = new RefSpliterator<>(Spliterators.spliterator(iterator, size, characteristics));
      if (iterator instanceof ReferenceCounting) {
        refSpliterator.track((ReferenceCounting) iterator);
      }
      return refSpliterator;
    }
  }
}
