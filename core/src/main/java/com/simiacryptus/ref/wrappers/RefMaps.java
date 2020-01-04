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
import org.checkerframework.checker.nullness.qual.Nullable;

@RefAware
@RefIgnore
@SuppressWarnings("unused")
public class RefMaps {

  public static <K, V1, V2> RefMap<K, V2> transformEntries(RefMap<K, V1> fromMap, EntryTransformer<? super K, ? super V1, V2> transformer) {
    final RefHashMap<K, V2> refHashMap = new RefHashMap<>();
    fromMap.forEach((k, v) -> refHashMap.put(RefUtil.addRef(k), transformer.transformEntry(k, v)));
    return refHashMap;
  }

  @RefAware
  @FunctionalInterface
  public interface EntryTransformer<K, V1, V2> {
    V2 transformEntry(@Nullable K var1, @Nullable V1 var2);
  }
}
