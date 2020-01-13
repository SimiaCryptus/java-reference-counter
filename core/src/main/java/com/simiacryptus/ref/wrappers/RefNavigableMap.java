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

import java.util.NavigableMap;

public interface RefNavigableMap<K, V> extends NavigableMap<K, V>, RefMap<K, V> {

  @Override
  RefNavigableMap<K, V> descendingMap();

  @Override
  RefNavigableSet<K> navigableKeySet();

  @Override
  RefNavigableSet<K> descendingKeySet();

  @Override
  RefNavigableMap<K, V> subMap(@RefAware K fromKey, boolean fromInclusive,
                               @RefAware K toKey, boolean toInclusive);

  @Override
  RefNavigableMap<K, V> headMap(@RefAware K toKey, boolean inclusive);

  @Override
  RefNavigableMap<K, V> tailMap(@RefAware K fromKey, boolean inclusive);

  @Override
  RefSortedMap<K, V> subMap(@RefAware K fromKey, @RefAware K toKey);

  @Override
  RefSortedMap<K, V> headMap(@RefAware K toKey);

  @Override
  RefSortedMap<K, V> tailMap(@RefAware K fromKey);
}
